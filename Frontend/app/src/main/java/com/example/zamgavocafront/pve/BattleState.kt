package com.example.zamgavocafront.pve

data class MonsterState(
    val template: MonsterTemplate,
    var hp: Int = template.maxHp,
    var attackCountdown: Int = template.attackEveryNTurns
) {
    val isAlive get() = hp > 0
    val hpPercent get() = if (template.maxHp > 0) hp.toFloat() / template.maxHp else 0f
}

data class BattleState(
    val stageId: Int,
    var roundIndex: Int,
    var playerHp: Int,
    val playerMaxHp: Int,
    val monsters: MutableList<MonsterState>,
    val playerBuffs: MutableList<StatusEffect>,
    val playerDebuffs: MutableList<StatusEffect>,
    val cardCooldowns: MutableMap<Int, Int>,  // wordId → 남은 쿨타임 턴
    var turn: Int = 1
) {
    val isPlayerAlive get() = playerHp > 0
    val allMonstersDefeated get() = monsters.all { !it.isAlive }
    val isBossRound get() = roundIndex == 4
    val playerHpPercent get() = if (playerMaxHp > 0) playerHp.toFloat() / playerMaxHp else 0f

    fun statusSummary(): String {
        val debuffs = playerDebuffs.joinToString(" ") { effect ->
            when (effect) {
                is StatusEffect.Poison    -> "☠독(${effect.turnsRemaining}턴)"
                is StatusEffect.Paralysis -> "⚡마비(${effect.turnsRemaining}턴)"
                else -> ""
            }
        }.trim()
        val buffs = playerBuffs.joinToString(" ") { effect ->
            when (effect) {
                is StatusEffect.AttackBuff -> "⬆공격+${effect.bonusDamage}"
                is StatusEffect.Shield     -> "🛡방어(${effect.count}회)"
                else -> ""
            }
        }.trim()
        return listOf(debuffs, buffs).filter { it.isNotEmpty() }.joinToString("  ")
    }
}

data class TurnResult(
    val damageDealt: Int,
    val targetMonsterIndex: Int,
    val effectType: SkillEffectType,
    val effectDesc: String,
    val monsterAttacks: List<MonsterAttackResult>,
    val poisonDamage: Int,
    val paralysisBlocked: Boolean,
    val roundCleared: Boolean,
    val playerDied: Boolean
)

data class MonsterAttackResult(
    val monsterName: String,
    val damage: Int,
    val shieldBlocked: Boolean,
    val statusApplied: MonsterStatusAttack
)
