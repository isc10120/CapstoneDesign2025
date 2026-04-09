package com.example.zamgavocafront.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.zamgavocafront.R
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.pve.CardDetailActivity
import com.example.zamgavocafront.pvp.CollectedCardManager
import kotlinx.coroutines.launch

class CollectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_collection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        refreshCards(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshCards(it) }
    }

    private fun refreshCards(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_collected_cards)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty)

        viewLifecycleOwner.lifecycleScope.launch {
            val cards = loadCollectedCards()
            if (cards.isEmpty()) {
                rv.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            } else {
                rv.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                rv.layoutManager = GridLayoutManager(requireContext(), 2)
                rv.adapter = CardAdapter(cards)
            }
        }
    }

    /** 서버에서 수집 스킬 목록을 가져온다. 실패 또는 비어있으면 로컬 저장소로 fallback. */
    private suspend fun loadCollectedCards(): List<CollectedCardManager.CollectedCard> {
        return try {
            val resp = ApiClient.api.getCollectedSkillList()
            if (resp.success && resp.data != null && resp.data.isNotEmpty()) {
                resp.data.map { skill ->
                    val localWord = WordRepository.allWords.find { it.id.toLong() == skill.wordId }
                    val grade = when (localWord?.difficulty) {
                        Difficulty.HARD -> "금급"
                        Difficulty.MEDIUM -> "은급"
                        else -> "동급"
                    }
                    CollectedCardManager.CollectedCard(
                        wordId = skill.wordId.toInt(),
                        word = localWord?.word ?: skill.name,
                        skillName = skill.name,
                        skillDescription = skill.explain,
                        damage = skill.damage,
                        imageBase64 = null,
                        grade = grade,
                        imageUrl = skill.imageURL.takeIf { it.isNotBlank() }
                    )
                }
            } else {
                CollectedCardManager.getCards(requireContext())
            }
        } catch (_: Exception) {
            CollectedCardManager.getCards(requireContext())
        }
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
                "금급" -> Color.parseColor("#FFC107")
                "은급" -> Color.parseColor("#9E9E9E")
                else -> Color.parseColor("#CD7F32")
            }
            holder.tvGrade.text = card.grade
            holder.tvGrade.backgroundTintList =
                android.content.res.ColorStateList.valueOf(gradeColor)

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
