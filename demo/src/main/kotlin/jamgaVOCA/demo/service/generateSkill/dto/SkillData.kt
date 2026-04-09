package jamgaVOCA.demo.service.generateSkill.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SkillData(
    val name: String,
    val description: String,
    val damage: Int,
    @JsonProperty("image_desc")
    val imageDesc: String
)
