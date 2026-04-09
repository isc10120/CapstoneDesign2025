package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.dto.NudgeRequest
import jamgaVOCA.demo.api.dto.WordResponse
import jamgaVOCA.demo.domain.dailynudgeword.DailyNudgeWord
import jamgaVOCA.demo.domain.dailynudgeword.DailyNudgeWordRepository
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.user.UserRepository
import jamgaVOCA.demo.domain.userwordskill.UserWordSkillRepository
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWord
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWordRepository
import jamgaVOCA.demo.domain.word.WordLevel
import jamgaVOCA.demo.domain.word.WordRepository
import jakarta.servlet.http.HttpSession
import jamgaVOCA.demo.service.generateSkill.SkillGeneratorService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WordService(
    private val wordRepository: WordRepository,
    private val userWordSkillRepository: UserWordSkillRepository,
    private val dailyNudgeWordRepository: DailyNudgeWordRepository,
    private val weekCollectedWordRepository: WeekCollectedWordRepository,
    private val userRepository: UserRepository,
    private val skillGeneratorService: SkillGeneratorService,
    private val skillRepository: SkillRepository,
    private val httpSession: HttpSession
) {
    private val log = LoggerFactory.getLogger(WordService::class.java)

    fun getDailyWordList(): List<WordResponse> {
        val userId = httpSession.getAttribute("userId") as? Long ?: throw RuntimeException("User not logged in")

        val nudgeWords = dailyNudgeWordRepository.findAllByUserId(userId)

        return nudgeWords.map { nudgeWord ->
            val word = nudgeWord.word
            WordResponse(
                id = word.id!!,
                word = word.englishWord,
                definition = word.koreanMeaning,
                partOfSpeech = word.partOfSpeech.name.lowercase(),
                example = word.exampleEn,
                exampleKor = word.exampleKr,
                nudge = nudgeWord.nudgeCount
            )
        }
    }

    @Transactional
    fun getNewDailyWordList(level: String): List<WordResponse> {
        val userId = httpSession.getAttribute("userId") as? Long ?: throw RuntimeException("User not logged in")
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }

        val wordLevel = WordLevel.entries.find { it.name == level.uppercase() }
            ?: throw IllegalArgumentException("Invalid level: $level")

        // TODO: 하단 로직 추후 쿼리로 최적화 필요
        // 1. 유저가 이미 수집한 단어 ID 목록 조회
        val collectedWordIds = userWordSkillRepository.findAllByUserId(userId)
            .map { it.word.id!! }
            .toSet()

        // 2. 해당 난이도의 전체 단어 조회
        val allWordsOfLevel = wordRepository.findAllByWordLevel(wordLevel)

        // 3. 수집되지 않은 단어만 필터링 후 10개 랜덤 추출
        val selectedWords = allWordsOfLevel
            .filter { it.id !in collectedWordIds }
            .shuffled()
            .take(10)

        dailyNudgeWordRepository.deleteAllByUserId(userId)
        dailyNudgeWordRepository.flush()
        selectedWords.forEach {
            dailyNudgeWordRepository.save(
            DailyNudgeWord(
                user = user,
                word = it
            )
        )}

        // 스킬 생성
        selectedWords.forEach {
            skillGeneratorService.generate(it.id!!)
        }

        return selectedWords.map {
            WordResponse(
                id = it.id!!,
                word = it.englishWord,
                definition = it.koreanMeaning,
                partOfSpeech = it.partOfSpeech.name.lowercase(),
                example = it.exampleEn,
                exampleKor = it.exampleKr
            )
        }
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
        val userId = httpSession.getAttribute("userId") as? Long ?: throw RuntimeException("User not logged in")
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }

        for (request in nudgeRequests) {
            val dailyNudgeWord = dailyNudgeWordRepository.findByUserIdAndWordId(userId, request.id)
                .orElseThrow { RuntimeException("dailyNudgeWord not found with wordID: ${request.id}, userID: $userId") }

            dailyNudgeWord.nudgeCount = request.nudge.toShort()

            if (dailyNudgeWord.nudgeCount >= 3) {
                if (weekCollectedWordRepository.existsByUserIdAndWordId(userId, request.id)) {
                    log.warn("Word already collected for the week: ${request.id}")
                    // 이미 이번 주 수집 목록에 있으면 DailyNudgeWord에서만 삭제
                    dailyNudgeWordRepository.delete(dailyNudgeWord)
                    continue
                }

                weekCollectedWordRepository.save(
                    WeekCollectedWord(
                        user = user,
                        word = dailyNudgeWord.word
                    )
                )
                
                dailyNudgeWordRepository.delete(dailyNudgeWord)

            } else {
                dailyNudgeWordRepository.save(dailyNudgeWord)  //세이브는 업데이트
            }
        }
    }

    fun getWeekCollectedList(): List<WordResponse> {
        val userId = httpSession.getAttribute("userId") as? Long ?: throw RuntimeException("User not logged in")

        val collectedWords = weekCollectedWordRepository.findAllByUserId(userId)

        return collectedWords.map { collected ->
            val word = collected.word
            val skill = skillRepository.findByWordId(word.id!!)
            WordResponse(
                id = word.id!!,
                word = word.englishWord,
                definition = word.koreanMeaning,
                partOfSpeech = word.partOfSpeech.name.lowercase(),
                example = word.exampleEn,
                exampleKor = word.exampleKr,
                skillId = skill?.id
            )
        }
    }
}
