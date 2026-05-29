package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.dto.pvp.BattleHistoryResponse
import jamgaVOCA.demo.api.dto.pvp.BattleResultResponse
import jamgaVOCA.demo.api.dto.pvp.BattleStatusResponse
import jamgaVOCA.demo.api.dto.pvp.PvpSkillRequest
import jamgaVOCA.demo.api.dto.pvp.PvpSkillResponse
import jamgaVOCA.demo.api.dto.pvp.StompSkillMessage
import jamgaVOCA.demo.service.BattleService
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pvp")
class PvpController(
    private val battleService: BattleService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    // 테스트용 - 운영 시 제거
    @PostMapping("/test/match")
    fun testMatch(): ApiResponse<Nothing> {
        battleService.weeklyReset()
        return ApiResponse.success(null)
    }

    @GetMapping("/status")
    fun getStatus(@AuthUser user: User): ApiResponse<BattleStatusResponse> {
        val battle = battleService.getCurrentBattle(user.id!!)
        return ApiResponse.success(BattleStatusResponse.from(battle, user.id!!))
    }

    @GetMapping("/result/latest")
    fun getLatestResult(@AuthUser user: User): ApiResponse<BattleResultResponse?> {
        val battle = battleService.getLatestUncheckedResult(user.id!!)
            ?: return ApiResponse.success(null)
        return ApiResponse.success(BattleResultResponse.from(battle, user.id!!))
    }

    @GetMapping("/result/history")
    fun getHistory(@AuthUser user: User): ApiResponse<List<BattleHistoryResponse>> {
        val battles = battleService.getBattleHistory(user.id!!)
        return ApiResponse.success(battles.map { BattleHistoryResponse.from(it, user.id!!) })
    }

    @PatchMapping("/result/confirm")
    fun confirmResult(@AuthUser user: User): ApiResponse<Nothing> {
        battleService.confirmResult(user.id!!)
        return ApiResponse.success(null)
    }

    @PostMapping("/skill")
    fun useSkill(@AuthUser user: User, @RequestBody request: PvpSkillRequest): ApiResponse<PvpSkillResponse> {
        val result = battleService.useSkill(user.id!!, request.skillId, request.wordId)

        // STOMP 푸시
        messagingTemplate.convertAndSend(
            "/topic/pvp/${result.battleId}",
            StompSkillMessage(
                senderId = user.id!!,
                skillName = result.skillName,
                skillType = result.skillType,
                damageDealt = result.applyResult.damageDealt,
                statusApplied = result.applyResult.statusApplied,
                shieldBlocked = result.applyResult.shieldBlocked,
                poisonDamageTaken = result.applyResult.poisonDamageTaken,
                paralyzed = result.applyResult.paralyzed,
                cleansedEffectId = result.applyResult.cleansedEffectId
            )
        )

        return ApiResponse.success(
            PvpSkillResponse.from(result.applyResult, result.skillName, result.skillType)
        )
    }

    @PostMapping("/skill/fail")
    fun failSkill(@AuthUser user: User, @RequestBody request: PvpSkillRequest): ApiResponse<PvpSkillResponse> {
        val result = battleService.failSkill(user.id!!, request.skillId, request.wordId)

        // STOMP 푸시 (실패 알림)
        messagingTemplate.convertAndSend(
            "/topic/pvp/${result.battleId}",
            StompSkillMessage(
                senderId = user.id!!,
                skillName = result.skillName,
                skillType = result.skillType,
                damageDealt = 0,
                statusApplied = null,
                shieldBlocked = false,
                poisonDamageTaken = result.applyResult.poisonDamageTaken,
                paralyzed = false,
                isFailed = true
            )
        )

        return ApiResponse.success(
            PvpSkillResponse.from(result.applyResult, result.skillName, result.skillType)
        )
    }
}