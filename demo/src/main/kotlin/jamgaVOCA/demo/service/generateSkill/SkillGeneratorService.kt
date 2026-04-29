package jamgaVOCA.demo.service.generateSkill

import jamgaVOCA.demo.service.generateSkill.dto.SkillData
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.word.WordRepository
import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.skill.SkillType
import jamgaVOCA.demo.infra.S3UploadService
import jamgaVOCA.demo.infra.ai.AiChatClient
import jamgaVOCA.demo.infra.ai.AiImageClient
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class SkillGeneratorService(
    private val skillRepository: SkillRepository,
    private val wordRepository: WordRepository,
    private val s3UploadService: S3UploadService,
    private val aiChatClient: AiChatClient,
    private val aiImageClient: AiImageClient
) {
    @Async("skillGenerationExecutor")
    fun generate(wordId: Long) {

        val word = wordRepository.findById(wordId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 단어입니다. wordId=$wordId") }

        if (skillRepository.existsByWordId(wordId)) return

        // TODO: 단어 난이도를 반영하도록 개선
        val skillData = requestSkillData(word.englishWord, word.koreanMeaning)

        val imageUrl = try {
            val base64 = aiImageClient.requestImageBase64WithRetry(skillData.imageDesc)
            s3UploadService.uploadBase64Image(base64)
        } catch (e: Exception) {
            ""
        }

        try {
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

        return aiChatClient.callJson(userPrompt, clazz = SkillData::class.java)
    }
}
