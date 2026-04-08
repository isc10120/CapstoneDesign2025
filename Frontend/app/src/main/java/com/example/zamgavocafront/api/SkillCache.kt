package com.example.zamgavocafront.api

import com.example.zamgavocafront.api.dto.SkillResponse
import com.example.zamgavocafront.model.WordData

/**
 * 아침 난이도 선택 시 오늘의 단어 스킬을 백그라운드에서 미리 조회해 캐싱.
 * PvpQuestionActivity/PveQuestionActivity에서 캐시 hit 시 추가 API 호출 없이 즉시 표시.
 *
 * - 실제 백엔드 GET /api/v1/skill-info?id={wordId} 를 사용
 * - 실패해도 무시 (각 화면 진입 시 on-demand 재시도)
 */
object SkillCache {
    private val cache = mutableMapOf<Int, SkillResponse>()

    /**
     * 단어 목록에 대해 스킬을 미리 조회한다.
     * 이미 캐시된 단어는 건너뜀. 실패해도 무시.
     */
    suspend fun preGenerate(words: List<WordData>) {
        for (word in words) {
            if (cache.containsKey(word.id)) continue
            try {
                val resp = ApiClient.api.getSkillInfo(word.id.toLong())
                if (resp.success && resp.data != null) {
                    cache[word.id] = resp.data
                }
            } catch (_: Exception) {
                // 실패 시 각 화면 진입 시점에 on-demand 재시도
            }
        }
    }

    /** wordId에 해당하는 캐시된 스킬 반환. 없으면 null. */
    fun get(wordId: Int): SkillResponse? = cache[wordId]

    /** 주간 리셋 등으로 전체 캐시를 비울 때 사용 */
    fun clear() { cache.clear() }
}
