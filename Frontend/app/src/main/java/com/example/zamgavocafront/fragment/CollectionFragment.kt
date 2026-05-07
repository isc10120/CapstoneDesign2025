package com.example.zamgavocafront.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
                            rv.layoutManager = GridLayoutManager(requireContext(), 3)
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
            val flCard: FrameLayout = v.findViewById(R.id.fl_card_frame)
            val ivImage: ImageView = v.findViewById(R.id.iv_skill_image)
            val ivFrame: ImageView = v.findViewById(R.id.iv_card_frame)
            val tvSkillName: TextView = v.findViewById(R.id.tv_skill_name)
            val tvDamage: TextView = v.findViewById(R.id.tv_skill_damage_compact)
            val tvWord: TextView = v.findViewById(R.id.tv_word)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_collected_card, parent, false)
            v.post {
                val fl = v.findViewById<FrameLayout>(R.id.fl_card_frame)
                val lp = fl.layoutParams
                lp.height = (v.width * 280f / 180f).toInt()
                fl.layoutParams = lp
            }
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val card = cards[position]
            holder.tvSkillName.text = card.skillName
            holder.tvDamage.text = "${card.damage} DMG"
            holder.tvWord.text = card.word

            val frameRes = when (card.grade) {
                "금급" -> R.drawable.cardframe_gold
                "은급" -> R.drawable.cardframe_silver
                else   -> R.drawable.cardframe_bronze
            }
            holder.ivFrame.setImageResource(frameRes)

            when {
                !card.imageUrl.isNullOrBlank() -> holder.ivImage.load(card.imageUrl) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                }
                card.imageBase64 != null -> {
                    runCatching {
                        val bytes = android.util.Base64.decode(card.imageBase64, android.util.Base64.DEFAULT)
                        holder.ivImage.setImageBitmap(
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        )
                    }.onFailure {
                        holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
                else -> holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            holder.itemView.setOnClickListener {
                startActivity(Intent(requireContext(), CardDetailActivity::class.java).apply {
                    putExtra(CardDetailActivity.EXTRA_WORD_ID, card.wordId)
                    putExtra(CardDetailActivity.EXTRA_WORD, card.word)
                    putExtra(CardDetailActivity.EXTRA_WORD_MEANING, card.wordMeaning)
                    putExtra(CardDetailActivity.EXTRA_SKILL_NAME, card.skillName)
                    putExtra(CardDetailActivity.EXTRA_SKILL_DESC, card.skillDescription)
                    putExtra(CardDetailActivity.EXTRA_DAMAGE, card.damage)
                    putExtra(CardDetailActivity.EXTRA_GRADE, card.grade)
                    putExtra(CardDetailActivity.EXTRA_IMAGE_B64, card.imageBase64)
                    putExtra(CardDetailActivity.EXTRA_IMAGE_URL, card.imageUrl)
                })
            }
        }

        override fun getItemCount() = cards.size
    }
}
