package com.example.zamgavocafront.pvp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
            rv.layoutManager = GridLayoutManager(this, 3)
            rv.adapter = Adapter(cards)
        }
    }

    inner class Adapter(private val cards: List<CollectedCardManager.CollectedCard>) :
        RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val flCard: FrameLayout = view.findViewById(R.id.fl_card_frame)
            val ivImage: ImageView  = view.findViewById(R.id.iv_skill_image)
            val ivFrame: ImageView  = view.findViewById(R.id.iv_card_frame)
            val tvName: TextView    = view.findViewById(R.id.tv_skill_name)
            val tvDamage: TextView  = view.findViewById(R.id.tv_skill_damage_compact)
            val tvWord: TextView    = view.findViewById(R.id.tv_word)
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
            holder.tvName.text = card.skillName
            holder.tvDamage.text = "${card.damage} DMG"
            holder.tvWord.text = card.word

            holder.ivFrame.setImageResource(gradeFrameRes(card.grade))

            when {
                !card.imageUrl.isNullOrBlank() -> holder.ivImage.load(card.imageUrl) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                }
                card.imageBase64 != null -> try {
                    val bytes = Base64.decode(card.imageBase64, Base64.DEFAULT)
                    holder.ivImage.setImageBitmap(
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    )
                } catch (_: Exception) {
                    holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                else -> holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        override fun getItemCount() = cards.size

        private fun gradeFrameRes(grade: String): Int = when (grade) {
            "금급" -> R.drawable.cardframe_gold
            "은급" -> R.drawable.cardframe_silver
            else   -> R.drawable.cardframe_bronze
        }
    }
}
