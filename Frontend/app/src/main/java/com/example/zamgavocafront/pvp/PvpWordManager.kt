package com.example.zamgavocafront.pvp

import android.content.Context
import com.example.zamgavocafront.WordProgressManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PvpWordManager {
    private const val PREFS = "pvp_state"
    private const val KEY_WEEK_START = "week_start"
    private const val KEY_PVP_WORDS = "pvp_words"          // 이번 주 해제된 단어 ID Set
    private const val KEY_USED_WORDS = "used_words"         // 이번 주 PVP에서 정답 맞힌 단어 ID Set
    private const val KEY_ATTACK_DATE = "attack_date"       // 하루 공격 횟수 리셋용 날짜
    private const val KEY_ATTACKS_LEFT = "attacks_left"     // 오늘 남은 공격 횟수
    private const val KEY_TOTAL_DAMAGE = "total_damage"     // 이번 주 누적 데미지
    private const val KEY_WORDS_ADDED_DATE = "words_added_date"
    private const val KEY_WORDS_ADDED_TODAY = "words_added_today"

    private const val MAX_ATTACKS_PER_DAY = 10
    private const val MAX_PVP_WORDS_PER_DAY = 10

    private fun getWeekStartMillis(): Long {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysBack = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /** 주가 바뀌었으면 PVP 상태 및 단어 진행도 초기화 */
    private fun checkWeekReset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val currentWeekStart = getWeekStartMillis()
        if (currentWeekStart > prefs.getLong(KEY_WEEK_START, 0L)) {
            prefs.edit()
                .putLong(KEY_WEEK_START, currentWeekStart)
                .putStringSet(KEY_PVP_WORDS, emptySet())
                .putStringSet(KEY_USED_WORDS, emptySet())
                .putInt(KEY_TOTAL_DAMAGE, 0)
                .apply()
            WordProgressManager.resetAll(context)
        }
    }

    /** 날이 바뀌었으면 공격 횟수 초기화 */
    private fun checkDayReset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_ATTACK_DATE, "") != today()) {
            prefs.edit()
                .putString(KEY_ATTACK_DATE, today())
                .putInt(KEY_ATTACKS_LEFT, MAX_ATTACKS_PER_DAY)
                .apply()
        }
    }

    /** 넛지 기믹 3회 해제 완료 시 PVP 단어 풀에 추가 */
    fun addUnlockedWord(context: Context, wordId: Int) {
        checkWeekReset(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // 하루 최대 10개 제한
        val lastDate = prefs.getString(KEY_WORDS_ADDED_DATE, "")
        val addedToday = if (lastDate != today()) 0 else prefs.getInt(KEY_WORDS_ADDED_TODAY, 0)
        if (addedToday >= MAX_PVP_WORDS_PER_DAY) return

        val current = prefs.getStringSet(KEY_PVP_WORDS, emptySet())!!.toMutableSet()
        if (current.add(wordId.toString())) {
            prefs.edit()
                .putStringSet(KEY_PVP_WORDS, current)
                .putString(KEY_WORDS_ADDED_DATE, today())
                .putInt(KEY_WORDS_ADDED_TODAY, addedToday + 1)
                .apply()
        }
    }

    /** 이번 주 PVP에서 이미 정답 처리된 단어 ID Set 반환 */
    fun getUsedWordIds(context: Context): Set<Int> {
        checkWeekReset(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_USED_WORDS, emptySet())!!
            .mapNotNull { it.toIntOrNull() }.toSet()
    }

    /** 이번 주 PVP에서 아직 사용하지 않은 단어 ID Set 반환 (로컬 fallback용) */
    fun getPvpAvailableWordIds(context: Context): Set<Int> {
        checkWeekReset(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val all = prefs.getStringSet(KEY_PVP_WORDS, emptySet())!!
        val used = prefs.getStringSet(KEY_USED_WORDS, emptySet())!!
        return (all - used).mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun getAttacksLeft(context: Context): Int {
        checkWeekReset(context)
        checkDayReset(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ATTACKS_LEFT, MAX_ATTACKS_PER_DAY)
    }

    /** 공격 1회 소모 */
    fun consumeAttack(context: Context) {
        checkDayReset(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val left = prefs.getInt(KEY_ATTACKS_LEFT, MAX_ATTACKS_PER_DAY)
        prefs.edit().putInt(KEY_ATTACKS_LEFT, (left - 1).coerceAtLeast(0)).apply()
    }

    /** 정답 맞힌 단어를 사용 완료 처리 */
    fun markWordUsed(context: Context, wordId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val used = prefs.getStringSet(KEY_USED_WORDS, emptySet())!!.toMutableSet()
        used.add(wordId.toString())
        prefs.edit().putStringSet(KEY_USED_WORDS, used).apply()
    }

    /** 데미지 누적 */
    fun addDamage(context: Context, damage: Int) {
        checkWeekReset(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_TOTAL_DAMAGE, 0)
        prefs.edit().putInt(KEY_TOTAL_DAMAGE, current + damage).apply()
    }

    fun getTotalDamage(context: Context): Int {
        checkWeekReset(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_TOTAL_DAMAGE, 0)
    }
}
