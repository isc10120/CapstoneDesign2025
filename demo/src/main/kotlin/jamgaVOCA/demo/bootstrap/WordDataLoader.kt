package jamgaVOCA.demo.bootstrap

import jamgaVOCA.demo.domain.word.PartOfSpeech
import jamgaVOCA.demo.domain.word.Word
import jamgaVOCA.demo.domain.word.WordLevel
import jamgaVOCA.demo.domain.word.CefrLevel
import jamgaVOCA.demo.domain.word.WordRepository

import com.opencsv.bean.CsvToBeanBuilder
import org.springframework.stereotype.Component
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional

//@Profile("local")
@Component
class WordDataLoader(
    private val wordRepository: WordRepository
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String) {

        // 초기화하고 싶을 때 사용
        // wordRepository.deleteAllInBatch()

        val inputStream = javaClass.classLoader
            .getResourceAsStream("data/word.csv")
            ?: throw IllegalArgumentException("CSV 파일 없음")

        val reader = inputStream.bufferedReader()

        val rows = CsvToBeanBuilder<WordCsvRow>(reader)
            .withType(WordCsvRow::class.java)
            .withIgnoreLeadingWhiteSpace(true)
            .build()
            .parse()

        rows.forEach {
            val word = Word(
                englishWord = it.englishWord.trim(),
                koreanMeaning = it.koreanMeaning,
                partOfSpeech = PartOfSpeech.from(it.partOfSpeech.trim()),
                wordLevel = CefrLevel.valueOf(it.cefrLevel.trim()).wordLevel,
                exampleEn = it.exampleEn.orEmpty().trim(),
                exampleKr = it.exampleKr.orEmpty().trim()
            )

            wordRepository.insertIgnore(word)
        }
    }
}