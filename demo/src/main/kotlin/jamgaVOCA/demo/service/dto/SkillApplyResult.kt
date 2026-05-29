package jamgaVOCA.demo.service.dto

data class SkillApplyResult(
    val damageDealt: Int = 0,
    val statusApplied: StatusAppliedInfo? = null,
    val shieldBlocked: Boolean = false,
    val poisonDamageTaken: Int = 0,
    val paralyzed: Boolean = false,
    val cleansedEffectId: Long? = null
)

data class StatusAppliedInfo(
    val id: Long,
    val type: String,
    val turns: Int
)