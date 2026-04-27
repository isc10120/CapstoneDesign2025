package jamgaVOCA.demo.domain.battle

import jamgaVOCA.demo.domain.user.User
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "battle",
    indexes = [
        Index(name = "idx_battle_user_a_id", columnList = "user_a_id"),
        Index(name = "idx_battle_user_b_id", columnList = "user_b_id")
    ]
)
class Battle(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false)
    val userA: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false)
    val userB: User,

    @Column(name = "user_a_damage", nullable = false)
    var userADamage: Int = 0,

    @Column(name = "user_b_damage", nullable = false)
    var userBDamage: Int = 0,

    @Column(name = "user_a_shield", nullable = false)
    var userAShield: Int = 0,

    @Column(name = "user_b_shield", nullable = false)
    var userBShield: Int = 0,

    @Column(name = "week_start", nullable = false)
    val weekStart: LocalDate,

    @Column(name = "week_end", nullable = false)
    val weekEnd: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 10)
    var result: BattleResult? = null,

    @Column(name = "result_checked_a", nullable = false)
    var resultCheckedA: Boolean = false,

    @Column(name = "result_checked_b", nullable = false)
    var resultCheckedB: Boolean = false

) {
    @OneToMany(
        mappedBy = "battle",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val effects: MutableList<BattleEffect> = mutableListOf()

    // 특정 유저가 A인지 B인지 판별
    fun isUserA(userId: Long) = userA.id == userId

    // 해당 유저에게 걸린 상태이상만 필터링
    fun effectsOf(userId: Long) = effects.filter { it.targetUser.id == userId }

    // 해당 유저의 쉴드 카운트
    fun shieldOf(userId: Long) = if (isUserA(userId)) userAShield else userBShield

    // 해당 유저의 누적 데미지
    fun damageOf(userId: Long) = if (isUserA(userId)) userADamage else userBDamage

    // 해당 유저 입장에서의 결과
    fun resultOf(userId: Long): String {
        val isUserA = isUserA(userId)
        return when {
            result == null -> "IN_PROGRESS"
            isUserA && result == BattleResult.WIN_A -> "WIN"
            !isUserA && result == BattleResult.WIN_B -> "WIN"
            result == BattleResult.DRAW -> "DRAW"
            else -> "LOSE"
        }
    }
}