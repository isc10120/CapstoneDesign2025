package jamgaVOCA.demo.legacy.generateSkill

import jamgaVOCA.demo.legacy.generateSkill.dto.ImageStatus
import jamgaVOCA.demo.legacy.generateSkill.dto.SkillData
import jamgaVOCA.demo.legacy.generateSkill.dto.SkillGenerateResponse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImagePrompt
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class SkillGeneratorService(
    private val repo: SkillLegacyRepository,
    private val chatModel: ChatModel,
    private val imageModel: ImageModel
) {
    private val mapper = jacksonObjectMapper()

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
                imageBase64 = existing.imageBase64,
                imageStatus = if (existing.imageBase64.isNullOrBlank()) ImageStatus.FAILED else ImageStatus.SUCCESS,
                imageError = if (existing.imageBase64.isNullOrBlank()) "DB에 이미지가 저장되어 있지 않습니다." else null
            )
        }

        // 1) ChatGPT로 스킬 생성
        val skillData = requestSkillData(word, meaningKo)

        // 2) 먼저 엔티티 저장 (이미지는 아래에서 채움)
        val saved = try {
            repo.save(
                SkillEntity(
                    word = word,
                    name = skillData.name,
                    description = skillData.description,
                    damage = skillData.damage,
                    imageDesc = skillData.imageDesc,
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
            val imageBase64 = requestImageBase64WithRetry(skillData.imageDesc)

            saved.imageBase64 = imageBase64
            val updated = repo.save(saved)

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
                imageError = e.message
            )
        }
    }

    /**
     * ===== Spring AI ChatModel 기반 스킬 생성 =====
     */
    private fun requestSkillData(word: String, meaningKo: String): SkillData {
        val userPrompt =
            """
            당신은 2D 픽셀 RPG 게임의 스킬 디자이너입니다.

            단어 '${word}'와 그 뜻인 '${meaningKo}'를 넣어 RPG 스킬을 창작해줘.
            반드시 아래 JSON 형식으로 답변. 다른 말 금지, 항목 누락 금지.

            { "name": "스킬명", "description": "설명", "damage": "숫자", "image_desc": "짧은 영어 이미지 묘사" }

            name에는 '${word}' 원형이 반드시 포함되어야 하며, 뜻이 비슷한 다른 단어로 대체하거나 해서는 안 된다.
            name은 최대 2단어의 영어이다. 게임의 '주문' 처럼, 외치기 좋은 형식이어야 한다.
            name을 지을 때, Strike, Blast, 와 같은 기본적인 단어들을 뒤에 붙이기보다는, 최대한 창의성을 발휘하여 뻔하지 않은 RPG 게임 공격 기술이 되도록 해라.

            description은, 만든 스킬 이름 기반으로, 주어진 '${word}'의 '뜻'인 '${meaningKo}'을 응용하여 멋지거나 웃긴 공격 기술이 되도록 한국어로 작성하라.
            반드시 '${meaningKo}'을 문장 안에 자연스럽게 포함해야 한다. 또한, 문장 안에 특수문자를 넣지 말 것.(특히 *)
            description의 말투는 ~다. 로 통일하며, 가급적 한 문장으로 완성하라.

            damage는 10~100 사이 숫자. damage를 정할 때엔 확실한 차등 기준을 두어, 더 강력한 공격에 더 높은 값을 부여하라. 기본값은 30데미지

            image_desc는 스킬 이펙트의 모습을 묘사하여, 영어로 작성하라. 2D 픽셀 RPG 게임에 적합한 도트 이펙트여야 한다.
            image_desc의 경우, DALL-E API 오류 (코드: content_policy_violation) 을 피할 수 있도록, Dall-e의 safety system 규정을 준수하고, 자극적인 말은 피해서 작성하라.

            결과 JSON은 반드시 name, description, damage, image_desc 네 개 항목 모두 포함.
            """.trimIndent()

        val prompt = Prompt(userPrompt)
        val response = chatModel.call(prompt)
        val content = response.result.output.content

        return mapper.readValue(content, SkillData::class.java)
    }

    /**
     * ===== Spring AI ImageClient 기반 이미지 생성 (재시도 + 프롬프트 수정 로직) =====
     *
     * 전략:
     * - 1~3회: 단순 재시도 (2초 대기)
     * - 4회차: 프롬프트를 안전 버전으로 수정 후 재시도
     * - 5회차: 수정된 프롬프트로 최종 시도
     */
    private fun requestImageBase64WithRetry(baseDesc: String): String {
        val pixelStyle = " 2D 픽셀 아트, 배경은 없고, 글자가 절대 포함되지 않은, RPG 게임 스킬 이펙트 그림, 과하지 않고 간결한"

        // 1~3회: 원본 프롬프트로 재시도
        repeat(3) { attempt ->
            try {
                return generateImage(baseDesc + pixelStyle)
            } catch (e: Exception) {
                if (attempt < 2) Thread.sleep(2000)
            }
        }

        // 4~5회: 프롬프트 수정 후 재시도
        val modifiedDesc = requestModifiedImageDesc(baseDesc)
        repeat(2) { attempt ->
            try {
                return generateImage(modifiedDesc + pixelStyle)
            } catch (e: Exception) {
                if (attempt < 1) Thread.sleep(2000)
            }
        }

        throw IllegalStateException("DALL·E 최대 재시도(5회) 초과")
    }

    private fun generateImage(prompt: String): String {
        val imagePrompt = ImagePrompt(prompt)
        val response = imageModel.call(imagePrompt)

        return response.result.output.b64Json
            ?: throw IllegalStateException("DALL·E 응답에 b64_json이 없습니다.")
    }

    private fun requestModifiedImageDesc(originalDesc: String): String {
        val userPrompt =
            """
            당신은 DALL-E 이미지 생성기의 안전 시스템을 우회하기 위해 프롬프트를 수정하는 전문 컨설턴트입니다.

            다음 영어 이미지 묘사가 DALL-E의 안전 시스템에 의해 거부되었습니다.
            의미는 유지하되, 정책 위반 가능성이 있는 부분을 제거하거나 순화하여 새로운 영어 묘사만 1개 출력해주세요.
            다른 설명이나 추가적인 문구는 절대 금지합니다.

            원본 묘사: "$originalDesc"
            """.trimIndent()

        return try {
            val prompt = Prompt(userPrompt)
            val response = chatModel.call(prompt)
            val content = response.result.output.content

            content.trim().replace(Regex("^\"|\"$"), "")
        } catch (e: Exception) {
            "$originalDesc, safe version"
        }
    }
}
