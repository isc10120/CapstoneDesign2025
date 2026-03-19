package jamgaVOCA.demo.domain.userwordskill

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserWordSkillRepository : JpaRepository<UserWordSkill, Long> {
    fun findAllByUserId(userId: Long): List<UserWordSkill>
    fun findAllByUserIdAndWordId(userId: Long, wordId: Long): List<UserWordSkill>
    fun findByUserIdAndWordIdAndSkillId(userId: Long, wordId: Long, skillId: Long): Optional<UserWordSkill>
    fun existsByUserIdAndWordIdAndSkillId(userId: Long, wordId: Long, skillId: Long): Boolean
}