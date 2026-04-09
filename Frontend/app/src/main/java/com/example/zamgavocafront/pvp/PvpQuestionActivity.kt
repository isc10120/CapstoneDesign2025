package com.example.zamgavocafront.pvp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.zamgavocafront.R
import coil.load
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.SkillCache
import com.example.zamgavocafront.api.dto.*
import com.example.zamgavocafront.model.Difficulty
import kotlinx.coroutines.*

class PvpQuestionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID = "word_id"
        const val EXTRA_WORD_TEXT = "word_text"
        const val EXTRA_WORD_MEANING = "word_meaning"
        const val EXTRA_DIFFICULTY = "difficulty"
        private const val PASS_SCORE = 60
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var tvWordHeader: TextView
    private lateinit var tvGradeBadge: TextView
    private lateinit var tvKoreanSentence: TextView
    private lateinit var tvWordHint: TextView
    private lateinit var tvAttacksInfo: TextView
    private lateinit var etAnswer: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvFeedback: TextView

    private var wordId = 0
    private var wordText = ""
    private var wordMeaning = ""
    private var difficulty = Difficulty.MEDIUM
    private var idealTranslation = ""
    private var koreanSentence = ""
    private var userLevel = "intermediate"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pvp_question)

        wordId = intent.getIntExtra(EXTRA_WORD_ID, 0)
        wordText = intent.getStringExtra(EXTRA_WORD_TEXT) ?: ""
        wordMeaning = intent.getStringExtra(EXTRA_WORD_MEANING) ?: ""
        difficulty = intent.getStringExtra(EXTRA_DIFFICULTY)
            ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: Difficulty.MEDIUM

        userLevel = when (difficulty) {
            Difficulty.EASY -> "beginner"
            Difficulty.MEDIUM -> "intermediate"
            Difficulty.HARD -> "advanced"
        }

        tvWordHeader = findViewById(R.id.tv_word_header)
        tvGradeBadge = findViewById(R.id.tv_grade_badge)
        tvKoreanSentence = findViewById(R.id.tv_korean_sentence)
        tvWordHint = findViewById(R.id.tv_word_hint)
        tvAttacksInfo = findViewById(R.id.tv_attacks_info)
        etAnswer = findViewById(R.id.et_answer)
        btnSubmit = findViewById(R.id.btn_submit)
        tvFeedback = findViewById(R.id.tv_feedback)

        val (gradeText, gradeColor) = gradeInfo(difficulty)
        tvWordHeader.text = "단어: $wordText"
        tvGradeBadge.text = gradeText
        tvGradeBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(gradeColor)

        updateAttackInfo()
        loadQuestion()

        btnSubmit.setOnClickListener { submitAnswer() }
        findViewById<android.widget.ImageButton>(R.id.btn_close).setOnClickListener { finish() }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.no_anim, R.anim.slide_down)
    }

    private fun updateAttackInfo() {
        val left = PvpWordManager.getAttacksLeft(this)
        tvAttacksInfo.text = "남은 공격: $left / 10"
        btnSubmit.isEnabled = left > 0
        if (left <= 0) {
            btnSubmit.text = "공격 횟수 소진"
        }
    }

    private fun loadQuestion() {
        tvKoreanSentence.text = "문제 로딩 중..."
        tvWordHint.text = ""
        btnSubmit.isEnabled = false

        scope.launch {
            try {
                val response = ApiClient.mockApi.createQuestion(
                    CreateQuestionRequest(targetWord = wordText, userLevel = userLevel)
                )
                if (response.success) {
                    koreanSentence = response.koreanSentence ?: ""
                    idealTranslation = response.ideal ?: ""
                    tvKoreanSentence.text = koreanSentence
                    tvWordHint.text = response.wordHint?.let { "힌트: $it" } ?: ""
                    updateAttackInfo()
                } else {
                    tvKoreanSentence.text = "문제 생성 실패: ${response.error}"
                }
            } catch (e: Exception) {
                tvKoreanSentence.text = "네트워크 오류: ${e.message}\n\n(백엔드 서버가 실행 중인지 확인하세요)"
            }
        }
    }

    private fun submitAnswer() {
        val userAnswer = etAnswer.text.toString().trim()
        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "답을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        if (koreanSentence.isEmpty()) {
            Toast.makeText(this, "문제를 먼저 로딩해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        tvFeedback.visibility = View.GONE

        scope.launch {
            // 공격 횟수 차감 (맞든 틀리든)
            PvpWordManager.consumeAttack(this@PvpQuestionActivity)
            updateAttackInfo()

            try {
                val evalResponse = ApiClient.mockApi.evaluate(
                    EvaluateRequest(
                        koreanSentence = koreanSentence,
                        userAnswer = userAnswer,
                        idealTranslation = idealTranslation,
                        targetWord = wordText,
                        userLevel = userLevel
                    )
                )

                val score = evalResponse.score ?: 0
                val isCorrect = score >= PASS_SCORE

                if (isCorrect) {
                    onCorrectAnswer(score, evalResponse)
                } else {
                    onWrongAnswer(score, evalResponse)
                }
            } catch (e: Exception) {
                tvFeedback.text = "채점 오류: ${e.message}"
                tvFeedback.setBackgroundColor(Color.parseColor("#FFEBEE"))
                tvFeedback.setTextColor(Color.parseColor("#C62828"))
                tvFeedback.visibility = View.VISIBLE
                updateAttackInfo()
            }
        }
    }

    private suspend fun onCorrectAnswer(score: Int, evalResponse: EvaluateResponse) {
        // 단어 사용 완료 처리
        PvpWordManager.markWordUsed(this, wordId)

        // 스킬 카드 조회: 캐시 → 실서버 → mock 순으로 fallback
        try {
            val skillResponse: SkillResponse? = SkillCache.get(wordId)
                ?: run {
                    try {
                        val resp = ApiClient.api.getSkillInfo(wordId.toLong())
                        resp.data
                    } catch (_: Exception) { null }
                }
                ?: ApiClient.mockApi.getSkillInfo(wordId.toLong()).data

            if (skillResponse != null) {
                // 데미지 누적
                PvpWordManager.addDamage(this, skillResponse.damage)

                // 수집 목록에 추가 (로컬)
                val (gradeText, _) = gradeInfo(difficulty)
                CollectedCardManager.addCard(
                    this,
                    CollectedCardManager.CollectedCard(
                        wordId = wordId,
                        word = wordText,
                        skillName = skillResponse.name,
                        skillDescription = skillResponse.explain,
                        damage = skillResponse.damage,
                        imageBase64 = null,
                        grade = gradeText,
                        imageUrl = skillResponse.imageURL.takeIf { it.isNotBlank() }
                    )
                )

                // 스킬 수집 백엔드 동기화
                try {
                    ApiClient.api.collectSkill(
                        CollectSkillRequest(
                            skillId = skillResponse.skillId,
                            wordId = wordId.toLong()
                        )
                    )
                } catch (_: Exception) { }

                showSkillCardDialog(skillResponse, gradeText)
            } else {
                PvpWordManager.addDamage(this, 50)
                val (gradeText, _) = gradeInfo(difficulty)
                showCorrectFallback(score, gradeText)
            }
        } catch (e: Exception) {
            // 스킬 조회 실패해도 데미지 기본값 처리 (50)
            PvpWordManager.addDamage(this, 50)
            val (gradeText, _) = gradeInfo(difficulty)
            showCorrectFallback(score, gradeText)
        }
    }

    private fun onWrongAnswer(score: Int, evalResponse: EvaluateResponse) {
        val sb = StringBuilder("❌ 오답! (점수: $score / 100)\n")
        evalResponse.feedback?.let { sb.append("\n피드백: $it") }
        evalResponse.correction?.let { sb.append("\n수정: $it") }

        tvFeedback.text = sb.toString()
        tvFeedback.setBackgroundColor(Color.parseColor("#FFEBEE"))
        tvFeedback.setTextColor(Color.parseColor("#C62828"))
        tvFeedback.visibility = View.VISIBLE

        val attacksLeft = PvpWordManager.getAttacksLeft(this)
        if (attacksLeft > 0) {
            btnSubmit.isEnabled = true
            btnSubmit.text = "다시 도전! (${attacksLeft}회 남음)"
        }
    }

    private fun showSkillCardDialog(skill: SkillResponse, gradeText: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_skill_card, null)

        val ivImage      = dialogView.findViewById<ImageView>(R.id.iv_skill_image)
        val tvGrade      = dialogView.findViewById<TextView>(R.id.tv_skill_grade)
        val tvName       = dialogView.findViewById<TextView>(R.id.tv_skill_name)
        val tvDamage     = dialogView.findViewById<TextView>(R.id.tv_skill_damage)
        val tvDesc       = dialogView.findViewById<TextView>(R.id.tv_skill_desc)
        val tvTotal      = dialogView.findViewById<TextView>(R.id.tv_total_damage)
        val tvCollected  = dialogView.findViewById<TextView>(R.id.tv_collected_badge)

        ivImage.load(skill.imageURL.takeIf { it.isNotBlank() }) {
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_gallery)
        }

        val (_, gradeColor) = gradeInfo(difficulty)
        tvGrade.text = gradeText
        tvGrade.backgroundTintList = android.content.res.ColorStateList.valueOf(gradeColor)
        tvName.text = skill.name
        tvDesc.text = skill.explain
        tvDamage.text = "+${skill.damage} 데미지!"
        tvTotal.text = "누적 데미지: ${PvpWordManager.getTotalDamage(this)}"
        tvCollected.text = "📦 '${skill.name}' 카드 수집 완료"

        // 카드 등장 애니메이션
        dialogView.scaleX = 0.6f
        dialogView.scaleY = 0.6f
        dialogView.alpha = 0f
        dialogView.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(250)
            .start()

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("⚔ 공격 완료!") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showCorrectFallback(score: Int, gradeText: String) {
        tvFeedback.text = "✅ 정답! (점수: $score / 100)\n스킬 생성 중 오류가 발생했지만 데미지는 적용되었어요."
        tvFeedback.setBackgroundColor(Color.parseColor("#E8F5E9"))
        tvFeedback.setTextColor(Color.parseColor("#2E7D32"))
        tvFeedback.visibility = View.VISIBLE
        btnSubmit.isEnabled = false
    }

    private fun gradeInfo(d: Difficulty): Pair<String, Int> = when (d) {
        Difficulty.HARD -> "금급" to Color.parseColor("#FFC107")
        Difficulty.MEDIUM -> "은급" to Color.parseColor("#9E9E9E")
        Difficulty.EASY -> "동급" to Color.parseColor("#CD7F32")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
