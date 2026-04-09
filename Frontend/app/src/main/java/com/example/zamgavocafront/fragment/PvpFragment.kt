package com.example.zamgavocafront.fragment

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.pvp.PvpQuestionActivity
import com.example.zamgavocafront.pvp.PvpWordAdapter
import com.example.zamgavocafront.pvp.PvpWordManager
import java.util.Calendar

class PvpFragment : Fragment() {

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
            Intent(requireContext(), PvpQuestionActivity::class.java).also { intent ->
                intent.putExtra(PvpQuestionActivity.EXTRA_WORD_ID, word.id)
                intent.putExtra(PvpQuestionActivity.EXTRA_WORD_TEXT, word.word)
                intent.putExtra(PvpQuestionActivity.EXTRA_WORD_MEANING, word.meaning)
                intent.putExtra(PvpQuestionActivity.EXTRA_DIFFICULTY, word.difficulty.name)
                startActivity(intent)
                // 드로어가 위로 올라가는 슬라이드업 애니메이션
                requireActivity().overridePendingTransition(R.anim.slide_up, R.anim.no_anim)
            }
        }

        rvWords.layoutManager = GridLayoutManager(requireContext(), 2)
        rvWords.adapter = adapter

        timerHandler.post(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
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

    private fun refreshUi() {
        val ctx = requireContext()
        val availableIds = PvpWordManager.getPvpAvailableWordIds(ctx)
        val words = WordRepository.allWords.filter { it.id in availableIds }

        // Player 1 (내 플레이어): 이번 주 누적 데미지
        val myDamage = PvpWordManager.getTotalDamage(ctx)
        tvPlayer1Damage.text = myDamage.toString()

        // Player 2 (상대방): 매칭 미구현으로 고정값
        tvPlayer2Damage.text = "0"

        // 잔여 공격 횟수
        val attacksLeft = PvpWordManager.getAttacksLeft(ctx)
        tvAttacksLeft.text = "오늘 잔여 공격 횟수 $attacksLeft"

        adapter.updateWords(words)

        if (words.isEmpty()) {
            rvWords.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvWords.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

}
