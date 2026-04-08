package jamgaVOCA.demo.service

import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.api.dto.CollectSkillRequest
import jamgaVOCA.demo.api.dto.SkillResponse
import jamgaVOCA.demo.api.dto.WordResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkillService(
    private val skillRepository: SkillRepository
) {
    fun getCollectedSkillList(): List<SkillResponse> {
        // 실제로는 유저별 수집 목록을 조회해야 함
        return listOf(
            SkillResponse(
                skillId = 1,
                name = "apple rain",
                explain = "사과의 비를 내려 공격한다.",
                damage = 10,
                skillType = "attack",
                lasting = null,
                imageURL = "https://example.com/s3/apple-rain.png",
                wordId = 1
            )
        )
    }

    fun getSkillInfo(skillId: Long): SkillResponse {
        val skill = skillRepository.findById(skillId).orElseThrow { RuntimeException("Skill not found") }
        return SkillResponse(
            skillId = skill.id!!,
            name = skill.name,
            explain = skill.explanation,
            damage = skill.damage,
            skillType = skill.skillType.name.lowercase(),
            lasting = skill.lasting,
            imageURL = skill.imageUrl,
            wordId = skill.word.id!!
        )
    }

    @Transactional
    fun collectSkill(request: CollectSkillRequest) {
        // PVP 성공 시 스킬 수집 처리 로직
    }

    fun getWeekCollectedList(): List<WordResponse> {
        // 그 주에 외운 단어 목록 조회 (스킬로 수집되지 않은 것 포함)
        return listOf(
            WordResponse(
                id = 1,
                word = "apple",
                definition = "사과",
                partOfSpeech = "noun",
                example = "This is an apple.",
                exampleKor = "이것은 사과이다.",
                skillId = 1
            )
        )
    }
}
