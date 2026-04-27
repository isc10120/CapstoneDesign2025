package jamgaVOCA.demo.api.dto.pvp

import jamgaVOCA.demo.domain.battle.Battle

data class BattleStatusResponse(
    val battleId: Long,
    val weekStart: String,
    val opponent: OpponentInfo,
    val my: SideStatus,
    val enemy: SideStatus
) {
    companion object {
        fun from(battle: Battle, userId: Long): BattleStatusResponse {
            val isUserA = battle.isUserA(userId)
            val opponentUser = if (isUserA) battle.userB else battle.userA

            return BattleStatusResponse(
                battleId = battle.id!!,
                weekStart = battle.weekStart.toString(),
                opponent = OpponentInfo(
                    userId = opponentUser.id!!,
                    nickname = opponentUser.nickname
                ),
                my = SideStatus(
                    totalDamage = battle.damageOf(userId),
                    statusEffects = battle.effectsOf(userId).map {
                        StatusEffectInfo(it.effectType.name, it.remainingTurns)
                    },
                    shieldCount = battle.shieldOf(userId)
                ),
                enemy = SideStatus(
                    totalDamage = battle.damageOf(opponentUser.id!!),
                    statusEffects = battle.effectsOf(opponentUser.id!!).map {
                        StatusEffectInfo(it.effectType.name, it.remainingTurns)
                    },
                    shieldCount = battle.shieldOf(opponentUser.id!!)
                )
            )
        }
    }
}

data class OpponentInfo(
    val userId: Long,
    val nickname: String
)

data class SideStatus(
    val totalDamage: Int,
    val statusEffects: List<StatusEffectInfo>,
    val shieldCount: Int
)

data class StatusEffectInfo(
    val type: String,
    val remainingTurns: Int
)