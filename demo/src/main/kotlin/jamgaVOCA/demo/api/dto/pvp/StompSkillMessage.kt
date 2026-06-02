package jamgaVOCA.demo.api.dto.pvp

import jamgaVOCA.demo.service.dto.StatusAppliedInfo

data class StompSkillMessage(
    val senderId: Long,
    val senderLevel: Int,
    val senderExp: Int,
    val skillName: String,
    val skillType: String,
    val damageDealt: Int,
    val statusApplied: StatusAppliedInfo?,
    val shieldBlocked: Boolean,
    val poisonDamageTaken: Int = 0,
    val paralyzed: Boolean = false,
    val isFailed: Boolean = false,
    val cleansedEffectId: Long? = null
)