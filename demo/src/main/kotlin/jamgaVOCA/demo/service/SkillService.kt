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
import jamgaVOCA.demo.infra.ImageColorExtractor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkillService(
    private val skillRepository: SkillRepository,
    private val userWordSkillRepository: UserWordSkillRepository,
    private val userService: UserService,
    private val wordRepository: WordRepository,
    private val skillGeneratorService: SkillGeneratorService,
    private val imageColorExtractor: ImageColorExtractor
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getCollectedSkillList(userId: Long): List<SkillResponse> {
        val collectedItems = userWordSkillRepository.findAllByUserId(userId)
        log.debug("[SKILL] 수집 스킬 목록 조회 - userId=$userId, count=${collectedItems.size}")

        return collectedItems.map { item ->
            val skill = item.skill
            if (skill.imageUrl.isBlank()) {
                log.info("[SKILL] 이미지 없는 스킬 감지, 재생성 요청 - skillId=${skill.id}, name=${skill.name}")
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
        val skills = skillRepository.findAllByWordId(wordId)

        if (skills.isEmpty()) {
            log.info("[SKILL] 스킬 없음, 비동기 생성 요청 - wordId=$wordId")
            skillGeneratorService.generate(wordId)
            return null
        }

        val skill = skills.first()

        if (skills.size > 1) {
            log.warn("[SKILL] 중복 스킬 감지 - wordId=$wordId, count=${skills.size}, firstSkillId=${skill.id}")
        }

        if (skill.imageUrl.isBlank()) {
            log.info("[SKILL] 이미지 없는 스킬 감지, 재생성 요청 - skillId=${skill.id}, wordId=$wordId")
            skillGeneratorService.generateImage(skill.id!!)
        }
        else if (skill.dominantColor.isNullOrBlank()) {
            log.info("[SKILL] 대표 색상 없는 스킬 감지, 색상 추출 요청 - skillId=${skill.id}, wordId=$wordId")
            skill.dominantColor = imageColorExtractor.extractFromUrl(skill.imageUrl)
            skillRepository.save(skill)
        }

        return skill
    }

    fun getSkillInfo(skillId: Long): SkillResponse {
        val skill = getSkillEntity(skillId)

        if (skill.imageUrl.isBlank()) {
            log.info("[SKILL] 이미지 없는 스킬 감지, 재생성 요청 - skillId=$skillId")
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

    fun getSkillEntity(skillId: Long): Skill =
        skillRepository.findById(skillId).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }

    @Transactional
    fun collectSkill(skillId: Long, wordId: Long, userId: Long) {
        val user = userService.getUser(userId)

        val word = wordRepository.findById(wordId).orElseThrow { AppException(ErrorCode.WORD_NOT_FOUND) }
        val skill = skillRepository.findById(skillId).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }

        if (userWordSkillRepository.existsByUserIdAndWordIdAndSkillId(userId, word.id!!, skill.id!!)) {
            log.debug("[SKILL] 이미 수집된 스킬, 스킵 - userId=$userId, skillId=$skillId, wordId=$wordId")
            return
        }

        val userWordSkill = UserWordSkill(
            user = user,
            word = word,
            skill = skill
        )
        userWordSkillRepository.save(userWordSkill)
        userService.addExp(userId, 15)
        log.info("[SKILL] 스킬 수집 완료 - userId=$userId, skillId=$skillId, skillName=${skill.name}, wordId=$wordId")
    }
}
