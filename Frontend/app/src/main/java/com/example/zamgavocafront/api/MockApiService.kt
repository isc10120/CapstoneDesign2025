package com.example.zamgavocafront.api

import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.dto.*

/**
 * 백엔드 API 키 없이 테스트할 수 있는 Mock 구현체.
 * ApiClient.USE_MOCK = true 일 때 사용됨.
 * 실제 서버 연결 시 ApiClient.USE_MOCK = false 로 변경.
 */
class MockApiService : ZamgaVocaApiService {

    // ── /test 단어 Mock API (구버전) ──────────────────────────────────────

    override suspend fun getDailyVocaList(num: Int): DailyVocaListResponse {
        val list = listOf(
            VocaDto(1L, "apple", "사과", "과일의 한 종류", "This is an apple.", "이것은 사과이다.",
                "apple rain", "사과의 비를 내려 공격", "", 50, 1),
            VocaDto(2L, "banana", "바나나", "원숭이가 좋아하는 과일", "This is a banana.", "이것은 바나나이다.",
                "banana rain", "바나나의 비를 내려 공격", "", 70, 2)
        )
        return DailyVocaListResponse(list.take(num))
    }

    override suspend fun collectWord(req: CollectRequest): CollectResponse =
        CollectResponse(ok = true, collectedId = req.id)

    override suspend fun getCollectedVocaList(): CollectedVocaListResponse =
        CollectedVocaListResponse(vocaIdList = emptyList(), vocaInfoURL = "/test/voca-info")

    override suspend fun getVocaInfo(id: Long): VocaDto =
        VocaDto(id, "word$id", "의미$id", "설명", "example", "예문", "스킬명", "스킬설명", "", 50, 1)

    // ── 번역 퀴즈: WordRepository 예문 활용 ───────────────────────────

    override suspend fun createQuestion(req: CreateQuestionRequest): CreateQuestionResponse {
        val word = WordRepository.allWords.find {
            it.word.equals(req.targetWord, ignoreCase = true)
        }
        return if (word != null) {
            CreateQuestionResponse(
                success = true,
                koreanSentence = word.exampleKr,
                targetWord = word.word,
                wordHint = makeHint(word.word),
                ideal = word.exampleEn
            )
        } else {
            CreateQuestionResponse(
                success = true,
                koreanSentence = "다음 단어를 영어로 쓰세요: (${req.targetWord}의 의미를 생각해보세요)",
                targetWord = req.targetWord,
                wordHint = makeHint(req.targetWord),
                ideal = req.targetWord
            )
        }
    }

    override suspend fun evaluate(req: EvaluateRequest): EvaluateResponse {
        val correct = req.userAnswer.contains(req.targetWord, ignoreCase = true)
        return if (correct) {
            EvaluateResponse(
                success = true,
                score = 80,
                feedback = "정답! 단어를 정확히 사용했습니다.",
                idealAnswer = req.idealTranslation
            )
        } else {
            EvaluateResponse(
                success = true,
                score = 25,
                feedback = "단어 '${req.targetWord}'를 포함하여 번역해보세요.",
                correction = req.idealTranslation,
                idealAnswer = req.idealTranslation
            )
        }
    }

    // ── 스킬 생성: 단어별 미리 정의된 mock 스킬 ──────────────────────

    override suspend fun generateSkill(req: SkillGenerateRequest): SkillGenerateResponse {
        val (name, desc, dmg) = MOCK_SKILLS[req.word.lowercase()]
            ?: Triple("${req.word}의 일격", "${req.meaningKo}의 힘으로 상대를 공격한다.", 75)

        return SkillGenerateResponse(
            id = null,
            word = req.word,
            name = name,
            description = desc,
            damage = dmg,
            imageDesc = "$name 스킬 이미지",
            imageBase64 = null,
            imageStatus = ImageStatus.FAILED,
            imageError = "mock mode"
        )
    }

    // ── /api/v1 Word API ──────────────────────────────────────────────

    override suspend fun getDailyWordList(): ApiResponse<List<WordResponse>> {
        val list = WordRepository.allWords.map { w ->
            WordResponse(
                id = w.id.toLong(),
                word = w.word,
                definition = w.meaning,
                partOfSpeech = "unknown",
                example = w.exampleEn,
                exampleKor = w.exampleKr,
                nudge = 0
            )
        }
        return ApiResponse(success = true, data = list)
    }

    override suspend fun getNewDailyWordList(level: String): ApiResponse<List<WordResponse>> {
        val list = WordRepository.allWords.map { w ->
            WordResponse(
                id = w.id.toLong(),
                word = w.word,
                definition = w.meaning,
                partOfSpeech = "unknown",
                example = w.exampleEn,
                exampleKor = w.exampleKr
            )
        }
        return ApiResponse(success = true, data = list)
    }

