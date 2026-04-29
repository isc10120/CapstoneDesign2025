package jamgaVOCA.demo.api.dto.pvp

import jamgaVOCA.demo.service.dto.SkillApplyResult
import jamgaVOCA.demo.service.dto.StatusAppliedInfo

data class PvpSkillResponse(
    val skillName: String,
    val skillType: String,
    val damageDealt: Int,
    val statusApplied: StatusAppliedInfo?,
    val shieldBlocked: Boolean,
    val poisonDamageTaken: Int,
    val paralyzed: Boolean,
) {
    companion object {
        fun from(result: SkillApplyResult, skillName: String, skillType: String) =
            PvpSkillResponse(
                skillName = skillName,
                skillType = skillType,
                damageDealt = result.damageDealt,
                statusApplied = result.statusApplied,
                shieldBlocked = result.shieldBlocked,
                poisonDamageTaken = result.poisonDamageTaken,
                paralyzed = result.paralyzed
            )
    }
}