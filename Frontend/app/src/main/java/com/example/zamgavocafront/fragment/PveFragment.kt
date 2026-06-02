package com.example.zamgavocafront.fragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.zamgavocafront.R
import com.example.zamgavocafront.pve.DeckManager
import com.example.zamgavocafront.pve.DeckSelectActivity
import com.example.zamgavocafront.pve.PveBattleEngine
import com.example.zamgavocafront.pve.PveDungeonActivity
import com.example.zamgavocafront.pve.PveStageData
import com.example.zamgavocafront.pvp.CollectedCardManager

class PveFragment : Fragment() {

    private lateinit var tvActiveDeck: TextView
    private lateinit var tvClearedCount: TextView
    private lateinit var tvContinueStage: TextView
    private lateinit var layoutStageInfo: LinearLayout
    private lateinit var layoutNoBattle: LinearLayout
    private lateinit var btnContinue: Button
    private lateinit var btnEnterRandom: Button
    private lateinit var rvDeckCards: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var deckAdapter: DeckCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pve, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvActiveDeck    = view.findViewById(R.id.tv_active_deck)
        tvClearedCount  = view.findViewById(R.id.tv_cleared_count)
        tvContinueStage = view.findViewById(R.id.tv_continue_stage)
        layoutStageInfo = view.findViewById(R.id.layout_stage_info)
        layoutNoBattle  = view.findViewById(R.id.layout_no_battle)
        btnContinue     = view.findViewById(R.id.btn_continue)
        btnEnterRandom  = view.findViewById(R.id.btn_enter_random)
        rvDeckCards     = view.findViewById(R.id.rv_deck_cards)
        tvEmpty         = view.findViewById(R.id.tv_empty)

        view.findViewById<Button>(R.id.btn_deck).setOnClickListener {
            startActivity(Intent(requireContext(), DeckSelectActivity::class.java))
        }
        btnContinue.setOnClickListener { onContinueBattle() }
        btnEnterRandom.setOnClickListener { onEnterRandom() }

