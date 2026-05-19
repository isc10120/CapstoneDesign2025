package com.example.zamgavocafront.pvp

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
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.viewmodel.PvpQuestionUiState
import com.example.zamgavocafront.viewmodel.PvpQuestionViewModel
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class PvpQuestionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID      = "word_id"
        const val EXTRA_WORD_TEXT    = "word_text"
        const val EXTRA_WORD_MEANING = "word_meaning"
        const val EXTRA_DIFFICULTY   = "difficulty"
        const val EXTRA_SKILL_ID     = "skill_id"
    }

    private val viewModel: PvpQuestionViewModel by viewModels()

    private lateinit var tvWordHeader: TextView
    private lateinit var tvGradeBadge: TextView
    private lateinit var tvQuestionLabel: TextView
    private lateinit var tvKoreanSentence: TextView
    private lateinit var tvWordHint: TextView
    private lateinit var tvAttacksInfo: TextView
    private lateinit var tilAnswer: TextInputLayout
    private lateinit var etAnswer: EditText
    private lateinit var llOptions: LinearLayout
    private lateinit var btnSubmit: Button
    private lateinit var tvFeedback: TextView

    private val optionButtons: List<Button> by lazy {
        listOf(
            findViewById(R.id.btn_option_0),
            findViewById(R.id.btn_option_1),
            findViewById(R.id.btn_option_2),
            findViewById(R.id.btn_option_3)
        )
    }

    private var isMultipleChoice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pvp_question)

        viewModel.wordId     = intent.getIntExtra(EXTRA_WORD_ID, 0)
        viewModel.wordText   = intent.getStringExtra(EXTRA_WORD_TEXT) ?: ""
        viewModel.wordMeaning = intent.getStringExtra(EXTRA_WORD_MEANING) ?: ""
        viewModel.skillId    = intent.getLongExtra(EXTRA_SKILL_ID, -1L).takeIf { it >= 0 }
        viewModel.difficulty = intent.getStringExtra(EXTRA_DIFFICULTY)
            ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: Difficulty.MEDIUM

        tvWordHeader    = findViewById(R.id.tv_word_header)
        tvGradeBadge    = findViewById(R.id.tv_grade_badge)
        tvQuestionLabel = findViewById(R.id.tv_question_label)
        tvKoreanSentence = findViewById(R.id.tv_korean_sentence)
        tvWordHint      = findViewById(R.id.tv_word_hint)
        tvAttacksInfo   = findViewById(R.id.tv_attacks_info)
        tilAnswer       = findViewById(R.id.til_answer)
        etAnswer        = findViewById(R.id.et_answer)
        llOptions       = findViewById(R.id.ll_options)
        btnSubmit       = findViewById(R.id.btn_submit)
        tvFeedback      = findViewById(R.id.tv_feedback)

        tvWordHeader.visibility = View.GONE
        tvGradeBadge.text = viewModel.difficulty.grade()
        tvGradeBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, viewModel.difficulty.gradeColorRes()))

        btnSubmit.setOnClickListener {
            val answer = etAnswer.text.toString().trim()
            if (answer.isEmpty()) {
                Toast.makeText(this, "답을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.submitAnswer(answer)
        }
        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.attacksLeft.collect { left ->
                    tvAttacksInfo.text = "남은 공격: $left / 10"
                }
            }
        }

        viewModel.refreshAttacks()
        viewModel.loadQuestion()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.no_anim, R.anim.slide_down)
    }

    private fun renderState(state: PvpQuestionUiState) {
        when (state) {
            is PvpQuestionUiState.Idle -> Unit

            is PvpQuestionUiState.LoadingQuestion -> {
                tvQuestionLabel.text = "문제 생성 중..."
                tvKoreanSentence.text = "잠시만 기다려 주세요."
                tvWordHint.visibility = View.GONE
                btnSubmit.isEnabled = false
                llOptions.visibility = View.GONE
                tilAnswer.visibility = View.VISIBLE
                tvFeedback.visibility = View.GONE
            }

            is PvpQuestionUiState.QuestionReady -> {
                isMultipleChoice = state.questionType.uppercase() in
                        setOf("WORD_DEFINITION", "SYNONYM")

                tvQuestionLabel.text = questionTypeLabel(state.questionType)
                tvKoreanSentence.text = state.question
                tvWordHint.text = state.hint ?: ""
                tvWordHint.visibility = if (state.hint != null) View.VISIBLE else View.GONE

                tilAnswer.visibility = if (isMultipleChoice) View.GONE else View.VISIBLE
                btnSubmit.visibility = if (isMultipleChoice) View.GONE else View.VISIBLE
                llOptions.visibility = if (isMultipleChoice) View.VISIBLE else View.GONE

                if (isMultipleChoice && !state.options.isNullOrEmpty()) {
                    setupOptions(state.options, enabled = viewModel.attacksLeft.value > 0)
                }

                btnSubmit.isEnabled = viewModel.attacksLeft.value > 0
                btnSubmit.text = if (viewModel.attacksLeft.value > 0) "제출 (공격!)" else "공격 횟수 소진"
                tvFeedback.visibility = View.GONE
            }

            is PvpQuestionUiState.Evaluating -> {
                btnSubmit.isEnabled = false
                optionButtons.forEach { it.isEnabled = false }
                tvFeedback.visibility = View.GONE
            }

            is PvpQuestionUiState.Correct -> {
                viewModel.onCorrectConsumed()
                if (state.skill != null) showSkillCardDialog(state)
                else showCorrectFallback(state.score)
            }

            is PvpQuestionUiState.Wrong -> {
                val sb = StringBuilder("❌ 오답! (점수: ${state.score} / 100)\n")
                state.feedback?.let { sb.append("\n피드백: $it") }
                state.correction?.let { sb.append("\n정답: $it") }
                tvFeedback.text = sb.toString()
                tvFeedback.setBackgroundColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_bg))
                tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_text))
                tvFeedback.visibility = View.VISIBLE

                val left = viewModel.attacksLeft.value
                if (left > 0) {
                    if (isMultipleChoice) {
                        optionButtons.forEach { it.isEnabled = true }
                    } else {
                        btnSubmit.isEnabled = true
                        btnSubmit.text = "다시 도전! (${left}회 남음)"
                    }
                }
            }

            is PvpQuestionUiState.Error -> {
                if (state.isEvalError) {
                    tvFeedback.text = state.message
                    tvFeedback.setBackgroundColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_bg))
                    tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_text))
                    tvFeedback.visibility = View.VISIBLE
                } else {
                    tvKoreanSentence.text = state.message
                }
                btnSubmit.isEnabled = viewModel.attacksLeft.value > 0
            }
        }
    }

    private fun setupOptions(options: List<String>, enabled: Boolean) {
        optionButtons.forEachIndexed { index, btn ->
            if (index < options.size) {
                btn.text = options[index]
                btn.isEnabled = enabled
                btn.visibility = View.VISIBLE
                btn.setOnClickListener { viewModel.submitAnswer(options[index]) }
            } else {
                btn.visibility = View.GONE
            }
        }
    }

    private fun questionTypeLabel(questionType: String): String = when (questionType.uppercase()) {
        "SPELLING"         -> "빈칸에 알맞은 글자를 채워 단어를 완성하세요:"
        "ANAGRAM"          -> "섞인 글자를 올바른 순서로 배열하세요:"
        "WORD_DEFINITION"  -> "올바른 뜻을 고르세요:"
        "SYNONYM"          -> "올바른 유의어를 고르세요:"
        "SENTENCE_WRITING" -> "다음 상황에 맞게 영어 문장을 작성하세요:"
        "TRANSLATION"      -> "다음 한국어 문장을 영어로 번역하세요:"
        else               -> "답을 입력하세요:"
    }

    private fun showSkillCardDialog(state: PvpQuestionUiState.Correct) {
        val skill = state.skill ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_skill_card, null)

        val ivImage     = dialogView.findViewById<ImageView>(R.id.iv_skill_image)
        val ivFrame     = dialogView.findViewById<ImageView>(R.id.iv_card_frame)
        val tvName      = dialogView.findViewById<TextView>(R.id.tv_skill_name)
        val tvDamage    = dialogView.findViewById<TextView>(R.id.tv_skill_damage)
        val tvEffect    = dialogView.findViewById<TextView>(R.id.tv_skill_effect)
        val tvDesc      = dialogView.findViewById<TextView>(R.id.tv_skill_desc)
        val tvTotal     = dialogView.findViewById<TextView>(R.id.tv_total_damage)
        val tvCollected = dialogView.findViewById<TextView>(R.id.tv_collected_badge)

        ivFrame.setImageResource(viewModel.difficulty.frameDrawableRes())

        when {
            skill.imageURL.isNotBlank() -> ivImage.load(skill.imageURL) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
            skill.imageBase64 != null -> runCatching {
                val bytes = Base64.decode(skill.imageBase64, Base64.DEFAULT)
                ivImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }.onFailure { ivImage.setImageResource(android.R.drawable.ic_menu_gallery) }
            else -> ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        tvName.text = skill.name
        tvDesc.text = skill.explain
        val displayDamage = state.pvpDamage ?: skill.damage
        tvDamage.text = "+$displayDamage 데미지!"
        tvEffect.text = if (state.effectType != null && state.effectTurns != null)
            "부가효과: ${state.effectType} ${state.effectTurns}턴"
        else "부가효과: X"
        tvTotal.text = "누적 데미지: ${PvpWordManager.getTotalDamage(this)}"
        tvCollected.text = "📦 '${skill.name}' 카드 수집 완료"

        dialogView.scaleX = 0.6f; dialogView.scaleY = 0.6f; dialogView.alpha = 0f
        dialogView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(250).start()

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("⚔ 공격 완료!") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showCorrectFallback(score: Int) {
        tvFeedback.text = "✅ 정답! (점수: $score / 100)\n스킬 생성 중 오류가 발생했지만 데미지는 적용되었어요."
        tvFeedback.setBackgroundColor(ContextCompat.getColor(this, R.color.color_feedback_correct_bg))
        tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.color_feedback_correct_text))
        tvFeedback.visibility = View.VISIBLE
        btnSubmit.isEnabled = true
        btnSubmit.visibility = View.VISIBLE
        btnSubmit.text = "닫기"
        btnSubmit.setOnClickListener { finish() }
    }
}
