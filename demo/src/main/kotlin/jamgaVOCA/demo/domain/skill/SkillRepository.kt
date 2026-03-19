package jamgaVOCA.demo.domain.skill

import org.springframework.data.jpa.repository.JpaRepository

interface SkillRepository : JpaRepository<Skill, Long> {
    fun findAllByWordId(wordId: Long): List<Skill>
    fun findAllBySkillType(skillType: SkillType): List<Skill>
}