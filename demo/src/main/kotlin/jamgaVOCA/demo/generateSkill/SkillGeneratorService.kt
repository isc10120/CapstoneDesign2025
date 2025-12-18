package jamgaVOCA.demo.generateSkill

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jamgaVOCA.demo.generateSkill.dto.ImageStatus
import jamgaVOCA.demo.generateSkill.dto.SkillGenerateResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Service
class SkillGeneratorService(
    private val repo: SkillRepository
) {
    private val mapper = ObjectMapper()
    private val client = HttpClient.newHttpClient()

    private val CHAT_API_URL = "https://api.openai.com/v1/chat/completions"
    private val IMAGE_API_URL = "https://api.openai.com/v1/images/generations"

    private val OPENAI_API_KEY = System.getenv("OPENAI_API_KEY")
        ?: throw IllegalStateException("OPENAI_API_KEY not set")

    fun generate(word: String, meaningKo: String): SkillGenerateResponse {

        // ✅ 0) 이미 DB에 있으면 (이미지까지) DB 값 그대로 응답
        repo.findByWord(word)?.let { existing ->
            return SkillGenerateResponse(
                id = existing.id!!,
                word = existing.word,
                name = existing.name,
                description = existing.description,
                damage = existing.damage,
                imageDesc = existing.imageDesc,
                imageBase64 = existing.imageBase64, // ✅ 재사용
                imageStatus = if (existing.imageBase64.isNullOrBlank()) ImageStatus.FAILED else ImageStatus.SUCCESS,
                imageError = if (existing.imageBase64.isNullOrBlank()) "DB에 이미지가 저장되어 있지 않습니다." else null
            )
        }

        // 1) ChatGPT로 스킬 생성
        val skillJson = requestSkillData(word, meaningKo)

        val name = skillJson["name"].asText()
        val description = skillJson["description"].asText()
        val damage = skillJson["damage"].asInt()
        val imageDesc = skillJson["image_desc"].asText()

        // 2) 먼저 엔티티 저장 (이미지는 아래에서 채움)
        val saved = try {
            repo.save(
                SkillEntity(
                    word = word,
                    name = name,
                    description = description,
                    damage = damage,
                    imageDesc = imageDesc,
                    imageBase64 = null
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // 동시성: 누가 먼저 word를 저장했을 수 있으니 조회해서 그걸 반환
            val existing = repo.findByWord(word)
                ?: throw IllegalStateException("DB 저장 충돌 후 word로 조회되지 않습니다: $word", e)

            return SkillGenerateResponse(
                id = existing.id!!,
                word = existing.word,
                name = existing.name,
                description = existing.description,
                damage = existing.damage,
                imageDesc = existing.imageDesc,
                imageBase64 = existing.imageBase64,
                imageStatus = if (existing.imageBase64.isNullOrBlank()) ImageStatus.FAILED else ImageStatus.SUCCESS,
                imageError = if (existing.imageBase64.isNullOrBlank()) "DB에 이미지가 저장되어 있지 않습니다." else null
            )
        }

        // 3) 이미지 생성 (재시도/수정 포함) → 성공하면 DB에 저장
        return try {
            val imageBase64 = requestImageBase64WithRetry(imageDesc)

            saved.imageBase64 = imageBase64
            val updated = repo.save(saved) // ✅ base64 반영

            SkillGenerateResponse(
                id = updated.id!!,
                word = updated.word,
                name = updated.name,
                description = updated.description,
                damage = updated.damage,
                imageDesc = updated.imageDesc,
                imageBase64 = updated.imageBase64,
                imageStatus = ImageStatus.SUCCESS,
                imageError = null
            )
        } catch (e: Exception) {
            // 실패했으면 base64는 null인 채로 DB에 남음
            SkillGenerateResponse(
                id = saved.id!!,
                word = saved.word,
                name = saved.name,
                description = saved.description,
                damage = saved.damage,
                imageDesc = saved.imageDesc,
                imageBase64 = null,
                imageStatus = ImageStatus.FAILED,
                imageError = e.message // ✅ 프론트 확인용으로 전문 유지
            )
        }
    }

    /**
     * ===== ChatGPT 스킬 생성 (프롬프트 유지) =====
     */
    private fun requestSkillData(word: String, meaningKo: String): JsonNode {
        val systemPrompt =
            "당신은 2D 픽셀 RPG 게임의 스킬 디자이너입니다. 사용자가 제공한 단어와 그 뜻을 사용하여 스킬을 창작하고, 반드시 JSON 형식으로만 답변해야 합니다."

        val userPrompt =
            "단어 '${word}'와 그 뜻인 '${meaningKo}'를 넣어 RPG 스킬을 창작해줘. " +
                    "반드시 아래 JSON 형식으로 답변. 다른 말 금지, 항목 누락 금지. " +
                    "{ \"name\": \"스킬명\", \"description\": \"설명\", \"damage\": \"숫자\", \"image_desc\": \"짧은 영어 이미지 묘사\" } " +
                    "name에는 '${word}' 원형이 반드시 포함되어야 하며, 뜻이 비슷한 다른 단어로 대체하거나 해서는 안 된다. 꼭 '${word}'을 포함하여 짓도록 해라!!!" +
                    "name은 최대 2단어의 영어이다. 게임의 '주문' 처럼, 외치기 좋은 형식이어야 한다. " +
                    "name을 지을 때, Strike, Blast, 와 같은 기본적인 단어들을 뒤에 붙이기보다는, 최대한 창의성을 발휘하여 뻔하지 않은 RPG 게임 공격 기술이 되도록 해라." +
                    "description은, 만든 스킬 이름 기반으로, 주어진 '${word}'의 '뜻'인 '${meaningKo}'을 응용하여 멋지거나 웃긴 공격 기술이 되도록 한국어로 작성하라." +
                    "반드시 '${meaningKo}'을 문장 안에 자연스럽게 포함해야 한다. 또한, 문장 안에 특수문자를 넣지 말 것.(특히 *) " +
                    "description의 말투는 ~다. 로 통일하며, 가급적 한 문장으로 완성하라. 문장을 읽는 데에 어색함이 없어야 한다. 검수하고 또 검수해서 작성할 것" +
                    "damage는 10~100 사이 숫자. damage를 정할 때엔 확실한 차등 기준을 두어, 더 강력한 공격에 더 높은 값을 부여하라. 기본값은 30데미지" +
                    "image_desc는 스킬 이펙트의 모습을 묘사하여, 영어로 작성하라. 2D 픽셀 RPG 게임에 적합한 도트 이펙트여야 한다. 명료하고 명확하며 스킬의 분위기가 잘 드러나도록 작성해라. " +
                    "결과 JSON은 반드시 name, description, damage, image_desc 네 개 항목 모두 포함." +
                    "image_desc의 경우, DALL-E API 오류 (코드: content_policy_violation) 을 피할 수 있도록, Dall-e의 safety system 규정을 준수하고, 자극적인 말은 피해서 작성하라.";

        val jsonBody = """
            {
              "model": "gpt-3.5-turbo",
              "messages": [
                { "role": "system", "content": ${mapper.writeValueAsString(systemPrompt)} },
                { "role": "user", "content": ${mapper.writeValueAsString(userPrompt)} }
              ],
              "response_format": { "type": "json_object" }
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(CHAT_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $OPENAI_API_KEY")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw IllegalStateException("ChatGPT API 오류: ${response.statusCode()} ${response.body()}")
        }

        val root = mapper.readTree(response.body())
        val content = root["choices"][0]["message"]["content"].asText()
        return mapper.readTree(content)
    }

    /**
     * ===== DALL·E 이미지 생성: Java 로직과 동일한 재시도 플로우 =====
     *
     * - 최대 5회
     * - 1~3회: 단순 재시도(2초 sleep)
     * - 4회차 진입 시: ChatGPT로 prompt를 "안전 규정 준수" 형태로 보수적 수정 후 계속 진행
     * - 400 + content_policy_violation/invalid_request_error: 재시도
     * - 그 외 400: 즉시 중단
     * - 200이면 b64_json 반환
     */
    private fun requestImageBase64WithRetry(baseDesc: String): String {
        val pixelStyle =
            "2D 픽셀 아트, 배경은 없고, 글자가 절대 포함되지 않은, RPG 게임 스킬 이펙트 그림, 과하지 않고 간결한"

        var currentBaseDesc = baseDesc

        val maxRetries = 5
        var attempt = 0

        while (attempt < maxRetries) {
            attempt++

            if (attempt == 4 && attempt < maxRetries) {
                // Java 코드: 3회 실패 후 프롬프트 수정 요청
                currentBaseDesc = requestModifiedImageDesc(currentBaseDesc)
            }

            val finalPrompt = currentBaseDesc + pixelStyle

            val jsonBody = """
                {
                  "model": "dall-e-3",
                  "prompt": ${mapper.writeValueAsString(finalPrompt)},
                  "n": 1,
                  "size": "1024x1024",
                  "response_format": "b64_json"
                }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(IMAGE_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $OPENAI_API_KEY")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val root = mapper.readTree(response.body())
                val b64 = root["data"][0]["b64_json"]?.asText()
                if (b64.isNullOrBlank()) {
                    throw IllegalStateException("DALL·E 응답에 b64_json이 없습니다.")
                }
                return b64
            }

            // 400 처리: Java 코드와 동일하게 error code 보고 분기
            if (response.statusCode() == 400) {
                val root = mapper.readTree(response.body())
                val errorCode = root.path("error").path("code").asText()

                if (errorCode == "content_policy_violation" || errorCode == "invalid_request_error") {
                    if (attempt >= maxRetries) {
                        throw IllegalStateException("DALL·E 최대 재시도 초과 (code=$errorCode).")
                    }
                    // 1~3회는 단순 재시도 텀(자바 코드와 동일하게 2초)
                    if (attempt < 3) Thread.sleep(2000)
                    continue
                }

                // 그 외 400은 즉시 중단
                throw IllegalStateException("DALL·E 400 오류(code=$errorCode).")
            }

            // 그 외 status는 중단(원하면 재시도 정책 바꿔도 됨)
            throw IllegalStateException("DALL·E API 오류: ${response.statusCode()}")
        }

        throw IllegalStateException("DALL·E 최대 재시도 초과.")
    }

    private fun requestModifiedImageDesc(originalDesc: String): String {
        val systemPrompt =
            "당신은 DALL-E 이미지 생성기의 안전 시스템을 우회하기 위해 프롬프트를 수정하는 전문 컨설턴트입니다."

        val userPrompt =
            "다음 영어 이미지 묘사가 DALL-E의 안전 시스템에 의해 거부되었습니다. 의미는 유지하되, 정책 위반 가능성이 있는 부분을 제거하거나 순화하여 새로운 영어 묘사만 1개 출력해주세요. 다른 설명이나 추가적인 문구는 절대 금지합니다. 원본 묘사: \"${originalDesc}\""

        val jsonBody = """
            {
              "model": "gpt-3.5-turbo",
              "messages": [
                { "role": "system", "content": ${mapper.writeValueAsString(systemPrompt)} },
                { "role": "user", "content": ${mapper.writeValueAsString(userPrompt)} }
              ]
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(CHAT_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $OPENAI_API_KEY")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 200) {
            val root = mapper.readTree(response.body())
            val modifiedText = root.path("choices").get(0).path("message").path("content").asText()
            // Java 코드처럼 양끝 따옴표 제거
            return modifiedText.trim().replace(Regex("^\"|\"$"), "")
        }

        // 수정 실패하면 원본 기반으로 매우 보수적인 suffix만 붙여서 반환
        return originalDesc + ", safe version"
    }
}
