package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.battle.Battle
import jamgaVOCA.demo.domain.battle.BattleEffect
import jamgaVOCA.demo.domain.battle.BattleEffectRepository
import jamgaVOCA.demo.domain.battle.BattleRepository
import jamgaVOCA.demo.domain.battle.BattleResult
import jamgaVOCA.demo.domain.battle.EffectType
import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.skill.SkillType
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWordRepository
import jamgaVOCA.demo.service.dto.SkillApplyResult
import jamgaVOCA.demo.service.dto.StatusAppliedInfo
import org.slf4j.LoggerFactory
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
    private val userService: UserService,
    private val wordService: WordService,
    private val skillService: SkillService,
    private val weekCollectedWordRepository: WeekCollectedWordRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val DAMAGE_BUFF_RATE_PER_STACK = 0.5
        const val POISON_DAMAGE = 20
    }

    // ===== 주간 정산 + 매칭 + 주간 단어 초기화 =====

    @Scheduled(cron = "0 0 0 * * MON")  // 매주 월요일 00:00
    fun weeklyReset() {
        log.info("[BATTLE] 주간 정산 및 매칭 시작")
        settleWeeklyBattles()
        createWeeklyMatches()
        wordService.resetWeeklyCollectedWords()
        log.info("[BATTLE] 주간 정산 및 매칭 완료")
    }

    private fun settleWeeklyBattles() {
        val unsettled = battleRepository.findAllByResultIsNull()
        log.info("[BATTLE] 정산 대상 배틀 수: ${unsettled.size}")
        unsettled.forEach { battle ->
            battle.result = when {
                battle.userAScore > battle.userBScore -> BattleResult.WIN_A
                battle.userBScore > battle.userAScore -> BattleResult.WIN_B
                else -> BattleResult.DRAW
            }
            log.debug(
                "[BATTLE] 배틀 정산 - battleId={}, userA={}({}), userB={}({}), result={}",
                battle.id,
                battle.userA.id,
                battle.userAScore,
                battle.userB.id,
                battle.userBScore,
                battle.result
            )
            grantPvpExp(battle)
        }
    }

    private fun grantPvpExp(battle: Battle) {
        val userA = battle.userA
        val userB = battle.userB
        // 더미 유저는 경험치 지급 제외
        when (battle.result) {
            BattleResult.WIN_A -> {
                if (!userA.isDummy) userService.addExp(userA.id!!, userB.level * 30)
                if (!userB.isDummy) userService.addExp(userB.id!!, userA.level * 10)
            }
            BattleResult.WIN_B -> {
                if (!userB.isDummy) userService.addExp(userB.id!!, userA.level * 30)
                if (!userA.isDummy) userService.addExp(userA.id!!, userB.level * 10)
            }
            BattleResult.DRAW -> {
                val expA = userB.level * 10
                val expB = userA.level * 10
                if (!userA.isDummy) userService.addExp(userA.id!!, expA)
                if (!userB.isDummy) userService.addExp(userB.id!!, expB)
            }
            null -> {}
        }
        log.info("[BATTLE] PVP 경험치 지급 - battleId=${battle.id}, result=${battle.result}")
    }

    private fun createWeeklyMatches() {
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)

        // 레벨 기준 정렬 후 동레벨대 매칭, 같은 레벨 내에서는 랜덤
        val users = userService.findAll()
            .filter { !it.isDummy }
            .groupBy { it.level }
            .toSortedMap()
            .values
            .flatMap { it.shuffled() }

        log.info("[BATTLE] 주간 매칭 생성 - 대상 유저 수: ${users.size}, 기간: $weekStart ~ $weekEnd")
        var matchCount = 0
        users.chunked(2).forEach { pair ->
            val userA = pair[0]
            val userB = if (pair.size == 2) {
                pair[1]
            } else {
                val dummy = userService.findOrCreateDummyByLevel(userA.level)
                log.debug("[BATTLE] 더미 매칭 - dummyId=${dummy.id}, level=${dummy.level}, userId=${userA.id}")
                dummy
            }

            battleRepository.save(
                Battle(
                    userA = userA,
                    userB = userB,
                    weekStart = weekStart,
                    weekEnd = weekEnd
                )
            )
            log.debug("[BATTLE] 매칭 생성 - userA=${userA.id}, userB=${userB.id}(dummy=${userB.isDummy})")
            matchCount++
        }
        log.info("[BATTLE] 총 ${matchCount}건 매칭 완료")
    }

    // ===== 신규 유저 더미 매칭 =====

    fun matchNewUserWithDummy(user: User) {
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)

        if (findBattleByUser(user, weekStart) != null) {
            log.debug("[BATTLE] 신규 유저 더미 매칭 스킵 - 이미 배틀 존재: userId=${user.id}")
            return
        }

        val dummy = userService.findOrCreateDummyByLevel(user.level)
        battleRepository.save(
            Battle(
                userA = user,
                userB = dummy,
                weekStart = weekStart,
                weekEnd = weekEnd
            )
        )
        log.info("[BATTLE] 신규 유저 더미 매칭 완료 - userId=${user.id}, dummyId=${dummy.id}, level=${dummy.level}, 기간: $weekStart ~ $weekEnd")
    }

    // ===== 배틀 조회 =====

    @Transactional
    fun getCurrentBattle(userId: Long): Battle {
        val user = getUser(userId)
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        val existingBattle = findBattleByUser(user, weekStart)

        if (existingBattle != null) {
            return existingBattle
        }

        log.warn("[BATTLE] 배틀을 찾지 못함. 더미와 매칭 시도 - userId=$userId")
        matchNewUserWithDummy(user)
        return findBattleByUser(user, weekStart)
            ?: throw AppException(ErrorCode.BATTLE_NOT_FOUND)
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

    // ===== 스킬 사용 프로세스 =====

    data class UseSkillResult(
        val applyResult: SkillApplyResult,
        val skillName: String,
        val skillType: String,
        val skillImageUrl: String,
        val skillDominantColor: String?,
        val battleId: Long,
        val senderLevel: Int,
        val senderExp: Int
    )

    fun useSkill(userId: Long, skillId: Long, wordId: Long): UseSkillResult {
        // 일일 스킬 제한 체크 및 업데이트
        userService.updateDailySkillCount(userId)

        // 스킬 조회
        val skill = skillService.getSkillEntity(skillId)

        // 현재 배틀 조회
        val battle = getCurrentBattle(userId)

        // WeekCollectedWord 검증 및 삭제 - 이번 주 수집된 단어인지 확인
        val weekCollectedWord = weekCollectedWordRepository.findByUserIdAndWordId(userId, wordId)
            .orElseThrow { AppException(ErrorCode.NOT_COLLECTED_THIS_WEEK) }

        weekCollectedWordRepository.delete(weekCollectedWord)

        // 스킬 효과 적용
        val applyResult = applySkill(battle, userId, skill)

        // UserWordSkill 수집 처리 (신규 카드면 +15 exp 지급)
        skillService.collectSkill(skillId, wordId, userId)

        val updatedUser = userService.getUser(userId)
        return UseSkillResult(
            applyResult = applyResult,
            skillName = skill.name,
            skillType = skill.skillType.name,
            skillImageUrl = skill.imageUrl,
            skillDominantColor = skill.dominantColor,
            battleId = battle.id!!,
            senderLevel = updatedUser.level,
            senderExp = updatedUser.expPoint
        )
    }

    fun failSkill(userId: Long, skillId: Long, wordId: Long): UseSkillResult {
        // 실패 시에도 일일 스킬 횟수는 차감 (턴 소비 개념)
        userService.updateDailySkillCount(userId)

        val skill = skillService.getSkillEntity(skillId)
        val battle = getCurrentBattle(userId)

        // 실패 로직: 독뎀 + 효과 턴 소모만 진행
        val opponentId = getOpponentId(battle, userId)
        val poisonStacks = battle.effectsOf(userId).count { it.effectType == EffectType.POISON }
        val poisonDamageTaken = poisonStacks * POISON_DAMAGE
        addDamageScore(battle, opponentId, poisonDamageTaken)

        tickEffects(battle, userId)

        log.info("[BATTLE] 스킬 실패(턴 소비) - battleId=${battle.id}, userId=$userId, poisonDamageTaken=$poisonDamageTaken")

        val user = userService.getUser(userId)
        return UseSkillResult(
            applyResult = SkillApplyResult(poisonDamageTaken = poisonDamageTaken),
            skillName = skill.name,
            skillType = skill.skillType.name,
            skillImageUrl = skill.imageUrl,
            skillDominantColor = skill.dominantColor,
            battleId = battle.id!!,
            senderLevel = user.level,
            senderExp = user.expPoint
        )
    }

    // ===== 스킬 효과 적용 =====

    fun applySkill(battle: Battle, attackerId: Long, skill: Skill): SkillApplyResult {
        val opponentId = getOpponentId(battle, attackerId)
        log.debug(
            "[BATTLE] 스킬 사용 시도 - battleId={}, attacker={}, skill={}({})",
            battle.id,
            attackerId,
            skill.name,
            skill.skillType
        )

        // 1. PARALYZE 체크
        val paralyzeStacks = battle.effectsOf(attackerId)
            .count { it.effectType == EffectType.PARALYZE }
        val isParalyzed = (1..paralyzeStacks).any { Random.nextFloat() < 0.3f }

        if (isParalyzed) {
            log.info("[BATTLE] 마비로 행동 불가 - userId=$attackerId")
            // 마비 시: 독뎀 > 효과 턴 소모 후 종료
            val poisonStacks = battle.effectsOf(attackerId).count { it.effectType == EffectType.POISON }
            val poisonDamage = poisonStacks * POISON_DAMAGE
            addDamageScore(battle, opponentId, poisonDamage)
            
            tickEffects(battle, attackerId)
            
            return SkillApplyResult(paralyzed = true, poisonDamageTaken = poisonDamage)
        }

        // 2. 데미지 버프 체크
        val buffStacks = battle.effectsOf(attackerId)
            .count { it.effectType == EffectType.DAMAGE_BUFF }
        val finalDamage = if (buffStacks > 0) {
            (skill.damage * (1.0 + buffStacks * DAMAGE_BUFF_RATE_PER_STACK)).toInt()
        } else {
            skill.damage
        }

        // 3. 정화 스킬일 시 정화 적용 (독뎀 전에 적용)
        var cleansedEffectId: Long? = null
        if (skill.skillType == SkillType.CLEANSE) {
            cleansedEffectId = cleanse(battle, attackerId)
        }

        // 4. 독뎀 적용 (정상 행동 시)
        val poisonStacks = battle.effectsOf(attackerId).count { it.effectType == EffectType.POISON }
        val poisonDamageTaken = poisonStacks * POISON_DAMAGE
        addDamageScore(battle, opponentId, poisonDamageTaken)

        // 5. 효과 턴 소모
        tickEffects(battle, attackerId)

        // 6. 정화를 제외한 스킬 효과 적용
        var shieldBlocked = addDamageScoreWithOpponentShield(battle, attackerId, finalDamage)
        var statusApplied: StatusAppliedInfo? = null

        when (skill.skillType) {
            SkillType.ATTACK -> {}
            SkillType.POISON -> {
                if (!shieldBlocked) {
                    statusApplied = applyEffect(battle, opponentId, EffectType.POISON, skill.lasting ?: 3)
                }
            }
            SkillType.PARALYZE -> {
                if (!shieldBlocked) {
                    statusApplied = applyEffect(battle, opponentId, EffectType.PARALYZE, skill.lasting ?: 3)
                }
            }
            SkillType.DEFEND -> {
                applyShield(battle, attackerId, 1)
            }
            SkillType.DAMAGE_BUFF -> {
                statusApplied = applyEffect(battle, attackerId, EffectType.DAMAGE_BUFF, skill.lasting ?: 3)
            }
            SkillType.CLEANSE -> {}
        }

        val result = SkillApplyResult(
            damageDealt = if (shieldBlocked) 0 else finalDamage,
            statusApplied = statusApplied,
            shieldBlocked = shieldBlocked,
            poisonDamageTaken = poisonDamageTaken,
            cleansedEffectId = cleansedEffectId
        )
        
        log.info("[BATTLE] 스킬 결과 - battleId=${battle.id}, attacker=$attackerId, skill=${skill.name}, damageDealt=${result.damageDealt}, status=${statusApplied?.type}, poison=$poisonDamageTaken")
        return result
    }

    // ===== private 헬퍼 =====

     private fun tickEffects(battle: Battle, userId: Long) {
        val effects = battle.effectsOf(userId)
        effects.forEach { it.remainingTurns-- }
        battleEffectRepository.deleteAll(effects.filter { it.remainingTurns <= 0 })
    }

    private fun cleanse(battle: Battle, userId: Long): Long? {
        val debuffs = battle.effectsOf(userId)
            .filter { it.effectType == EffectType.POISON || it.effectType == EffectType.PARALYZE }
        
        return if (debuffs.isNotEmpty()) {
            val randomDebuff = debuffs.random()
            val deletedId = randomDebuff.id!!
            battleEffectRepository.delete(randomDebuff)
            log.info("[BATTLE] 정화 적용 - userId=$userId, 삭제된 디버프=${randomDebuff.effectType}, id=$deletedId")
            deletedId
        } else {
            log.debug("[BATTLE] 정화 실패 - 삭제할 디버프 없음: userId=$userId")
            null
        }
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

    private fun applyEffect(battle: Battle, targetUserId: Long, effectType: EffectType, turns: Int): StatusAppliedInfo {
        val targetUser = if (battle.isUserA(targetUserId)) battle.userA else battle.userB
        val effect = battleEffectRepository.save(
            BattleEffect(
                battle = battle,
                targetUser = targetUser,
                effectType = effectType,
                remainingTurns = turns
            )
        )
        return StatusAppliedInfo(effect.id!!, effectType.name, turns)
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




    private fun getUser(userId: Long): User =
        userService.getUser(userId)
}