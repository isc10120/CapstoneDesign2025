package com.example.zamgavocafront.pve

/** 플레이어 / 몬스터에게 적용되는 상태이상 */
sealed class StatusEffect {
    abstract val turnsRemaining: Int

    /** 독: 매 턴 dmgPerTurn 데미지 */
    data class Poison(override val turnsRemaining: Int, val dmgPerTurn: Int = 15) : StatusEffect()

    /** 마비: turnsRemaining 턴 동안 30% 확률로 스킬 발동 실패 */
    data class Paralysis(override val turnsRemaining: Int) : StatusEffect()

    /** 공격력 버프: 다음 공격 스킬의 데미지 +bonusDamage (1회 소비) */
    data class AttackBuff(val bonusDamage: Int) : StatusEffect() {
        override val turnsRemaining: Int get() = 1
    }

    /** 방어막: count 회 공격 차단 후 소멸 */
    data class Shield(val count: Int) : StatusEffect() {
        override val turnsRemaining: Int get() = count
    }

    /** 턴이 지난 후 감소된 복사본 반환. null이면 소멸 */
    fun tick(): StatusEffect? = when (this) {
        is Poison    -> if (turnsRemaining > 1) copy(turnsRemaining = turnsRemaining - 1) else null
        is Paralysis -> if (turnsRemaining > 1) copy(turnsRemaining = turnsRemaining - 1) else null
        is AttackBuff, is Shield -> this  // 별도 로직으로 처리
    }
}

/** 카드가 발동하는 스킬 효과 종류 */
enum class SkillEffectType(val displayName: String, val icon: String) {
    ATTACK      ("공격",       "⚔"),
    ATTACK_BUFF ("공격력 버프", "⬆"),
    DEFENSE     ("방어",       "🛡"),
    POISON      ("독",         "☠"),
    PARALYSIS   ("마비",       "⚡"),
    CLEANSE     ("정화",       "✨")
}
