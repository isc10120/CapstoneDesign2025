package jamgaVOCA.demo.infra.ai

import com.fasterxml.jackson.databind.ObjectMapper
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
        val messages = buildList {
            if (systemPrompt != null) add(SystemMessage(systemPrompt))
            add(UserMessage(userPrompt))
        }
        val content = chatModel.call(Prompt(messages)).result.output.content ?: ""
        return content
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
    }

    fun <T> callJson(userPrompt: String, systemPrompt: String? = null, clazz: Class<T>): T {
        val raw = call(userPrompt, systemPrompt)
        val jsonStr = extractJson(raw)
        return objectMapper.readValue(jsonStr, clazz)
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) text.substring(start, end + 1) else text
    }
}
