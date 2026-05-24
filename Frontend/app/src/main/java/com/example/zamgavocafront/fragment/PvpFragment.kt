package com.example.zamgavocafront.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.api.dto.BattleResultResponse
import com.example.zamgavocafront.api.dto.SideStatus
import com.example.zamgavocafront.model.WordData
import com.example.zamgavocafront.pvp.PvpQuestionActivity
import com.example.zamgavocafront.pvp.PvpWordAdapter
import com.example.zamgavocafront.pvp.PvpWordManager
import com.example.zamgavocafront.viewmodel.PvpViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

class PvpFragment : Fragment() {

    private val viewModel: PvpViewModel by viewModels()

    private lateinit var tvTimer: TextView
    private lateinit var tvPlayer1Name: TextView
    private lateinit var tvPlayer1Damage: TextView
    private lateinit var tvPlayer2Name: TextView
    private lateinit var tvPlayer2Damage: TextView
    private lateinit var tvAttacksLeft: TextView
    private lateinit var rvWords: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PvpWordAdapter

    // 버프/상태이상 텍스트뷰 — 플레이어1(나)
    private lateinit var tvP1Attack: TextView
    private lateinit var tvP1Shield: TextView
    private lateinit var tvP1Paralysis: TextView
    private lateinit var tvP1Poison: TextView

    // 버프/상태이상 텍스트뷰 — 플레이어2(상대)
    private lateinit var tvP2Attack: TextView
    private lateinit var tvP2Shield: TextView
    private lateinit var tvP2Paralysis: TextView
    private lateinit var tvP2Poison: TextView

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimer()
            timerHandler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pvp, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvTimer        = view.findViewById(R.id.tv_pvp_timer)
        tvPlayer1Name  = view.findViewById(R.id.tv_player1_name)
        tvPlayer1Damage = view.findViewById(R.id.tv_player1_damage)
        tvPlayer2Name  = view.findViewById(R.id.tv_player2_name)
        tvPlayer2Damage = view.findViewById(R.id.tv_player2_damage)
        tvAttacksLeft  = view.findViewById(R.id.tv_attacks_left)
        rvWords        = view.findViewById(R.id.rv_pvp_words)
        tvEmpty        = view.findViewById(R.id.tv_empty)

        tvP1Attack   = view.findViewById(R.id.tv_p1_buff_attack)
        tvP1Shield   = view.findViewById(R.id.tv_p1_buff_shield)
        tvP1Paralysis = view.findViewById(R.id.tv_p1_buff_paralysis)
        tvP1Poison   = view.findViewById(R.id.tv_p1_buff_poison)
        tvP2Attack   = view.findViewById(R.id.tv_p2_buff_attack)
        tvP2Shield   = view.findViewById(R.id.tv_p2_buff_shield)
        tvP2Paralysis = view.findViewById(R.id.tv_p2_buff_paralysis)
        tvP2Poison   = view.findViewById(R.id.tv_p2_buff_poison)

        adapter = PvpWordAdapter(emptyList()) { word ->
            val attacksLeft = PvpWordManager.getAttacksLeft(requireContext())
            if (attacksLeft <= 0) {
                Toast.makeText(requireContext(), "오늘 공격 횟수를 모두 사용했어요! (10/10)", Toast.LENGTH_SHORT).show()
                return@PvpWordAdapter
            }
            startActivity(Intent(requireContext(), PvpQuestionActivity::class.java).apply {
                putExtra(PvpQuestionActivity.EXTRA_WORD_ID, word.id)
                putExtra(PvpQuestionActivity.EXTRA_WORD_TEXT, word.word)
                putExtra(PvpQuestionActivity.EXTRA_WORD_MEANING, word.meaning)
                putExtra(PvpQuestionActivity.EXTRA_DIFFICULTY, word.difficulty.name)
                putExtra(PvpQuestionActivity.EXTRA_SKILL_ID, word.skillId ?: -1L)
            })
            requireActivity().overridePendingTransition(R.anim.slide_up, R.anim.no_anim)
        }

        rvWords.layoutManager = GridLayoutManager(requireContext(), 2)
        rvWords.adapter = adapter

        // 상대 닉네임 롱클릭 → 테스트 매칭 (개발용)
        tvPlayer2Name.setOnLongClickListener {
            viewModel.testMatch()
            Toast.makeText(requireContext(), "테스트 매칭 요청 중...", Toast.LENGTH_SHORT).show()
            true
        }

        // 내 데미지 롱클릭 → 공격 횟수 리셋 (개발용)
        tvPlayer1Damage.setOnLongClickListener {
            PvpWordManager.resetAttacks(requireContext())
            updatePvpUi(viewModel.pvpWords.value)
            Toast.makeText(requireContext(), "공격 횟수 리셋! (${PvpWordManager.getAttacksLeft(requireContext())}회)", Toast.LENGTH_SHORT).show()
            true
        }

