package jamgaVOCA.demo.legacy.translation

import com.fasterxml.jackson.databind.ObjectMapper
import jamgaVOCA.demo.legacy.translation.dto.CreateQuestionRequest
import jamgaVOCA.demo.legacy.translation.dto.CreateQuestionResponse
import jamgaVOCA.demo.legacy.translation.dto.EvaluateRequest
import jamgaVOCA.demo.legacy.translation.dto.EvaluateResponse
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

@Service
class TranslationService(
    private val chatModel: ChatModel
) {

    private val mapper = ObjectMapper()

    private fun callGpt(prompt: String, system: String? = null): String {
        val messages = mutableListOf<org.springframework.ai.chat.messages.Message>()
        system?.let { messages.add(SystemMessage(it)) }
        messages.add(UserMessage(prompt))

        val response = chatModel.call(Prompt(messages))
        var content = response.result.output.content ?: ""

        // Markdown JSON block 제거
        if (content.startsWith("```json")) {
            content = content.removePrefix("```json").removeSuffix("```").trim()
        } else if (content.startsWith("```")) {
            content = content.removePrefix("```").removeSuffix("```").trim()
        }
        
        return content
    }

    /**
     * Step 1: 번역 문제 생성
     */
    fun createQuestion(req: CreateQuestionRequest): CreateQuestionResponse {
        val levelDesc = mapOf(
            "beginner" to "초등학생도 이해할 수 있는 쉽고 짧은 문장 (5-8단어)",
            "intermediate" to "고등학생 수준의 일상적인 문장 (8-12단어)",
            "advanced" to "대학생/성인 수준의 복잡한 문장 (12-18단어)"
        )

        val prompt = """
영어 단어 '${req.targetWord}'를 사용해야 하는 한국어 문장을 만들어주세요.

난이도: ${levelDesc[req.userLevel] ?: levelDesc["intermediate"]}

요구사항:
1. 한국어 문장은 자연스럽고 일상적이어야 함
2. 영어로 번역할 때 반드시 '${req.targetWord}' 단어가 사용되어야 함
3. 모범 영어 번역도 함께 제공

반드시 아래 JSON 형식으로만 응답하세요:
{"korean_sentence":"...", "ideal_translation":"...", "word_hint":"..."}
        """.trimIndent()

        return try {
            val raw = callGpt(prompt)
            val json = mapper.readTree(raw)

            CreateQuestionResponse(
                success = true,
                koreanSentence = json["korean_sentence"].asText(),
                targetWord = req.targetWord,
                wordHint = json["word_hint"].asText(),
                ideal = json["ideal_translation"].asText()
            )
        } catch (e: Exception) {
            CreateQuestionResponse(success = false, error = e.message)
        }
    }

    /**
     * Step 2: 사용자 번역 평가
     */
    fun evaluate(req: EvaluateRequest): EvaluateResponse {

        val system = """
당신은 영어 학습 앱의 번역 평가자입니다.
규칙:
1. 반드시 JSON만 응답
2. 0-100점
3. 피드백은 한국어
        """.trimIndent()

        val prompt = """
[평가 대상]
- 한국어 원문: ${req.koreanSentence}
- 학습 단어: ${req.targetWord}
- 사용자 번역: ${req.userAnswer}
- 모범 답안: ${req.idealTranslation}

JSON:
{"score":0,"breakdown":{"meaning":0,"grammar":0,"word_usage":0,"naturalness":0},"feedback":"...","correction":null}
        """.trimIndent()

        return try {
            val raw = callGpt(prompt, system)
            val json = mapper.readTree(raw)

            EvaluateResponse(
                success = true,
                score = json["score"].asInt(),
                breakdown = json["breakdown"].fields().asSequence()
                    .associate { it.key to it.value.asInt() },
                feedback = json["feedback"].asText(),
                correction = json["correction"]?.takeIf { !it.isNull }?.asText(),
                idealAnswer = req.idealTranslation
            )
        } catch (e: Exception) {
            EvaluateResponse(success = false, error = e.message)
        }
    }
}
