package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.deck.Deck
import jamgaVOCA.demo.domain.deck.DeckRepository
import jamgaVOCA.demo.domain.deck.DeckSkill
import jamgaVOCA.demo.domain.deck.DeckSkillRepository
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.user.LevelUtil
import jamgaVOCA.demo.domain.userwordskill.UserWordSkillRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeckService(
    private val deckRepository: DeckRepository,
    private val deckSkillRepository: DeckSkillRepository,
    private val skillRepository: SkillRepository,
    private val userWordSkillRepository: UserWordSkillRepository,
    private val userService: UserService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getDecks(userId: Long): List<DeckSummary> {
        return deckRepository.findAllByUserId(userId).map { deck ->
            val firstSkill = deck.deckSkills.firstOrNull()?.skill
            DeckSummary(
                deckId = deck.id!!,
                name = deck.name,
                skillCount = deck.deckSkills.size,
                firstSkill = firstSkill?.let {
                    SkillInfo(it.id!!, it.name, it.imageUrl, it.skillType.name, it.damage)
                }
            )
        }
    }

    fun getDeck(userId: Long, deckId: Long): DeckDetail {
        val deck = getDeckOwned(userId, deckId)
        val skillIds = deck.deckSkills.map { it.skill.id!! }
        return DeckDetail(deck.id!!, deck.name, skillIds)
    }

    @Transactional
    fun createDeck(userId: Long, name: String, skillIds: List<Long>): DeckDetail {
        val user = userService.getUser(userId)
        val maxSize = LevelUtil.maxDeckSize(user.level)
        validateDeckSize(skillIds.size, maxSize)
        validateSkillsOwned(userId, skillIds)

        val deck = deckRepository.save(Deck(name = name, user = user))
        skillIds.forEach { skillId ->
            val skill = skillRepository.findById(skillId).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }
            deckSkillRepository.save(DeckSkill(deck = deck, skill = skill))
        }
        log.info("[DECK] 덱 생성 - userId=$userId, deckId=${deck.id}, skillCount=${skillIds.size}")
        return DeckDetail(deck.id!!, deck.name, skillIds)
    }

    @Transactional
    fun updateDeck(userId: Long, deckId: Long, name: String?, skillIds: List<Long>?): DeckDetail {
        val deck = getDeckOwned(userId, deckId)
        val user = userService.getUser(userId)

        name?.let { deck.name = it }

        if (skillIds != null) {
            val maxSize = LevelUtil.maxDeckSize(user.level)
            validateDeckSize(skillIds.size, maxSize)
            validateSkillsOwned(userId, skillIds)

            deck.deckSkills.clear()
            skillIds.forEach { skillId ->
                val skill = skillRepository.findById(skillId).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }
                deck.deckSkills.add(DeckSkill(deck = deck, skill = skill))
            }
        }

        log.info("[DECK] 덱 수정 - userId=$userId, deckId=$deckId")
        val finalSkillIds = deck.deckSkills.map { it.skill.id!! }
        return DeckDetail(deck.id!!, deck.name, finalSkillIds)
    }

    @Transactional
    fun deleteDeck(userId: Long, deckId: Long) {
        val deck = getDeckOwned(userId, deckId)
        deckRepository.delete(deck)
        log.info("[DECK] 덱 삭제 - userId=$userId, deckId=$deckId")
    }

    private fun getDeckOwned(userId: Long, deckId: Long): Deck {
        val deck = deckRepository.findById(deckId).orElseThrow { AppException(ErrorCode.DECK_NOT_FOUND) }
        if (deck.user.id != userId) throw AppException(ErrorCode.FORBIDDEN)
        return deck
    }

    private fun validateDeckSize(size: Int, maxSize: Int) {
        if (size < 10) throw AppException(ErrorCode.DECK_SIZE_TOO_SMALL)
        if (size > maxSize) throw AppException(ErrorCode.DECK_SIZE_EXCEEDED)
    }

    private fun validateSkillsOwned(userId: Long, skillIds: List<Long>) {
        skillIds.forEach { skillId ->
            if (!userWordSkillRepository.existsByUserIdAndSkillId(userId, skillId)) {
                throw AppException(ErrorCode.SKILL_NOT_IN_COLLECTION)
            }
        }
    }

    data class SkillInfo(val skillId: Long, val name: String, val imageUrl: String, val skillType: String, val damage: Int)
    data class DeckSummary(val deckId: Long, val name: String, val skillCount: Int, val firstSkill: SkillInfo?)
    data class DeckDetail(val deckId: Long, val name: String, val skillIds: List<Long>)
}
