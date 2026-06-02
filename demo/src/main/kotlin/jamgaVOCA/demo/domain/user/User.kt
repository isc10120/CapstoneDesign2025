package jamgaVOCA.demo.domain.user

import jamgaVOCA.demo.domain.deck.Deck
import jamgaVOCA.demo.domain.userwordskill.UserWordSkill
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    indexes = [Index(name = "idx_users_email", columnList = "email")]
)
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Column(nullable = false, unique = true, length = 100)
    var email: String,

    @Column(name = "password", nullable = false, length = 255)
    private var passwordHash: String,

    @Column(name = "exp_point", nullable = false)
    var expPoint: Int = 0,

    @Column(name = "level", nullable = false)
    var level: Int = 1,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_dummy", nullable = false)
    val isDummy: Boolean = false,

    @Column(name = "last_daily_word_date")
    var lastDailyWordDate: java.time.LocalDate? = null,

    @Column(name = "daily_skill_count", nullable = false)
    var dailySkillCount: Int = 0,

    @Column(name = "last_skill_date")
    var lastSkillDate: java.time.LocalDate? = null

) : UserDetails {
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var settings: UserSettings? = null

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val decks: MutableList<Deck> = mutableListOf()

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val userWordSkills: MutableList<UserWordSkill> = mutableListOf()

    // UserDetails 구현
    override fun getUsername(): String = email

    override fun getPassword(): String = passwordHash

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_USER"))

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}