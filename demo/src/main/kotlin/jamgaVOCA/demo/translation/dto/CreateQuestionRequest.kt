package jamgaVOCA.demo.translation.dto

data class CreateQuestionRequest(
    val targetWord: String,
    val userLevel: String = "intermediate"
)
