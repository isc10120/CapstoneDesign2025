package jamgaVOCA.demo.api.dto.pvp

import jamgaVOCA.demo.domain.battle.Battle

data class BattleResultResponse(
    val battleId: Long,
    val weekStart: String,
    val weekEnd: String,
    val result: String,
    val my: ResultSideInfo,
    val opponent: ResultSideInfo
) {
    companion object {
        fun from(battle: Battle, userId: Long): BattleResultResponse {
            val isUserA = battle.isUserA(userId)
            val opponentUser = if (isUserA) battle.userB else battle.userA

            return BattleResultResponse(
                battleId = battle.id!!,
                weekStart = battle.weekStart.toString(),
                weekEnd = battle.weekEnd.toString(),
                result = battle.resultOf(userId),
                my = ResultSideInfo(
                    totalDamage = battle.damageOf(userId),
                    nickname = if (isUserA) battle.userA.nickname else battle.userB.nickname
                ),
                opponent = ResultSideInfo(
                    totalDamage = battle.damageOf(opponentUser.id!!),
                    nickname = opponentUser.nickname
                )
            )
        }
    }
}

data class ResultSideInfo(
    val totalDamage: Int,
    val nickname: String
)