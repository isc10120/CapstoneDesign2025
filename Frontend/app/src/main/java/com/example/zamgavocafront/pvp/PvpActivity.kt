package com.example.zamgavocafront.pvp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.model.WordData

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
        val availableIds = PvpWordManager.getPvpAvailableWordIds(this)
        val words = WordRepository.allWords.filter { it.id in availableIds }

        tvTotalDamage.text = PvpWordManager.getTotalDamage(this).toString()
        val attacksLeft = PvpWordManager.getAttacksLeft(this)
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
