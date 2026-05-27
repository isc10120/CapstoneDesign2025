package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.service.BattleService
import jamgaVOCA.demo.service.UserService
import jamgaVOCA.demo.api.dto.pvp.BattleStatusResponse
import jamgaVOCA.demo.api.dto.pvp.BattleResultResponse
import jamgaVOCA.demo.api.dto.pvp.BattleHistoryResponse
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.dto.pvp.PvpSkillRequest
import jamgaVOCA.demo.api.dto.pvp.PvpSkillResponse
import jamgaVOCA.demo.api.dto.pvp.StompSkillMessage
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWordRepository
import jamgaVOCA.demo.service.SkillService
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pvp")
class PvpController(
    private val battleService: BattleService,
    private val userService: UserService,
    private val skillService: SkillService,
    private val skillRepository: SkillRepository,
    private val weekCollectedWordRepository: WeekCollectedWordRepository,
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
        // 일일 스킬 제한 체크 및 업데이트
        userService.updateDailySkillCount(user.id!!)

        // 스킬 조회
        val skill = skillRepository.findById(request.skillId)
            .orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }

        // 현재 배틀 조회
        val battle = battleService.getCurrentBattle(user.id!!)

        // WeekCollectedWord 검증 및 삭제 - 이번 주 수집된 단어인지 확인
        val weekCollectedWord = weekCollectedWordRepository.findByUserIdAndWordId(user.id!!, request.wordId)
            .orElseThrow { AppException(ErrorCode.NOT_COLLECTED_THIS_WEEK) }
        
        weekCollectedWordRepository.delete(weekCollectedWord)

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

        return ApiResponse.success(
            PvpSkillResponse.from(applyResult, skill.name, skill.skillType.name)
        )
    }
}