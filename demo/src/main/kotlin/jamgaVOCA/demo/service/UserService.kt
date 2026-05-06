package jamgaVOCA.demo.service

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
            .orElseThrow { IllegalArgumentException("존재하지 않는 유저입니다.") }

    fun findByEmail(email: String): User =
        userRepository.findByEmail(email).orElseThrow { RuntimeException("해당 이메일의 유저가 존재하지 않습니다.") }

    @Transactional
    fun save(user: User): User =
        userRepository.save(user)

    fun findAll(): List<User> =
        userRepository.findAll()

    fun findByIsDummyTrue(): User? =
        userRepository.findByIsDummyTrue()
}