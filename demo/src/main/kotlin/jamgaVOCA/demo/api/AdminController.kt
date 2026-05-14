package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.dto.ApiResponse
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
import com.fasterxml.jackson.annotation.JsonProperty
import jamgaVOCA.demo.domain.auth.RefreshTokenRepository
import jamgaVOCA.demo.infra.S3UploadService
import jamgaVOCA.demo.infra.ai.AiChatClient
import jamgaVOCA.demo.infra.ai.AiImageClient
import jamgaVOCA.demo.domain.battle.BattleRepository
import jamgaVOCA.demo.domain.dailynudgeword.DailyNudgeWordRepository
import jamgaVOCA.demo.domain.skill.Skill
import jamgaVOCA.demo.domain.skill.SkillRepository
import jamgaVOCA.demo.domain.skill.SkillType
import jamgaVOCA.demo.domain.user.User
import jamgaVOCA.demo.domain.user.UserRepository
import jamgaVOCA.demo.domain.userwordskill.UserWordSkillRepository
import jamgaVOCA.demo.domain.weekcollectedword.WeekCollectedWordRepository
import jamgaVOCA.demo.domain.word.PartOfSpeech
import jamgaVOCA.demo.domain.word.Word
import jamgaVOCA.demo.domain.word.WordLevel
import jamgaVOCA.demo.domain.word.WordRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
@Transactional
class AdminController(
    private val wordRepository: WordRepository,
    private val skillRepository: SkillRepository,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val dailyNudgeWordRepository: DailyNudgeWordRepository,
    private val weekCollectedWordRepository: WeekCollectedWordRepository,
    private val userWordSkillRepository: UserWordSkillRepository,
    private val battleRepository: BattleRepository,
    private val aiChatClient: AiChatClient,
    private val aiImageClient: AiImageClient,
    private val s3UploadService: S3UploadService,
) {

    // ─── Word ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @GetMapping("/words")
    fun getWords(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) level: String?,
    ): ApiResponse<List<WordAdminRes>> {
        val words = when {
            !search.isNullOrBlank() -> wordRepository.findAllByEnglishWordContainingIgnoreCase(search)
            !level.isNullOrBlank()  -> wordRepository.findAllByWordLevel(WordLevel.valueOf(level.uppercase()))
            else                    -> wordRepository.findAll()
        }
        return ApiResponse.success(words.map { it.toRes() })
    }

    @PostMapping("/words")
    fun createWord(@RequestBody req: WordReq): ApiResponse<WordAdminRes> {
        val word = Word(
            englishWord  = req.englishWord,
            koreanMeaning = req.koreanMeaning,
            partOfSpeech = PartOfSpeech.valueOf(req.partOfSpeech.uppercase()),
            wordLevel    = WordLevel.valueOf(req.wordLevel.uppercase()),
            exampleEn    = req.exampleEn ?: "",
            exampleKr    = req.exampleKr ?: "",
        )
        return ApiResponse.success(wordRepository.save(word).toRes())
    }

    @PutMapping("/words/{id}")
    fun updateWord(@PathVariable id: Long, @RequestBody req: WordReq): ApiResponse<WordAdminRes> {
        val word = wordRepository.findById(id).orElseThrow { AppException(ErrorCode.WORD_NOT_FOUND) }
        word.englishWord  = req.englishWord
        word.koreanMeaning = req.koreanMeaning
        word.partOfSpeech = PartOfSpeech.valueOf(req.partOfSpeech.uppercase())
        word.wordLevel    = WordLevel.valueOf(req.wordLevel.uppercase())
        if (req.exampleEn != null) word.exampleEn = req.exampleEn
        if (req.exampleKr != null) word.exampleKr = req.exampleKr
        return ApiResponse.success(wordRepository.save(word).toRes())
    }

    @DeleteMapping("/words/{id}")
    fun deleteWord(@PathVariable id: Long): ApiResponse<Nothing> {
        val word = wordRepository.findById(id).orElseThrow { AppException(ErrorCode.WORD_NOT_FOUND) }
        userWordSkillRepository.deleteAll(word.userWordSkills)
        word.skills.forEach { skill ->
            userWordSkillRepository.deleteAll(skill.userWordSkills)
            skillRepository.delete(skill)
        }
        wordRepository.delete(word)
        return ApiResponse.success(null)
    }

    // ─── Skill ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @GetMapping("/skills")
    fun getSkills(@RequestParam(required = false) wordId: Long?): ApiResponse<List<SkillAdminRes>> {
        val skills = if (wordId != null) skillRepository.findAllByWordId(wordId)
                     else skillRepository.findAll()
        return ApiResponse.success(skills.map { it.toRes() })
    }

    @PutMapping("/skills/{id}")
    fun updateSkill(@PathVariable id: Long, @RequestBody req: SkillReq): ApiResponse<SkillAdminRes> {
        val skill = skillRepository.findById(id).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }
        if (req.name        != null) skill.name        = req.name
        if (req.explanation != null) skill.explanation = req.explanation
        if (req.damage      != null) skill.damage      = req.damage
        if (req.skillType   != null) skill.skillType   = SkillType.valueOf(req.skillType.uppercase())
        if (req.lasting     != null) skill.lasting     = req.lasting
        return ApiResponse.success(skillRepository.save(skill).toRes())
    }

    @PostMapping("/skills/{id}/preview-images")
    fun previewImages(@PathVariable id: Long): ApiResponse<List<String>> {
        val skill = skillRepository.findById(id).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }
        val prompt = """
            For a 2D pixel RPG skill named "${skill.name}" with description: "${skill.explanation}"
            Generate a short English image description for a pixel art skill effect.
            Respond with only JSON: {"image_desc": "..."}
        """.trimIndent()
        data class ImageDescResp(@JsonProperty("image_desc") val imageDesc: String)
        val desc = aiChatClient.callJson(prompt, null, ImageDescResp::class.java).imageDesc
        val img1 = aiImageClient.requestImageBase64WithRetry(desc)
        val img2 = aiImageClient.requestImageBase64WithRetry(desc)
        return ApiResponse.success(listOf(img1, img2))
    }

    @PostMapping("/skills/{id}/confirm-image")
    fun confirmImage(@PathVariable id: Long, @RequestBody req: ConfirmImageReq): ApiResponse<SkillAdminRes> {
        val skill = skillRepository.findById(id).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }
        val imageUrl = s3UploadService.uploadBase64Image(req.base64)
        skill.imageUrl = imageUrl
        return ApiResponse.success(skillRepository.save(skill).toRes())
    }

    @DeleteMapping("/skills/{id}")
    fun deleteSkill(@PathVariable id: Long): ApiResponse<Nothing> {
        val skill = skillRepository.findById(id).orElseThrow { AppException(ErrorCode.SKILL_NOT_FOUND) }
        userWordSkillRepository.deleteAll(skill.userWordSkills)
        skillRepository.delete(skill)
        return ApiResponse.success(null)
    }

    // ─── User ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @GetMapping("/users")
    fun getUsers(): ApiResponse<List<UserAdminRes>> =
        ApiResponse.success(userRepository.findAll().map { it.toRes() })

    @PutMapping("/users/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody req: UserReq): ApiResponse<UserAdminRes> {
        val user = userRepository.findById(id).orElseThrow { AppException(ErrorCode.USER_NOT_FOUND) }
        if (req.nickname != null) user.nickname = req.nickname
        if (req.expPoint != null) user.expPoint = req.expPoint
        return ApiResponse.success(userRepository.save(user).toRes())
    }

    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: Long): ApiResponse<Nothing> {
        val user = userRepository.findById(id).orElseThrow { AppException(ErrorCode.USER_NOT_FOUND) }
        val battles = battleRepository.findAllByUserAOrUserBOrderByWeekStartDesc(user, user)
        battleRepository.deleteAll(battles)
        refreshTokenRepository.deleteByUser(user)
        dailyNudgeWordRepository.deleteAllByUserId(id)
        weekCollectedWordRepository.deleteAllByUserId(id)
        userRepository.delete(user)
        return ApiResponse.success(null)
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    data class WordReq(
        val englishWord: String,
        val koreanMeaning: String,
        val partOfSpeech: String,
        val wordLevel: String,
        val exampleEn: String? = null,
        val exampleKr: String? = null,
    )

    data class WordAdminRes(
        val id: Long,
        val englishWord: String,
        val koreanMeaning: String,
        val partOfSpeech: String,
        val wordLevel: String,
        val exampleEn: String,
        val exampleKr: String,
        val skillCount: Int,
    )

    data class SkillReq(
        val name: String? = null,
        val explanation: String? = null,
        val damage: Int? = null,
        val skillType: String? = null,
        val lasting: Int? = null,
    )

    data class SkillAdminRes(
        val id: Long,
        val wordId: Long,
        val wordName: String,
        val name: String,
        val explanation: String,
        val imageUrl: String,
        val damage: Int,
        val skillType: String,
        val lasting: Int?,
    )

    data class ConfirmImageReq(val base64: String)

    data class UserReq(
        val nickname: String? = null,
        val expPoint: Int? = null,
    )

    data class UserAdminRes(
        val id: Long,
        val nickname: String,
        val email: String,
        val expPoint: Int,
        val createdAt: String,
        val isDummy: Boolean,
    )

    private fun Word.toRes() = WordAdminRes(
        id            = id!!,
        englishWord   = englishWord,
        koreanMeaning = koreanMeaning,
        partOfSpeech  = partOfSpeech.name,
        wordLevel     = wordLevel.name,
        exampleEn     = exampleEn,
        exampleKr     = exampleKr,
        skillCount    = skills.size,
    )

    private fun Skill.toRes() = SkillAdminRes(
        id          = id!!,
        wordId      = word.id!!,
        wordName    = word.englishWord,
        name        = name,
        explanation = explanation,
        imageUrl    = imageUrl,
        damage      = damage,
        skillType   = skillType.name,
        lasting     = lasting,
    )

    private fun User.toRes() = UserAdminRes(
        id        = id!!,
        nickname  = nickname,
        email     = email,
        expPoint  = expPoint,
        createdAt = createdAt.toString(),
        isDummy   = isDummy,
    )
}
