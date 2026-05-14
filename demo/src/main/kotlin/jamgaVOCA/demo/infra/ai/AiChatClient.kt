package jamgaVOCA.demo.infra.ai

import com.fasterxml.jackson.databind.ObjectMapper
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Component

@Component
class AiChatClient(
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper
) {
    fun call(userPrompt: String, systemPrompt: String? = null): String {
        return try {
            val messages = buildList {
                if (systemPrompt != null) add(SystemMessage(systemPrompt))
                add(UserMessage(userPrompt))
            }
            val content = chatModel.call(Prompt(messages)).result.output.content ?: ""
            content
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
        } catch (e: Exception) {
            throw AppException(ErrorCode.AI_CHAT_CALL_FAILED)
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
            throw AppException(ErrorCode.AI_JSON_PARSE_FAILED)
        }
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) text.substring(start, end + 1) else text
    }
}
