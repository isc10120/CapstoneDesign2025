package jamgaVOCA.demo.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "openai")
data class OpenAiProperties(
    val apiKey: String,
    val chatModel: String,
    val imageModel: String
)
