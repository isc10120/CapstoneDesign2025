package jamgaVOCA.demo.domain.dailynudgeword

import jakarta.persistence.*
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.word.Word

@Entity
@Table(
    name = "daily_nudge_word",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_daily_nudge_word_user_id_word_id",
            columnNames = ["user_id", "word_id"]
        )
    ],
    indexes = [
        Index(name = "idx_daily_nudge_word_user_id", columnList = "user_id")
    ]
)
class DailyNudgeWord (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    var word: Word,

    @Column(name = "nudge_count", nullable = false)
    var nudgeCount: Short = 0
)