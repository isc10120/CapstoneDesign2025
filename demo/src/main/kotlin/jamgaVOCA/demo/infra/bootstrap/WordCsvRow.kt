package jamgaVOCA.demo.infra.bootstrap

import com.opencsv.bean.CsvBindByName

data class WordCsvRow(
    @CsvBindByName(column = "Word")
    val englishWord: String = "",

    @CsvBindByName(column = "Meaning")
    val koreanMeaning: String = "",

    @CsvBindByName(column = "POS")
    val partOfSpeech: String = "",

    @CsvBindByName(column = "Level")
    val cefrLevel: String = "",

    @CsvBindByName(column = "Example_EN")
    val exampleEn: String? = null,

    @CsvBindByName(column = "Example_KR")
    val exampleKr: String? = null
)