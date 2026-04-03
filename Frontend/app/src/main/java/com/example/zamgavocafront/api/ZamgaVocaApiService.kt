package com.example.zamgavocafront.api

import com.example.zamgavocafront.api.dto.*
import retrofit2.http.*

interface ZamgaVocaApiService {

    // ───── /test 단어 Mock API ─────

    @GET("test/daily-voca-list")
    suspend fun getDailyVocaList(@Query("num") num: Int = 10): DailyVocaListResponse

    @POST("test/collect")
    suspend fun collectWord(@Body req: CollectRequest): CollectResponse

    @GET("test/collected-voca-list")
    suspend fun getCollectedVocaList(): CollectedVocaListResponse

    @GET("test/voca-info")
    suspend fun getVocaInfo(@Query("id") id: Long): VocaDto

    // ───── 번역 퀴즈 API ─────

    @POST("api/translation/question")
    suspend fun createQuestion(@Body req: CreateQuestionRequest): CreateQuestionResponse

    @POST("api/translation/evaluate")
    suspend fun evaluate(@Body req: EvaluateRequest): EvaluateResponse

    // ───── 스킬 생성 API ─────

    @POST("api/skills/generate")
    suspend fun generateSkill(@Body req: SkillGenerateRequest): SkillGenerateResponse
}
