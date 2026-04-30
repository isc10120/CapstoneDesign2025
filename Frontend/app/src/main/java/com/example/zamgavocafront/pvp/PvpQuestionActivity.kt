package com.example.zamgavocafront.pvp

import android.content.res.ColorStateList
import android.os.Bundle
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
import com.example.zamgavocafront.R
import coil.load
import com.example.zamgavocafront.api.dto.SkillResponse
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.viewmodel.PvpQuestionUiState
import com.example.zamgavocafront.viewmodel.PvpQuestionViewModel
import kotlinx.coroutines.launch

class PvpQuestionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID = "word_id"
        const val EXTRA_WORD_TEXT = "word_text"
        const val EXTRA_WORD_MEANING = "word_meaning"
        const val EXTRA_DIFFICULTY = "difficulty"
        const val EXTRA_SKILL_ID = "skill_id"
    }

    private val viewModel: PvpQuestionViewModel by viewModels()

    private lateinit var tvWordHeader: TextView
    private lateinit var tvGradeBadge: TextView
    private lateinit var tvKoreanSentence: TextView
    private lateinit var tvWordHint: TextView
    private lateinit var tvAttacksInfo: TextView
    private lateinit var etAnswer: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvFeedback: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pvp_question)

        // ViewModel에 Intent 값 전달
        viewModel.wordId = intent.getIntExtra(EXTRA_WORD_ID, 0)
        viewModel.wordText = intent.getStringExtra(EXTRA_WORD_TEXT) ?: ""
        viewModel.wordMeaning = intent.getStringExtra(EXTRA_WORD_MEANING) ?: ""
        viewModel.skillId = intent.getLongExtra(EXTRA_SKILL_ID, -1L).takeIf { it >= 0 }
        viewModel.difficulty = intent.getStringExtra(EXTRA_DIFFICULTY)
            ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: Difficulty.MEDIUM
        viewModel.userLevel = when (viewModel.difficulty) {
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

        tvWordHeader.text = "단어: ${viewModel.wordText}"
        tvGradeBadge.text = viewModel.difficulty.grade()
        tvGradeBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, viewModel.difficulty.gradeColorRes()))

        btnSubmit.setOnClickListener {
            val answer = etAnswer.text.toString().trim()
            if (answer.isEmpty()) {
                Toast.makeText(this, "답을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (viewModel.uiState.value is PvpQuestionUiState.LoadingQuestion) {
                Toast.makeText(this, "문제를 먼저 로딩해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.submitAnswer(answer)
        }
        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }

        // UI 상태 관찰
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> renderState(state) }
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
                tvKoreanSentence.text = "문제 로딩 중..."
                tvWordHint.text = ""
                btnSubmit.isEnabled = false
                tvFeedback.visibility = View.GONE
            }

            is PvpQuestionUiState.QuestionReady -> {
                tvKoreanSentence.text = state.koreanSentence
                tvWordHint.text = state.hint
                btnSubmit.isEnabled = viewModel.attacksLeft.value > 0
                btnSubmit.text = if (viewModel.attacksLeft.value > 0) "제출" else "공격 횟수 소진"
                tvFeedback.visibility = View.GONE
            }

            is PvpQuestionUiState.Evaluating -> {
                btnSubmit.isEnabled = false
                tvFeedback.visibility = View.GONE
            }

            is PvpQuestionUiState.Correct -> {
                // 상태를 먼저 소비해 앱 복귀 시 다이얼로그 중복 표시 방지
                viewModel.onCorrectConsumed()
                if (state.skill != null) showSkillCardDialog(state.skill)
                else showCorrectFallback(state.score)
            }

            is PvpQuestionUiState.Wrong -> {
                val sb = StringBuilder("❌ 오답! (점수: ${state.score} / 100)\n")
                state.feedback?.let { sb.append("\n피드백: $it") }
                state.correction?.let { sb.append("\n수정: $it") }
                tvFeedback.text = sb.toString()
                tvFeedback.setBackgroundColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_bg))
                tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.color_feedback_wrong_text))
                tvFeedback.visibility = View.VISIBLE
                val left = viewModel.attacksLeft.value
                if (left > 0) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "다시 도전! (${left}회 남음)"
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

    private fun showSkillCardDialog(skill: SkillResponse) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_skill_card, null)

        val ivImage     = dialogView.findViewById<ImageView>(R.id.iv_skill_image)
        val tvGrade     = dialogView.findViewById<TextView>(R.id.tv_skill_grade)
        val tvName      = dialogView.findViewById<TextView>(R.id.tv_skill_name)
        val tvDamage    = dialogView.findViewById<TextView>(R.id.tv_skill_damage)
        val tvDesc      = dialogView.findViewById<TextView>(R.id.tv_skill_desc)
        val tvTotal     = dialogView.findViewById<TextView>(R.id.tv_total_damage)
        val tvCollected = dialogView.findViewById<TextView>(R.id.tv_collected_badge)

        if (skill.imageURL.isNotBlank()) {
            ivImage.load(skill.imageURL) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
        } else {
            ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        tvGrade.text = viewModel.difficulty.grade()
        tvGrade.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, viewModel.difficulty.gradeColorRes()))
        tvName.text = skill.name
        tvDesc.text = skill.explain
        tvDamage.text = "+${skill.damage} 데미지!"
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
        btnSubmit.text = "닫기"
        btnSubmit.setOnClickListener { finish() }
    }
}
