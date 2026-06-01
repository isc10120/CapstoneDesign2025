package jamgaVOCA.demo.service.generateQuestion

import com.fasterxml.jackson.annotation.JsonProperty
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.word.PartOfSpeech
import jamgaVOCA.demo.domain.word.WordLevel
import jamgaVOCA.demo.domain.word.WordRepository
import jamgaVOCA.demo.infra.ai.AiChatClient
import jamgaVOCA.demo.service.generateQuestion.dto.EvaluateData
import jamgaVOCA.demo.service.generateQuestion.dto.EvaluateRequest
import jamgaVOCA.demo.service.generateQuestion.dto.QuestionData
import jamgaVOCA.demo.service.generateQuestion.dto.QuestionRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class QuestionGeneratorService(
    private val wordRepository: WordRepository,
    private val aiChatClient: AiChatClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateQuestion(questionType: String, req: QuestionRequest): QuestionData {
        log.info("[QUESTION] 문제 생성 요청 - type=$questionType, wordId=${req.wordId}")
        val word = wordRepository.findById(req.wordId)
            .orElseThrow { AppException(ErrorCode.WORD_NOT_FOUND) }

        return when (questionType) {
            "spelling" -> {
                if (word.englishWord.length <= 3) {
                    throw AppException(ErrorCode.WORD_TOO_SHORT_FOR_SPELLING)
                }
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
                    주의사항:
                    - 각 틀린 뜻은 "${word.koreanMeaning}"과 의미가 명확히 달라야 합니다.
                    - 형태나 발음이 비슷한 다른 영어 단어의 뜻, 혹은 같은 어근에서 파생된 다른 품사의 뜻 등 학습자가 혼동하기 쉬운 뜻을 사용하세요.
                    - 3개의 틀린 뜻은 서로 내용이 달라야 합니다.
                    - 각 뜻은 5단어 이내의 간결한 한국어로 작성하세요.
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
                    throw AppException(ErrorCode.NOUN_NOT_SUPPORTED_FOR_SYNONYM)
                }
                val prompt = """
                    영어 단어 "${word.englishWord}" (품사: ${word.partOfSpeech.alias}, 뜻: ${word.koreanMeaning})
                    이 단어의 진짜 유의어 1개와, 유사해 보이지만 유의어가 아닌 영어 단어 3개를 생성해주세요.
                    주의사항 (IGCSE/IB Language 기준):
                    - 유의어는 "${word.englishWord}"와 반드시 같은 품사(${word.partOfSpeech.alias})여야 합니다.
                    - 유의어는 해당 맥락에서 "${word.englishWord}"와 실제로 교환 사용이 가능한 단어여야 합니다.
                    - 유의어에 "${word.englishWord}" 자체 또는 그 파생형·굴절형을 사용하지 마세요.
                    - 오답 보기 3개도 같은 품사(${word.partOfSpeech.alias})로 구성하세요.
                    - 오답 보기에 "${word.englishWord}" 자체를 포함하지 마세요.
                    - 오답 보기에 실제 유의어 관계인 단어가 섞이지 않도록, 의미적으로 관련은 있지만 혼동하기 쉬운 단어로 구성하세요.
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
                    question = "'${word.englishWord}'가 들어간 영어 문장을 작성하세요. (시제 변화·복수형 등 변형 형태도 인정됩니다.)",
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
                    영어 단어 "${word.englishWord}" (${word.koreanMeaning})가 자연스럽게 사용되는 영어 번역 문제를 만들어주세요.
                    한국어 문장은 ${wordCount}개 단어 분량으로 구성되어야 합니다.
                    주의사항:
                    - 한국어 문장은 반드시 자연스러운 순수 한국어로만 작성하세요. 영어 단어 "${word.englishWord}"를 한국어 문장 안에 직접 노출하지 마세요.
                    - 한국어 문장의 내용을 영어로 번역할 때 "${word.englishWord}"가 자연스럽게 등장해야 합니다.
                    - ideal_answer는 "${word.englishWord}"의 원형 또는 적절한 굴절형이 포함된 완전한 영어 문장이어야 합니다.
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

            else -> throw AppException(ErrorCode.UNSUPPORTED_QUESTION_TYPE)
        }
    }

    fun evaluate(req: EvaluateRequest): EvaluateData {
        log.info("[QUESTION] 채점 요청 - type=${req.questionType}, wordId=${req.wordId}")
        val word = wordRepository.findById(req.wordId)
            .orElseThrow { AppException(ErrorCode.WORD_NOT_FOUND) }

        return when (req.questionType) {
            "spelling", "anagram" -> {
                val correct = req.userAnswer.trim().equals(word.englishWord, ignoreCase = true)
                log.info("[QUESTION] 채점 완료 - type=${req.questionType}, wordId=${req.wordId}, correct=$correct")
                if (correct) {
                    EvaluateData(correct = true, score = 100, feedback = "정답입니다!", correctAnswer = word.englishWord)
                } else {
                    EvaluateData(correct = false, score = 0, feedback = "오답입니다. 정답은 '${word.englishWord}'입니다.", correctAnswer = word.englishWord)
                }
            }

            "word_definition", "synonym" -> {
                val correct = req.userAnswer.trim().equals(req.modelAnswer?.trim(), ignoreCase = true)
                log.info("[QUESTION] 채점 완료 - type=${req.questionType}, wordId=${req.wordId}, correct=$correct")
                if (correct) {
                    EvaluateData(correct = true, score = 100, feedback = "정답입니다!", correctAnswer = req.modelAnswer)
                } else {
                    EvaluateData(correct = false, score = 0, feedback = "오답입니다. 정답은 '${req.modelAnswer}'입니다.", correctAnswer = req.modelAnswer)
                }
            }

            "sentence_writing" -> {
                val prompt = """
                    영어 단어 "${word.englishWord}" (${word.koreanMeaning})를 사용한 영어 문장 작성 문제입니다.
                    단어 난이도: ${word.wordLevel.name}
                    학습자 답변: "${req.userAnswer}"

                    [채점 기준 - ESL / IGCSE Second Language / IB Language B 기준]

                    1. 어휘 사용 (40점)
                       - "${word.englishWord}"의 원형 또는 문법적으로 적절한 굴절형(시제 변화·복수형·비교급·파생형 등)이 사용되면 만점입니다.
                         예) go → went / goes / going, happy → happier / happiness
                       - 단어가 전혀 없거나 전혀 다른 단어가 쓰이면 0점입니다.
                       - 변형 형태를 사용했더라도 문맥상 올바르면 틀리다고 판정하지 마세요.

                    2. 문법 정확성 (30점)
                       - 주어-동사 일치, 시제, 전치사 등 핵심 문법 항목을 평가하세요.
                       - ESL 원칙: 의사소통에 지장이 없는 경미한 오류는 감점을 최소화하세요.

                    3. 의미 전달 및 자연스러움 (30점)
                       - IB Communicative Competence 기준: 문장이 명확한 의미를 전달하는가
                       - 자연스럽고 맥락에 적합한 영어 표현인가

                    60점 이상이면 correct를 true로 설정하세요.
                    feedback은 반드시 한국어로, IGCSE Formative Feedback 원칙에 따라 잘된 점을 먼저 언급한 후 개선할 점을 구체적으로 작성하세요.
                    correct_answer는 "${word.englishWord}"를 포함한 자연스럽고 완전한 모범 문장을 제시하세요.
                    반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
                    {"correct": true또는false, "score": 실제채점점수, "feedback": "한국어피드백", "correct_answer": "모범답안"}
                """.trimIndent()
                val resp = aiChatClient.callJson(prompt, clazz = GptEvaluationResponse::class.java)
                log.info("[QUESTION] AI 채점 완료 - type=sentence_writing, wordId=${req.wordId}, correct=${resp.correct}, score=${resp.score}")
                EvaluateData(
                    correct = resp.correct,
                    score = resp.score,
                    feedback = resp.feedback,
                    correctAnswer = resp.correctAnswer
                )
            }

            "translation" -> {
                val prompt = """
                    한국어 문장 영어 번역 채점 문제입니다.
                    핵심 어휘: "${word.englishWord}" (${word.koreanMeaning}) — 학습자의 번역에 이 단어(또는 적절한 굴절형)가 포함되어야 합니다.
                    영어 모범 번역: "${req.modelAnswer}"
                    학습자 번역: "${req.userAnswer}"

                    [채점 기준 - IGCSE Second Language English / IB Language B 기준]

                    1. 의미 전달 (40점)
                       - 모범 번역과 동일한 의미를 전달하는가
                       - IB 기준: 표현이 달라도 의미가 같으면 정답으로 인정하세요.
                       - 동의어·유사 표현을 사용한 경우에도 의미가 충분히 통하면 높은 점수를 부여하세요.

                    2. 핵심 어휘 사용 (30점)
                       - "${word.englishWord}"의 원형 또는 문법에 맞는 굴절형이 포함되어 있는가
                       - IGCSE 기준: 의미가 통하는 적절한 대체 어휘를 사용한 경우 부분 점수를 부여하세요.

                    3. 문법 및 표현 (30점)
                       - 영어 문법 정확성과 자연스러운 표현 여부

                    60점 이상이면 correct를 true로 설정하세요.
                    feedback은 반드시 한국어로, IGCSE Formative Feedback 원칙에 따라 잘된 점을 먼저 언급한 후 개선할 점을 구체적으로 작성하세요.
                    correct_answer에는 모범 번역을 그대로 제시하세요.
                    반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요:
                    {"correct": true또는false, "score": 실제채점점수, "feedback": "한국어피드백", "correct_answer": "모범번역"}
                """.trimIndent()
                val resp = aiChatClient.callJson(prompt, clazz = GptEvaluationResponse::class.java)
                log.info("[QUESTION] AI 채점 완료 - type=translation, wordId=${req.wordId}, correct=${resp.correct}, score=${resp.score}")
                EvaluateData(
                    correct = resp.correct,
                    score = resp.score,
                    feedback = resp.feedback,
                    correctAnswer = resp.correctAnswer
                )
            }

            else -> throw AppException(ErrorCode.UNSUPPORTED_QUESTION_TYPE)
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
