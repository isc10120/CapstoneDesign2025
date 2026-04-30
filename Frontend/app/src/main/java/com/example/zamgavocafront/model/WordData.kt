package com.example.zamgavocafront.model

import androidx.annotation.ColorRes
import com.example.zamgavocafront.R

data class WordData(
    val id: Int,
    val word: String,
    val meaning: String,
    val exampleEn: String = "",
    val exampleKr: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val skillId: Long? = null
)

enum class Difficulty {
    EASY, MEDIUM, HARD;

    fun displayName(): String = when (this) {
        EASY -> "하급"
        MEDIUM -> "중급"
        HARD -> "상급"
    }

    fun toApiLevel(): String = when (this) {
        EASY -> "Beginner"
        MEDIUM -> "Intermediate"
        HARD -> "Advanced"
    }

    /** PVP 게임 내 등급 명칭 (동급/은급/금급) */
    fun grade(): String = when (this) {
        EASY -> "동급"
        MEDIUM -> "은급"
        HARD -> "금급"
    }

    /** 등급에 대응하는 배지 배경색 리소스 ID */
    @ColorRes
    fun gradeColorRes(): Int = when (this) {
        EASY -> R.color.color_grade_bronze
        MEDIUM -> R.color.color_grade_silver
        HARD -> R.color.color_grade_gold
    }
}