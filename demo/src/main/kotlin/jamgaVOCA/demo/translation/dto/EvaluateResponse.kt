package jamgaVOCA.demo.translation.dto

data class EvaluateResponse(
    val success: Boolean,
    val score: Int? = null,
    val breakdown: Map<String, Int>? = null,
    val feedback: String? = null,
    val correction: String? = null,
    val idealAnswer: String? = null,
    val error: String? = null
)
