package com.example.zamgavocafront.model

data class WordData(
    val id: Int,
    val word: String,
    val meaning: String,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

enum class Difficulty {
    EASY, MEDIUM, HARD;

    fun displayName(): String = when (this) {
        EASY -> "하급"
        MEDIUM -> "중급"
        HARD -> "상급"
    }
}
