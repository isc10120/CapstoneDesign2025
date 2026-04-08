package com.example.zamgavocafront.api.dto

import com.google.gson.annotations.SerializedName

// ───── /test 단어 Mock API DTOs ─────

data class VocaDto(
    val id: Long,
    val word: String,
    val definition: String,
    val explanation: String,
    val example: String,
    val exampleKor: String,
    val skillName: String,
    val skillDef: String,
    val skillImg: String,
    val skillDmg: Int,
    val widgetType: Int
)

data class DailyVocaListResponse(
    val vocaList: List<VocaDto>
)

data class CollectRequest(val id: Long)

data class CollectResponse(
    val ok: Boolean,
    val collectedId: Long? = null,
    val message: String? = null
)

data class CollectedVocaListResponse(
    val vocaIdList: List<Long>,
    val vocaInfoURL: String
)

// ───── 번역 퀴즈 DTOs ─────

data class CreateQuestionRequest(
    val targetWord: String,
    val userLevel: String = "intermediate"
)

data class CreateQuestionResponse(
    val success: Boolean,
    val koreanSentence: String? = null,
    val targetWord: String? = null,
    val wordHint: String? = null,
    val ideal: String? = null,
    val error: String? = null
)

data class EvaluateRequest(
    val koreanSentence: String,
    val userAnswer: String,
    val idealTranslation: String,
    val targetWord: String,
    val userLevel: String = "intermediate"
)

data class EvaluateResponse(
    val success: Boolean,
    val score: Int? = null,
    val breakdown: Map<String, Int>? = null,
    val feedback: String? = null,
    val correction: String? = null,
    val idealAnswer: String? = null,
    val error: String? = null
)

// ───── 스킬 생성 DTOs ─────

data class SkillGenerateRequest(
    val word: String,
    val meaningKo: String
)

enum class ImageStatus { SUCCESS, FAILED }

data class SkillGenerateResponse(
    val id: Long?,
    val word: String,
    val name: String,
    val description: String,
    val damage: Int,
    @SerializedName("imageDesc") val imageDesc: String,
    val imageBase64: String?,
    val imageStatus: ImageStatus,
    val imageError: String? = null
)

// ───── /api/v1 공통 응답 래퍼 ─────

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String
)

// ───── /api/v1 Word DTOs ─────

data class WordResponse(
    val id: Long,
    val word: String,
    val definition: String,
    val partOfSpeech: String,
    val example: String,
    val exampleKor: String,
    val nudge: Int? = null,
    val skillId: Long? = null
)

/** PATCH /api/v1/nudge 요청 바디 아이템 */
data class NudgeUpdateRequest(
    val id: Long,
    val nudge: Int
)

// ───── /api/v1 Skill DTOs ─────

data class SkillResponse(
    val skillId: Long,
    val name: String,
    val explain: String,
    val damage: Int,
    val skillType: String,
    val lasting: Int? = null,
    val imageURL: String,
    val wordId: Long
)

data class CollectSkillRequest(
    val skillId: Long,
    val wordId: Long
)

// ───── /api/v1 Auth DTOs ─────

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
