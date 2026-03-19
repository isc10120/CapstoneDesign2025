package jamgaVOCA.demo.domain.deck

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DeckSkillRepository : JpaRepository<DeckSkill, Long> {
    fun findAllByDeckId(deckId: Long): List<DeckSkill>
    fun findAllBySkillId(skillId: Long): List<DeckSkill>
    fun findByDeckIdAndSkillId(deckId: Long, skillId: Long): Optional<DeckSkill>
    fun existsByDeckIdAndSkillId(deckId: Long, skillId: Long): Boolean
    fun deleteByDeckIdAndSkillId(deckId: Long, skillId: Long)
}