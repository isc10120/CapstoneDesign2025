package com.example.zamgavocafront.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    /**
     * true  → MockApiService 사용 (API 키 없이 로컬 테스트)
     * false → 실제 백엔드 서버 사용
     */
    const val USE_MOCK = false

    private const val BASE_URL = "http://ec2-54-116-110-178.ap-northeast-2.compute.amazonaws.com:8080/"

    val gson: Gson = Gson()

    // ───── 토큰 관리 ─────

    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set

    fun setTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    fun clearTokens() {
        accessToken = null
        refreshToken = null
    }

    fun saveTokensToPrefs(context: Context) {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("accessToken", accessToken)
            .putString("refreshToken", refreshToken)
            .apply()
    }

    fun loadTokensFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        accessToken = prefs.getString("accessToken", null)
        refreshToken = prefs.getString("refreshToken", null)
    }

    // ───── 토큰 갱신 (동기, refreshOkClient 사용해 재귀 방지) ─────

    private val refreshOkClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun tryRefreshSync(): String? {
        val rToken = refreshToken ?: return null
        return try {
            val body = """{"refreshToken":"$rToken"}"""
                .toRequestBody("application/json".toMediaTypeOrNull())
            val response = refreshOkClient.newCall(
                Request.Builder()
                    .url("${BASE_URL}api/v1/auth/refresh")
                    .post(body)
                    .build()
            ).execute()
            if (!response.isSuccessful) return null
            val responseBody = response.body?.string() ?: return null
            val parsed = gson.fromJson(responseBody, JsonObject::class.java)
            val newToken = parsed?.getAsJsonObject("data")?.get("accessToken")?.asString
            if (newToken != null) accessToken = newToken
            newToken
        } catch (e: Exception) {
            null
        }
    }

    // ───── OkHttpClient: Authorization 헤더 자동 주입 + 401 시 토큰 갱신 재시도 ─────

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = accessToken
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            var response = chain.proceed(request)

            if (response.code == 401 && refreshToken != null) {
                response.close()
                val newToken = tryRefreshSync()
                val retryRequest = chain.request().newBuilder().apply {
                    if (newToken != null) header("Authorization", "Bearer $newToken")
                    else if (token != null) header("Authorization", "Bearer $token")
                }.build()
                response = chain.proceed(retryRequest)
            }
            response
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val realApi: ZamgaVocaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ZamgaVocaApiService::class.java)
    }

    val api: ZamgaVocaApiService
        get() = if (USE_MOCK) MockApiService() else realApi

    /** 번역 문제(createQuestion/evaluate)는 레거시 미완성이므로 항상 Mock 사용 */
    val mockApi: ZamgaVocaApiService = MockApiService()

    /** 인증(로그인/회원가입)은 항상 실제 서버 사용 */
    val authApi: ZamgaVocaApiService get() = realApi
}
