package jamgaVOCA.demo.domain.skill

import jamgaVOCA.demo.domain.deck.DeckSkill
import jamgaVOCA.demo.domain.userwordskill.UserWordSkill
import jamgaVOCA.demo.domain.word.Word
import jakarta.persistence.*

@Entity
@Table(
    name = "skill",
    indexes = [Index(name = "idx_skill_word_id", columnList = "word_id")]
)
class Skill(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    var word: Word,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 500)
    var explanation: String,

    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String = "",

    @Column(nullable = false)
    var damage: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 30)
    var skillType: SkillType,

    @Column
    var lasting: Int? = null
) {
    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY)
    val userWordSkills: MutableList<UserWordSkill> = mutableListOf()

    @OneToMany(mappedBy = "skill", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val deckSkills: MutableList<DeckSkill> = mutableListOf()
}