package jamgaVOCA.demo.domain.deck

import org.springframework.data.jpa.repository.JpaRepository

interface DeckRepository : JpaRepository<Deck, Long> {
    fun findAllByUserId(userId: Long): List<Deck>
}
