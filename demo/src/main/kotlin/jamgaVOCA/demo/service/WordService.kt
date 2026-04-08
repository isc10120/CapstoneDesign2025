package jamgaVOCA.demo.service

import jamgaVOCA.demo.domain.word.WordRepository
import jamgaVOCA.demo.api.dto.NudgeRequest
import jamgaVOCA.demo.api.dto.WordResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WordService(
    private val wordRepository: WordRepository
) {
    fun getDailyWordList(): List<WordResponse> {
        // 실제 구현은 유저별 학습 상태를 고려해야 하지만, 현재는 목업 데이터 반환
        return listOf(
            WordResponse(
                id = 1,
                word = "apple",
                definition = "사과",
                partOfSpeech = "noun",
                example = "This is an apple.",
                exampleKor = "이것은 사과이다.",
                nudge = 2
            )
        )
    }

    fun getNewDailyWordList(level: String): List<WordResponse> {
        // 백엔드에서 스킬 데이터를 AI로 생성한다는 비고가 있음 (현재는 목업)
        return listOf(
            WordResponse(
                id = 2,
                word = "banana",
                definition = "바나나",
                partOfSpeech = "noun",
                example = "Banana is yellow.",
                exampleKor = "바나나는 노란색이다."
            )
        )
    }

    fun getWordInfo(wordId: Long): WordResponse {
        val word = wordRepository.findById(wordId).orElseThrow { RuntimeException("Word not found") }
        return WordResponse(
            id = word.id!!,
            word = word.englishWord,
            definition = word.koreanMeaning,
            partOfSpeech = word.partOfSpeech.name.lowercase(),
            example = word.exampleEn,
            exampleKor = word.exampleKr
        )
    }

    @Transactional
    fun updateNudge(nudgeRequests: List<NudgeRequest>) {
        // 넛지 횟수 업데이트 및 3회 도달 시 수집 처리 로직 필요
        // 현재는 로직 호출됨을 확인하는 수준
    }
}
