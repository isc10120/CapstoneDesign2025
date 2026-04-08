package com.example.zamgavocafront

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
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
            // 2열 그리드 레이아웃 적용
            layoutManager = GridLayoutManager(this@TodayWordActivity, 2)
            this.adapter = this@TodayWordActivity.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun showWordDetailDialog(word: WordData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_word_detail, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme) // 테마 적용 권장
            .setView(dialogView)
            .create()

        // 다이얼로그 배경을 투명하게 해서 둥근 모서리가 보이게 함
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_detail_word).text = word.word
        dialogView.findViewById<TextView>(R.id.tv_detail_meaning).text = word.meaning
        dialogView.findViewById<TextView>(R.id.tv_detail_example_en).text = word.exampleEn
        dialogView.findViewById<TextView>(R.id.tv_detail_example_kr).text = word.exampleKr

        dialogView.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // ─── RecyclerView Adapter ──────────────────────────────────────────────

    inner class WordAdapter(
        private val words: List<WordData>,
        private val onClick: (WordData) -> Unit
    ) : RecyclerView.Adapter<WordAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvWordEn: TextView = itemView.findViewById(R.id.tv_word_en)
            val tvWordKr: TextView = itemView.findViewById(R.id.tv_word_kr)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_word_grid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val word = words[position]
            val count = WordProgressManager.getCount(this@TodayWordActivity, word.id)
            val max = WordProgressManager.MAX_COUNT
            val isCompleted = count >= max

            holder.tvWordEn.text = word.word
            holder.tvWordKr.text = word.meaning

            // 완료된 단어는 반투명하게 표시하거나 디자인에 따라 변경
            holder.itemView.alpha = if (isCompleted) 0.5f else 1f
            holder.itemView.setOnClickListener { onClick(word) }
        }

        override fun getItemCount() = words.size
    }
}
