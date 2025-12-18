package jamgaVOCA.demo.generateSkill

import jamgaVOCA.demo.generateSkill.dto.SkillGenerateRequest
import jamgaVOCA.demo.generateSkill.dto.SkillGenerateResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/skills")
class SkillGenerateController(
    private val service: SkillGeneratorService
) {

    @PostMapping("/generate")
    fun generate(@RequestBody req: SkillGenerateRequest): SkillGenerateResponse {
        return service.generate(
            word = req.word,
            meaningKo = req.meaningKo
        )
    }
}
