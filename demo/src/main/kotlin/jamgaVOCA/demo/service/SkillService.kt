package jamgaVOCA.demo.service

import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.api.dto.SkillResponse
import jamgaVOCA.demo.domain.user.UserRepository
import jamgaVOCA.demo.domain.userwordskill.UserWordSkill
import jamgaVOCA.demo.domain.userwordskill.UserWordSkillRepository
import jamgaVOCA.demo.domain.word.WordRepository
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkillService(
    private val skillRepository: SkillRepository,
    private val userWordSkillRepository: UserWordSkillRepository,
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository,
    private val httpSession: HttpSession
) {
    fun getCollectedSkillList(): List<SkillResponse> {
        val userId =1L
        val collectedItems = userWordSkillRepository.findAllByUserId(userId)

        return collectedItems.map { item ->
            val skill = item.skill
            SkillResponse(
                skillId = skill.id!!,
                name = skill.name,
                explain = skill.explanation,
                damage = skill.damage,
                skillType = skill.skillType.name,
                lasting = skill.lasting,
                imageURL = skill.imageUrl,
                wordId = skill.word.id!!
            )
        }
    }

    fun getSkillInfo(skillId: Long): SkillResponse {
        val skill = skillRepository.findById(skillId).orElseThrow { RuntimeException("Skill not found") }
        return SkillResponse(
            skillId = skill.id!!,
            name = skill.name,
            explain = skill.explanation,
            damage = skill.damage,
            skillType = skill.skillType.name,
            lasting = skill.lasting,
            imageURL = skill.imageUrl,
            wordId = skill.word.id!!
        )
    }

    @Transactional
    fun collectSkill(skillId: Long, wordId: Long, userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }

        val word = wordRepository.findById(wordId).orElseThrow { RuntimeException("Word not found") }
        val skill = skillRepository.findById(skillId).orElseThrow { RuntimeException("Skill not found") }

        // 이미 수집된 조합인지 확인
        if (userWordSkillRepository.existsByUserIdAndWordIdAndSkillId(userId, word.id!!, skill.id!!)) {
            return
        }

        val userWordSkill = UserWordSkill(
            user = user,
            word = word,
            skill = skill
        )
        userWordSkillRepository.save(userWordSkill)
    }
}
