package jamgaVOCA.demo.legacy.generateSkill.dto
import jakarta.validation.constraints.NotBlank

data class SkillGenerateRequest(
    @field:NotBlank val word: String,
    @field:NotBlank val meaningKo: String
)
