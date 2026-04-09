package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.dto.CollectSkillRequest
import jamgaVOCA.demo.api.dto.SkillResponse
import jamgaVOCA.demo.api.dto.WordResponse
import jamgaVOCA.demo.service.SkillService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class SkillController(
    private val skillService: SkillService
) {

    @GetMapping("/collected-skill-list")
    fun getCollectedSkillList(): ApiResponse<List<SkillResponse>> {
        val data = skillService.getCollectedSkillList()
        return ApiResponse.success(data)
    }

    @GetMapping("/skill-info")
    fun getSkillInfo(@RequestParam id: Long): ApiResponse<SkillResponse> {
        val data = skillService.getSkillInfo(id)
        return ApiResponse.success(data)
    }

    @PostMapping("/collect-skill")
    fun collectSkill(@RequestBody request: CollectSkillRequest): ApiResponse<Nothing> {
        skillService.collectSkill(request)
        return ApiResponse.success(null)
    }
}
