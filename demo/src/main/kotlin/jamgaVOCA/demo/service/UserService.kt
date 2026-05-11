package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
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

    fun findAll(): List<User> =
        userRepository.findAll()

    fun findByIsDummyTrue(): User =
        userRepository.findByIsDummyTrue()
            ?: throw AppException(ErrorCode.DUMMY_USER_NOT_FOUND)
}