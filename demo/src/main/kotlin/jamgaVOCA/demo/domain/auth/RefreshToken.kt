package jamgaVOCA.demo.domain.auth

import jamgaVOCA.demo.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_token")
class RefreshToken(

    @Id
    @Column(name = "user_id")
    val userId: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(nullable = false, length = 500)
    var token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime
)