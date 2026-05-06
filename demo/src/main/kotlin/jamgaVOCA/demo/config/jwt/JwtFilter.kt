package jamgaVOCA.demo.config.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jamgaVOCA.demo.domain.user.UserRepository
import jamgaVOCA.demo.config.jwt.JwtProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null && jwtProvider.validateToken(token)) {
            val userId = jwtProvider.getUserId(token)

            // User 객체 조회 및 검증
            val user = userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("존재하지 않는 유저입니다.") }

            // Spring Security 인증 객체 생성 (principal에 User 객체 저장)
            val authentication = UsernamePasswordAuthenticationToken(
                user,            // principal (User 객체)
                null,            // credentials
                listOf(SimpleGrantedAuthority("ROLE_USER"))
            )
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.substring(7)
    }
}