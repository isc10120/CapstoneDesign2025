package jamgaVOCA.demo.config.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.domain.user.UserRepository
import jamgaVOCA.demo.config.jwt.JwtProvider
import jamgaVOCA.demo.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtFilter(
    private val jwtProvider: JwtProvider,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        try {
            if (token != null && jwtProvider.validateToken(token)) {
                val userId = jwtProvider.getUserId(token)

                // User 객체 조회 및 검증
                val user = userService.getUser(userId)

                // Spring Security 인증 객체 생성 (principal에 User 객체 저장)
                val authentication = UsernamePasswordAuthenticationToken(
                    user,            // principal (User 객체)
                    null,            // credentials
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }  catch (e: AppException) { // 예외 직접 잡아서 직접 응답
            response.status = e.errorCode.status.value()
            response.contentType = "application/json;charset=UTF-8"
            val body = ApiResponse.error<Nothing>(e.errorCode.code, e.message)
            response.writer.write(objectMapper.writeValueAsString(body))
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.substring(7)
    }
}