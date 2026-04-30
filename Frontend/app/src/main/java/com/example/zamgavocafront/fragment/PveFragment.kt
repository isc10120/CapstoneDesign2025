package com.example.zamgavocafront.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.pve.DeckManager
import com.example.zamgavocafront.pve.DeckSelectActivity
import com.example.zamgavocafront.pve.PveBattleEngine
import com.example.zamgavocafront.pve.PveDungeonActivity
import com.example.zamgavocafront.pve.PveStageData
import com.example.zamgavocafront.pve.StageTemplate

class PveFragment : Fragment() {

    private lateinit var tvActiveDeck: TextView
    private lateinit var cardContinue: CardView
    private lateinit var tvContinueStage: TextView
    private lateinit var btnContinue: Button
    private lateinit var btnEnterRandom: Button
    private lateinit var rvStages: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pve, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvActiveDeck = view.findViewById(R.id.tv_active_deck)
        cardContinue = view.findViewById(R.id.card_continue)
        tvContinueStage = view.findViewById(R.id.tv_continue_stage)
        btnContinue = view.findViewById(R.id.btn_continue)
        btnEnterRandom = view.findViewById(R.id.btn_enter_random)
        rvStages = view.findViewById(R.id.rv_stages)

        view.findViewById<Button>(R.id.btn_deck).setOnClickListener {
            startActivity(Intent(requireContext(), DeckSelectActivity::class.java))
        }

        btnContinue.setOnClickListener { onContinueBattle() }
        btnEnterRandom.setOnClickListener { onEnterRandom() }

        rvStages.layoutManager = LinearLayoutManager(requireContext())
        rvStages.adapter = StageAdapter(PveStageData.stages)
    }

    override fun onResume() {
        super.onResume()
        updateActiveDeckDisplay()
        updateContinueUI()
    }

    private fun updateActiveDeckDisplay() {
        val deck = DeckManager.getActiveDeck(requireContext())
        tvActiveDeck.text = if (deck != null) {
            "${deck.name}  (${deck.cardWordIds.size}장)"
        } else {
            "덱 없음 - 덱 구성 후 선택해주세요"
        }
    }

    private fun updateContinueUI() {
        if (PveBattleEngine.isActive) {
            val s = PveBattleEngine.state
            val stage = PveStageData.stages.firstOrNull { it.id == s.stageId }
            tvContinueStage.text = if (stage != null) {
                "${stage.emoji} ${stage.name}  |  라운드 ${s.roundIndex + 1}/5" +
                        if (s.isBossRound) " 🔥보스" else ""
            } else {
                "스테이지 ${s.stageId}"
            }
            cardContinue.visibility = View.VISIBLE
            btnEnterRandom.visibility = View.GONE
        } else {
            cardContinue.visibility = View.GONE
            btnEnterRandom.visibility = View.VISIBLE
        }
        // 스테이지 현황 목록 갱신
        rvStages.adapter?.notifyDataSetChanged()
    }

    /** 진행 중인 전투를 이어서 진행 */
    private fun onContinueBattle() {
        val s = PveBattleEngine.state
        val intent = Intent(requireContext(), PveDungeonActivity::class.java).apply {
            putExtra(PveDungeonActivity.EXTRA_STAGE_ID, s.stageId)
            putExtra(PveDungeonActivity.EXTRA_DECK_SLOT, 0) // 전투 활성 상태이므로 무시됨
        }
        startActivity(intent)
    }

    /** 랜덤 스테이지로 새 전투 시작 */
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
            .setMessage("랜덤으로 선택된 스테이지!\n잡몹 4라운드 + 보스 1라운드\n\n선택된 덱: ${deck.name} (${cards.size}장)\n\n입장하시겠습니까?")
            .setPositiveButton("입장!") { _, _ ->
                val intent = Intent(requireContext(), PveDungeonActivity::class.java).apply {
                    putExtra(PveDungeonActivity.EXTRA_STAGE_ID, randomStage.id)
                    putExtra(PveDungeonActivity.EXTRA_DECK_SLOT, deck.slotIndex)
                }
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 스테이지 현황 어댑터 (클릭 없이 현황만 표시) ──────────────────
    private inner class StageAdapter(
        private val stages: List<StageTemplate>
    ) : RecyclerView.Adapter<StageAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvEmoji: TextView = v.findViewById(R.id.tv_stage_emoji)
            val tvName: TextView = v.findViewById(R.id.tv_stage_name)
            val tvDetail: TextView = v.findViewById(R.id.tv_stage_detail)
            val tvStatus: TextView = v.findViewById(R.id.tv_stage_status)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_stage, parent, false))

        override fun getItemCount() = stages.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val stage = stages[pos]
            val cleared = DeckManager.getClearedStageCount(requireContext())
            h.tvEmoji.text = stage.emoji
            h.tvName.text = "스테이지 ${stage.id}  ${stage.name}"
            h.tvDetail.text = "잡몹 4라운드 + 보스 1라운드"
            h.tvStatus.text = if (pos < cleared) "✅ 클리어" else "미클리어"
            h.tvStatus.backgroundTintList = ColorStateList.valueOf(
                if (pos < cleared) ContextCompat.getColor(h.itemView.context, R.color.difficulty_easy)
                else ContextCompat.getColor(h.itemView.context, R.color.difficulty_medium)
            )
        }
    }
}
