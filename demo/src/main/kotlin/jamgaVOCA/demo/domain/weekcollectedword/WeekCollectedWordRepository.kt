package jamgaVOCA.demo.domain.weekcollectedword

import org.springframework.data.jpa.repository.JpaRepository

interface WeekCollectedWordRepository : JpaRepository<WeekCollectedWord, Long> {
    fun findAllByUserId(userId: Long): List<WeekCollectedWord>
    fun existsByUserIdAndWordId(userId: Long, wordId: Long): Boolean
}
