package jamgaVOCA.demo.config

import com.fasterxml.jackson.databind.ObjectMapper
import jamgaVOCA.demo.config.jwt.JwtFilter
import jamgaVOCA.demo.config.jwt.JwtProvider
import jamgaVOCA.demo.service.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }         // JWT 방식이라 CSRF 불필요
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 세션 안 씀
            }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/api/v1/auth/**").permitAll()  // 로그인/회원가입은 허용
                    .requestMatchers("/api/v1/pvp/test/**").permitAll()  // 테스트용
                    .anyRequest().authenticated()  // 나머지는 인증 필요
            }
            .addFilterBefore(
                JwtFilter(jwtProvider, userService, objectMapper),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}