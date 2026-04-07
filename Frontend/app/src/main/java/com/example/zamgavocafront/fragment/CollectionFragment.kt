package com.example.zamgavocafront.fragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.pve.CardDetailActivity
import com.example.zamgavocafront.pvp.CollectedCardManager

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
        val cards = CollectedCardManager.getCards(requireContext())

        if (cards.isEmpty()) {
            rv.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = CardAdapter(cards)
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

            if (card.imageBase64 != null) {
                try {
                    val bytes = Base64.decode(card.imageBase64, Base64.DEFAULT)
                    holder.ivImage.setImageBitmap(
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    )
                } catch (e: Exception) {
                    holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
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
