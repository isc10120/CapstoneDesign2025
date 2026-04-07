package com.example.zamgavocafront.pve

import android.content.Context
import com.example.zamgavocafront.pvp.CollectedCardManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 덱 저장/관리. 최대 3개 덱, 각 덱은 수집한 카드 중 MIN_CARDS~maxCards 장으로 구성.
 * 레벨에 따라 maxCards가 증가 (레벨 2 증가마다 +1, 최대 20장).
 */
object DeckManager {

    private const val PREFS = "pve_decks"
    private const val KEY_DECKS = "decks_json"
    private const val KEY_ACTIVE_DECK_IDX = "active_deck_idx"
    private const val KEY_CLEARED_CARD_IDS = "cleared_card_ids"  // 이전 클리어에 사용된 카드 IDs

    const val MAX_DECK_SLOTS = 3
    const val MIN_CARDS = 10
    const val INITIAL_MAX_CARDS = 10  // 레벨 1 기준

    data class Deck(
        val slotIndex: Int,      // 0~2
        val name: String,
        val cardWordIds: List<Int>,
        val lastUsedAt: Long = 0L
    )

    // ── CRUD ────────────────────────────────────────────────────────

    fun getDecks(context: Context): List<Deck> {
        val json = prefs(context).getString(KEY_DECKS, null) ?: return emptyList()
        return try {
            Gson().fromJson(json, object : TypeToken<List<Deck>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    fun saveDeck(context: Context, deck: Deck) {
        val decks = getDecks(context).toMutableList()
        val i = decks.indexOfFirst { it.slotIndex == deck.slotIndex }
        if (i >= 0) decks[i] = deck else decks.add(deck)
        prefs(context).edit().putString(KEY_DECKS, Gson().toJson(decks)).apply()
    }

    fun deleteDeck(context: Context, slotIndex: Int) {
        val decks = getDecks(context).filter { it.slotIndex != slotIndex }
        prefs(context).edit().putString(KEY_DECKS, Gson().toJson(decks)).apply()
        if (getActiveDeckIndex(context) == slotIndex) setActiveDeckIndex(context, -1)
    }

    // ── 활성 덱 ─────────────────────────────────────────────────────

    fun getActiveDeckIndex(context: Context): Int =
        prefs(context).getInt(KEY_ACTIVE_DECK_IDX, -1)

    fun setActiveDeckIndex(context: Context, idx: Int) {
        prefs(context).edit().putInt(KEY_ACTIVE_DECK_IDX, idx).apply()
    }

    fun getActiveDeck(context: Context): Deck? {
        val idx = getActiveDeckIndex(context)
        return getDecks(context).find { it.slotIndex == idx }
    }

    /** 덱의 카드 ID → 실제 CollectedCard 객체로 변환 */
    fun resolveCards(context: Context, deck: Deck): List<CollectedCardManager.CollectedCard> {
        val byId = CollectedCardManager.getCards(context).associateBy { it.wordId }
        return deck.cardWordIds.mapNotNull { byId[it] }
    }

    // ── 스테이지 클리어 카드 추적 ────────────────────────────────────
    // 기획: 스테이지 클리어에 사용한 덱 카드들은 다음 스테이지 클리어 전까지 재사용 불가

    fun getClearedCardIds(context: Context): Set<Int> {
        val json = prefs(context).getString(KEY_CLEARED_CARD_IDS, null) ?: return emptySet()
        return try { Gson().fromJson(json, object : TypeToken<Set<Int>>() {}.type) }
        catch (e: Exception) { emptySet() }
    }

    fun setClearedCardIds(context: Context, ids: Set<Int>) {
        prefs(context).edit().putString(KEY_CLEARED_CARD_IDS, Gson().toJson(ids)).apply()
    }

    /** 스테이지 클리어 시 사용한 덱 카드 ID들을 '클리어 카드 목록'에 추가 */
    fun markDeckCardsAsCleared(context: Context, deck: Deck) {
        val current = getClearedCardIds(context).toMutableSet()
        current.addAll(deck.cardWordIds)
        setClearedCardIds(context, current)
    }

    /** 새 스테이지 클리어 후 클리어 카드 목록 초기화 (새로운 카드 사용 권장 주기) */
    fun resetClearedCards(context: Context) {
        prefs(context).edit().remove(KEY_CLEARED_CARD_IDS).apply()
    }

    // ── 레벨에 따른 최대 덱 카드 수 ─────────────────────────────────
    fun getMaxCards(playerLevel: Int): Int =
        (INITIAL_MAX_CARDS + playerLevel / 2).coerceAtMost(20)

    // ── 유효성 검사 ──────────────────────────────────────────────────
    fun isDeckValid(deck: Deck, playerLevel: Int): Boolean {
        val max = getMaxCards(playerLevel)
        return deck.cardWordIds.size in MIN_CARDS..max
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── 클리어 스테이지 수 저장 (리더보드용) ──────────────────────────

    private const val KEY_CLEARED_STAGES = "cleared_stages"

    fun getClearedStageCount(context: Context): Int =
        prefs(context).getInt(KEY_CLEARED_STAGES, 0)

    fun incrementClearedStage(context: Context) {
        val n = getClearedStageCount(context)
        prefs(context).edit().putInt(KEY_CLEARED_STAGES, n + 1).apply()
    }
}
