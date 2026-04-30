package com.example.zamgavocafront.fragment

import android.os.Bundle
import android.text.SpannableString
import android.widget.Toast
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordProgressManager
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.model.WordData
import com.example.zamgavocafront.viewmodel.TodayWordViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

class TodayWordFragment : Fragment() {

    private val viewModel: TodayWordViewModel by viewModels()

    private lateinit var adapter: WordAdapter
    private lateinit var tvProgress: TextView
    private lateinit var tvTooltip: TextView

    private val tips = listOf(
        "용사님, 오늘 학습할 단어들입니다. \n'랜덤 퀘스트'를 클리어해 학습하세요."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_today_word, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvProgress = view.findViewById(R.id.tv_progress_summary)
        tvTooltip = view.findViewById(R.id.tv_today_tooltip)
        tvTooltip.text = tips[Random.nextInt(tips.size)]

        adapter = WordAdapter(emptyList()) { word -> showWordDetailDialog(word) }

        view.findViewById<RecyclerView>(R.id.rv_words).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@TodayWordFragment.adapter
        }

        // ViewModel의 단어 목록을 관찰해 UI 업데이트
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.words.collect { words ->
                    adapter.updateWords(words)
                    refreshProgress(words)
                }
            }
        }

        // API 실패 시 Toast로 에러 표시
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { message ->
                    if (message != null) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }

        viewModel.loadDailyWords()
    }

    override fun onResume() {
        super.onResume()
        // 넛지 완료 등으로 진행도가 바뀔 수 있어 onResume마다 갱신
        refreshProgress(viewModel.words.value)
        adapter.notifyDataSetChanged()
    }

    private fun refreshProgress(words: List<WordData>) {
        val completed = words.count { WordProgressManager.isCompleted(requireContext(), it.id) }
        tvProgress.text = "완료 $completed / ${words.size}"
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

        val nudgeCount = WordProgressManager.getCount(requireContext(), word.id)
        dialogView.findViewById<TextView>(R.id.tv_nudge_count).text =
            "퀘스트 진행도  $nudgeCount / ${WordProgressManager.MAX_COUNT}"

        dialogView.findViewById<TextView>(R.id.tv_detail_word).text = word.word
        dialogView.findViewById<TextView>(R.id.tv_detail_meaning).text =
            word.meaning.replace(";", "\n").replace(",", "\n")

        // 영어 예문에서 타겟 단어 강조
        val exampleText = word.exampleEn
        val spannable = SpannableString(exampleText)
        val startIdx = exampleText.lowercase().indexOf(word.word.lowercase())
        if (startIdx >= 0) {
            val endIdx = startIdx + word.word.length
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.main_navy)), startIdx, endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), startIdx, endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        dialogView.findViewById<TextView>(R.id.tv_detail_example_en).text = spannable
        dialogView.findViewById<TextView>(R.id.tv_detail_example_kr).text = word.exampleKr

        dialogView.findViewById<TextView>(R.id.btn_close).setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    inner class WordAdapter(
        private var words: List<WordData>,
        private val onClick: (WordData) -> Unit
    ) : RecyclerView.Adapter<WordAdapter.VH>() {

        fun updateWords(newWords: List<WordData>) {
            words = newWords
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val card: CardView = v.findViewById(R.id.card_word)
            val tvWordEn: TextView = v.findViewById(R.id.tv_word_en)
            val tvWordKr: TextView = v.findViewById(R.id.tv_word_kr)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_word_grid, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val word = words[position]
            val done = WordProgressManager.getCount(requireContext(), word.id) >= WordProgressManager.MAX_COUNT

            holder.tvWordEn.text = word.word
            holder.tvWordKr.text = word.meaning
            holder.card.setCardBackgroundColor(
                if (done) ContextCompat.getColor(requireContext(), R.color.color_word_completed)
                else ContextCompat.getColor(requireContext(), R.color.main_yellow)
            )
            holder.itemView.setOnClickListener { onClick(word) }
        }

        override fun getItemCount() = words.size
    }
}
