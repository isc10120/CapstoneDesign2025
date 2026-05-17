package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.dto.CollectSkillRequest
import jamgaVOCA.demo.api.dto.SkillResponse
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.service.SkillService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class SkillController(
    private val skillService: SkillService
) {

    @GetMapping("/collected-skill-list")
    fun getCollectedSkillList(@AuthUser user: User): ApiResponse<List<SkillResponse>> {
        val data = skillService.getCollectedSkillList(user.id!!)
        return ApiResponse.success(data)
    }

    @GetMapping("/skill-info")
    fun getSkillInfo(@RequestParam id: Long): ApiResponse<SkillResponse> {
        val data = skillService.getSkillInfo(id)
        return ApiResponse.success(data)
    }

    @PostMapping("/collect-skill")
    fun collectSkill(@AuthUser user: User, @RequestBody request: CollectSkillRequest): ApiResponse<Nothing> {
        skillService.collectSkill(request.skillId, request.wordId, user.id!!)
        return ApiResponse.success(null)
    }
}
