package jamgaVOCA.demo.domain.userwordskill

import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.word.Word
import jakarta.persistence.*

@Entity
@Table(
    name = "user_word_skill",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_word_skill_user_word_skill",
            columnNames = ["user_id", "word_id", "skill_id"]
        )
    ],
    indexes = [
        Index(name = "idx_user_word_skill_user_id", columnList = "user_id")
    ]
)
class UserWordSkill(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    var word: Word,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    var skill: Skill
)