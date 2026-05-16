package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.api.dto.SkillResponse
import jamgaVOCA.demo.domain.userwordskill.UserWordSkill
import jamgaVOCA.demo.domain.userwordskill.UserWordSkillRepository
import jamgaVOCA.demo.domain.word.WordRepository
import jamgaVOCA.demo.service.generateSkill.SkillGeneratorService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkillService(
    private val skillRepository: SkillRepository,
    private val userWordSkillRepository: UserWordSkillRepository,
    private val userService: UserService,
    private val wordRepository: WordRepository,
    private val skillGeneratorService: SkillGeneratorService
) {
    fun getCollectedSkillList(userId: Long): List<SkillResponse> {
        val collectedItems = userWordSkillRepository.findAllByUserId(userId)

        return collectedItems.map { item ->
            val skill = item.skill
            // 이미지가 없으면 재생성 시도
            if (skill.imageUrl.isBlank()) {
                skillGeneratorService.generateImage(skill.id!!)
            }
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

    fun getSkillByWordId(wordId: Long): Skill? {
        val skill = skillRepository.findByWordId(wordId)

        // 스킬이 없으면 생성 시도
        if (skill == null) {
            skillGeneratorService.generate(wordId)
            return null  // 비동기 생성 중이므로 null 반환
        }

        // 이미지가 없으면 재생성 시도
        if (skill.imageUrl.isBlank()) {
            skillGeneratorService.generateImage(skill.id!!)
        }

        return skill
    }

    fun getSkillInfo(skillId: Long): SkillResponse {
        val skill = skillRepository.findById(skillId).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }

        // 이미지가 없으면 재생성 시도
        if (skill.imageUrl.isBlank()) {
            skillGeneratorService.generateImage(skillId)
        }

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
        val user = userService.getUser(userId)

        val word = wordRepository.findById(wordId).orElseThrow { AppException(ErrorCode.WORD_NOT_FOUND) }
        val skill = skillRepository.findById(skillId).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }

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
