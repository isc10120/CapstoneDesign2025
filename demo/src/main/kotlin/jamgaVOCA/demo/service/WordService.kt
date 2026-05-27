package jamgaVOCA.demo.service

import jamgaVOCA.demo.api.dto.NudgeRequest
import jamgaVOCA.demo.api.dto.WordResponse
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import jamgaVOCA.demo.domain.dailynudgeword.DailyNudgeWord
import jamgaVOCA.demo.domain.dailynudgeword.DailyNudgeWordRepository
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.userwordskill.UserWordSkillRepository
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWord
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWordRepository
import jamgaVOCA.demo.domain.word.WordLevel
import jamgaVOCA.demo.domain.word.WordRepository
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
    private val userService: UserService,
    private val skillGeneratorService: SkillGeneratorService,
    private val skillService: SkillService
) {
    private val log = LoggerFactory.getLogger(WordService::class.java)

    fun getDailyWordList(userId: Long): List<WordResponse> {
        val nudgeWords = dailyNudgeWordRepository.findAllByUserId(userId)

        return nudgeWords.map { nudgeWord ->
            val word = nudgeWord.word
            WordResponse(
                id = word.id!!,
                word = word.englishWord,
                definition = word.koreanMeaning,
                partOfSpeech = word.partOfSpeech.name,
                example = word.exampleEn,
                exampleKor = word.exampleKr,
                nudge = nudgeWord.nudgeCount
            )
        }
    }

    @Transactional
    fun getNewDailyWordList(userId: Long, level: String): List<WordResponse> {
        val user = userService.getUser(userId)
        val today = java.time.LocalDate.now()

        if (user.lastDailyWordDate == today) {
            throw AppException(ErrorCode.DAILY_WORD_LIST_ALREADY_GENERATED)
        }

        user.lastDailyWordDate = today

        val wordLevel = WordLevel.entries.find { it.name == level.uppercase() }
            ?: throw AppException(ErrorCode.INVALID_WORD_LEVEL)

        // TODO: 하단 로직 추후 쿼리로 최적화 필요
        // 1. 유저가 이미 수집한 단어 ID 목록 조회
        val collectedWordIds = userWordSkillRepository.findAllByUserId(userId)
            .map { it.word.id!! }
            .toMutableSet()

        // 2. 주간수집단어도 이미 수집한 단어로 추가
        val weekCollectedWordIds = weekCollectedWordRepository.findAllByUserId(userId)
            .map { it.word.id!! }
        collectedWordIds.addAll(weekCollectedWordIds)

        // 3. 해당 난이도의 전체 단어 조회
        val allWordsOfLevel = wordRepository.findAllByWordLevel(wordLevel)

        // 4. 수집되지 않은 단어만 필터링 후 10개 랜덤 추출
        val selectedWords = allWordsOfLevel
            .filter { it.id !in collectedWordIds }
            .shuffled()
            .take(10)

        log.info("[WORD] 새 일일 단어 목록 생성 - userId=$userId, level=$wordLevel, 전체=${allWordsOfLevel.size}, 미수집=${allWordsOfLevel.size - collectedWordIds.size}, 선택=${selectedWords.size}")

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
        log.debug("[WORD] 스킬 생성 요청 완료 - wordIds=${selectedWords.map { it.id }}")

        return selectedWords.map {
            WordResponse(
                id = it.id!!,
                word = it.englishWord,
                definition = it.koreanMeaning,
                partOfSpeech = it.partOfSpeech.name,
                example = it.exampleEn,
                exampleKor = it.exampleKr
            )
        }
    }

    fun getWordInfo(wordId: Long): WordResponse {
        val word = wordRepository.findById(wordId).orElseThrow { AppException(ErrorCode.WORD_NOT_FOUND) }
        return WordResponse(
            id = word.id!!,
            word = word.englishWord,
            definition = word.koreanMeaning,
            partOfSpeech = word.partOfSpeech.name,
            example = word.exampleEn,
            exampleKor = word.exampleKr
        )
    }

    @Transactional
    fun updateNudge(userId: Long, nudgeRequests: List<NudgeRequest>) {
        val user = userService.getUser(userId)

        for (request in nudgeRequests) {
            val dailyNudgeWord = dailyNudgeWordRepository.findByUserIdAndWordId(userId, request.id)
                .orElseThrow { AppException(ErrorCode.DAILY_NUDGE_WORD_NOT_FOUND) }

            dailyNudgeWord.nudgeCount = request.nudge.toShort()

            if (dailyNudgeWord.nudgeCount >= 3) {
                if (weekCollectedWordRepository.existsByUserIdAndWordId(userId, request.id)) {
                    log.warn("[WORD] 주간 수집 중복 - userId=$userId, wordId=${request.id}")
                    dailyNudgeWordRepository.delete(dailyNudgeWord)
                    continue
                }

                weekCollectedWordRepository.save(
                    WeekCollectedWord(
                        user = user,
                        word = dailyNudgeWord.word
                    )
                )
                log.info("[WORD] 주간 단어 수집 완료 - userId=$userId, wordId=${request.id}, word=${dailyNudgeWord.word.englishWord}")
                dailyNudgeWordRepository.delete(dailyNudgeWord)

            } else {
                dailyNudgeWordRepository.save(dailyNudgeWord)
                log.debug("[WORD] 넛지 횟수 업데이트 - userId=$userId, wordId=${request.id}, nudgeCount=${dailyNudgeWord.nudgeCount}")
            }
        }
    }

    fun getWeekCollectedList(userId: Long): List<WordResponse> {
        val collectedWords = weekCollectedWordRepository.findAllByUserId(userId)

        return collectedWords.map { collected ->
            val word = collected.word
            val skill = skillService.getSkillByWordId(word.id!!)
            WordResponse(
                id = word.id!!,
                word = word.englishWord,
                definition = word.koreanMeaning,
                partOfSpeech = word.partOfSpeech.name,
                example = word.exampleEn,
                exampleKor = word.exampleKr,
                skillId = skill?.id
            )
        }
    }

    @Transactional
    fun resetWeeklyCollectedWords() {
        log.info("[WORD] 주간 수집 단어 전체 초기화 시작")
        weekCollectedWordRepository.deleteAll()
        log.info("[WORD] 주간 수집 단어 전체 초기화 완료")
    }
}
