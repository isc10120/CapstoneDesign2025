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
    val cleansedEffectId: Long? = null,
    val currentLevel: Int,
    val currentExp: Int
) {
    companion object {
        fun from(result: SkillApplyResult, skillName: String, skillType: String, level: Int, exp: Int) =
            PvpSkillResponse(
                skillName = skillName,
                skillType = skillType,
                damageDealt = result.damageDealt,
                statusApplied = result.statusApplied,
                shieldBlocked = result.shieldBlocked,
                poisonDamageTaken = result.poisonDamageTaken,
                paralyzed = result.paralyzed,
                cleansedEffectId = result.cleansedEffectId,
                currentLevel = level,
                currentExp = exp
            )
    }
}