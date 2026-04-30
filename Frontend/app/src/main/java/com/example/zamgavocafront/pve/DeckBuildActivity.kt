package com.example.zamgavocafront.pve

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.pvp.CollectedCardManager
import com.google.android.material.textfield.TextInputEditText

class DeckBuildActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SLOT_INDEX = "slot_index"
        const val EXTRA_EDIT_MODE  = "edit_mode"
        private const val PLAYER_LEVEL = 1  // TODO: 실제 레벨 시스템 연동 시 교체
    }

    private lateinit var etDeckName: TextInputEditText
    private lateinit var tvSelectedCount: TextView
    private lateinit var tvGuide: TextView
    private lateinit var rvCards: RecyclerView
    private lateinit var btnSave: Button

    private var slotIndex = 0
    private var editMode = false
    private val selectedCardIds = mutableSetOf<Int>()
    private var allCards: List<CollectedCardManager.CollectedCard> = emptyList()
    private var clearedCardIds: Set<Int> = emptySet()
    private val maxCards = DeckManager.getMaxCards(PLAYER_LEVEL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_build)

        slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, 0)
        editMode  = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

        etDeckName        = findViewById(R.id.et_deck_name)
        tvSelectedCount   = findViewById(R.id.tv_selected_count)
        tvGuide           = findViewById(R.id.tv_guide)
        rvCards           = findViewById(R.id.rv_cards)
        btnSave           = findViewById(R.id.btn_save_deck)

        tvGuide.text = "카드를 선택해 덱을 구성하세요. (최소 ${DeckManager.MIN_CARDS}장 ~ 최대 ${maxCards}장)"

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        btnSave.setOnClickListener { saveDeck() }

        clearedCardIds = DeckManager.getClearedCardIds(this)
        allCards = CollectedCardManager.getCards(this).distinctBy { it.wordId }

        // 테스트 카드 추가 버튼
        findViewById<Button>(R.id.btn_seed_mock).setOnClickListener {
            CollectedCardManager.seedMockCards(this)
            allCards = CollectedCardManager.getCards(this).distinctBy { it.wordId }
            rvCards.adapter?.notifyDataSetChanged()
            Toast.makeText(this, "테스트 카드 10장 추가됨", Toast.LENGTH_SHORT).show()
        }

        // 편집 모드: 기존 덱 데이터 로드
        if (editMode) {
            DeckManager.getDecks(this).find { it.slotIndex == slotIndex }?.let { deck ->
                etDeckName.setText(deck.name)
                selectedCardIds.addAll(deck.cardWordIds)
            }
        }

        setupRecyclerView()
        updateCountDisplay()
    }

    private fun setupRecyclerView() {
        val adapter = DeckCardAdapter()
        rvCards.layoutManager = GridLayoutManager(this, 3)
        rvCards.adapter = adapter
    }

    private fun updateCountDisplay() {
        tvSelectedCount.text = "${selectedCardIds.size} / ${maxCards}장"
        tvSelectedCount.setTextColor(
            if (selectedCardIds.size >= DeckManager.MIN_CARDS) ContextCompat.getColor(this, R.color.color_deck_ready)
            else ContextCompat.getColor(this, R.color.color_deck_incomplete)
        )
    }

    private fun saveDeck() {
        val name = etDeckName.text.toString().trim().ifEmpty { "덱 ${slotIndex + 1}" }
        if (selectedCardIds.size < DeckManager.MIN_CARDS) {
            Toast.makeText(this, "최소 ${DeckManager.MIN_CARDS}장 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCardIds.size > maxCards) {
            Toast.makeText(this, "최대 ${maxCards}장까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val deck = DeckManager.Deck(
            slotIndex = slotIndex,
            name = name,
            cardWordIds = selectedCardIds.toList(),
            lastUsedAt = 0L
        )
        DeckManager.saveDeck(this, deck)
        Toast.makeText(this, "'$name' 덱이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        finish()
    }

    // ── 카드 선택 어댑터 ─────────────────────────────────────────────
    private inner class DeckCardAdapter : RecyclerView.Adapter<DeckCardAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvGrade: TextView = v.findViewById(R.id.tv_grade)
            val tvSkillName: TextView = v.findViewById(R.id.tv_skill_name)
            val tvEffectType: TextView = v.findViewById(R.id.tv_effect_type)
            val tvDamage: TextView = v.findViewById(R.id.tv_damage)
            val tvWord: TextView = v.findViewById(R.id.tv_word)
            val selectedOverlay: FrameLayout = v.findViewById(R.id.selected_overlay)
            val lockedOverlay: FrameLayout = v.findViewById(R.id.locked_overlay)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_deck_card, parent, false))

        override fun getItemCount() = allCards.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val card = allCards[pos]
            val effect = PveBattleEngine.getCardEffect(card)
            val isSelected = card.wordId in selectedCardIds
            val isLocked = card.wordId in clearedCardIds

            h.tvGrade.text = card.grade
            h.tvGrade.backgroundTintList = ColorStateList.valueOf(gradeColor(card.grade))
            h.tvSkillName.text = card.skillName
            h.tvEffectType.text = "${effect.icon} ${effect.displayName}"
            h.tvDamage.text = "DMG ${card.damage}"
            h.tvWord.text = card.word

            h.selectedOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            h.lockedOverlay.visibility = if (isLocked) View.VISIBLE else View.GONE
            // 선택 안 된 카드는 살짝 투명하게 (선택된 카드가 더 도드라져 보임)
            h.itemView.alpha = when {
                isLocked   -> 0.4f
                isSelected -> 1.0f
                else       -> 0.65f
            }

            h.itemView.setOnClickListener {
                if (isLocked) {
                    Toast.makeText(this@DeckBuildActivity, "이전 스테이지 클리어 카드 - 재사용 불가", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (isSelected) {
                    selectedCardIds.remove(card.wordId)
                } else {
                    if (selectedCardIds.size >= maxCards) {
                        Toast.makeText(this@DeckBuildActivity, "최대 ${maxCards}장까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    selectedCardIds.add(card.wordId)
                }
                updateCountDisplay()
                notifyItemChanged(pos)
            }

            h.itemView.setOnLongClickListener {
                showCardDetail(card)
                true
            }
        }

        private fun gradeColor(grade: String): Int = when (grade) {
            "금급" -> ContextCompat.getColor(this@DeckBuildActivity, R.color.color_grade_gold)
            "은급" -> ContextCompat.getColor(this@DeckBuildActivity, R.color.color_grade_silver)
            else   -> ContextCompat.getColor(this@DeckBuildActivity, R.color.color_grade_bronze)
        }
    }

    private fun showCardDetail(card: CollectedCardManager.CollectedCard) {
        startActivity(Intent(this, CardDetailActivity::class.java).apply {
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
