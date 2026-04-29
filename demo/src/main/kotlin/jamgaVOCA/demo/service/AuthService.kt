package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.dto.SignInRequest
import jamgaVOCA.demo.api.dto.SignInResponse
import jamgaVOCA.demo.api.dto.SignUpRequest
import jamgaVOCA.demo.api.dto.SilentNudgeRange
import jamgaVOCA.demo.domain.user.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val battleService: BattleService
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun signUp(request: SignUpRequest) {
        val user = User(
            nickname = request.nickName,
            email = request.email,
            password = request.password, // 실무에서는 암호화 필수
            level = UserLevel.BEGINNER
        )
        val settings = UserSettings(user = user)
        user.settings = settings
        userRepository.save(user)

        // 신규 유저 매칭
        battleService.matchNewUserWithDummy(user)
    }

    fun signIn(request: SignInRequest): SignInResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { RuntimeException("User not found") }
        
        if (user.password != request.password) {
            throw RuntimeException("Invalid password")
        }

        val settings = user.settings ?: throw RuntimeException("User settings not found")

        return SignInResponse(
            userId = user.id!!,
            email = user.email,
            nickName = user.nickname,
            nudgeEnabled = settings.nudgeEnabled,
            nudgeInterval = settings.nudgeInterval,
            silentNudge = listOfNotNull(
                if (settings.silentNudgeStart != null && settings.silentNudgeEnd != null)
                    SilentNudgeRange(settings.silentNudgeStart.toString(), settings.silentNudgeEnd.toString())
                else null
            )
        )
    }
}