    override suspend fun getWordInfo(id: Long): ApiResponse<WordResponse> {
        val w = WordRepository.allWords.find { it.id.toLong() == id }
            ?: return ApiResponse(success = false, data = null,
                error = ApiError("NOT_FOUND", "단어를 찾을 수 없습니다."))
        return ApiResponse(
            success = true,
            data = WordResponse(
                id = w.id.toLong(),
                word = w.word,
                definition = w.meaning,
                partOfSpeech = "unknown",
                example = w.exampleEn,
                exampleKor = w.exampleKr
            )
        )
    }

    override suspend fun updateNudge(nudgeRequests: List<NudgeUpdateRequest>): ApiResponse<Any?> =
        ApiResponse(success = true, data = null)

    // ── /api/v1 Skill API ─────────────────────────────────────────────

    override suspend fun getCollectedSkillList(): ApiResponse<List<SkillResponse>> {
        val list = MOCK_SKILLS.entries.mapIndexed { idx, (word, triple) ->
            val w = WordRepository.allWords.find { it.word == word }
            SkillResponse(
                skillId = (idx + 1).toLong(),
                name = triple.first,
                explain = triple.second,
                damage = triple.third,
                skillType = "attack",
                lasting = null,
                imageURL = "",
                wordId = w?.id?.toLong() ?: (idx + 1).toLong()
            )
        }
        return ApiResponse(success = true, data = list)
    }

    override suspend fun getSkillInfo(id: Long): ApiResponse<SkillResponse> {
        // wordId로 WordRepository에서 단어 텍스트를 찾아 MOCK_SKILLS 조회
        val wordText = WordRepository.allWords.find { it.id.toLong() == id }?.word?.lowercase()
        val triple = (wordText?.let { MOCK_SKILLS[it] })
            ?: MOCK_SKILLS.entries.toList().getOrNull((id - 1).toInt())?.value
            ?: return ApiResponse(success = false, data = null,
                error = ApiError("NOT_FOUND", "스킬을 찾을 수 없습니다."))
        return ApiResponse(
            success = true,
            data = SkillResponse(
                skillId = id,
                name = triple.first,
                explain = triple.second,
                damage = triple.third,
                skillType = "attack",
                lasting = null,
                imageURL = "",
                wordId = id
            )
        )
    }

    override suspend fun collectSkill(req: CollectSkillRequest): ApiResponse<Any?> =
        ApiResponse(success = true, data = null)

    override suspend fun getWeekCollectedList(): ApiResponse<List<WordResponse>> {
        val list = WordRepository.allWords.take(5).map { w ->
            WordResponse(
                id = w.id.toLong(),
                word = w.word,
                definition = w.meaning,
                partOfSpeech = "unknown",
                example = w.exampleEn,
                exampleKor = w.exampleKr
            )
        }
        return ApiResponse(success = true, data = list)
    }

    // ── /api/v1 Auth API ──────────────────────────────────────────────

    override suspend fun signUp(req: SignUpRequest): ApiResponse<Any?> =
        ApiResponse(success = true, data = null)

    override suspend fun signIn(req: SignInRequest): ApiResponse<SignInResponse> =
        ApiResponse(
            success = true,
            data = SignInResponse(
                userId = 1L,
                email = req.email,
                nickName = "테스트유저",
                nudgeEnabled = true,
                nudgeInterval = 20,
                silentNudge = emptyList()
            )
        )

    // ── helpers ──────────────────────────────────────────────────────

    private fun makeHint(word: String): String {
        if (word.length <= 2) return word
        return "${word.first()}${"_".repeat(word.length - 2)}${word.last()}"
    }

    companion object {
        private val MOCK_SKILLS: Map<String, Triple<String, String, Int>> = mapOf(
            "ephemeral"     to Triple("찰나의 베기",    "순간에 모든 것을 담은 강렬한 베기를 날린다.", 120),
            "serendipity"   to Triple("행운의 빛",      "예기치 않은 행운이 상대를 강타한다.",        90),
            "eloquent"      to Triple("설득의 파동",    "유창한 언변이 파동이 되어 상대를 꿰뚫는다.", 100),
            "benevolent"    to Triple("자비의 방패",    "자비로운 기운이 역으로 적에게 충격을 준다.", 80),
            "melancholy"    to Triple("우울의 안개",    "짙은 우울이 상대를 잠식한다.",               110),
            "perseverance"  to Triple("불굴의 일격",    "포기하지 않는 의지가 폭발적인 일격이 된다.", 150),
            "ambiguous"     to Triple("혼돈의 파장",    "모호한 기운이 상대를 혼란에 빠뜨린다.",      95),
            "tenacious"     to Triple("집착의 족쇄",    "끈질긴 힘이 상대를 강하게 속박한다.",        130),
            "profound"      to Triple("심연의 충격",    "심오한 힘이 상대에게 깊은 충격을 준다.",     140),
            "candid"        to Triple("진실의 화살",    "솔직한 진실이 화살이 되어 상대를 꿰뚫는다.", 105),
            "apple"         to Triple("사과 폭풍",      "수많은 사과가 상대에게 쏟아진다.",           50),
            "banana"        to Triple("바나나 미끄러짐","바나나 껍질로 상대를 넘어뜨린다.",           70),
        )
    }
}
