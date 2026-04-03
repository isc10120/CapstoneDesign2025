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
}
