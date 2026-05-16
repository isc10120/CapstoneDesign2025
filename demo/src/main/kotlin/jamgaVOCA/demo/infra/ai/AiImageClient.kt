package jamgaVOCA.demo.infra.ai

import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(javaClass)

    fun requestImageBase64WithRetry(baseDesc: String): String {
        var desc = baseDesc
        val maxAttempts = 5
        var delayMillis = 15_000L

        repeat(maxAttempts) { attempt ->
            try {
                return generateImage(desc + PIXEL_STYLE)
            } catch (e: Exception) {
                log.info("Failed to generate image (attempt ${attempt}): ${e.javaClass.simpleName} - ${e.message}")

                // 안전 시스템 정책 위반 감지: 설명을 순화해서 재시도
                if (e.message?.contains("content_policy_violation") == true) {
                    log.info("Content policy violation detected for attempt $attempt, trying to modify description")
                    try {
                        desc = requestModifiedImageDesc(desc)
                    } catch (modifyE: Exception) {
                        log.error("Failed to modify description: ${modifyE.javaClass.simpleName} - ${modifyE.message}")
                    }
                }

                // 마지막 시도가 아니면 지수적 백오프
                if (attempt < maxAttempts - 1) {
                    try {
                        Thread.sleep(delayMillis)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        log.warn("Sleep interrupted during backoff", ie)
                        // 인터럽트가 걸리면 재시도를 중단
                        throw AppException(ErrorCode.AI_IMAGE_GENERATION_FAILED)
                    }
                    delayMillis *= 2L
                }
            }
        }

        throw AppException(ErrorCode.AI_IMAGE_GENERATION_FAILED)
    }

    private fun generateImage(prompt: String): String {
        val response = imageModel.call(ImagePrompt(prompt))
        return response.result.output.b64Json
            ?: throw AppException(ErrorCode.AI_IMAGE_RESPONSE_INVALID)
    }

    private fun requestModifiedImageDesc(originalDesc: String): String {
        val userPrompt = """
            다음 영어 이미지 묘사가 DALL-E의 안전 시스템에 의해 거부되었습니다.
            의미는 유지하되, 정책 위반 가능성이 있는 부분을 제거하거나 순화하여 새로운 영어 묘사만 1개 출력해주세요.
            다른 설명이나 추가적인 문구는 절대 금지합니다.

            원본 묘사: "$originalDesc"
            
            다음 JSON 형식으로 image_desc를 생성해주세요.
            { "image_desc": "영어 이미지 묘사" }
        """.trimIndent()

        return try {
            aiChatClient.callJson(userPrompt, clazz = Map::class.java)["image_desc"] as String
        } catch (e: Exception) {
            log.warn("Failed to request modified image description, returning original", e)
            originalDesc
        }
    }
}
