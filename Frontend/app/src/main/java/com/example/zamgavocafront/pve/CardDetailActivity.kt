package com.example.zamgavocafront.pve

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.zamgavocafront.R

class CardDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID    = "word_id"
        const val EXTRA_WORD       = "word"
        const val EXTRA_SKILL_NAME = "skill_name"
        const val EXTRA_SKILL_DESC = "skill_desc"
        const val EXTRA_DAMAGE     = "damage"
        const val EXTRA_GRADE      = "grade"
        const val EXTRA_IMAGE_B64  = "image_b64"
        // 단어 의미 (CollectionFragment에서 넘길 때 사용)
        const val EXTRA_WORD_MEANING = "word_meaning"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_detail)

        val wordId   = intent.getIntExtra(EXTRA_WORD_ID, 0)
        val word     = intent.getStringExtra(EXTRA_WORD) ?: ""
        val skillName = intent.getStringExtra(EXTRA_SKILL_NAME) ?: ""
        val skillDesc = intent.getStringExtra(EXTRA_SKILL_DESC) ?: ""
        val damage   = intent.getIntExtra(EXTRA_DAMAGE, 0)
        val grade    = intent.getStringExtra(EXTRA_GRADE) ?: "동급"
        val imageB64 = intent.getStringExtra(EXTRA_IMAGE_B64)
        val wordMeaning = intent.getStringExtra(EXTRA_WORD_MEANING) ?: "뜻 정보 없음"

        // 가상의 CollectedCard로 효과 계산
        val fakeCard = com.example.zamgavocafront.pvp.CollectedCardManager.CollectedCard(
            wordId = wordId, word = word, skillName = skillName,
            skillDescription = skillDesc, damage = damage, imageBase64 = imageB64, grade = grade
        )
        val effect = PveBattleEngine.getCardEffect(fakeCard)

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener { finish() }

        val ivImage = findViewById<ImageView>(R.id.iv_skill_image)
        if (imageB64 != null) {
            try {
                val bytes = Base64.decode(imageB64, Base64.DEFAULT)
                ivImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Exception) { }
        }

        val tvGrade = findViewById<TextView>(R.id.tv_grade)
        tvGrade.text = grade
        tvGrade.backgroundTintList = android.content.res.ColorStateList.valueOf(
            when (grade) {
                "금급" -> Color.parseColor("#FFC107")
                "은급" -> Color.parseColor("#9E9E9E")
                else   -> Color.parseColor("#CD7F32")
            }
        )

        findViewById<TextView>(R.id.tv_skill_name).text = skillName
        findViewById<TextView>(R.id.tv_word).text = word
        findViewById<TextView>(R.id.tv_word_meaning).text = wordMeaning
        findViewById<TextView>(R.id.tv_damage).text = "$damage"
        findViewById<TextView>(R.id.tv_effect_type).text = "${effect.icon} ${effect.displayName}"
        findViewById<TextView>(R.id.tv_skill_description).text = skillDesc
    }
}