        deckAdapter = DeckCardAdapter(emptyList())
        rvDeckCards.layoutManager = GridLayoutManager(requireContext(), 3)
        rvDeckCards.adapter = deckAdapter
    }

    override fun onResume() {
        super.onResume()
        updateDeckDisplay()
        updateBattleState()
    }

    private fun updateDeckDisplay() {
        val deck = DeckManager.getActiveDeck(requireContext())
        tvActiveDeck.text = if (deck != null) "${deck.name} (${deck.cardWordIds.size}장)" else "없음"
        tvClearedCount.text = "${DeckManager.getClearedStageCount(requireContext())} 스테이지"

        val cards = if (deck != null) DeckManager.resolveCards(requireContext(), deck) else emptyList()
        deckAdapter.updateCards(cards)
        rvDeckCards.visibility = if (cards.isEmpty()) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateBattleState() {
        if (PveBattleEngine.isActive) {
            val s = PveBattleEngine.state
            val stage = PveStageData.stages.firstOrNull { it.id == s.stageId }
            tvContinueStage.text = if (stage != null) {
                "${stage.emoji} ${stage.name}  |  라운드 ${s.roundIndex + 1}/5" +
                        if (s.isBossRound) " 🔥보스" else ""
            } else {
                "스테이지 ${s.stageId}"
            }
            layoutStageInfo.visibility = View.VISIBLE
            layoutNoBattle.visibility = View.GONE
            btnContinue.visibility = View.VISIBLE
            btnEnterRandom.visibility = View.GONE
        } else {
            layoutStageInfo.visibility = View.GONE
            layoutNoBattle.visibility = View.VISIBLE
            btnContinue.visibility = View.GONE
            btnEnterRandom.visibility = View.VISIBLE
        }
    }

    private fun onContinueBattle() {
        val s = PveBattleEngine.state
        startActivity(Intent(requireContext(), PveDungeonActivity::class.java).apply {
            putExtra(PveDungeonActivity.EXTRA_STAGE_ID, s.stageId)
            putExtra(PveDungeonActivity.EXTRA_DECK_SLOT, 0)
        })
    }

    private fun onEnterRandom() {
        val deck = DeckManager.getActiveDeck(requireContext())
        if (deck == null) {
            AlertDialog.Builder(requireContext())
                .setTitle("덱 필요")
                .setMessage("스테이지에 입장하려면 덱을 구성하고 선택해주세요.")
                .setPositiveButton("덱 구성하기") { _, _ ->
                    startActivity(Intent(requireContext(), DeckSelectActivity::class.java))
                }
                .setNegativeButton("취소", null)
                .show()
            return
        }
        if (!DeckManager.isDeckValid(deck, playerLevel = 1)) {
            AlertDialog.Builder(requireContext())
                .setTitle("덱 부족")
                .setMessage("덱에 카드가 부족합니다. 최소 ${DeckManager.MIN_CARDS}장 이상 구성해주세요.")
                .setPositiveButton("확인", null)
                .show()
            return
        }
        val cards = DeckManager.resolveCards(requireContext(), deck)
        val randomStage = PveStageData.stages.random()
        AlertDialog.Builder(requireContext())
            .setTitle("${randomStage.emoji} ${randomStage.name}")
            .setMessage(
                "랜덤으로 선택된 스테이지!\n잡몹 4라운드 + 보스 1라운드\n\n" +
                "선택된 덱: ${deck.name} (${cards.size}장)\n\n입장하시겠습니까?"
            )
            .setPositiveButton("입장!") { _, _ ->
                startActivity(Intent(requireContext(), PveDungeonActivity::class.java).apply {
                    putExtra(PveDungeonActivity.EXTRA_STAGE_ID, randomStage.id)
                    putExtra(PveDungeonActivity.EXTRA_DECK_SLOT, deck.slotIndex)
                })
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 덱 카드 어댑터 ────────────────────────────────────────────────
    private inner class DeckCardAdapter(
        private var cards: List<CollectedCardManager.CollectedCard>
    ) : RecyclerView.Adapter<DeckCardAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivImage: ImageView  = v.findViewById(R.id.iv_skill_image)
            val ivFrame: ImageView  = v.findViewById(R.id.iv_card_frame)
            val tvName: TextView    = v.findViewById(R.id.tv_skill_name)
            val tvDamage: TextView  = v.findViewById(R.id.tv_skill_damage_compact)
            val tvWord: TextView    = v.findViewById(R.id.tv_word)
            val flFrame: FrameLayout = v.findViewById(R.id.fl_card_frame)
        }

        fun updateCards(newCards: List<CollectedCardManager.CollectedCard>) {
            cards = newCards
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pve_card, parent, false)
            v.post {
                val fl = v.findViewById<FrameLayout>(R.id.fl_card_frame)
                val lp = fl.layoutParams
                lp.height = (v.width * 280f / 180f).toInt()
                fl.layoutParams = lp
            }
            return VH(v)
        }

        override fun getItemCount() = cards.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val card = cards[pos]
            h.tvName.text   = card.skillName
            h.tvDamage.text = "${card.damage} DMG"
            h.tvWord.text   = card.word
            h.ivFrame.setImageResource(when (card.grade) {
                "금급" -> R.drawable.cardframe_gold
                "은급" -> R.drawable.cardframe_silver
                else   -> R.drawable.cardframe_bronze
            })
            when {
                !card.imageUrl.isNullOrBlank() -> h.ivImage.load(card.imageUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                }
                card.imageBase64 != null -> runCatching {
                    val bytes = Base64.decode(card.imageBase64, Base64.DEFAULT)
                    h.ivImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                }.onFailure { h.ivImage.setImageResource(android.R.drawable.ic_menu_gallery) }
                else -> h.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}
