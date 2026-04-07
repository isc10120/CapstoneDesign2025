package com.example.zamgavocafront.pve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeckSelectActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EDIT_SLOT = "edit_slot"
    }

    private lateinit var rvDecks: RecyclerView
    private lateinit var adapter: DeckSlotAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_select)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_new_deck).setOnClickListener { openDeckBuild(newSlot = true) }

        rvDecks = findViewById(R.id.rv_decks)
        rvDecks.layoutManager = LinearLayoutManager(this)
        adapter = DeckSlotAdapter()
        rvDecks.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.refresh()
    }

    private fun openDeckBuild(newSlot: Boolean, slotIndex: Int = -1) {
        val decks = DeckManager.getDecks(this)
        if (newSlot && decks.size >= DeckManager.MAX_DECK_SLOTS) {
            AlertDialog.Builder(this)
                .setTitle("덱 슬롯 꽉 참")
                .setMessage("덱을 최대 ${DeckManager.MAX_DECK_SLOTS}개까지 저장할 수 있습니다.\n기존 덱을 삭제 후 새로 만들어주세요.")
                .setPositiveButton("확인", null)
                .show()
            return
        }
        val targetSlot = if (newSlot) {
            val usedSlots = decks.map { it.slotIndex }.toSet()
            (0 until DeckManager.MAX_DECK_SLOTS).first { it !in usedSlots }
        } else slotIndex

        startActivity(Intent(this, DeckBuildActivity::class.java).apply {
            putExtra(DeckBuildActivity.EXTRA_SLOT_INDEX, targetSlot)
            if (!newSlot) putExtra(DeckBuildActivity.EXTRA_EDIT_MODE, true)
        })
    }

    // ── 덱 슬롯 어댑터 ──────────────────────────────────────────────
    private inner class DeckSlotAdapter : RecyclerView.Adapter<DeckSlotAdapter.VH>() {
        private var decks: List<DeckManager.Deck> = emptyList()
        private var activeDeckIdx = -1

        fun refresh() {
            decks = DeckManager.getDecks(this@DeckSelectActivity)
            activeDeckIdx = DeckManager.getActiveDeckIndex(this@DeckSelectActivity)
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tv_deck_name)
            val tvCount: TextView = v.findViewById(R.id.tv_deck_cards_count)
            val tvLastUsed: TextView = v.findViewById(R.id.tv_deck_last_used)
            val tvActiveBadge: TextView = v.findViewById(R.id.tv_active_badge)
            val btnSelect: Button = v.findViewById(R.id.btn_select_deck)
            val btnEdit: Button = v.findViewById(R.id.btn_edit_deck)
            val btnDelete: Button = v.findViewById(R.id.btn_delete_deck)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_deck_slot, parent, false))

        override fun getItemCount() = decks.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val deck = decks[pos]
            h.tvName.text = deck.name
            h.tvCount.text = "카드 ${deck.cardWordIds.size}장"
            h.tvLastUsed.text = if (deck.lastUsedAt > 0L)
                "마지막 사용: ${formatDate(deck.lastUsedAt)}"
            else "미사용"

            val isActive = deck.slotIndex == activeDeckIdx
            h.tvActiveBadge.visibility = if (isActive) View.VISIBLE else View.GONE

            h.btnSelect.text = if (isActive) "선택됨" else "선택"
            h.btnSelect.isEnabled = !isActive
            h.btnSelect.setOnClickListener {
                DeckManager.setActiveDeckIndex(this@DeckSelectActivity, deck.slotIndex)
                refresh()
            }
            h.btnEdit.setOnClickListener {
                openDeckBuild(newSlot = false, slotIndex = deck.slotIndex)
            }
            h.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@DeckSelectActivity)
                    .setTitle("덱 삭제")
                    .setMessage("'${deck.name}' 덱을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        DeckManager.deleteDeck(this@DeckSelectActivity, deck.slotIndex)
                        refresh()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }

        private fun formatDate(ts: Long): String =
            SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
