package com.example.zamgavocafront.api

import com.google.gson.Gson
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ApiClient {
    /**
     * true  → MockApiService 사용 (API 키 없이 로컬 테스트)
     * false → 실제 백엔드 서버 사용
     *
     * 실제 서버 연결 시 false로 변경 + BASE_URL 확인
     */
    const val USE_MOCK = false

    private const val BASE_URL = "http://ec2-54-116-110-178.ap-northeast-2.compute.amazonaws.com:8080/"

    val gson: Gson = Gson()

    // 로그인 세션 쿠키를 유지해서 이후 요청에 자동으로 붙여줌
    // ConcurrentHashMap: 코루틴 멀티스레드 환경에서의 race condition 방지
    private val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return store[url.host] ?: emptyList()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
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
