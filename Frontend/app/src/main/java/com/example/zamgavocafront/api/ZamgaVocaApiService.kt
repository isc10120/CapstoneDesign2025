package com.example.zamgavocafront.api

import com.example.zamgavocafront.api.dto.*
import retrofit2.http.*

interface ZamgaVocaApiService {

    // ───── /test 단어 Mock API (구버전 — 미사용) ─────

//    @GET("test/daily-voca-list")
//    suspend fun getDailyVocaList(@Query("num") num: Int = 10): DailyVocaListResponse
//
//    @POST("test/collect")
//    suspend fun collectWord(@Body req: CollectRequest): CollectResponse
//
//    @GET("test/collected-voca-list")
//    suspend fun getCollectedVocaList(): CollectedVocaListResponse
//
//    @GET("test/voca-info")
//    suspend fun getVocaInfo(@Query("id") id: Long): VocaDto

    // ───── 번역 퀴즈 API ─────

    @POST("api/translation/question")
    suspend fun createQuestion(@Body req: CreateQuestionRequest): CreateQuestionResponse

    @POST("api/translation/evaluate")
    suspend fun evaluate(@Body req: EvaluateRequest): EvaluateResponse

    // ───── 스킬 생성 API ─────

    @POST("api/skills/generate")
    suspend fun generateSkill(@Body req: SkillGenerateRequest): SkillGenerateResponse

    // ───── /api/v1 Word API ─────

    @GET("api/v1/daily-word-list")
    suspend fun getDailyWordList(): ApiResponse<List<WordResponse>>

    @GET("api/v1/daily-word-list/new")
    suspend fun getNewDailyWordList(@Query("level") level: String): ApiResponse<List<WordResponse>>

    @GET("api/v1/word-info")
    suspend fun getWordInfo(@Query("id") id: Long): ApiResponse<WordResponse>

    @PATCH("api/v1/nudge")
    suspend fun updateNudge(@Body nudgeRequests: List<NudgeUpdateRequest>): ApiResponse<Any?>

    // ───── /api/v1 Skill API ─────

    @GET("api/v1/collected-skill-list")
    suspend fun getCollectedSkillList(): ApiResponse<List<SkillResponse>>

    @GET("api/v1/skill-info")
    suspend fun getSkillInfo(@Query("id") id: Long): ApiResponse<SkillResponse>

    @POST("api/v1/collect-skill")
    suspend fun collectSkill(@Body req: CollectSkillRequest): ApiResponse<Any?>

    @GET("api/v1/week-collected-list")
    suspend fun getWeekCollectedList(): ApiResponse<List<WordResponse>>

    // ───── /api/v1 Auth API ─────

    @POST("api/v1/sign-up")
    suspend fun signUp(@Body req: SignUpRequest): ApiResponse<Any?>

    @POST("api/v1/sign-in")
    suspend fun signIn(@Body req: SignInRequest): ApiResponse<SignInResponse>
}
