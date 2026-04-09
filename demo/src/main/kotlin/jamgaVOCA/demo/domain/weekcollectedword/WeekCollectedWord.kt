package jamgaVOCA.demo.domain.weekcollectedword

import jakarta.persistence.*
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.word.Word

@Entity
@Table(
    name = "week_collected_word",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_week_collected_word_user_id_word_id",
            columnNames = ["user_id", "word_id"]
        )
    ],
    indexes = [Index(name = "idx_week_collected_word_user_id", columnList = "user_id")]
)
class WeekCollectedWord (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    var word: Word
)