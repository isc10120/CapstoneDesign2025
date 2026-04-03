package com.example.zamgavocafront

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.model.WordData

class TodayWordActivity : AppCompatActivity() {

    private lateinit var adapter: WordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_words)

        adapter = WordAdapter(WordRepository.allWords) { word ->
            showWordDetailDialog(word)
        }

        findViewById<RecyclerView>(R.id.rv_words).apply {
            layoutManager = LinearLayoutManager(this@TodayWordActivity)
            this.adapter = this@TodayWordActivity.adapter
        }

        updateProgressSummary()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateProgressSummary()
    }

    private fun updateProgressSummary() {
        val completed = WordRepository.allWords.count {
            WordProgressManager.isCompleted(this, it.id)
        }
        findViewById<TextView>(R.id.tv_progress_summary).text =
            "완료 $completed / ${WordRepository.allWords.size}"
    }

    private fun showWordDetailDialog(word: WordData) {
        val count = WordProgressManager.getCount(this, word.id)
        val max = WordProgressManager.MAX_COUNT
        val isCompleted = count >= max

        val message = buildString {
            append("뜻: ${word.meaning}\n\n")
            append("[예문]\n")
            append("${word.exampleEn}\n")
            append(word.exampleKr)
        }

        val countLabel = if (isCompleted) "완료 ($max/$max)" else "$count / $max"
        val title = "${word.word}  [$countLabel]"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("닫기", null)
            .show()
    }

    // ─── RecyclerView Adapter ──────────────────────────────────────────────

    inner class WordAdapter(
        private val words: List<WordData>,
        private val onClick: (WordData) -> Unit
    ) : RecyclerView.Adapter<WordAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvWord: TextView = itemView.findViewById(R.id.tv_word)
            val tvMeaning: TextView = itemView.findViewById(R.id.tv_meaning)
            val tvCount: TextView = itemView.findViewById(R.id.tv_count_badge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_word, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val word = words[position]
            val count = WordProgressManager.getCount(this@TodayWordActivity, word.id)
            val max = WordProgressManager.MAX_COUNT
            val isCompleted = count >= max

            holder.tvWord.text = word.word
            holder.tvMeaning.text = word.meaning
            holder.tvCount.text = "$count/$max"

            // 완료된 단어는 배지 색상을 노란색으로 표시
            if (isCompleted) {
                holder.tvCount.setBackgroundResource(R.drawable.bg_count_badge_done)
                holder.tvCount.setTextColor(0xFF001740.toInt())
            } else {
                holder.tvCount.setBackgroundResource(R.drawable.bg_count_badge)
                holder.tvCount.setTextColor(0xFFFFFFFF.toInt())
            }

            holder.itemView.alpha = if (isCompleted) 0.55f else 1f
            holder.itemView.setOnClickListener { onClick(word) }
        }

        override fun getItemCount() = words.size
    }
}
