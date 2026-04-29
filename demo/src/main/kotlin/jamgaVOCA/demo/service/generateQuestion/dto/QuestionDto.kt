package jamgaVOCA.demo.service.generateQuestion.dto

data class QuestionRequest(val wordId: Long)

data class QuestionData(
    val questionType: String,
    val wordId: Long,
    val word: String,
    val question: String,
    val hint: String? = null,
    val options: List<String>? = null,
    val correctIndex: Int? = null,
    val blankedWord: String? = null,
    val shuffledLetters: String? = null,
    val answer: String? = null,
    val idealAnswer: String? = null
)

data class EvaluateRequest(
    val wordId: Long,
    val questionType: String,
    val userAnswer: String,
    val prompt: String? = null
)

data class EvaluateData(
    val correct: Boolean,
    val score: Int,
    val feedback: String,
    val correctAnswer: String? = null
)
