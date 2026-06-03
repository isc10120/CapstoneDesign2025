package jamgaVOCA.demo.domain.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
    fun findByIsDummyTrue(): User?
    fun findByIsDummyTrueAndLevel(level: Int): User?

    fun findAllByIsDummyFalseOrderByExpPointDescLevelDesc(pageable: Pageable): Page<User>

    @Query("""
        SELECT u FROM User u
        WHERE u.isDummy = false
        ORDER BY (SELECT COUNT(uws) FROM UserWordSkill uws WHERE uws.user = u) DESC, u.nickname ASC
    """)
    fun findAllOrderBySkillCountDesc(pageable: Pageable): Page<User>

    @Query("SELECT COUNT(uws) FROM UserWordSkill uws WHERE uws.user.id = :userId")
    fun countSkillsByUserId(userId: Long): Long

    // 본인보다 exp가 높은 유저 수 + 1 = 본인 순위
    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.isDummy = false AND u.expPoint > :expPoint")
    fun findExpRankByExpPoint(expPoint: Int): Long

    // 본인보다 스킬 수가 많은 유저 수 + 1 = 본인 순위
    @Query("""
        SELECT COUNT(DISTINCT u.id) + 1 FROM User u
        WHERE u.isDummy = false
        AND (SELECT COUNT(uws) FROM UserWordSkill uws WHERE uws.user = u) >
            (SELECT COUNT(uws2) FROM UserWordSkill uws2 WHERE uws2.user.id = :userId)
    """)
    fun findSkillRankByUserId(userId: Long): Long
}