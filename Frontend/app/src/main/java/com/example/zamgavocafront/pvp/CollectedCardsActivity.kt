package com.example.zamgavocafront.pvp

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.zamgavocafront.R

class CollectedCardsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collected_cards)

        val rv = findViewById<RecyclerView>(R.id.rv_collected_cards)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty)
        val cards = CollectedCardManager.getCards(this)

        if (cards.isEmpty()) {
            rv.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            rv.layoutManager = GridLayoutManager(this, 2)
            rv.adapter = Adapter(cards)
        }
    }

    inner class Adapter(private val cards: List<CollectedCardManager.CollectedCard>) :
        RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.iv_card_image)
            val tvSkillName: TextView = view.findViewById(R.id.tv_skill_name)
            val tvGrade: TextView = view.findViewById(R.id.tv_grade)
            val tvWord: TextView = view.findViewById(R.id.tv_word)
            val tvDamage: TextView = view.findViewById(R.id.tv_damage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_collected_card, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val card = cards[position]
            holder.tvSkillName.text = card.skillName
            holder.tvWord.text = card.word
            holder.tvDamage.text = "⚔ 데미지: ${card.damage}"

            val gradeColor = gradeColor(card.grade)
            holder.tvGrade.text = card.grade
            holder.tvGrade.backgroundTintList =
                ColorStateList.valueOf(gradeColor)

            when {
                !card.imageUrl.isNullOrBlank() -> holder.ivImage.load(card.imageUrl) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                }
                card.imageBase64 != null -> try {
                    val bytes = Base64.decode(card.imageBase64, Base64.DEFAULT)
                    holder.ivImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                } catch (e: Exception) {
                    holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                else -> holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        override fun getItemCount() = cards.size

        private fun gradeColor(grade: String): Int = when (grade) {
            "금급" -> ContextCompat.getColor(this@CollectedCardsActivity, R.color.color_grade_gold)
            "은급" -> ContextCompat.getColor(this@CollectedCardsActivity, R.color.color_grade_silver)
            else   -> ContextCompat.getColor(this@CollectedCardsActivity, R.color.color_grade_bronze)
        }
    }
}
