package com.example.zamgavocafront.pve

import com.example.zamgavocafront.pvp.CollectedCardManager
import kotlin.random.Random

/**
 * PVE 전투 로직 엔진 (싱글톤 상태 관리)
 * 화면 전환 시에도 상태가 유지됨.
 * 전투 종료 시 반드시 resetBattle() 호출.
 */
object PveBattleEngine {

    // ── 플레이어 기본 스탯 (필요 시 조정) ──────────────────────────
    private const val PLAYER_BASE_HP = 500
    private const val CARD_COOLDOWN_TURNS = 3
    private const val PARALYSIS_FAIL_CHANCE = 30  // %

    private var _state: BattleState? = null
    val state: BattleState get() = checkNotNull(_state) { "배틀이 시작되지 않았습니다." }
    val isActive get() = _state != null

    // 현재 전투에 사용 중인 덱
    var currentDeck: List<CollectedCardManager.CollectedCard> = emptyList()
        private set

    // ── 전투 시작 ───────────────────────────────────────────────────
    fun startBattle(stageId: Int, deck: List<CollectedCardManager.CollectedCard>) {
        val stage = PveStageData.stages.first { it.id == stageId }
        currentDeck = deck
        _state = BattleState(
            stageId = stageId,
            roundIndex = 0,
            playerHp = PLAYER_BASE_HP,
            playerMaxHp = PLAYER_BASE_HP,
            monsters = buildMonsters(stage.rounds[0]),
            playerBuffs = mutableListOf(),
            playerDebuffs = mutableListOf(),
            cardCooldowns = mutableMapOf()
        )
    }

    // ── 카드의 스킬 효과 결정 (wordId 기반 결정론적) ──────────────
    fun getCardEffect(card: CollectedCardManager.CollectedCard): SkillEffectType = when (card.grade) {
        "동급" -> SkillEffectType.ATTACK
        "은급" -> when (card.wordId % 3) {
            0 -> SkillEffectType.ATTACK
            1 -> SkillEffectType.ATTACK_BUFF
            else -> SkillEffectType.DEFENSE
        }
        "금급" -> when (card.wordId % 4) {
            0 -> SkillEffectType.ATTACK
            1 -> SkillEffectType.POISON
            2 -> SkillEffectType.PARALYSIS
            else -> SkillEffectType.CLEANSE
        }
        else -> SkillEffectType.ATTACK
    }

    /**
     * 카드 사용 처리 (문제 정답 후 호출)
     * @param card 사용할 카드
     * @param targetIdx 공격 대상 몬스터 인덱스. -1이면 첫 번째 생존 몬스터
     */
    fun useCard(card: CollectedCardManager.CollectedCard, targetIdx: Int = -1): TurnResult {
        val s = state
        val effect = getCardEffect(card)

        // 마비 체크 (30% 실패 확률)
        val paralysisBlocked = s.playerDebuffs.any { it is StatusEffect.Paralysis }
                && Random.nextInt(100) < PARALYSIS_FAIL_CHANCE

        var damageDealt = 0
        var effectDesc = ""
        val resolvedTarget = resolveTarget(s, targetIdx)

        if (!paralysisBlocked) {
            // 공격력 버프 수집 후 소비
            val attackBonus = s.playerBuffs.filterIsInstance<StatusEffect.AttackBuff>()
                .sumOf { it.bonusDamage }
            s.playerBuffs.removeAll { it is StatusEffect.AttackBuff }

            when (effect) {
                SkillEffectType.ATTACK -> {
                    damageDealt = card.damage + attackBonus
                    dealDamageToMonster(s, resolvedTarget, damageDealt)
                    effectDesc = "⚔ ${damageDealt} 데미지!"
                }
                SkillEffectType.ATTACK_BUFF -> {
                    val bonus = (card.damage * 0.6).toInt().coerceAtLeast(10)
                    s.playerBuffs.add(StatusEffect.AttackBuff(bonus))
                    effectDesc = "⬆ 다음 공격 +${bonus} 데미지 버프!"
                }
                SkillEffectType.DEFENSE -> {
                    s.playerBuffs.add(StatusEffect.Shield(1))
                    effectDesc = "🛡 방어막 1회 생성!"
                }
                SkillEffectType.POISON -> {
                    damageDealt = card.damage + attackBonus
                    dealDamageToMonster(s, resolvedTarget, damageDealt)
                    // 몬스터 독 상태는 현재 데미지로만 표현 (간소화)
                    effectDesc = "☠ 독 공격 ${damageDealt} 데미지!"
                }
                SkillEffectType.PARALYSIS -> {
                    damageDealt = card.damage + attackBonus
                    dealDamageToMonster(s, resolvedTarget, damageDealt)
                    effectDesc = "⚡ 마비 공격 ${damageDealt} 데미지!"
                }
                SkillEffectType.CLEANSE -> {
                    if (s.playerDebuffs.isNotEmpty()) {
                        val idx = Random.nextInt(s.playerDebuffs.size)
                        val removed = s.playerDebuffs.removeAt(idx)
                        val removedName = when (removed) {
                            is StatusEffect.Poison    -> "독"
                            is StatusEffect.Paralysis -> "마비"
                            else -> "상태이상"
                        }
                        effectDesc = "✨ $removedName 해제!"
                    } else {
                        effectDesc = "✨ 해제할 상태이상 없음"
                    }
                }
            }
        } else {
            effectDesc = "⚡ 마비! 스킬 발동 실패"
        }

        // 카드 쿨타임 설정: 마비로 막힌 경우엔 쿨타임 없음 (스킬 미발동)
        if (!paralysisBlocked) {
            s.cardCooldowns[card.wordId] = CARD_COOLDOWN_TURNS
        }

        // 독 틱 처리
        val poisonDmg = tickPoison(s)

        // 몬스터 공격 처리
        val monsterAttacks = processMonsterAttacks(s)

        // 쿨타임 & 디버프 턴 감소
        tickCooldowns(s)
        tickDebuffs(s)

        s.turn++

        return TurnResult(
            damageDealt = damageDealt,
            targetMonsterIndex = resolvedTarget,
            effectType = if (paralysisBlocked) SkillEffectType.PARALYSIS else effect,
            effectDesc = effectDesc,
            monsterAttacks = monsterAttacks,
            poisonDamage = poisonDmg,
            paralysisBlocked = paralysisBlocked,
            roundCleared = s.allMonstersDefeated,
            playerDied = !s.isPlayerAlive
        )
    }

