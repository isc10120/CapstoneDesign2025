package com.example.zamgavocafront.pvp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData
import com.example.zamgavocafront.viewmodel.PvpViewModel
import kotlinx.coroutines.launch

class PvpActivity : AppCompatActivity() {

    private val viewModel: PvpViewModel by viewModels()

    private lateinit var rvWords: RecyclerView
    private lateinit var tvTotalDamage: TextView
    private lateinit var tvAttacksLeft: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PvpWordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pvp)

        rvWords = findViewById(R.id.rv_pvp_words)
        tvTotalDamage = findViewById(R.id.tv_total_damage)
        tvAttacksLeft = findViewById(R.id.tv_attacks_left)
        tvEmpty = findViewById(R.id.tv_empty)

        adapter = PvpWordAdapter(emptyList()) { word ->
            val attacksLeft = PvpWordManager.getAttacksLeft(this)
            if (attacksLeft <= 0) {
                android.widget.Toast.makeText(this, "오늘 공격 횟수를 모두 사용했어요! (10/10)", android.widget.Toast.LENGTH_SHORT).show()
                return@PvpWordAdapter
            }
            startActivity(Intent(this, PvpQuestionActivity::class.java).apply {
                putExtra(PvpQuestionActivity.EXTRA_WORD_ID, word.id)
                putExtra(PvpQuestionActivity.EXTRA_WORD_TEXT, word.word)
                putExtra(PvpQuestionActivity.EXTRA_WORD_MEANING, word.meaning)
                putExtra(PvpQuestionActivity.EXTRA_DIFFICULTY, word.difficulty.name)
                putExtra(PvpQuestionActivity.EXTRA_SKILL_ID, word.skillId ?: -1L)
            })
        }

        rvWords.layoutManager = LinearLayoutManager(this)
        rvWords.adapter = adapter

        findViewById<android.widget.Button>(R.id.btn_collected_cards).setOnClickListener {
            startActivity(Intent(this, CollectedCardsActivity::class.java))
        }

        // ViewModel의 단어 목록 관찰
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pvpWords.collect { words -> updateUi(words) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
        viewModel.loadPvpWords(filterUsed = false)
    }

    private fun refreshStats() {
        tvTotalDamage.text = PvpWordManager.getTotalDamage(this).toString()
        tvAttacksLeft.text = "${PvpWordManager.getAttacksLeft(this)} / 10"
    }

    private fun updateUi(words: List<WordData>) {
        adapter.updateWords(words)
        rvWords.visibility = if (words.isEmpty()) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
    }
}
