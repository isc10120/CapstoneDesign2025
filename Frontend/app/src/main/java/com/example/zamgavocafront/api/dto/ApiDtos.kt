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
    val imageBase64: String? = null,
    val wordId: Long
)

data class CollectSkillRequest(
    val skillId: Long,
    val wordId: Long
)

// ───── /api/v1 PVP DTOs ─────

data class BattleStatusResponse(
    val battleId: Long,
    val weekStart: String,
    val opponent: OpponentInfo,
    val my: SideStatus,
    val enemy: SideStatus
)

data class OpponentInfo(
    val userId: Long,
    val nickname: String
)

data class SideStatus(
    val totalDamage: Int,
    val statusEffects: List<StatusEffect>,
    val shieldCount: Int
)

data class StatusEffect(
    val type: String,
    val remainingTurns: Int
)

data class PvpSkillRequest(
    val skillId: Long,
    val wordId: Long
)

data class PvpSkillResponse(
    val skillName: String,
    val skillType: String,
    val damageDealt: Int,
    val statusApplied: StatusApplied?,
    val shieldBlocked: Boolean,
    val collected: Boolean
)

data class StatusApplied(
    val type: String,
    val turns: Int
)

data class BattleResultResponse(
    val battleId: Long,
    val weekStart: String,
    val weekEnd: String,
    val result: String?,
    val myTotalDamage: Int,
    val opponentTotalDamage: Int,
    val opponentNickName: String
)

data class StompSkillMessage(
    val senderId: Long,
    val skillName: String,
    val skillType: String,
    val damageDealt: Int,
    val statusApplied: StatusApplied?,
    val shieldBlocked: Boolean
)

// ───── /api/v1 Question Generation DTOs ─────

data class QuestionRequest(
    val wordId: Long
)

data class QuestionResponse(
    val questionType: String,
    val wordId: Long,
    val word: String,
    val question: String,
    val hint: String? = null,
    val blankedWord: String? = null,
    val shuffledLetters: String? = null,
    val options: List<String>? = null,
    val correctIndex: Int? = null,
    val answer: String? = null
)

data class EvaluateNewRequest(
    val wordId: Long,
    val questionType: String,
    val userAnswer: String,
    val prompt: String
)

data class EvaluateNewResponse(
    val correct: Boolean,
    val score: Int,
    val feedback: String?,
    val correctAnswer: String?
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
