package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.service.RankingService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/rankings")
class RankingController(
    private val rankingService: RankingService
) {
    @GetMapping("/exp")
    fun getExpRanking(
        @AuthUser user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<RankingService.RankingResponse> {
        return ApiResponse.success(rankingService.getExpRanking(user.id!!, page, size))
    }

    @GetMapping("/skills")
    fun getSkillCountRanking(
        @AuthUser user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<RankingService.RankingResponse> {
        return ApiResponse.success(rankingService.getSkillCountRanking(user.id!!, page, size))
    }
}
