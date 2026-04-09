package jamgaVOCA.demo.domain.dailynudgeword

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

interface DailyNudgeWordRepository : JpaRepository<DailyNudgeWord, Long> {
    fun findAllByUserId(userId: Long): List<DailyNudgeWord>
    fun findByUserIdAndWordId(userId: Long, wordId: Long): Optional<DailyNudgeWord>
    @Modifying
    @Transactional
    @Query("DELETE FROM DailyNudgeWord d WHERE d.user.id = :userId")
    fun deleteAllByUserId(userId: Long)
}
