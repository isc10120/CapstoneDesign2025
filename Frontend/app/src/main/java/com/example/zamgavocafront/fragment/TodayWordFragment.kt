package com.example.zamgavocafront.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordProgressManager
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.model.WordData
import kotlinx.coroutines.launch
import kotlin.random.Random

class TodayWordFragment : Fragment() {

    private lateinit var adapter: WordAdapter
    private lateinit var tvProgress: TextView
    private lateinit var tvTooltip: TextView

    private val tips = listOf(
        "용사님, 오늘 학습할 단어들입니다. \n‘랜덤 퀘스트’를 클리어해 학습하세요."

    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_today_word, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvProgress = view.findViewById(R.id.tv_progress_summary)
        tvTooltip = view.findViewById(R.id.tv_today_tooltip)

        // 랜덤 툴팁 설정
        tvTooltip.text = tips[Random.nextInt(tips.size)]

        adapter = WordAdapter(WordRepository.allWords) { word ->
            showWordDetailDialog(word)
        }

        view.findViewById<RecyclerView>(R.id.rv_words).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@TodayWordFragment.adapter
        }

        loadDailyVocaList()
        refreshProgress()
    }

    private fun loadDailyVocaList() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getDailyWordList() }
                .onSuccess { response ->
                    response.data?.let { wordList ->
                        WordRepository.allWords.clear()
                        WordRepository.allWords.addAll(wordList.map { dto ->
                            WordData(
                                id = dto.id.toInt(),
                                word = dto.word,
                                meaning = dto.definition,
                                exampleEn = dto.example,
                                exampleKr = dto.exampleKor,
                                difficulty = Difficulty.MEDIUM
                            )
                        })
                        adapter.notifyDataSetChanged()
                        refreshProgress()
                    }
                }
        }
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_word_detail, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.7f)
        }

        // 넛지 카운트 (n/3)
        val nudgeCount = WordProgressManager.getCount(requireContext(), word.id)
        dialogView.findViewById<TextView>(R.id.tv_nudge_count).text =
            "퀘스트 진행도  $nudgeCount / ${WordProgressManager.MAX_COUNT}"

        dialogView.findViewById<TextView>(R.id.tv_detail_word).text = word.word
        dialogView.findViewById<TextView>(R.id.tv_detail_meaning).text =
            word.meaning.replace(";", "\n").replace(",", "\n")

        // 영어 예문에서 타겟 단어 네이비 강조
        val exampleText = word.exampleEn
        val spannable = SpannableString(exampleText)
        val wordLower = word.word.lowercase()
        val startIdx = exampleText.lowercase().indexOf(wordLower)
        if (startIdx >= 0) {
            val endIdx = startIdx + word.word.length
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#0F2A71")), startIdx, endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), startIdx, endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        dialogView.findViewById<TextView>(R.id.tv_detail_example_en).text = spannable
        dialogView.findViewById<TextView>(R.id.tv_detail_example_kr).text = word.exampleKr

        dialogView.findViewById<TextView>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        // 다이얼로그 window 너비를 화면 전체로 강제 설정 (기본값이 너무 좁음)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    inner class WordAdapter(
        private val words: List<WordData>,
        private val onClick: (WordData) -> Unit
    ) : RecyclerView.Adapter<WordAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val card: CardView = v.findViewById(R.id.card_word)
            val tvWordEn: TextView = v.findViewById(R.id.tv_word_en)
            val tvWordKr: TextView = v.findViewById(R.id.tv_word_kr)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_word_grid, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val word = words[position]
            val count = WordProgressManager.getCount(requireContext(), word.id)
            val max = WordProgressManager.MAX_COUNT
            val done = count >= max

            holder.tvWordEn.text = word.word
            holder.tvWordKr.text = word.meaning
            // 완료 시: 회색 카드 배경 / 미완료 시: 노란색 카드 배경
            holder.card.setCardBackgroundColor(
                if (done) Color.parseColor("#CCCCCC")
                else requireContext().getColor(R.color.main_yellow)
            )
            holder.itemView.alpha = 1f
            holder.itemView.setOnClickListener { onClick(word) }
        }

        override fun getItemCount() = words.size
    }
}
