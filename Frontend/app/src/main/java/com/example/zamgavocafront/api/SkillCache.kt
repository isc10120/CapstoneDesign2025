package com.example.zamgavocafront.api

import com.example.zamgavocafront.api.dto.SkillGenerateRequest
import com.example.zamgavocafront.api.dto.SkillGenerateResponse
import com.example.zamgavocafront.model.WordData

/**
 * 아침 난이도 선택 시 오늘의 단어 스킬을 백그라운드에서 미리 생성해 캐싱.
 * PvpQuestionActivity에서 캐시 hit 시 추가 API 호출 없이 즉시 표시.
 *
 * 미래 구조:
 *  - 아침 오버레이에서 난이도 선택 → OverlayService.showWordListOverlay() →
 *    SkillCache.preGenerate(words) 백그라운드 실행
 *  - 각 단어별 generateSkill API 호출 결과를 wordId 키로 저장
 */
object SkillCache {
    private val cache = mutableMapOf<Int, SkillGenerateResponse>()

    /**
     * 단어 목록에 대해 스킬을 미리 생성한다.
     * 이미 캐시된 단어는 건너뜀. 실패해도 무시 (PVP 시 on-demand 재시도).
     * Suspend 함수이므로 반드시 코루틴 컨텍스트에서 호출할 것.
     */
    suspend fun preGenerate(words: List<WordData>) {
        for (word in words) {
            if (cache.containsKey(word.id)) continue
            try {
                val skill = ApiClient.api.generateSkill(
                    SkillGenerateRequest(word = word.word, meaningKo = word.meaning)
                )
                cache[word.id] = skill
            } catch (_: Exception) {
                // 실패 시 PVP 진입 시점에 on-demand 재시도
            }
        }
    }

    /** wordId에 해당하는 캐시된 스킬 반환. 없으면 null. */
    fun get(wordId: Int): SkillGenerateResponse? = cache[wordId]

    /** 주간 리셋 등으로 전체 캐시를 비울 때 사용 */
    fun clear() { cache.clear() }
}
