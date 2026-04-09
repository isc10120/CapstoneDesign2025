package jamgaVOCA.demo.api.dto

// Word DTOs
data class WordResponse(
    val id: Long,
    val word: String,
    val definition: String,
    val partOfSpeech: String,
    val example: String,
    val exampleKor: String,
    val nudge: Short = 0,
    val skillId: Long? = null
)

data class NudgeRequest(
    val id: Long,
    val nudge: Int
)

// Skill DTOs
data class SkillResponse(
    val skillId: Long,
    val name: String,
    val explain: String,
    val damage: Int,
    val skillType: String,
    val lasting: Int?,
    val imageURL: String,
    val wordId: Long
)

data class CollectSkillRequest(
    val skillId: Long,
    val wordId: Long
)

// Auth DTOs
data class SignUpRequest(
    val email: String,
    val password: String,
    val nickName: String
)

data class SignInRequest(
    val email: String,
    val password: String
)

data class SignInResponse(
    val userId: Long,
    val email: String,
    val nickName: String,
    val nudgeEnabled: Boolean,
    val nudgeInterval: Int,
    val silentNudge: List<SilentNudgeRange>
)

data class SilentNudgeRange(
    val start: String,
    val end: String
)
