package com.example.zamgavocafront.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordProgressManager
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.model.WordData

class TodayWordFragment : Fragment() {

    private lateinit var adapter: WordAdapter
    private lateinit var tvProgress: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_today_word, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvProgress = view.findViewById(R.id.tv_progress_summary)

        adapter = WordAdapter(WordRepository.allWords) { word ->
            showWordDetailDialog(word)
        }

        view.findViewById<RecyclerView>(R.id.rv_words).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TodayWordFragment.adapter
        }

        refreshProgress()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        refreshProgress()
    }

    private fun refreshProgress() {
        val completed = WordRepository.allWords.count {
            WordProgressManager.isCompleted(requireContext(), it.id)
        }
        tvProgress.text = "완료 $completed / ${WordRepository.allWords.size}"
    }

    private fun showWordDetailDialog(word: WordData) {
        val count = WordProgressManager.getCount(requireContext(), word.id)
        val max = WordProgressManager.MAX_COUNT
        val countLabel = if (count >= max) "완료 ($max/$max)" else "$count / $max"

        AlertDialog.Builder(requireContext())
            .setTitle("${word.word}  [$countLabel]")
            .setMessage("뜻: ${word.meaning}\n\n[예문]\n${word.exampleEn}\n${word.exampleKr}")
            .setPositiveButton("닫기", null)
            .show()
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    inner class WordAdapter(
        private val words: List<WordData>,
        private val onClick: (WordData) -> Unit
    ) : RecyclerView.Adapter<WordAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvWord: TextView = v.findViewById(R.id.tv_word)
            val tvMeaning: TextView = v.findViewById(R.id.tv_meaning)
            val tvCount: TextView = v.findViewById(R.id.tv_count_badge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val word = words[position]
            val count = WordProgressManager.getCount(requireContext(), word.id)
            val max = WordProgressManager.MAX_COUNT
            val done = count >= max

            holder.tvWord.text = word.word
            holder.tvMeaning.text = word.meaning
            holder.tvCount.text = "$count/$max"

            if (done) {
                holder.tvCount.setBackgroundResource(R.drawable.bg_count_badge_done)
                holder.tvCount.setTextColor(0xFF001740.toInt())
            } else {
                holder.tvCount.setBackgroundResource(R.drawable.bg_count_badge)
                holder.tvCount.setTextColor(0xFFFFFFFF.toInt())
            }

            holder.itemView.alpha = if (done) 0.55f else 1f
            holder.itemView.setOnClickListener { onClick(word) }
        }

        override fun getItemCount() = words.size
    }
}
