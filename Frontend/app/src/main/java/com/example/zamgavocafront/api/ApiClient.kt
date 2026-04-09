package com.example.zamgavocafront.api

import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    /**
     * true  → MockApiService 사용 (API 키 없이 로컬 테스트)
     * false → 실제 백엔드 서버 사용
     *
     * 실제 서버 연결 시 false로 변경 + BASE_URL 확인
     */
    const val USE_MOCK = true

    private const val BASE_URL = "http://ec2-54-116-110-178.ap-northeast-2.compute.amazonaws.com:8080/"

    val gson: Gson = Gson()


    private val realApi: ZamgaVocaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
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