        // 내 닉네임은 로컬 prefs에서
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        tvPlayer1Name.text = prefs.getString("nickName", "나") ?: "나"

        // battleStatus 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.battleStatus.collect { status ->
                    if (status != null) {
                        tvPlayer2Name.text = status.opponent.nickname
                        tvPlayer1Damage.text = status.my.totalDamage.toString()
                        updateBuffDisplay(status.my, status.enemy)
                    }
                }
            }
        }

        // 실시간 상대 데미지 관찰 (STOMP)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.enemyDamage.collect { damage ->
                    tvPlayer2Damage.text = damage.toString()
                }
            }
        }

        // 단어 목록 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pvpWords.collect { words -> updatePvpUi(words) }
            }
        }

        // 지난 주차 결과 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.latestResult.collect { result ->
                    if (result != null) showResultDialog(result)
                }
            }
        }

        timerHandler.post(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBattleStatus()
        viewModel.checkLatestResult()
        viewModel.loadPvpWords()
        tvAttacksLeft.text = "오늘 잔여 공격 횟수 ${PvpWordManager.getAttacksLeft(requireContext())}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updatePvpUi(words: List<WordData>) {
        val ctx = requireContext()
        tvAttacksLeft.text = "오늘 잔여 공격 횟수 ${PvpWordManager.getAttacksLeft(ctx)}"
        // 서버 배틀 데이터가 없으면 로컬 누적값으로 표시
        if (viewModel.battleStatus.value == null) {
            tvPlayer1Damage.text = PvpWordManager.getTotalDamage(ctx).toString()
        }
        adapter.updateWords(words)
        rvWords.visibility = if (words.isEmpty()) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateBuffDisplay(my: SideStatus, enemy: SideStatus) {
        fun findEffect(effects: List<com.example.zamgavocafront.api.dto.StatusEffect>, type: String) =
            effects.find { it.type == type }

        // 나
        tvP1Attack.text   = my.statusEffects.find { it.type == "DAMAGE_BUFF" }?.let { "${it.remainingTurns}턴" } ?: ""
        tvP1Shield.text   = if (my.shieldCount > 0) "×${my.shieldCount}" else ""
        tvP1Paralysis.text = my.statusEffects.find { it.type == "PARALYZE" }?.let { "${it.remainingTurns}턴" } ?: ""
        tvP1Poison.text   = my.statusEffects.find { it.type == "POISON" }?.let { "${it.remainingTurns}턴" } ?: ""

        // 상대
        tvP2Attack.text    = enemy.statusEffects.find { it.type == "DAMAGE_BUFF" }?.let { "${it.remainingTurns}턴" } ?: ""
        tvP2Shield.text    = if (enemy.shieldCount > 0) "×${enemy.shieldCount}" else ""
        tvP2Paralysis.text = enemy.statusEffects.find { it.type == "PARALYZE" }?.let { "${it.remainingTurns}턴" } ?: ""
        tvP2Poison.text    = enemy.statusEffects.find { it.type == "POISON" }?.let { "${it.remainingTurns}턴" } ?: ""
    }

    private fun showResultDialog(result: BattleResultResponse) {
        val resultText = when (result.result) {
            "WIN_A", "WIN_B" -> {
                val myDmg = result.myTotalDamage
                val opDmg = result.opponentTotalDamage
                if (myDmg >= opDmg) "🏆 승리!" else "💀 패배..."
            }
            "DRAW" -> "🤝 무승부"
            else   -> "결과 없음"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("지난 주차 결과 (${result.weekStart})")
            .setMessage(
                "$resultText\n\n" +
                "나: ${result.myTotalDamage} 데미지\n" +
                "상대 (${result.opponentNickName}): ${result.opponentTotalDamage} 데미지"
            )
            .setPositiveButton("확인") { _, _ -> viewModel.confirmResult() }
            .setCancelable(false)
            .show()
    }

    private fun updateTimer() {
        val now = System.currentTimeMillis()
        val nextMonday = getNextMondayMillis()
        val diff = nextMonday - now
        if (diff <= 0) { tvTimer.text = "초기화 중..."; return }
        val days    = diff / (24L * 60 * 60 * 1000)
        val hours   = (diff % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)
        val minutes = (diff % (60L * 60 * 1000)) / (60L * 1000)
        tvTimer.text = "${days}일 ${hours}시간 ${minutes}분"
    }

    private fun getNextMondayMillis(): Long {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToAdd = when (dayOfWeek) {
            Calendar.MONDAY -> 7
            Calendar.SUNDAY -> 1
            else -> (Calendar.MONDAY + 7 - dayOfWeek)
        }
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
