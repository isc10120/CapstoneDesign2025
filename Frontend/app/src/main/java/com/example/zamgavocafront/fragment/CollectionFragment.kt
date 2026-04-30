package com.example.zamgavocafront.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.zamgavocafront.R
import com.example.zamgavocafront.pve.CardDetailActivity
import com.example.zamgavocafront.pvp.CollectedCardManager
import com.example.zamgavocafront.viewmodel.CollectionUiState
import com.example.zamgavocafront.viewmodel.CollectionViewModel
import kotlinx.coroutines.launch

class CollectionFragment : Fragment() {

    private val viewModel: CollectionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_collection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_collected_cards)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CollectionUiState.Loading -> {
                            rv.visibility = View.GONE
                            tvEmpty.visibility = View.GONE
                        }
                        is CollectionUiState.Empty -> {
                            rv.visibility = View.GONE
                            tvEmpty.visibility = View.VISIBLE
                        }
                        is CollectionUiState.Success -> {
                            rv.visibility = View.VISIBLE
                            tvEmpty.visibility = View.GONE
                            rv.layoutManager = GridLayoutManager(requireContext(), 2)
                            rv.adapter = CardAdapter(state.cards)
                        }
                        is CollectionUiState.Error -> {
                            rv.visibility = View.GONE
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = state.message
                        }
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCards()
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    inner class CardAdapter(private val cards: List<CollectedCardManager.CollectedCard>) :
        RecyclerView.Adapter<CardAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivImage: ImageView = v.findViewById(R.id.iv_card_image)
            val tvSkillName: TextView = v.findViewById(R.id.tv_skill_name)
            val tvGrade: TextView = v.findViewById(R.id.tv_grade)
            val tvWord: TextView = v.findViewById(R.id.tv_word)
            val tvDamage: TextView = v.findViewById(R.id.tv_damage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_collected_card, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val card = cards[position]
            holder.tvSkillName.text = card.skillName
            holder.tvWord.text = card.word
            holder.tvDamage.text = "⚔ 데미지: ${card.damage}"

            val gradeColor = when (card.grade) {
                "금급" -> ContextCompat.getColor(holder.itemView.context, R.color.color_grade_gold)
                "은급" -> ContextCompat.getColor(holder.itemView.context, R.color.color_grade_silver)
                else   -> ContextCompat.getColor(holder.itemView.context, R.color.color_grade_bronze)
            }
            holder.tvGrade.text = card.grade
            holder.tvGrade.backgroundTintList = ColorStateList.valueOf(gradeColor)

            holder.ivImage.load(card.imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }

            holder.itemView.setOnClickListener {
                startActivity(Intent(requireContext(), CardDetailActivity::class.java).apply {
                    putExtra(CardDetailActivity.EXTRA_WORD_ID, card.wordId)
                    putExtra(CardDetailActivity.EXTRA_WORD, card.word)
                    putExtra(CardDetailActivity.EXTRA_SKILL_NAME, card.skillName)
                    putExtra(CardDetailActivity.EXTRA_SKILL_DESC, card.skillDescription)
                    putExtra(CardDetailActivity.EXTRA_DAMAGE, card.damage)
                    putExtra(CardDetailActivity.EXTRA_GRADE, card.grade)
                    putExtra(CardDetailActivity.EXTRA_IMAGE_B64, card.imageBase64)
                })
            }
        }

        override fun getItemCount() = cards.size
    }
}
