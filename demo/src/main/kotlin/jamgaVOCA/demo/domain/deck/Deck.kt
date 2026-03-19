package jamgaVOCA.demo.domain.deck

import jamgaVOCA.demo.domain.user.User
import jakarta.persistence.*

@Entity
@Table(
    name = "deck",
    indexes = [
        Index(name = "idx_deck_user_id", columnList = "user_id")
    ]
)
class Deck(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User
) {
    @OneToMany(mappedBy = "deck", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val deckSkills: MutableList<DeckSkill> = mutableListOf()
}