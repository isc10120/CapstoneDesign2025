package jamgaVOCA.demo.generateSkill

import jakarta.persistence.*

@Entity
@Table(
    name = "skill",
    indexes = [Index(name = "idx_skill_word", columnList = "word")],
    uniqueConstraints = [UniqueConstraint(name = "uk_skill_word", columnNames = ["word"])]
)
class SkillEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var word: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, columnDefinition = "text")
    var description: String,

    @Column(nullable = false)
    var damage: Int,

    @Column(nullable = false, columnDefinition = "text")
    var imageDesc: String,

    @Column(columnDefinition = "text")
    var imageBase64: String? = null
)
