package jamgaVOCA.demo.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(OpenAiProperties::class)
class OpenAiConfig(
    private val props: OpenAiProperties
) {
    @Bean
    fun openAiWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${props.apiKey}")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .build()
}
