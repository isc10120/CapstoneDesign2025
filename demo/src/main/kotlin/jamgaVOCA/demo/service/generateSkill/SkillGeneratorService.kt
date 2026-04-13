package jamgaVOCA.demo.service.generateSkill

import jamgaVOCA.demo.service.generateSkill.dto.SkillData
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.word.WordRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.skill.SkillType
import jamgaVOCA.demo.infra.S3UploadService
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImagePrompt
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class SkillGeneratorService(
    private val skillRepository: SkillRepository,
    private val wordRepository: WordRepository,
    private val s3UploadService: S3UploadService,
    private val chatModel: ChatModel,
    private val imageModel: ImageModel
) {
    private val mapper = jacksonObjectMapper()

    @Async("skillGenerationExecutor")
    fun generate(wordId: Long) {

        val word = wordRepository.findById(wordId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 단어입니다. wordId=$wordId") }

        // 이미 생성된 스킬이면 스킵
        if (skillRepository.existsByWordId(wordId)) return

        // GPT로 스킬 데이터 생성
        // TODO: 단어 난이도를 반영하도록 개선
        val skillData = requestSkillData(word.englishWord, word.koreanMeaning)

        // 이미지 생성 및 업로드
        val imageUrl = try {
            val base64 = requestImageBase64WithRetry(skillData.imageDesc)
            s3UploadService.uploadBase64Image(base64)
        } catch (e: Exception) {
            ""  // 실패 시 빈 값, 나중에 재시도 가능하도록
        }

        // Skill 엔티티 저장
        val saved = try {
            skillRepository.save(
                Skill(
                    word = word,
                    name = skillData.name,
                    explanation = skillData.description,
                    damage = skillData.damage,
                    skillType = SkillType.ATTACK,   // TODO: GPT로 스킬 타입도 생성하도록 개선
                    lasting = null,
                    imageUrl = imageUrl
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청 충돌 시 무시 (이미 저장된 것으로 간주) 애초에 중복 생성하지 않도록 세마포어?
            return
        }
    }

    private fun requestSkillData(word: String, meaningKo: String): SkillData {
        val userPrompt = """
            당신은 2D 픽셀 RPG 게임의 스킬 디자이너입니다.
            단어 '${word}'와 그 뜻인 '${meaningKo}'를 넣어 RPG 스킬을 창작해줘.
            반드시 아래 JSON 형식으로 답변. 다른 말 금지, 항목 누락 금지.

            { "name": "스킬명", "description": "설명", "damage": "숫자", "image_desc": "짧은 영어 이미지 묘사" }

            name에는 '${word}'이 반드시 포함되어야 하며, 뜻이 비슷한 다른 단어로 대체하거나 해서는 안 된다.
            name은 최대 2단어의 영어이다. 게임의 '주문' 처럼, 외치기 좋은 형식이어야 한다.
            name을 지을 때, Strike, Blast 와 같은 기본적인 단어들을 뒤에 붙이기보다는, 최대한 창의성을 발휘하여 뻔하지 않은 RPG 게임 공격 기술이 되도록 해라.

            description은 만든 스킬 이름 기반으로, '${word}'의 뜻인 '${meaningKo}'을 응용하여 멋지거나 웃긴 공격 기술이 되도록 한국어로 작성하라.
            반드시 '${meaningKo}'을 문장 안에 자연스럽게 포함해야 한다. 또한, 문장 안에 특수문자를 넣지 말 것.(특히 *)
            description의 말투는 ~다. 로 통일하며, 가급적 한 문장으로 완성하라.

            damage는 10~100 사이 숫자. 더 강력한 공격에 더 높은 값을 부여하라. 기본값은 30.

            image_desc는 스킬 이펙트의 모습을 묘사하여 영어로 작성하라. 2D 픽셀 RPG 게임에 적합한 도트 이펙트여야 한다.
            image_desc의 경우, DALL-E API 오류 (코드: content_policy_violation)을 피할 수 있도록 Dall-e의 safety system 규정을 준수하고, 자극적인 말은 피해서 작성하라.

            결과 JSON은 반드시 name, description, damage, image_desc 네 개 항목 모두 포함.
        """.trimIndent()

        val response = chatModel.call(Prompt(userPrompt))
        val content = response.result.output.content
        return mapper.readValue(content, SkillData::class.java)
    }

    private fun requestImageBase64WithRetry(baseDesc: String): String {
        val pixelStyle = " 2D 픽셀 아트, 배경은 없고, 글자가 절대 포함되지 않은, RPG 게임 스킬 이펙트 그림, 과하지 않고 간결한"

        repeat(3) { attempt ->
            try {
                return generateImage(baseDesc + pixelStyle)
            } catch (e: Exception) {
                if (attempt < 2) Thread.sleep(2000)
            }
        }

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
        val response = imageModel.call(ImagePrompt(prompt))
        return response.result.output.b64Json
            ?: throw IllegalStateException("DALL·E 응답에 b64_json이 없습니다.")
    }

    private fun requestModifiedImageDesc(originalDesc: String): String {
        val userPrompt = """
            당신은 DALL-E 이미지 생성기의 안전 시스템을 우회하기 위해 프롬프트를 수정하는 전문 컨설턴트입니다.
            다음 영어 이미지 묘사가 DALL-E의 안전 시스템에 의해 거부되었습니다.
            의미는 유지하되, 정책 위반 가능성이 있는 부분을 제거하거나 순화하여 새로운 영어 묘사만 1개 출력해주세요.
            다른 설명이나 추가적인 문구는 절대 금지합니다.

            원본 묘사: "$originalDesc"
        """.trimIndent()

        return try {
            val response = chatModel.call(Prompt(userPrompt))
            response.result.output.content.trim().replace(Regex("^\"|\"$"), "")
        } catch (e: Exception) {
            originalDesc
        }
    }
}