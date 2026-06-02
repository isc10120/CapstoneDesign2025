package jamgaVOCA.demo.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PveService(
    private val userService: UserService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun completeMobRound(userId: Long): ExpRewardResult {
        val user = userService.getUser(userId)
        val gained = user.level * 3
        userService.addExp(user, gained)
        log.info("[PVE] 잡몹 라운드 완료 - userId=$userId, gained=$gained")
        return ExpRewardResult(gained, user.expPoint, user.level)
    }

    @Transactional
    fun completeBossRound(userId: Long): ExpRewardResult {
        val user = userService.getUser(userId)
        val gained = user.level * 5
        userService.addExp(user, gained)
        log.info("[PVE] 보스 라운드 완료 - userId=$userId, gained=$gained")
        return ExpRewardResult(gained, user.expPoint, user.level)
    }

    data class ExpRewardResult(val gainedExp: Int, val totalExp: Int, val level: Int)
}
