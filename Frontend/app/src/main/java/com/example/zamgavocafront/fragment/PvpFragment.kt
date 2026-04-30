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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
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
        tvTimer = view.findViewById(R.id.tv_pvp_timer)
        tvPlayer1Name = view.findViewById(R.id.tv_player1_name)
        tvPlayer1Damage = view.findViewById(R.id.tv_player1_damage)
        tvPlayer2Name = view.findViewById(R.id.tv_player2_name)
        tvPlayer2Damage = view.findViewById(R.id.tv_player2_damage)
        tvAttacksLeft = view.findViewById(R.id.tv_attacks_left)
        rvWords = view.findViewById(R.id.rv_pvp_words)
        tvEmpty = view.findViewById(R.id.tv_empty)

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

        // ViewModel의 단어 목록 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pvpWords.collect { words -> updatePvpUi(words) }
            }
        }

        timerHandler.post(timerRunnable)

        // SharedPreferences에서 닉네임 읽어 표시
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        tvPlayer1Name.text = prefs.getString("nickName", "나") ?: "나"
        tvPlayer2Name.text = "상대방"
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPvpWords(filterUsed = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updatePvpUi(words: List<WordData>) {
        val ctx = requireContext()
        tvPlayer1Damage.text = PvpWordManager.getTotalDamage(ctx).toString()
        tvPlayer2Damage.text = "0"
        tvAttacksLeft.text = "오늘 잔여 공격 횟수 ${PvpWordManager.getAttacksLeft(ctx)}"
        adapter.updateWords(words)
        rvWords.visibility = if (words.isEmpty()) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateTimer() {
        val now = System.currentTimeMillis()
        val nextMonday = getNextMondayMillis()
        val diff = nextMonday - now
        if (diff <= 0) {
            tvTimer.text = "초기화 중..."
            return
        }
        val days = diff / (24L * 60 * 60 * 1000)
        val hours = (diff % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)
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
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
