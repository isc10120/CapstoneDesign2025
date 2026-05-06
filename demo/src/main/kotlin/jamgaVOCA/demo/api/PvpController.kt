package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.domain.user.User
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
    fun getStatus(@AuthUser user: User): ResponseEntity<BattleStatusResponse> {
        val battle = battleService.getCurrentBattle(user.id!!)
        return ResponseEntity.ok(BattleStatusResponse.from(battle, user.id!!))
    }

    @GetMapping("/result/latest")
    fun getLatestResult(@AuthUser user: User): ResponseEntity<BattleResultResponse?> {
        val battle = battleService.getLatestUncheckedResult(user.id!!)
            ?: return ResponseEntity.ok(null)
        return ResponseEntity.ok(BattleResultResponse.from(battle, user.id!!))
    }

    @GetMapping("/result/history")
    fun getHistory(@AuthUser user: User): ResponseEntity<List<BattleHistoryResponse>> {
        val battles = battleService.getBattleHistory(user.id!!)
        return ResponseEntity.ok(battles.map { BattleHistoryResponse.from(it, user.id!!) })
    }

    @PatchMapping("/result/confirm")
    fun confirmResult(@AuthUser user: User): ResponseEntity<Unit> {
        battleService.confirmResult(user.id!!)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/skill")
    fun useSkill(@AuthUser user: User, @RequestBody request: PvpSkillRequest): ResponseEntity<PvpSkillResponse> {
        // 스킬 조회
        val skill = skillRepository.findById(request.skillId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 스킬입니다.") }

        // 현재 배틀 조회
        val battle = battleService.getCurrentBattle(user.id!!)

        // WeekCollectedWord 검증 - 이번 주 수집된 단어인지 확인
       if (!weekCollectedWordRepository.existsByUserIdAndWordId(user.id!!, request.wordId))
            throw IllegalArgumentException("이번 주 수집된 단어가 아닙니다.")

        // 스킬 효과 적용
        val applyResult = battleService.applySkill(battle, user.id!!, skill)

        // UserWordSkill 수집 처리
        skillService.collectSkill(request.skillId, request.wordId, user.id!!)

        // STOMP 푸시
        messagingTemplate.convertAndSend(
            "/topic/pvp/${battle.id}",
            StompSkillMessage(
                senderId = user.id!!,
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
}