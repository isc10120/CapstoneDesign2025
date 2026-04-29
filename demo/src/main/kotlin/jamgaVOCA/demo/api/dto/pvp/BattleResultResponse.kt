package jamgaVOCA.demo.api.dto.pvp

import jamgaVOCA.demo.domain.battle.Battle

data class BattleResultResponse(
    val battleId: Long,
    val weekStart: String,
    val weekEnd: String,
    val result: String,
    val myTotalDamage: Int,
    val opponentTotalDamage: Int,
    val opponentNickname: String
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
                myTotalDamage = battle.damageOf(userId),
                opponentTotalDamage = battle.damageOf(opponentUser.id!!),
                opponentNickname = opponentUser.nickname
            )
        }
    }
}
