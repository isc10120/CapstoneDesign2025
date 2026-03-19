package jamgaVOCA.demo.domain.deck

import jamgaVOCA.demo.domain.skill.Skill
import jakarta.persistence.*

@Entity
@Table(
    name = "deck_skill",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_deck_skill_deck_id_skill_id", columnNames = ["deck_id", "skill_id"])
    ],
    indexes = [
        Index(name = "idx_deck_skill_deck_id", columnList = "deck_id")
    ]
)
class DeckSkill(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    var deck: Deck,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    var skill: Skill
)