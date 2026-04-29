package jamgaVOCA.demo.domain.battle

import jamgaVOCA.demo.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BattleRepository : JpaRepository<Battle, Long> {

    // 이번 주 진행 중인 배틀 조회
    fun findFirstByUserAAndWeekStartAndResultIsNull(userA: User, weekStart: LocalDate): Battle?
    fun findFirstByUserBAndWeekStartAndResultIsNull(userB: User, weekStart: LocalDate): Battle?

    // 가장 최근 종료된 미확인 결과 조회
    fun findTopByUserAAndResultNotNullAndResultCheckedAFalseOrderByWeekEndDesc(userA: User): Battle?
    fun findTopByUserBAndResultNotNullAndResultCheckedBFalseOrderByWeekEndDesc(userB: User): Battle?

    // 미확인 결과 전체 조회
    fun findAllByUserAAndResultNotNullAndResultCheckedAFalse(userA: User): List<Battle>
    fun findAllByUserBAndResultNotNullAndResultCheckedBFalse(userB: User): List<Battle>

    // 전체 기록 조회
    fun findAllByUserAOrUserBOrderByWeekStartDesc(userA: User, userB: User): List<Battle>

    fun findAllByResultIsNull(): List<Battle>
}