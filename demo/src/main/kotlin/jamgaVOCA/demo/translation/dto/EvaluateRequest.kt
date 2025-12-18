package jamgaVOCA.demo.translation.dto

data class EvaluateRequest(
    val koreanSentence: String,
    val userAnswer: String,
    val idealTranslation: String,
    val targetWord: String,
    val userLevel: String = "intermediate"
)
