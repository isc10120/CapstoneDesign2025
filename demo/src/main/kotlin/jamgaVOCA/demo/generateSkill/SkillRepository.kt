package jamgaVOCA.demo.generateSkill

import org.springframework.data.jpa.repository.JpaRepository

interface SkillRepository : JpaRepository<SkillEntity, Long> {
    fun findByWord(word: String): SkillEntity?
}
