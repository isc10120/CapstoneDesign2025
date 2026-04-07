package com.example.zamgavocafront.pve

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.zamgavocafront.R
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.SkillCache
import com.example.zamgavocafront.api.dto.*
import com.example.zamgavocafront.pvp.CollectedCardManager
import kotlinx.coroutines.*

class PveQuestionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID     = "word_id"
        const val EXTRA_WORD_TEXT   = "word_text"
        const val EXTRA_SKILL_NAME  = "skill_name"
        const val EXTRA_SKILL_GRADE = "skill_grade"
        const val EXTRA_SKILL_DAMAGE = "skill_damage"
        const val EXTRA_EFFECT_TYPE = "effect_type"
        const val RESULT_WORD_ID    = "result_word_id"
        private const val PASS_SCORE = 60
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var tvCardSkillName: TextView
    private lateinit var tvCardGrade: TextView
    private lateinit var tvCardEffect: TextView
    private lateinit var tvCardDamage: TextView
    private lateinit var tvKoreanSentence: TextView
    private lateinit var tvWordHint: TextView
    private lateinit var etAnswer: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnSkip: Button
    private lateinit var tvFeedback: TextView

    private var wordId = 0
    private var wordText = ""
    private var skillName = ""
    private var skillGrade = ""
    private var skillDamage = 0
    private var effectTypeName = "ATTACK"

    private var koreanSentence = ""
    private var idealTranslation = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pve_question)

        wordId       = intent.getIntExtra(EXTRA_WORD_ID, 0)
        wordText     = intent.getStringExtra(EXTRA_WORD_TEXT) ?: ""
        skillName    = intent.getStringExtra(EXTRA_SKILL_NAME) ?: wordText
        skillGrade   = intent.getStringExtra(EXTRA_SKILL_GRADE) ?: "동급"
        skillDamage  = intent.getIntExtra(EXTRA_SKILL_DAMAGE, 50)
        effectTypeName = intent.getStringExtra(EXTRA_EFFECT_TYPE) ?: "ATTACK"

        tvCardSkillName  = findViewById(R.id.tv_card_skill_name)
        tvCardGrade      = findViewById(R.id.tv_card_grade)
        tvCardEffect     = findViewById(R.id.tv_card_effect)
        tvCardDamage     = findViewById(R.id.tv_card_damage)
        tvKoreanSentence = findViewById(R.id.tv_korean_sentence)
        tvWordHint       = findViewById(R.id.tv_word_hint)
        etAnswer         = findViewById(R.id.et_answer)
        btnSubmit        = findViewById(R.id.btn_submit)
        btnSkip          = findViewById(R.id.btn_skip)
        tvFeedback       = findViewById(R.id.tv_feedback)

        val effect = runCatching { SkillEffectType.valueOf(effectTypeName) }.getOrDefault(SkillEffectType.ATTACK)

        tvCardSkillName.text = skillName
        tvCardGrade.text = skillGrade
        tvCardGrade.backgroundTintList = android.content.res.ColorStateList.valueOf(gradeColor(skillGrade))
        tvCardEffect.text = "${effect.icon} ${effect.displayName}"
        tvCardDamage.text = "데미지: $skillDamage"

        btnSubmit.setOnClickListener { submitAnswer() }
        btnSkip.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        loadQuestion()
    }

    private fun loadQuestion() {
        tvKoreanSentence.text = "문제 로딩 중..."
        tvWordHint.text = ""
        btnSubmit.isEnabled = false

        scope.launch {
            try {
                val resp = ApiClient.api.createQuestion(
                    CreateQuestionRequest(targetWord = wordText, userLevel = "intermediate")
                )
                if (resp.success) {
                    koreanSentence = resp.koreanSentence ?: ""
                    idealTranslation = resp.ideal ?: ""
                    tvKoreanSentence.text = koreanSentence
                    tvWordHint.text = resp.wordHint?.let { "힌트: $it" } ?: ""
                    btnSubmit.isEnabled = true
                } else {
                    tvKoreanSentence.text = "문제 생성 실패: ${resp.error}"
                }
            } catch (e: Exception) {
                tvKoreanSentence.text = "오류: ${e.message}"
                btnSubmit.isEnabled = true
            }
        }
    }

    private fun submitAnswer() {
        val answer = etAnswer.text.toString().trim()
        if (answer.isEmpty()) {
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
            try {
                val evalResp = ApiClient.api.evaluate(
                    EvaluateRequest(
                        koreanSentence = koreanSentence,
                        userAnswer = answer,
                        idealTranslation = idealTranslation,
                        targetWord = wordText,
                        userLevel = "intermediate"
                    )
                )
                val score = evalResp.score ?: 0
                if (score >= PASS_SCORE) {
                    onCorrectAnswer(evalResp)
                } else {
                    onWrongAnswer(score, evalResp)
                }
            } catch (e: Exception) {
                tvFeedback.text = "채점 오류: ${e.message}"
                tvFeedback.setBackgroundColor(Color.parseColor("#FFEBEE"))
                tvFeedback.setTextColor(Color.parseColor("#C62828"))
                tvFeedback.visibility = View.VISIBLE
                btnSubmit.isEnabled = true
            }
        }
    }

    private suspend fun onCorrectAnswer(evalResp: EvaluateResponse) {
        // 스킬 카드가 아직 수집 안 됐으면 수집 처리 (PVE에서 처음 정답 시)
        val existing = CollectedCardManager.getCards(this).find { it.wordId == wordId }
        if (existing == null) {
            try {
                val skillResp = SkillCache.get(wordId)
                    ?: ApiClient.api.generateSkill(SkillGenerateRequest(word = wordText, meaningKo = ""))
                CollectedCardManager.addCard(
                    this,
                    CollectedCardManager.CollectedCard(
                        wordId = wordId,
                        word = wordText,
                        skillName = skillResp.name,
                        skillDescription = skillResp.description,
                        damage = skillResp.damage,
                        imageBase64 = skillResp.imageBase64,
                        grade = skillGrade
                    )
                )
                showSkillActivationDialog(skillResp.imageBase64, newCollect = true)
            } catch (e: Exception) {
                showSkillActivationDialog(null, newCollect = false)
            }
        } else {
            showSkillActivationDialog(existing.imageBase64, newCollect = false)
        }
    }

    private fun showSkillActivationDialog(imageBase64: String?, newCollect: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_skill_card, null)
        val ivImage  = dialogView.findViewById<ImageView>(R.id.iv_skill_image)
        val tvGrade  = dialogView.findViewById<TextView>(R.id.tv_skill_grade)
        val tvName   = dialogView.findViewById<TextView>(R.id.tv_skill_name)
        val tvDamage = dialogView.findViewById<TextView>(R.id.tv_skill_damage)
        val tvDesc   = dialogView.findViewById<TextView>(R.id.tv_skill_desc)

        if (imageBase64 != null) {
            try {
                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                ivImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Exception) { }
        }

        tvGrade.text = skillGrade
        tvGrade.backgroundTintList = android.content.res.ColorStateList.valueOf(gradeColor(skillGrade))
        tvName.text = if (newCollect) "🆕 $skillName 획득!" else "✅ $skillName 발동!"
        tvDamage.text = "⚔ 기본 데미지: $skillDamage"
        tvDesc.text = if (newCollect) "카드가 수집 목록에 추가되었습니다!" else "스킬이 발동됩니다!"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("발동!") { _, _ ->
                val data = android.content.Intent().putExtra(RESULT_WORD_ID, wordId)
                setResult(RESULT_OK, data)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun onWrongAnswer(score: Int, evalResp: EvaluateResponse) {
        val sb = StringBuilder("❌ 오답! (점수: $score)\n")
        evalResp.feedback?.let { sb.appendLine("\n피드백: $it") }
        evalResp.correction?.let { sb.appendLine("수정: $it") }

        tvFeedback.text = sb.toString().trim()
        tvFeedback.setBackgroundColor(Color.parseColor("#FFEBEE"))
        tvFeedback.setTextColor(Color.parseColor("#C62828"))
        tvFeedback.visibility = View.VISIBLE
        btnSubmit.isEnabled = true
        btnSubmit.text = "다시 도전!"
    }

    private fun gradeColor(grade: String): Int = when (grade) {
        "금급" -> Color.parseColor("#FFC107")
        "은급" -> Color.parseColor("#9E9E9E")
        else   -> Color.parseColor("#CD7F32")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
