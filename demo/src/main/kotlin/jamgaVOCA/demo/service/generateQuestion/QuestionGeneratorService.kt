package jamgaVOCA.demo.service.generateQuestion

import com.fasterxml.jackson.annotation.JsonProperty
import jamgaVOCA.demo.domain.word.PartOfSpeech
import jamgaVOCA.demo.domain.word.WordLevel
import jamgaVOCA.demo.domain.word.WordRepository
import jamgaVOCA.demo.infra.ai.AiChatClient
import jamgaVOCA.demo.service.generateQuestion.dto.EvaluateData
import jamgaVOCA.demo.service.generateQuestion.dto.EvaluateRequest
import jamgaVOCA.demo.service.generateQuestion.dto.QuestionData
import jamgaVOCA.demo.service.generateQuestion.dto.QuestionRequest
import org.springframework.stereotype.Service

@Service
class QuestionGeneratorService(
    private val wordRepository: WordRepository,
    private val aiChatClient: AiChatClient
) {

    fun generateQuestion(questionType: String, req: QuestionRequest): QuestionData {
        val word = wordRepository.findById(req.wordId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 단어입니다. wordId=${req.wordId}") }

        return when (questionType) {
            "spelling" -> {
                val blanked = blankMiddle(word.englishWord)
                QuestionData(
                    questionType = questionType,
                    wordId = word.id!!,
                    word = word.englishWord,
                    question = "빈칸에 알맞은 글자를 입력하여 단어를 완성하세요.",
                    hint = word.koreanMeaning,
                    blankedWord = blanked,
                    answer = word.englishWord
                )
            }

            "anagram" -> {
                val shuffled = shuffleWord(word.englishWord)
                QuestionData(
                    questionType = questionType,
                    wordId = word.id!!,
                    word = word.englishWord,
                    question = "철자를 재배열하여 올바른 단어를 찾으세요.",
                    hint = word.koreanMeaning,
                    shuffledLetters = shuffled,
                    answer = word.englishWord
                )
            }

            "word_definition" -> {
                val prompt = """
                    영어 단어 "${word.englishWord}"의 한국어 뜻은 "${word.koreanMeaning}"입니다.
                    이 단어와 헷갈릴 수 있는 틀린 한국어 뜻 3가지를 생성해주세요.
                    반드시 다음 JSON 형식으로만 답변하세요:
                    {"wrong_definitions": ["틀린뜻1", "틀린뜻2", "틀린뜻3"]}
                """.trimIndent()
                val resp = aiChatClient.callJson(prompt, clazz = WrongDefinitionsResponse::class.java)
                val options = (listOf(word.koreanMeaning) + resp.wrongDefinitions).toMutableList()
                options.shuffle()
                val correctIndex = options.indexOf(word.koreanMeaning)
                QuestionData(
                    questionType = questionType,
                    wordId = word.id!!,
                    word = word.englishWord,
                    question = "다음 단어의 올바른 뜻을 고르세요.",
                    options = options,
                    correctIndex = correctIndex,
                    answer = word.koreanMeaning
                )
            }

            "synonym" -> {
                if (word.partOfSpeech == PartOfSpeech.NOUN) {
                    throw IllegalArgumentException("NOUN 품사는 유의어 문제를 지원하지 않습니다.")
                }
                val prompt = """
                    영어 단어 "${word.englishWord}" (품사: ${word.partOfSpeech.alias}, 뜻: ${word.koreanMeaning})
                    이 단어의 진짜 유의어 1개와, 유사해 보이지만 유의어가 아닌 영어 단어 3개를 생성해주세요.
                    반드시 다음 JSON 형식으로만 답변하세요:
                    {"synonym": "진짜유의어", "wrong_words": ["틀린단어1", "틀린단어2", "틀린단어3"]}
                """.trimIndent()
                val resp = aiChatClient.callJson(prompt, clazz = SynonymResponse::class.java)
                val options = (listOf(resp.synonym) + resp.wrongWords).toMutableList()
                options.shuffle()
                val correctIndex = options.indexOf(resp.synonym)
                QuestionData(
                    questionType = questionType,
                    wordId = word.id!!,
                    word = word.englishWord,
                    question = "다음 단어의 유의어를 고르세요.",
                    options = options,
                    correctIndex = correctIndex,
                    answer = resp.synonym
                )
            }

            "sentence_writing" -> {
                QuestionData(
                    questionType = questionType,
                    wordId = word.id!!,
                    word = word.englishWord,
                    question = "'${word.englishWord}'를 사용하여 영어 문장을 작성하세요.",
                    hint = word.koreanMeaning
                )
            }

            "translation" -> {
                val wordCount = when (word.wordLevel) {
                    WordLevel.BEGINNER -> "5-8"
                    WordLevel.INTERMEDIATE -> "8-12"
                    WordLevel.ADVANCED -> "12-18"
                }
                val prompt = """
                    영어 단어 "${word.englishWord}" (${word.koreanMeaning})를 영어로 번역하는 문제를 만들어주세요.
                    한국어 문장은 ${wordCount}개 단어로 구성되어야 합니다.
                    반드시 다음 JSON 형식으로만 답변하세요:
                    {"question": "번역할 한국어 문장", "ideal_answer": "영어 모범 번역"}
                """.trimIndent()
                val resp = aiChatClient.callJson(prompt, clazz = TranslationQuestionResponse::class.java)
                QuestionData(
                    questionType = questionType,
                    wordId = word.id!!,
                    word = word.englishWord,
                    question = resp.question,
                    idealAnswer = resp.idealAnswer
                )
            }

            else -> throw IllegalArgumentException("지원하지 않는 문제 유형입니다: $questionType")
        }
    }

    fun evaluate(req: EvaluateRequest): EvaluateData {
        val word = wordRepository.findById(req.wordId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 단어입니다. wordId=${req.wordId}") }

        return when (req.questionType) {
            "spelling", "anagram" -> {
                val correct = req.userAnswer.trim().equals(word.englishWord, ignoreCase = true)
                if (correct) {
                    EvaluateData(correct = true, score = 100, feedback = "정답입니다!", correctAnswer = word.englishWord)
                } else {
                    EvaluateData(correct = false, score = 0, feedback = "오답입니다. 정답은 '${word.englishWord}'입니다.", correctAnswer = word.englishWord)
                }
            }

            "word_definition", "synonym" -> {
                val correct = req.userAnswer.trim().equals(req.prompt?.trim(), ignoreCase = true)
                if (correct) {
                    EvaluateData(correct = true, score = 100, feedback = "정답입니다!", correctAnswer = req.prompt)
                } else {
                    EvaluateData(correct = false, score = 0, feedback = "오답입니다. 정답은 '${req.prompt}'입니다.", correctAnswer = req.prompt)
                }
            }

            "sentence_writing" -> {
                val prompt = """
                    영어 단어 "${word.englishWord}" (${word.koreanMeaning})를 사용한 영어 문장 작성 문제입니다.
                    학습자 답변: "${req.userAnswer}"

                    다음 기준으로 0~100점 채점해주세요:
                    1. 단어 사용: "${word.englishWord}"가 올바르게 포함되어 있는가
                    2. 문법: 영어 문법이 올바른가
                    3. 자연스러움: 자연스러운 영어 표현인가

                    60점 이상이면 correct를 true로 설정하세요.
                    feedback은 반드시 한국어로 작성하고, 비워두지 마세요. 잘된 점과 개선할 점을 구체적으로 작성하세요.
                    반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
                    {"correct": true, "score": 85, "feedback": "단어를 올바르게 사용했고 문법도 자연스럽습니다.", "correct_answer": "모범 답안 예시"}
                """.trimIndent()
                val resp = aiChatClient.callJson(prompt, clazz = GptEvaluationResponse::class.java)
                EvaluateData(
                    correct = resp.correct,
                    score = resp.score,
                    feedback = resp.feedback,
                    correctAnswer = resp.correctAnswer
                )
            }

            "translation" -> {
                val prompt = """
                    번역 문제입니다.
                    한국어 원문: "${req.prompt}"
                    학습자 번역: "${req.userAnswer}"

                    다음 기준으로 0~100점 채점해주세요:
                    1. 의미 전달: 한국어 의미를 올바르게 전달하는가
                    2. 문법: 영어 문법이 올바른가
                    3. 단어 사용: 적절한 영어 단어를 사용했는가
                    4. 자연스러움: 자연스러운 영어 표현인가

                    60점 이상이면 correct를 true로 설정하세요.
                    feedback은 반드시 한국어로 작성하고, 비워두지 마세요. 잘된 점과 개선할 점을 구체적으로 작성하세요.
                    반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
                    {"correct": true, "score": 85, "feedback": "의미 전달이 정확하고 문법도 올바릅니다.", "correct_answer": "모범 번역 예시"}
                """.trimIndent()
                val resp = aiChatClient.callJson(prompt, clazz = GptEvaluationResponse::class.java)
                EvaluateData(
                    correct = resp.correct,
                    score = resp.score,
                    feedback = resp.feedback,
                    correctAnswer = resp.correctAnswer
                )
            }

            else -> throw IllegalArgumentException("지원하지 않는 문제 유형입니다: ${req.questionType}")
        }
    }

    private fun blankMiddle(word: String): String {
        if (word.length <= 2) return word
        val middle = word.substring(1, word.length - 1)
        val blanks = middle.length / 2
        val blanked = "_".repeat(blanks) + middle.substring(blanks)
        return "${word.first()}$blanked${word.last()}"
    }

    private fun shuffleWord(word: String): String {
        if (word.length <= 1) return word
        val chars = word.toMutableList()
        var attempts = 0
        do {
            chars.shuffle()
            attempts++
        } while (chars.joinToString("") == word && attempts < 100)
        return chars.joinToString("")
    }
}

private data class WrongDefinitionsResponse(
    @JsonProperty("wrong_definitions") val wrongDefinitions: List<String>
)

private data class SynonymResponse(
    val synonym: String,
    @JsonProperty("wrong_words") val wrongWords: List<String>
)

private data class TranslationQuestionResponse(
    val question: String,
    @JsonProperty("ideal_answer") val idealAnswer: String
)

private data class GptEvaluationResponse(
    val correct: Boolean,
    val score: Int,
    val feedback: String,
    @JsonProperty("correct_answer") val correctAnswer: String? = null
)
