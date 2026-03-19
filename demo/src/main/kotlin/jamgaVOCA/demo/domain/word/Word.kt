package jamgaVOCA.demo.domain.word

import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.userwordskill.UserWordSkill
import jakarta.persistence.*

@Entity
@Table(
    name = "word",
    indexes = [Index(name = "idx_word_word_level", columnList = "word_level")]
)
class Word(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "english_word", nullable = false, length = 100)
    var englishWord: String,

    @Column(name = "korean_meaning", nullable = false, length = 255)
    var koreanMeaning: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "part_of_speech", nullable = false, length = 20)
    var partOfSpeech: PartOfSpeech,

    @Enumerated(EnumType.STRING)
    @Column(name = "word_level", nullable = false, length = 20)
    var wordLevel: WordLevel,

    @Column(name = "example_en", nullable = false, length = 500)
    var exampleEn: String = "",

    @Column(name = "example_kr", nullable = false, length = 500)
    var exampleKr: String = ""

) {
    @OneToMany(mappedBy = "word", fetch = FetchType.LAZY)
    val skills: MutableList<Skill> = mutableListOf()

    @OneToMany(mappedBy = "word", fetch = FetchType.LAZY)
    val userWordSkills: MutableList<UserWordSkill> = mutableListOf()
}