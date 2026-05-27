package com.example.zamgavocafront.utils

fun questionTypeLabel(questionType: String): String = when (questionType.uppercase()) {
    "SPELLING"         -> "빈칸에 알맞은 글자를 채워 단어를 완성하세요:"
    "ANAGRAM"          -> "섞인 글자를 올바른 순서로 배열하세요:"
    "WORD_DEFINITION"  -> "올바른 뜻을 고르세요:"
    "SYNONYM"          -> "올바른 유의어를 고르세요:"
    "SENTENCE_WRITING" -> "다음 상황에 맞게 영어 문장을 작성하세요:"
    "TRANSLATION"      -> "다음 한국어 문장을 영어로 번역하세요:"
    else               -> "답을 입력하세요:"
}

fun pvpEffectTypeLabel(effectType: String): String = when (effectType.uppercase()) {
    "POISON"      -> "☠ 독"
    "PARALYZE"    -> "⚡ 마비"
    "DAMAGE_BUFF" -> "⬆ 공격력 버프"
    else          -> effectType
}
