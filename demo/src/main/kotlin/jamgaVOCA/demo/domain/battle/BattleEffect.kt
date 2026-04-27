package jamgaVOCA.demo.domain.battle

import jamgaVOCA.demo.domain.user.User
import jakarta.persistence.*

@Entity
@Table(
    name = "battle_effect",
    indexes = [Index(name = "idx_battle_effect_battle_id", columnList = "battle_id")]
)
class BattleEffect(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "effect_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    val battle: Battle,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    val targetUser: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_type", nullable = false, length = 20)
    val effectType: EffectType,

    @Column(name = "remaining_turns", nullable = false)
    var remainingTurns: Int
)