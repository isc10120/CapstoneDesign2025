package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.user.LevelUtil
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository
) {
    fun getUser(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { AppException(ErrorCode.USER_NOT_FOUND) }

    fun findByEmail(email: String): User =
        userRepository.findByEmail(email)
            .orElseThrow { AppException(ErrorCode.USER_NOT_FOUND) }

    fun existsByEmail(email: String): Boolean =
        userRepository.existsByEmail(email)

    @Transactional
    fun save(user: User): User =
        userRepository.save(user)

    @Transactional
    fun updateDailySkillCount(userId: Long) {
        val user = getUser(userId)
        val today = java.time.LocalDate.now()

        if (user.lastSkillDate == today) {
            if (user.dailySkillCount >= 10) {
                throw AppException(ErrorCode.DAILY_SKILL_LIMIT_EXCEEDED)
            }
            user.dailySkillCount++
        } else {
            user.lastSkillDate = today
            user.dailySkillCount = 1
        }
    }

    @Transactional
    fun addExp(userId: Long, amount: Int) {
        addExp(getUser(userId), amount)
    }

    @Transactional
    fun addExp(user: User, amount: Int) {
        user.expPoint += amount
        user.level = LevelUtil.calcLevel(user.expPoint)
    }

    fun findAll(): List<User> =
        userRepository.findAll()

    fun findByIsDummyTrue(): User =
        userRepository.findByIsDummyTrue()
            ?: throw AppException(ErrorCode.DUMMY_USER_NOT_FOUND)
}