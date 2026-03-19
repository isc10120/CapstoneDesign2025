package jamgaVOCA.demo.domain.word

import org.springframework.data.jpa.repository.JpaRepository

interface WordRepository : JpaRepository<Word, Long> {
    fun findAllByWordLevel(wordLevel: WordLevel): List<Word>
    fun findAllByPartOfSpeech(partOfSpeech: PartOfSpeech): List<Word>
    fun findAllByEnglishWordContainingIgnoreCase(keyword: String): List<Word>
}