package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.service.PveService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pve")
class PveController(
    private val pveService: PveService
) {
    @PostMapping("/mob-round")
    fun completeMobRound(@AuthUser user: User): ApiResponse<PveService.ExpRewardResult> {
        return ApiResponse.success(pveService.completeMobRound(user.id!!))
    }

    @PostMapping("/boss-round")
    fun completeBossRound(@AuthUser user: User): ApiResponse<PveService.ExpRewardResult> {
        return ApiResponse.success(pveService.completeBossRound(user.id!!))
    }
}
