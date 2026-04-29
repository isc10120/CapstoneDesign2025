package jamgaVOCA.demo.api

import jamgaVOCA.demo.service.BattleService
import jamgaVOCA.demo.api.dto.pvp.BattleStatusResponse
import jamgaVOCA.demo.api.dto.pvp.BattleResultResponse
import jamgaVOCA.demo.api.dto.pvp.BattleHistoryResponse
import jakarta.servlet.http.HttpSession
import jamgaVOCA.demo.api.dto.pvp.PvpSkillRequest
import jamgaVOCA.demo.api.dto.pvp.PvpSkillResponse
import jamgaVOCA.demo.api.dto.pvp.StompSkillMessage
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWordRepository
import jamgaVOCA.demo.service.SkillService
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pvp")
class PvpController(
    private val battleService: BattleService,
    private val skillService: SkillService,
    private val skillRepository: SkillRepository,
    private val weekCollectedWordRepository: WeekCollectedWordRepository,
    private val httpSession: HttpSession,
    private val messagingTemplate: SimpMessagingTemplate
) {
    // 테스트용 - 운영 시 제거
    @PostMapping("/test/match")
    fun testMatch(): ResponseEntity<Unit> {
        battleService.settleAndMatch()
        return ResponseEntity.ok().build()
    }

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

    @PostMapping("/skill")
    fun useSkill(@RequestBody request: PvpSkillRequest): ResponseEntity<PvpSkillResponse> {
        val userId = getUserId()

        // 스킬 조회
        val skill = skillRepository.findById(request.skillId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 스킬입니다.") }

        // 현재 배틀 조회
        val battle = battleService.getCurrentBattle(userId)

        // WeekCollectedWord 검증 - 이번 주 수집된 단어인지 확인
       if (!weekCollectedWordRepository.existsByUserIdAndWordId(userId, request.wordId))
            throw IllegalArgumentException("이번 주 수집된 단어가 아닙니다.")

        // 스킬 효과 적용
        val applyResult = battleService.applySkill(battle, userId, skill)

        // UserWordSkill 수집 처리
        val collected = skillService.collectSkill(request.skillId, request.wordId, userId)

        // STOMP 푸시
        messagingTemplate.convertAndSend(
            "/topic/pvp/${battle.id}",
            StompSkillMessage(
                senderId = userId,
                skillName = skill.name,
                skillType = skill.skillType.name,
                damageDealt = applyResult.damageDealt,
                statusApplied = applyResult.statusApplied,
                shieldBlocked = applyResult.shieldBlocked
            )
        )

        return ResponseEntity.ok(
            PvpSkillResponse.from(applyResult, skill.name, skill.skillType.name)
        )
    }

    private fun getUserId(): Long = 1L
//        httpSession.getAttribute("userId") as? Long
//            ?: throw RuntimeException("User not logged in")
}