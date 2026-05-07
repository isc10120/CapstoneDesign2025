package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.dto.SignInRequest
import jamgaVOCA.demo.api.dto.SignInResponse
import jamgaVOCA.demo.api.dto.SignUpRequest
import jamgaVOCA.demo.api.dto.SilentNudgeRange
import jamgaVOCA.demo.domain.auth.RefreshToken
import jamgaVOCA.demo.domain.auth.RefreshTokenRepository
import jamgaVOCA.demo.domain.user.*
import jamgaVOCA.demo.config.jwt.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userService: UserService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun signUp(request: SignUpRequest) {
        val user = User(
            nickname = request.nickName,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
        )
        val settings = UserSettings(user = user)
        user.settings = settings
        userService.save(user)
    }

    @Transactional
    fun signIn(request: SignInRequest): SignInResponse {
        val user = userService.findByEmail(request.email)

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw RuntimeException("Invalid password")
        }

        val accessToken = jwtProvider.generateAccessToken(user.id!!)
        val refreshToken = jwtProvider.generateRefreshToken(user.id!!)

        // Refresh Token DB 저장 (기존 있으면 교체)
        val existing = refreshTokenRepository.findById(user.id!!).orElse(null)
        if (existing != null) {
            existing.token = refreshToken
        } else {
            refreshTokenRepository.save(
                RefreshToken(
                    user = user,
                    token = refreshToken,
                    expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpirationTime())
                )
            )
        }

        val settings = user.settings ?: throw RuntimeException("User settings not found")

        return SignInResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
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

    @Transactional
    fun signOut(userId: Long) {
        val user = userService.getUser(userId)
        refreshTokenRepository.deleteByUser(user)
    }

    @Transactional
    fun refresh(refreshToken: String): String {
        val token = refreshTokenRepository.findByToken(refreshToken)
            ?: throw RuntimeException("유효하지 않은 Refresh Token입니다.")

        if (token.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token)
            throw RuntimeException("만료된 Refresh Token입니다.")
        }

        return jwtProvider.generateAccessToken(token.user.id!!)
    }
}
