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

    // ───── /api/v1 PVP API ─────

    @GET("api/v1/pvp/status")
    suspend fun getPvpStatus(): ApiResponse<BattleStatusResponse>

    @POST("api/v1/pvp/skill")
    suspend fun usePvpSkill(@Body req: PvpSkillRequest): ApiResponse<PvpSkillResponse>

    @POST("api/v1/pvp/skill/fail")
    suspend fun failPvpSkill(@Body req: PvpSkillRequest): ApiResponse<PvpSkillResponse>

    @GET("api/v1/pvp/result/history")
    suspend fun getPvpHistory(): ApiResponse<List<BattleResultResponse>>

    @GET("api/v1/pvp/result/latest")
    suspend fun getPvpLatestResult(): ApiResponse<BattleResultResponse?>

    @PATCH("api/v1/pvp/result/confirm")
    suspend fun confirmPvpResult(): ApiResponse<Any?>

    @POST("api/v1/pvp/test/match")
    suspend fun testMatch(): ApiResponse<Any?>

    // ───── /api/v1 Question Generation API ─────

    @POST("api/v1/question-generation/{questionType}")
    suspend fun generateQuestion(
        @Path("questionType") questionType: String,
        @Body req: QuestionRequest
    ): ApiResponse<QuestionResponse>

    @POST("api/v1/question-generation/evaluate")
    suspend fun evaluateNewAnswer(@Body req: EvaluateNewRequest): ApiResponse<EvaluateNewResponse>

    // ───── /api/v1 Auth API ─────

    @POST("api/v1/auth/sign-up")
    suspend fun signUp(@Body req: SignUpRequest): ApiResponse<Any?>

    @POST("api/v1/auth/sign-in")
    suspend fun signIn(@Body req: SignInRequest): ApiResponse<SignInResponse>

    @POST("api/v1/user/sign-out")
    suspend fun signOut(): ApiResponse<Any?>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body req: RefreshTokenRequest): ApiResponse<RefreshTokenResponse>

    // ───── /api/v1 Deck API ─────

    @GET("api/v1/decks")
    suspend fun getDecks(): ApiResponse<List<DeckListItemResponse>>

    @GET("api/v1/decks/{deckId}")
    suspend fun getDeckDetail(@Path("deckId") deckId: Long): ApiResponse<DeckDetailResponse>

    @POST("api/v1/decks")
    suspend fun createDeck(@Body req: DeckCreateRequest): ApiResponse<DeckDetailResponse>

    @PUT("api/v1/decks/{deckId}")
    suspend fun updateDeck(
        @Path("deckId") deckId: Long,
        @Body req: DeckUpdateRequest
    ): ApiResponse<DeckDetailResponse>

    @DELETE("api/v1/decks/{deckId}")
    suspend fun deleteDeck(@Path("deckId") deckId: Long): ApiResponse<Any?>

    // ───── /api/v1 PVE API ─────

    @POST("api/v1/pve/mob-round")
    suspend fun completeMobRound(): ApiResponse<PveRoundResultResponse>

    @POST("api/v1/pve/boss-round")
    suspend fun completeBossRound(): ApiResponse<PveRoundResultResponse>

    // ───── /api/v1 Ranking API ─────

    @GET("api/v1/rankings/exp")
    suspend fun getExpRanking(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<RankingResponse>

    @GET("api/v1/rankings/skills")
    suspend fun getSkillsRanking(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<RankingResponse>
}
