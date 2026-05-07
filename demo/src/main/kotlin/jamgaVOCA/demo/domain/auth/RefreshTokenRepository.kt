package jamgaVOCA.demo.domain.auth

import jamgaVOCA.demo.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun deleteByUser(user: User)
}