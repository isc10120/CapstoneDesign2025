package jamgaVOCA.demo.service

import jamgaVOCA.demo.domain.battle.Battle
import jamgaVOCA.demo.domain.battle.BattleEffect
import jamgaVOCA.demo.domain.battle.BattleEffectRepository
import jamgaVOCA.demo.domain.battle.BattleRepository
import jamgaVOCA.demo.domain.battle.BattleResult
import jamgaVOCA.demo.domain.battle.EffectType
import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.skill.SkillType
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.user.UserRepository
import jamgaVOCA.demo.service.dto.SkillApplyResult
import jamgaVOCA.demo.service.dto.StatusAppliedInfo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

@Service
@Transactional
class BattleService(
    private val battleRepository: BattleRepository,
    private val battleEffectRepository: BattleEffectRepository,
    private val userRepository: UserRepository
) {
    companion object {
        const val DAMAGE_BUFF_RATE_PER_STACK = 0.5
        const val POISON_DAMAGE = 20
    }

    // ===== 주간 정산 + 매칭 =====

    @Scheduled(cron = "0 0 0 * * MON")  // 매주 월요일 00:00
    fun settleAndMatch() {
        settleWeeklyBattles()
        createWeeklyMatches()
    }

    private fun settleWeeklyBattles() {
        battleRepository.findAllByResultIsNull().forEach { battle ->
            battle.result = when {
                battle.userAScore > battle.userBScore -> BattleResult.WIN_A
                battle.userBScore > battle.userAScore -> BattleResult.WIN_B
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

    fun applySkill(battle: Battle, attackerId: Long, skill: Skill): SkillApplyResult {
        val opponentId = getOpponentId(battle, attackerId)

        // PARALYZE 체크 - 스택 수만큼 독립시행
        val paralyzeStacks = battle.effectsOf(attackerId)
            .count { it.effectType == EffectType.PARALYZE }
        val isParalyzed = (1..paralyzeStacks).any { Random.nextFloat() < 0.3f }
        if (isParalyzed) {
            tickEffects(battle, attackerId)
            return SkillApplyResult(paralyzed = true)
        }

        // DAMAGE_BUFF 체크 - 합연산
        val buffStacks = battle.effectsOf(attackerId)
            .count { it.effectType == EffectType.DAMAGE_BUFF }
        val finalDamage = if (buffStacks > 0) {
            (skill.damage * (1.0 + buffStacks * DAMAGE_BUFF_RATE_PER_STACK)).toInt()
        } else {
            skill.damage
        }

        // 독 데미지 계산 (턴 소모 전에)
        val poisonDamageTaken = battle.effectsOf(attackerId)
            .count { it.effectType == EffectType.POISON } * POISON_DAMAGE

        // 독 데미지 상대 점수에 반영
        addDamageScore(battle, opponentId, poisonDamageTaken)

        // 스킬 타입별 처리
        val shieldBlocked = addDamageScoreWithOpponentShield(battle, attackerId, finalDamage)
        val statusApplied: StatusAppliedInfo?

        when (skill.skillType) {
            SkillType.ATTACK -> {
                statusApplied = null
            }
            SkillType.POISON -> {
                statusApplied = if (!shieldBlocked) {
                    applyEffect(battle, opponentId, EffectType.POISON, skill.lasting ?: 3)
                    StatusAppliedInfo("POISON", skill.lasting ?: 3)
                } else null
            }
            SkillType.PARALYZE -> {
                statusApplied = if (!shieldBlocked) {
                    applyEffect(battle, opponentId, EffectType.PARALYZE, skill.lasting ?: 3)
                    StatusAppliedInfo("PARALYZE", skill.lasting ?: 3)
                } else null
            }
            SkillType.DEFEND -> {
                applyShield(battle, attackerId, skill.lasting ?: 1)
                statusApplied = null
            }
            SkillType.DAMAGE_BUFF -> {
                applyEffect(battle, attackerId, EffectType.DAMAGE_BUFF, skill.lasting ?: 3)
                statusApplied = StatusAppliedInfo("DAMAGE_BUFF", skill.lasting ?: 3)
            }
            SkillType.CLEANSE -> {
                cleanse(battle, attackerId)
                statusApplied = null
            }
        }

        // 효과 적용 후 턴 소모
        tickEffects(battle, attackerId)

        return SkillApplyResult(
            damageDealt = if (shieldBlocked) 0 else finalDamage,
            statusApplied = statusApplied,
            shieldBlocked = shieldBlocked,
            poisonDamageTaken = poisonDamageTaken
        )
    }

    // ===== private 헬퍼 =====

     private fun tickEffects(battle: Battle, userId: Long) {
        val effects = battle.effectsOf(userId)
        effects.forEach { it.remainingTurns-- }
        battleEffectRepository.deleteAll(effects.filter { it.remainingTurns <= 0 })
    }

    private fun cleanse(battle: Battle, userId: Long) {
        battleEffectRepository.deleteAll(battle.effectsOf(userId))
    }

    private fun addDamageScoreWithOpponentShield(battle: Battle, userId: Long, damage: Int): Boolean {
        if (battle.isUserA(userId)) {
            if (battle.userBShield > 0) { battle.userBShield--; return true }
            battle.userAScore += damage
        } else {
            if (battle.userAShield > 0) { battle.userAShield--; return true }
            battle.userBScore += damage
        }
        return false
    }

    private fun applyShield(battle: Battle, userId: Long, count: Int) {
        if (battle.isUserA(userId)) battle.userAShield += count
        else battle.userBShield += count
    }

    private fun applyEffect(battle: Battle, targetUserId: Long, effectType: EffectType, turns: Int) {
        val targetUser = if (battle.isUserA(targetUserId)) battle.userA else battle.userB
        battleEffectRepository.save(
            BattleEffect(
                battle = battle,
                targetUser = targetUser,
                effectType = effectType,
                remainingTurns = turns
            )
        )
    }

    private fun addDamageScore(battle: Battle, userId: Long, damage: Int) {
        if (damage == 0) return
        if (battle.isUserA(userId)) battle.userAScore += damage
        else battle.userBScore += damage
    }

    private fun findBattleByUser(user: User, weekStart: LocalDate): Battle? =
        battleRepository.findFirstByUserAAndWeekStartAndResultIsNull(user, weekStart)
            ?: battleRepository.findFirstByUserBAndWeekStartAndResultIsNull(user, weekStart)

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