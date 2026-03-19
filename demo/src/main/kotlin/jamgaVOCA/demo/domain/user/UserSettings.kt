package jamgaVOCA.demo.domain.user

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(
    name = "user_settings",
    indexes = [Index(name = "idx_user_settings_user_id", columnList = "user_id")]
)
class UserSettings(

    @Id
    @Column(name = "user_id")
    val userId: Long? = null,

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User,

    @Column(name = "nudge_enabled", nullable = false)
    var nudgeEnabled: Boolean = true,

    @Column(name = "nudge_interval", nullable = false)
    var nudgeInterval: Int = 30,

    @Column(name = "silent_nudge_start")
    var silentNudgeStart: LocalTime? = null,

    @Column(name = "silent_nudge_end")
    var silentNudgeEnd: LocalTime? = null
)