package com.example.zamgavocafront.pvp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CollectedCardManager {
    private const val PREFS = "collected_cards"
    private const val KEY_CARDS = "cards_json"

    data class CollectedCard(
        val wordId: Int,
        val word: String,
        val skillName: String,
        val skillDescription: String,
        val damage: Int,
        val imageBase64: String?,
        val grade: String,   // "금급" / "은급" / "동급"
        val collectedAt: Long = System.currentTimeMillis()
    )

    fun addCard(context: Context, card: CollectedCard) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = getCards(context).toMutableList()
        list.add(0, card)
        prefs.edit().putString(KEY_CARDS, Gson().toJson(list)).apply()
    }

    fun getCards(context: Context): List<CollectedCard> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CARDS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CollectedCard>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 테스트용 Mock 카드 10장 생성. 이미 존재하는 wordId는 건너뜀. */
    fun seedMockCards(context: Context) {
        val existing = getCards(context).map { it.wordId }.toSet()
        val mockCards = listOf(
            CollectedCard(1,  "ephemeral",    "찰나의 베기",    "순간에 모든 것을 담은 강렬한 베기를 날린다.", 120, null, "금급"),
            CollectedCard(2,  "serendipity",  "행운의 빛",      "예기치 않은 행운이 상대를 강타한다.",        90,  null, "금급"),
            CollectedCard(3,  "eloquent",     "설득의 파동",    "유창한 언변이 파동이 되어 상대를 꿰뚫는다.", 100, null, "은급"),
            CollectedCard(4,  "benevolent",   "자비의 방패",    "자비로운 기운이 역으로 적에게 충격을 준다.", 80,  null, "동급"),
            CollectedCard(5,  "melancholy",   "우울의 안개",    "짙은 우울이 상대를 잠식한다.",               110, null, "은급"),
            CollectedCard(6,  "perseverance", "불굴의 일격",    "포기하지 않는 의지가 폭발적인 일격이 된다.", 150, null, "동급"),
            CollectedCard(7,  "ambiguous",    "혼돈의 파장",    "모호한 기운이 상대를 혼란에 빠뜨린다.",      95,  null, "은급"),
            CollectedCard(8,  "tenacious",    "집착의 족쇄",    "끈질긴 힘이 상대를 강하게 속박한다.",        130, null, "금급"),
            CollectedCard(9,  "profound",     "심연의 충격",    "심오한 힘이 상대에게 깊은 충격을 준다.",     140, null, "금급"),
            CollectedCard(10, "candid",       "진실의 화살",    "솔직한 진실이 화살이 되어 상대를 꿰뚫는다.", 105, null, "동급"),
        )
        val toAdd = mockCards.filter { it.wordId !in existing }
        if (toAdd.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = getCards(context).toMutableList()
        list.addAll(toAdd)
        prefs.edit().putString(KEY_CARDS, Gson().toJson(list)).apply()
    }
}
