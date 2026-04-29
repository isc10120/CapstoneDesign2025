package jamgaVOCA.demo.api.dto.pvp

import jamgaVOCA.demo.service.dto.StatusAppliedInfo

data class StompSkillMessage(
    val senderId: Long,
    val skillName: String,
    val skillType: String,
    val damageDealt: Int,
    val statusApplied: StatusAppliedInfo?,
    val shieldBlocked: Boolean
)