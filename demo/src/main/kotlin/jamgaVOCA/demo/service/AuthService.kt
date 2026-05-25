package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.dto.SignInRequest
import jamgaVOCA.demo.api.dto.SignInResponse
import jamgaVOCA.demo.api.dto.SignUpRequest
import jamgaVOCA.demo.api.dto.SilentNudgeRange
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.auth.RefreshToken
import jamgaVOCA.demo.domain.auth.RefreshTokenRepository
import jamgaVOCA.demo.domain.user.*
import jamgaVOCA.demo.config.jwt.JwtProvider
import org.slf4j.LoggerFactory
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
    private val passwordEncoder: PasswordEncoder,
    private val battleService: BattleService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun signUp(request: SignUpRequest) {
        if (userService.existsByEmail(request.email)) {
            log.warn("[AUTH] 회원가입 실패 - 이메일 중복: ${request.email}")
            throw AppException(ErrorCode.DUPLICATE_EMAIL)
        }

        val user = User(
            nickname = request.nickName,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
        )
        val settings = UserSettings(user = user)
        user.settings = settings
        userService.save(user)

        log.info("[AUTH] 회원가입 완료 - email=${request.email}, nickname=${request.nickName}")
        battleService.matchNewUserWithDummy(user)
    }

    @Transactional
    fun signIn(request: SignInRequest): SignInResponse {
        val user = userService.findByEmail(request.email)

        if (!passwordEncoder.matches(request.password, user.password)) {
            log.warn("[AUTH] 로그인 실패 - 비밀번호 불일치: email=${request.email}")
            throw AppException(ErrorCode.INVALID_PASSWORD)
        }

        val accessToken = jwtProvider.generateAccessToken(user.id!!)
        val refreshToken = jwtProvider.generateRefreshToken(user.id!!)

        // Refresh Token DB 저장 (기존 있으면 교체)
        val existing = refreshTokenRepository.findById(user.id!!).orElse(null)
        if (existing != null) {
            existing.token = refreshToken
            log.debug("[AUTH] 리프레시 토큰 갱신 - userId=${user.id}")
        } else {
            refreshTokenRepository.save(
                RefreshToken(
                    user = user,
                    token = refreshToken,
                    expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpirationTime())
                )
            )
            log.debug("[AUTH] 리프레시 토큰 신규 발급 - userId=${user.id}")
        }

        log.info("[AUTH] 로그인 성공 - userId=${user.id}, email=${user.email}")

        val settings = user.settings ?: throw AppException(ErrorCode.USER_SETTINGS_NOT_FOUND)

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
        log.info("[AUTH] 로그아웃 - userId=$userId")
    }

    @Transactional
    fun refresh(refreshToken: String): String {
        val token = refreshTokenRepository.findByToken(refreshToken)
            ?: run {
                log.warn("[AUTH] 토큰 갱신 실패 - 존재하지 않는 리프레시 토큰")
                throw AppException(ErrorCode.INVALID_REFRESH_TOKEN)
            }

        if (token.expiresAt.isBefore(LocalDateTime.now())) {
            log.warn("[AUTH] 토큰 갱신 실패 - 만료된 리프레시 토큰: userId=${token.user.id}")
            refreshTokenRepository.delete(token)
            throw AppException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        log.info("[AUTH] 액세스 토큰 재발급 - userId=${token.user.id}")
        return jwtProvider.generateAccessToken(token.user.id!!)
    }
}
