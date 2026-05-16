package jamgaVOCA.demo.infra.ai

import com.fasterxml.jackson.databind.ObjectMapper
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class AiChatClient(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun call(userPrompt: String, systemPrompt: String? = null): String {
        try {
            val messages = buildList {
                if (systemPrompt != null) add(SystemMessage(systemPrompt))
                add(UserMessage(userPrompt))
            }
            val content = chatModel.call(Prompt(messages)).result.output.text ?: ""
            return content
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            when {
                msg.contains("http 429") || msg.contains("rate limit") || msg.contains("quota") || msg.contains("limit") ->
                    throw AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED)
                msg.contains("http 401") || msg.contains("http 403") ->
                    throw AppException(ErrorCode.AI_AUTHENTICATION_FAILED)
                else ->
                    throw AppException(ErrorCode.AI_CHAT_CALL_FAILED)
            }
        }
    }

    fun <T> callJson(userPrompt: String, systemPrompt: String? = null, clazz: Class<T>): T {
        return try {
            val raw = call(userPrompt, systemPrompt)
            val jsonStr = extractJson(raw)
            objectMapper.readValue(jsonStr, clazz)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            log.warn("Failed to parse AI JSON response: ${e.javaClass.simpleName} - ${e.message}")
            throw AppException(ErrorCode.AI_JSON_PARSE_FAILED)
        }
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) text.substring(start, end + 1) else text
    }
}
