package com.example.zamgavocafront.pve

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.zamgavocafront.R
import com.example.zamgavocafront.viewmodel.PveQuestionUiState
import com.example.zamgavocafront.viewmodel.PveQuestionViewModel
import kotlinx.coroutines.launch

class PveQuestionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID      = "word_id"
        const val EXTRA_WORD_TEXT    = "word_text"
        const val EXTRA_SKILL_NAME   = "skill_name"
        const val EXTRA_SKILL_GRADE  = "skill_grade"
        const val EXTRA_SKILL_DAMAGE = "skill_damage"
        const val EXTRA_EFFECT_TYPE  = "effect_type"
        const val RESULT_WORD_ID     = "result_word_id"
    }

    private val viewModel: PveQuestionViewModel by viewModels()

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

    // Intent에서 받아온 스킬 카드 표시용 정보
    private var skillName = ""
    private var skillGrade = ""
    private var skillDamage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pve_question)

        // ViewModel에 전달
        viewModel.wordId    = intent.getIntExtra(EXTRA_WORD_ID, 0)
        viewModel.wordText  = intent.getStringExtra(EXTRA_WORD_TEXT) ?: ""

        // 스킬 카드 표시용 (ViewModel 불필요 — 화면에만 표시)
        skillName  = intent.getStringExtra(EXTRA_SKILL_NAME) ?: viewModel.wordText
        skillGrade = intent.getStringExtra(EXTRA_SKILL_GRADE) ?: "동급"

        // 카드 등급 → 퀴즈 난이도 변환
        viewModel.userLevel = when (skillGrade) {
            "금급" -> "advanced"
            "은급" -> "intermediate"
            else   -> "beginner"
        }
        skillDamage = intent.getIntExtra(EXTRA_SKILL_DAMAGE, 50)
        val effectTypeName = intent.getStringExtra(EXTRA_EFFECT_TYPE) ?: "ATTACK"

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

        val effect = runCatching { SkillEffectType.valueOf(effectTypeName) }
            .getOrDefault(SkillEffectType.ATTACK)

        tvCardSkillName.text = skillName
        tvCardGrade.text = skillGrade
        tvCardGrade.backgroundTintList =
            ColorStateList.valueOf(gradeColor(skillGrade))
        tvCardEffect.text = "${effect.icon} ${effect.displayName}"
        tvCardDamage.text = "데미지: $skillDamage"

        btnSubmit.setOnClickListener {
            val answer = etAnswer.text.toString().trim()
            if (answer.isEmpty()) {
                Toast.makeText(this, "답을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (viewModel.uiState.value is PveQuestionUiState.LoadingQuestion) {
                Toast.makeText(this, "문제를 먼저 로딩해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.submitAnswer(answer)
        }
        btnSkip.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // UI 상태 관찰
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> renderState(state) }
            }
        }

        viewModel.loadQuestion()
    }

    private fun renderState(state: PveQuestionUiState) {
        when (state) {
            is PveQuestionUiState.Idle -> Unit

            is PveQuestionUiState.LoadingQuestion -> {
                tvKoreanSentence.text = "문제 로딩 중..."
                tvWordHint.text = ""
                btnSubmit.isEnabled = false
                tvFeedback.visibility = View.GONE
            }

            is PveQuestionUiState.QuestionReady -> {
                tvKoreanSentence.text = state.koreanSentence
                tvWordHint.text = state.hint
                btnSubmit.isEnabled = true
                tvFeedback.visibility = View.GONE
            }

            is PveQuestionUiState.Evaluating -> {
                btnSubmit.isEnabled = false
                tvFeedback.visibility = View.GONE
            }

            is PveQuestionUiState.Correct -> {
                // 상태를 먼저 소비해 앱 복귀 시 다이얼로그 중복 표시 방지
                viewModel.onCorrectConsumed()
                showSkillActivationDialog(imageUrl = state.imageUrl, imageBase64 = state.imageBase64)
            }

            is PveQuestionUiState.Wrong -> {
                val sb = StringBuilder("❌ 오답! (점수: ${state.score})\n")
                state.feedback?.let { sb.appendLine("\n피드백: $it") }
                state.correction?.let { sb.appendLine("수정: $it") }
                tvFeedback.text = sb.toString().trim()
                tvFeedback.setBackgroundColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_bg))
                tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_text))
                tvFeedback.visibility = View.VISIBLE
                btnSubmit.isEnabled = true
                btnSubmit.text = "다시 도전!"
            }

            is PveQuestionUiState.Error -> {
                tvKoreanSentence.text = state.message
                btnSubmit.isEnabled = true
            }
        }
    }

    private fun showSkillActivationDialog(imageUrl: String?, imageBase64: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_skill_card, null)
        val ivImage  = dialogView.findViewById<ImageView>(R.id.iv_skill_image)
        val tvGrade  = dialogView.findViewById<TextView>(R.id.tv_skill_grade)
        val tvName   = dialogView.findViewById<TextView>(R.id.tv_skill_name)
        val tvDamage = dialogView.findViewById<TextView>(R.id.tv_skill_damage)
        val tvDesc   = dialogView.findViewById<TextView>(R.id.tv_skill_desc)
        val tvTotal  = dialogView.findViewById<TextView>(R.id.tv_total_damage)
        val tvBadge  = dialogView.findViewById<TextView>(R.id.tv_collected_badge)

        when {
            !imageUrl.isNullOrBlank() -> ivImage.load(imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
            imageBase64 != null -> {
                runCatching {
                    val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    ivImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                }.onFailure {
                    ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
            else -> ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        tvGrade.text = skillGrade
        tvGrade.backgroundTintList =
            ColorStateList.valueOf(gradeColor(skillGrade))
        tvName.text = skillName
        tvDamage.text = "+$skillDamage 데미지!"
        tvDesc.text = "스킬 발동!"
        tvTotal?.visibility = View.GONE
        tvBadge?.text = "✅ 스킬 발동!"

        dialogView.scaleX = 0.6f; dialogView.scaleY = 0.6f; dialogView.alpha = 0f
        dialogView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(250).start()

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("⚔ 발동!") { _, _ ->
                val data = android.content.Intent().putExtra(RESULT_WORD_ID, viewModel.wordId)
                setResult(RESULT_OK, data)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun gradeColor(grade: String): Int = when (grade) {
        "금급" -> ContextCompat.getColor(this, R.color.color_grade_gold)
        "은급" -> ContextCompat.getColor(this, R.color.color_grade_silver)
        else   -> ContextCompat.getColor(this, R.color.color_grade_bronze)
    }
}
