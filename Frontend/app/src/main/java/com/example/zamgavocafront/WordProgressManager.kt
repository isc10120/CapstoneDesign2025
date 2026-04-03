package com.example.zamgavocafront

import android.content.Context
import com.example.zamgavocafront.model.WordData

object WordProgressManager {

    private const val PREFS_NAME = "word_progress"
    const val MAX_COUNT = 3

    fun getCount(context: Context, wordId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("word_$wordId", 0)
    }

    /** 카운트를 1 증가시키고 새 카운트를 반환한다. MAX_COUNT 이상은 증가하지 않는다. */
    fun incrementCount(context: Context, wordId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt("word_$wordId", 0)
        val newCount = (current + 1).coerceAtMost(MAX_COUNT)
        prefs.edit().putInt("word_$wordId", newCount).apply()
        return newCount
    }

    fun isCompleted(context: Context, wordId: Int): Boolean {
        return getCount(context, wordId) >= MAX_COUNT
    }

    /** MAX_COUNT에 도달하지 않은 단어만 반환한다. */
    fun getAvailableWords(context: Context, words: List<WordData>): List<WordData> {
        return words.filter { !isCompleted(context, it.id) }
    }

    /** 매주 월요일 리셋 시 모든 진행도를 초기화한다. */
    fun resetAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
