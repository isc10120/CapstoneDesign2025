package jamgaVOCA.demo.infra.ai

import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImagePrompt
import org.springframework.stereotype.Component

@Component
class AiImageClient(
    private val imageModel: ImageModel,
    private val aiChatClient: AiChatClient
) {
    companion object {
        private const val PIXEL_STYLE = " 2D pixel art, no background, no text, RPG game skill effect, simple and clean"
    }

    fun requestImageBase64WithRetry(baseDesc: String): String {
        repeat(3) { attempt ->
            try {
                return generateImage(baseDesc + PIXEL_STYLE)
            } catch (e: Exception) {
                if (attempt < 2) Thread.sleep(2000)
            }
        }

        val modifiedDesc = requestModifiedImageDesc(baseDesc)
        repeat(2) { attempt ->
            try {
                return generateImage(modifiedDesc + PIXEL_STYLE)
            } catch (e: Exception) {
                if (attempt < 1) Thread.sleep(2000)
            }
        }

        throw IllegalStateException("DALL·E 최대 재시도(5회) 초과")
    }

    private fun generateImage(prompt: String): String {
        val response = imageModel.call(ImagePrompt(prompt))
        return response.result.output.b64Json
            ?: throw IllegalStateException("DALL·E 응답에 b64_json이 없습니다.")
    }

    private fun requestModifiedImageDesc(originalDesc: String): String {
        val userPrompt = """
            다음 영어 이미지 묘사가 DALL-E의 안전 시스템에 의해 거부되었습니다.
            의미는 유지하되, 정책 위반 가능성이 있는 부분을 제거하거나 순화하여 새로운 영어 묘사만 1개 출력해주세요.
            다른 설명이나 추가적인 문구는 절대 금지합니다.

            원본 묘사: "$originalDesc"
        """.trimIndent()

        return try {
            aiChatClient.call(userPrompt).trim().replace(Regex("^\"|\"$"), "")
        } catch (e: Exception) {
            originalDesc
        }
    }
}
