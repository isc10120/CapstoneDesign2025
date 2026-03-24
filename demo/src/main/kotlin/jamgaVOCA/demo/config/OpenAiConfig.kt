package jamgaVOCA.demo.config

import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry

@Configuration
@EnableRetry
class OpenAiConfig {
    // Spring AI auto-configuration은 application.yaml의 설정을 자동으로 적용합니다
    // - ChatModel: spring.ai.openai.chat.options (model, response-format 등)
    // - ImageModel: spring.ai.openai.image.options (model, size, response-format 등)
}