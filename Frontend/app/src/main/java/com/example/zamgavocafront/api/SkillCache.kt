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
     * wordId 기준으로 캐시 확인 → 없으면 skillId로 GET /api/v1/skill-info 조회.
     * skillId가 null이면 null 반환 (스킬 미생성 또는 아직 week-collected 미달).
     */
    suspend fun fetchOrGenerate(wordId: Int, skillId: Long?, word: String, meaning: String): SkillResponse? {
        cache[wordId]?.let { return it }
        if (skillId == null) return null
        return try {
            val resp = ApiClient.api.getSkillInfo(skillId)
            if (resp.success && resp.data != null) {
                cache[wordId] = resp.data
                resp.data
            } else null
        } catch (_: Exception) { null }
    }

    /** wordId에 해당하는 캐시된 스킬 반환. 없으면 null. */
    fun get(wordId: Int): SkillResponse? = cache[wordId]

    /** 주간 리셋 등으로 전체 캐시를 비울 때 사용 */
    fun clear() { cache.clear() }
}
