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

    // 에뮬레이터: 10.0.2.2 = 호스트 PC의 localhost
    // 실기기(WiFi): PC의 로컬 IP 주소(예: 192.168.x.x)로 변경
    private const val BASE_URL = "http://10.0.2.2:8080/"

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
}
