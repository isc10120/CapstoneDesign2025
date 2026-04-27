package jamgaVOCA.demo.service

import jamgaVOCA.demo.domain.battle.Battle
import jamgaVOCA.demo.domain.battle.BattleEffect
import jamgaVOCA.demo.domain.battle.BattleEffectRepository
import jamgaVOCA.demo.domain.battle.BattleRepository
import jamgaVOCA.demo.domain.battle.BattleResult
import jamgaVOCA.demo.domain.battle.EffectType
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.user.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate

@Service
@Transactional
class BattleService(
    private val battleRepository: BattleRepository,
    private val battleEffectRepository: BattleEffectRepository,
    private val userRepository: UserRepository
) {

    // ===== 주간 정산 + 매칭 =====

    @Scheduled(cron = "0 0 0 * * MON")  // 매주 월요일 00:00
    fun settleAndMatch() {
        settleWeeklyBattles()
        createWeeklyMatches()
    }

    private fun settleWeeklyBattles() {
        val lastWeekStart = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1)
        val battles = battleRepository.findAllByWeekStart(lastWeekStart)
            .filter { it.result == null }

        battles.forEach { battle ->
            battle.result = when {
                battle.userADamage > battle.userBDamage -> BattleResult.WIN_A
                battle.userBDamage > battle.userADamage -> BattleResult.WIN_B
                else -> BattleResult.DRAW
            }
        }
    }

    private fun createWeeklyMatches() {
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        val dummyUser = getDummyUser()

        // 더미 유저 제외한 실제 유저만 셔플
        val users = userRepository.findAll()
            .filter { !it.isDummy }
            .shuffled()

        users.chunked(2).forEach { pair ->
            val userA = pair[0]
            val userB = if (pair.size == 2) pair[1] else dummyUser

            battleRepository.save(
                Battle(
                    userA = userA,
                    userB = userB,
                    weekStart = weekStart,
                    weekEnd = weekEnd
                )
            )
        }
    }

    // ===== 신규 유저 더미 매칭 =====

    fun matchNewUserWithDummy(user: User) {
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)

        // 이미 이번 주 배틀이 있으면 스킵
        if (findBattleByUser(user, weekStart) != null) return

        battleRepository.save(
            Battle(
                userA = user,
                userB = getDummyUser(),
                weekStart = weekStart,
                weekEnd = weekEnd
            )
        )
    }

    // ===== 배틀 조회 =====

    @Transactional(readOnly = true)
    fun getCurrentBattle(userId: Long): Battle {
        val user = getUser(userId)
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        return findBattleByUser(user, weekStart)
            ?: throw IllegalStateException("진행 중인 배틀이 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getLatestUncheckedResult(userId: Long): Battle? {
        val user = getUser(userId)
        return battleRepository
            .findTopByUserAAndResultNotNullAndResultCheckedAFalseOrderByWeekEndDesc(user)
            ?: battleRepository
                .findTopByUserBAndResultNotNullAndResultCheckedBFalseOrderByWeekEndDesc(user)
    }

    @Transactional(readOnly = true)
    fun getBattleHistory(userId: Long): List<Battle> {
        val user = getUser(userId)
        return battleRepository.findAllByUserAOrUserBOrderByWeekStartDesc(user, user)
    }

    fun confirmResult(userId: Long) {
        val user = getUser(userId)

        val uncheckedA = battleRepository.findAllByUserAAndResultNotNullAndResultCheckedAFalse(user)
        val uncheckedB = battleRepository.findAllByUserBAndResultNotNullAndResultCheckedBFalse(user)

        uncheckedA.forEach { it.resultCheckedA = true }
        uncheckedB.forEach { it.resultCheckedB = true }
    }

    // ===== 스킬 효과 적용 =====

    fun applySkillEffect(
        battle: Battle,
        attackerUserId: Long,
        damage: Int,
        effectType: EffectType?,
        effectTurns: Int?,
        shieldCount: Int
    ): Boolean {  // 쉴드 막혔는지 여부 반환
        val targetUserId = getOpponentId(battle, attackerUserId)

        // 쉴드 처리
        if (battle.isUserA(targetUserId)) {
            if (battle.userAShield > 0) {
                battle.userAShield--
                return true  // 쉴드 막힘
            }
            battle.userADamage += damage
        } else {
            if (battle.userBShield > 0) {
                battle.userBShield--
                return true
            }
            battle.userBDamage += damage
        }

        // 상태이상 적용
        if (effectType != null && effectTurns != null) {
            battleEffectRepository.save(
                BattleEffect(
                    battle = battle,
                    targetUser = getUser(targetUserId),
                    effectType = effectType,
                    remainingTurns = effectTurns
                )
            )
        }

        // 쉴드 버프 적용 (본인에게)
        if (shieldCount > 0) {
            if (battle.isUserA(attackerUserId)) battle.userAShield += shieldCount
            else battle.userBShield += shieldCount
        }

        return false  // 쉴드 안 막힘
    }

    fun tickEffects(battle: Battle, userId: Long) {
        val effects = battle.effectsOf(userId)
        effects.forEach { it.remainingTurns-- }
        battleEffectRepository.deleteAll(effects.filter { it.remainingTurns <= 0 })
    }

    fun cleanse(battle: Battle, userId: Long) {
        battleEffectRepository.deleteAll(battle.effectsOf(userId))
    }

    // ===== 헬퍼 =====

    private fun findBattleByUser(user: User, weekStart: LocalDate): Battle? =
        battleRepository.findByUserAAndWeekStart(user, weekStart)
            ?: battleRepository.findByUserBAndWeekStart(user, weekStart)

    private fun getOpponentId(battle: Battle, userId: Long): Long =
        if (battle.isUserA(userId)) battle.userB.id!!
        else battle.userA.id!!

    private fun getDummyUser(): User =
        userRepository.findByIsDummyTrue()
            ?: throw IllegalStateException("더미 유저가 존재하지 않습니다.")

    private fun getUser(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 유저입니다.") }
}