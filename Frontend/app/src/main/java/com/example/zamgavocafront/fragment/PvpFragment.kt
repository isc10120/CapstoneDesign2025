package com.example.zamgavocafront.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.pvp.PvpQuestionActivity
import com.example.zamgavocafront.pvp.PvpWordAdapter
import com.example.zamgavocafront.pvp.PvpWordManager

class PvpFragment : Fragment() {

    private lateinit var tvTotalDamage: TextView
    private lateinit var tvAttacksLeft: TextView
    private lateinit var rvWords: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PvpWordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pvp, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvTotalDamage = view.findViewById(R.id.tv_total_damage)
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
            }
        }

        rvWords.layoutManager = LinearLayoutManager(requireContext())
        rvWords.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val availableIds = PvpWordManager.getPvpAvailableWordIds(requireContext())
        val words = WordRepository.allWords.filter { it.id in availableIds }

        tvTotalDamage.text = PvpWordManager.getTotalDamage(requireContext()).toString()
        val attacksLeft = PvpWordManager.getAttacksLeft(requireContext())
        tvAttacksLeft.text = "$attacksLeft / 10"
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
