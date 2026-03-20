package jamgaVOCA.demo.legacy.generateSkill

import org.springframework.data.jpa.repository.JpaRepository

interface SkillLegacyRepository : JpaRepository<SkillEntity, Long> {
    fun findByWord(word: String): SkillEntity?
}
