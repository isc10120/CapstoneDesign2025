package jamgaVOCA.demo.api.dto.pvp

import jamgaVOCA.demo.domain.battle.Battle

data class BattleResultResponse(
    val battleId: Long,
    val weekStart: String,
    val weekEnd: String,
    val result: String,
    val myTotalDamage: Int,
    val opponentTotalDamage: Int,
    val opponentNickname: String,
    val opponentLevel: Int,
    val currentLevel: Int,
    val currentExp: Int
) {
    companion object {
        fun from(battle: Battle, userId: Long): BattleResultResponse {
            val isUserA = battle.isUserA(userId)
            val myUser = if (isUserA) battle.userA else battle.userB
            val opponentUser = if (isUserA) battle.userB else battle.userA

            return BattleResultResponse(
                battleId = battle.id!!,
                weekStart = battle.weekStart.toString(),
                weekEnd = battle.weekEnd.toString(),
                result = battle.resultOf(userId),
                myTotalDamage = battle.damageOf(userId),
                opponentTotalDamage = battle.damageOf(opponentUser.id!!),
                opponentNickname = opponentUser.nickname,
                opponentLevel = opponentUser.level,
                currentLevel = myUser.level,
                currentExp = myUser.expPoint
            )
        }
    }
}
