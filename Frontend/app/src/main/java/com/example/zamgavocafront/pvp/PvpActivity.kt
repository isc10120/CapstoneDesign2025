package com.example.zamgavocafront.pvp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.model.WordData
import kotlinx.coroutines.launch

class PvpActivity : AppCompatActivity() {

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
            Intent(this, PvpQuestionActivity::class.java).also { intent ->
                intent.putExtra(PvpQuestionActivity.EXTRA_WORD_ID, word.id)
                intent.putExtra(PvpQuestionActivity.EXTRA_WORD_TEXT, word.word)
                intent.putExtra(PvpQuestionActivity.EXTRA_WORD_MEANING, word.meaning)
                intent.putExtra(PvpQuestionActivity.EXTRA_DIFFICULTY, word.difficulty.name)
                startActivity(intent)
            }
        }

        rvWords.layoutManager = LinearLayoutManager(this)
        rvWords.adapter = adapter

        findViewById<android.widget.Button>(R.id.btn_collected_cards).setOnClickListener {
            startActivity(Intent(this, CollectedCardsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        tvTotalDamage.text = PvpWordManager.getTotalDamage(this).toString()
        tvAttacksLeft.text = "${PvpWordManager.getAttacksLeft(this)} / 10"

        lifecycleScope.launch {
            val words = loadPvpWords()
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

    /** 이번 주 수집 단어를 서버에서 가져온다. 실패 시 로컬 데이터로 fallback. */
    private suspend fun loadPvpWords(): List<WordData> {
        return try {
            val resp = ApiClient.api.getWeekCollectedList()
            if (resp.success && resp.data != null && resp.data.isNotEmpty()) {
                resp.data.map { dto ->
                    // 난이도는 서버가 제공하지 않으므로 로컬 WordRepository에서 보완
                    val local = WordRepository.allWords.find { it.id.toLong() == dto.id }
                    WordData(
                        id = dto.id.toInt(),
                        word = dto.word,
                        meaning = dto.definition,
                        exampleEn = dto.example,
                        exampleKr = dto.exampleKor,
                        difficulty = local?.difficulty ?: Difficulty.MEDIUM
                    )
                }
            } else {
                localFallback()
            }
        } catch (_: Exception) {
            localFallback()
        }
    }

    private fun localFallback(): List<WordData> {
        val availableIds = PvpWordManager.getPvpAvailableWordIds(this)
        return WordRepository.allWords.filter { it.id in availableIds }
    }
}
