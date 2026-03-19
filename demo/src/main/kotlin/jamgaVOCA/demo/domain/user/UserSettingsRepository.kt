package jamgaVOCA.demo.domain.user
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserSettingsRepository : JpaRepository<UserSettings, Long> {
    fun findByUserId(userId: Long): Optional<UserSettings>
}