package jamgaVOCA.demo.translation.dto

data class CreateQuestionResponse(
    val success: Boolean,
    val koreanSentence: String? = null,
    val targetWord: String? = null,
    val wordHint: String? = null,
    val ideal: String? = null,   // _ideal (프론트에 숨겨도 됨)
    val error: String? = null
)
