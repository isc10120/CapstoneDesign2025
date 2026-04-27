package jamgaVOCA.demo.api

import jamgaVOCA.demo.service.BattleService
import jamgaVOCA.demo.api.dto.pvp.BattleStatusResponse
import jamgaVOCA.demo.api.dto.pvp.BattleResultResponse
import jamgaVOCA.demo.api.dto.pvp.BattleHistoryResponse
import jakarta.servlet.http.HttpSession
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pvp")
class PvpController(
    private val battleService: BattleService,
    private val httpSession: HttpSession
) {

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<BattleStatusResponse> {
        val userId = getUserId()
        val battle = battleService.getCurrentBattle(userId)
        return ResponseEntity.ok(BattleStatusResponse.from(battle, userId))
    }

    @GetMapping("/result/latest")
    fun getLatestResult(): ResponseEntity<BattleResultResponse?> {
        val userId = getUserId()
        val battle = battleService.getLatestUncheckedResult(userId)
            ?: return ResponseEntity.ok(null)
        return ResponseEntity.ok(BattleResultResponse.from(battle, userId))
    }

    @GetMapping("/result/history")
    fun getHistory(): ResponseEntity<List<BattleHistoryResponse>> {
        val userId = getUserId()
        val battles = battleService.getBattleHistory(userId)
        return ResponseEntity.ok(battles.map { BattleHistoryResponse.from(it, userId) })
    }

    @PatchMapping("/result/confirm")
    fun confirmResult(): ResponseEntity<Unit> {
        val userId = getUserId()
        battleService.confirmResult(userId)
        return ResponseEntity.ok().build()
    }

    private fun getUserId(): Long = 1L
//        httpSession.getAttribute("userId") as? Long
//            ?: throw RuntimeException("User not logged in")
}