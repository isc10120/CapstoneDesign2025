package jamgaVOCA.demo.config.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: Long): String {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessExpiration * 1000))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: Long): String {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshExpiration * 1000))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: ExpiredJwtException) {
            throw AppException(ErrorCode.ACCESS_TOKEN_EXPIRED)
        } catch (e: JwtException) {
            throw AppException(ErrorCode.INVALID_TOKEN)
        }
    }

    fun getUserId(token: String): Long {
        return getClaims(token).subject.toLong()
    }

    fun getRefreshExpirationTime(): Long = refreshExpiration

    private fun getClaims(token: String) =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}