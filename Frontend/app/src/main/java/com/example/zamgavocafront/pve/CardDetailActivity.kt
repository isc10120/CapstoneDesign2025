package com.example.zamgavocafront.pve

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.zamgavocafront.R

class CardDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID      = "word_id"
        const val EXTRA_WORD         = "word"
        const val EXTRA_SKILL_NAME   = "skill_name"
        const val EXTRA_SKILL_DESC   = "skill_desc"
        const val EXTRA_DAMAGE       = "damage"
        const val EXTRA_GRADE        = "grade"
        const val EXTRA_IMAGE_B64    = "image_b64"
        const val EXTRA_WORD_MEANING = "word_meaning"
        const val EXTRA_IMAGE_URL    = "image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_detail)

        val word        = intent.getStringExtra(EXTRA_WORD) ?: ""
        val skillName   = intent.getStringExtra(EXTRA_SKILL_NAME) ?: ""
        val skillDesc   = intent.getStringExtra(EXTRA_SKILL_DESC) ?: ""
        val damage      = intent.getIntExtra(EXTRA_DAMAGE, 0)
        val grade       = intent.getStringExtra(EXTRA_GRADE) ?: "동급"
        val imageB64    = intent.getStringExtra(EXTRA_IMAGE_B64)
        val imageUrl    = intent.getStringExtra(EXTRA_IMAGE_URL)
findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }

        val ivImage = findViewById<ImageView>(R.id.iv_skill_image)
        when {
            !imageUrl.isNullOrBlank() -> ivImage.load(imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
            imageB64 != null -> runCatching {
                val bytes = Base64.decode(imageB64, Base64.DEFAULT)
                ivImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }
        }

        findViewById<ImageView>(R.id.iv_card_frame).setImageResource(
            when (grade) {
                "금급" -> R.drawable.cardframe_gold
                "은급" -> R.drawable.cardframe_silver
                else   -> R.drawable.cardframe_bronze
            }
        )

        findViewById<TextView>(R.id.tv_skill_name).text = skillName
        findViewById<TextView>(R.id.tv_skill_damage).text = "+$damage 데미지!"
        findViewById<TextView>(R.id.tv_word).text = word
        findViewById<TextView>(R.id.tv_skill_description).text = skillDesc
    }
}
