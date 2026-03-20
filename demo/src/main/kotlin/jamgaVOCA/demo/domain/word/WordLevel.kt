package jamgaVOCA.demo.domain.word

enum class WordLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

enum class CefrLevel(
    val wordLevel: WordLevel
) {
    A1(WordLevel.BEGINNER),
    A2(WordLevel.BEGINNER),

    B1(WordLevel.INTERMEDIATE),
    B2(WordLevel.INTERMEDIATE),

    C1(WordLevel.ADVANCED),
    C2(WordLevel.ADVANCED);
}