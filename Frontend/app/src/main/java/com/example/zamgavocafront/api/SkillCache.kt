package com.example.zamgavocafront.api

import com.example.zamgavocafront.api.dto.SkillResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * 스킬 정보 캐시.
 * week-collected-list에서 받은 skillId로 GET /api/v1/skill-info를 호출해 캐싱.
 * 스킬은 daily-word-list/new 호출 시 백엔드에서 생성되며,
 * skillId는 GET /api/v1/week-collected-list 응답에 포함되어 있다.
 */
object SkillCache {
    // ConcurrentHashMap: 여러 코루틴이 동시에 캐시를 읽고 쓸 수 있도록 thread-safe 보장
    private val cache = ConcurrentHashMap<Int, SkillResponse>()

    /**
     * wordId 기준으로 캐시 확인 → skillId로 GET /api/v1/skill-info 조회.
     * skillId가 null이면 week-collected-list를 재조회해 백엔드 비동기 생성 완료 여부를 확인한다.
     */
    suspend fun fetchOrGenerate(wordId: Int, skillId: Long?, word: String, meaning: String): SkillResponse? {
        cache[wordId]?.let { return it }

        // 1) skillId가 있으면 서버에서 스킬 정보 조회
        if (skillId != null) {
            val fromServer = runCatching {
                val resp = ApiClient.api.getSkillInfo(skillId)
                if (resp.success && resp.data != null) resp.data else null
            }.getOrNull()
            if (fromServer != null) {
                cache[wordId] = fromServer
                return fromServer
            }
        }

        // 2) skillId가 없거나 조회 실패: week-collected-list 재조회로 백엔드 비동기 생성 완료 확인
        val freshSkillId = runCatching {
            val listResp = ApiClient.api.getWeekCollectedList()
            listResp.data?.find { it.id == wordId.toLong() }?.skillId
        }.getOrNull()

        if (freshSkillId != null) {
            val fromServer = runCatching {
                val resp = ApiClient.api.getSkillInfo(freshSkillId)
                if (resp.success && resp.data != null) resp.data else null
            }.getOrNull()
            if (fromServer != null) {
                cache[wordId] = fromServer
                return fromServer
            }
        }

        return null
    }

    /** wordId에 해당하는 캐시된 스킬 반환. 없으면 null. */
    fun get(wordId: Int): SkillResponse? = cache[wordId]

    /** 주간 리셋 등으로 전체 캐시를 비울 때 사용 */
    fun clear() { cache.clear() }
}
