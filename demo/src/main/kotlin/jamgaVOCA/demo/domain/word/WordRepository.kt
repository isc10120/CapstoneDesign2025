package jamgaVOCA.demo.domain.word

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WordRepository : JpaRepository<Word, Long> {
    fun findAllByWordLevel(wordLevel: WordLevel): List<Word>
    fun findAllByPartOfSpeech(partOfSpeech: PartOfSpeech): List<Word>
    fun findAllByEnglishWordContainingIgnoreCase(keyword: String): List<Word>

    @Modifying
    @Query(
        value = """
        INSERT INTO word (
            english_word,
            korean_meaning,
            part_of_speech,
            word_level,
            example_en,
            example_kr
        )
        VALUES (
            :englishWord,
            :koreanMeaning,
            :partOfSpeech,
            :wordLevel,
            :exampleEn,
            :exampleKr
        )
        ON CONFLICT (english_word, korean_meaning) DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertIgnoreInternal(
        @Param("englishWord") englishWord: String,
        @Param("koreanMeaning") koreanMeaning: String,
        @Param("partOfSpeech") partOfSpeech: String,
        @Param("wordLevel") wordLevel: String,
        @Param("exampleEn") exampleEn: String,
        @Param("exampleKr") exampleKr: String
    ): Int

    fun insertIgnore(word: Word): Int {
        return insertIgnoreInternal(
            englishWord = word.englishWord,
            koreanMeaning = word.koreanMeaning,
            partOfSpeech = word.partOfSpeech.name,
            wordLevel = word.wordLevel.name,
            exampleEn = word.exampleEn,
            exampleKr = word.exampleKr
        )
    } // 성공: 1, 실패: 0 (중복이라 무시됨)
}