    /** 다음 라운드로 진행. true = 다음 라운드 있음, false = 스테이지 클리어 */
    fun advanceToNextRound(): Boolean {
        val s = state
        val stage = PveStageData.stages.first { it.id == s.stageId }
        val next = s.roundIndex + 1
        return if (next < stage.rounds.size) {
            s.roundIndex = next
            s.monsters.clear()
            s.monsters.addAll(buildMonsters(stage.rounds[next]))
            true
        } else {
            false
        }
    }

    fun getAvailableCards(): List<CollectedCardManager.CollectedCard> =
        currentDeck.filter { (state.cardCooldowns[it.wordId] ?: 0) <= 0 }

    fun getCooldown(wordId: Int): Int = _state?.cardCooldowns?.get(wordId) ?: 0

    fun getCurrentStageTemplate(): StageTemplate =
        PveStageData.stages.first { it.id == state.stageId }

    fun resetBattle() {
        _state = null
        currentDeck = emptyList()
    }

    // ── private helpers ─────────────────────────────────────────────

    private fun buildMonsters(round: RoundTemplate): MutableList<MonsterState> =
        round.monsters.map { MonsterState(it) }.toMutableList()

    private fun resolveTarget(s: BattleState, preferred: Int): Int {
        if (preferred >= 0 && preferred < s.monsters.size && s.monsters[preferred].isAlive)
            return preferred
        return s.monsters.indexOfFirst { it.isAlive }.coerceAtLeast(0)
    }

    private fun dealDamageToMonster(s: BattleState, idx: Int, dmg: Int) {
        if (idx in s.monsters.indices) {
            s.monsters[idx].hp = (s.monsters[idx].hp - dmg).coerceAtLeast(0)
        }
    }

    private fun tickPoison(s: BattleState): Int {
        val dmg = s.playerDebuffs.filterIsInstance<StatusEffect.Poison>().sumOf { it.dmgPerTurn }
        if (dmg > 0) s.playerHp = (s.playerHp - dmg).coerceAtLeast(0)
        return dmg
    }

    private fun processMonsterAttacks(s: BattleState): List<MonsterAttackResult> {
        val results = mutableListOf<MonsterAttackResult>()
        for (monster in s.monsters.filter { it.isAlive }) {
            monster.attackCountdown--
            if (monster.attackCountdown <= 0) {
                monster.attackCountdown = monster.template.attackEveryNTurns

                // 방어막 체크
                val shield = s.playerBuffs.filterIsInstance<StatusEffect.Shield>().firstOrNull()
                if (shield != null) {
                    s.playerBuffs.removeAll { it is StatusEffect.Shield }
                    if (shield.count > 1) s.playerBuffs.add(StatusEffect.Shield(shield.count - 1))
                    results.add(MonsterAttackResult(monster.template.name, 0, true, MonsterStatusAttack.NONE))
                } else {
                    val dmg = monster.template.baseDamage
                    s.playerHp = (s.playerHp - dmg).coerceAtLeast(0)

                    // 몬스터 특수 공격 적용
                    when (monster.template.statusAttack) {
                        MonsterStatusAttack.POISON ->
                            s.playerDebuffs.add(StatusEffect.Poison(turnsRemaining = 3))
                        MonsterStatusAttack.PARALYSIS ->
                            s.playerDebuffs.add(StatusEffect.Paralysis(turnsRemaining = 3))
                        MonsterStatusAttack.NONE -> {}
                    }
                    results.add(MonsterAttackResult(monster.template.name, dmg, false, monster.template.statusAttack))
                }
            }
        }
        return results
    }

    private fun tickCooldowns(s: BattleState) {
        s.cardCooldowns.keys.toList().forEach { k ->
            s.cardCooldowns[k] = ((s.cardCooldowns[k] ?: 0) - 1).coerceAtLeast(0)
        }
    }

    private fun tickDebuffs(s: BattleState) {
        val updated = s.playerDebuffs.mapNotNull { it.tick() }
        s.playerDebuffs.clear()
        s.playerDebuffs.addAll(updated)
    }
}
