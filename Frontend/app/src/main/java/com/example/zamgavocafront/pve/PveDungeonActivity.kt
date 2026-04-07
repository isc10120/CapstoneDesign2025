package com.example.zamgavocafront.pve

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.pvp.CollectedCardManager

class PveDungeonActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STAGE_ID = "stage_id"
        const val EXTRA_DECK_SLOT = "deck_slot"
    }

    private lateinit var tvStageRound: TextView
    private lateinit var tvTurn: TextView
    private lateinit var tvPlayerStatus: TextView
    private lateinit var tvPlayerHp: TextView
    private lateinit var tvPlayerBuffs: TextView
    private lateinit var pbPlayerHp: ProgressBar
    private lateinit var tvAvailableCount: TextView
    private lateinit var rvMonsters: RecyclerView
    private lateinit var rvCards: RecyclerView

    private lateinit var monsterAdapter: MonsterAdapter
    private lateinit var cardAdapter: DungeonCardAdapter

    private var pendingCard: CollectedCardManager.CollectedCard? = null

    private val questionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val card = pendingCard ?: return@registerForActivityResult
        pendingCard = null

        if (result.resultCode == RESULT_OK) {
            val wordId = result.data?.getIntExtra(PveQuestionActivity.RESULT_WORD_ID, -1) ?: -1
            if (wordId < 0) return@registerForActivityResult
            onCardAnsweredCorrectly(card)
        } else {
            Toast.makeText(this, "카드 발동 실패! 다른 카드를 선택하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pve_dungeon)

        val stageId = intent.getIntExtra(EXTRA_STAGE_ID, 1)
        val deckSlot = intent.getIntExtra(EXTRA_DECK_SLOT, 0)

        // 덱 로드 & 배틀 시작
        val deck = DeckManager.getDecks(this).find { it.slotIndex == deckSlot }
        val deckCards = deck?.let { DeckManager.resolveCards(this, it) } ?: emptyList()

        if (!PveBattleEngine.isActive) {
            PveBattleEngine.startBattle(stageId, deckCards)
        }

        bindViews()
        setupRecyclerViews()
        refreshUI()
    }

    private fun bindViews() {
        tvStageRound = findViewById(R.id.tv_stage_round)
        tvTurn = findViewById(R.id.tv_turn)
        tvPlayerStatus = findViewById(R.id.tv_player_status)
        tvPlayerHp = findViewById(R.id.tv_player_hp)
        tvPlayerBuffs = findViewById(R.id.tv_player_buffs)
        pbPlayerHp = findViewById(R.id.pb_player_hp)
        tvAvailableCount = findViewById(R.id.tv_available_count)
        rvMonsters = findViewById(R.id.rv_monsters)
        rvCards = findViewById(R.id.rv_cards)

        findViewById<Button>(R.id.btn_retreat).setOnClickListener { confirmRetreat() }
    }

    private fun setupRecyclerViews() {
        monsterAdapter = MonsterAdapter()
        rvMonsters.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMonsters.adapter = monsterAdapter

        cardAdapter = DungeonCardAdapter { card -> onCardSelected(card) }
        rvCards.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCards.adapter = cardAdapter
    }

    private fun refreshUI() {
        val s = PveBattleEngine.state
        val stage = PveBattleEngine.getCurrentStageTemplate()

        tvStageRound.text = "스테이지 ${s.stageId} · 라운드 ${s.roundIndex + 1}/5" +
                if (s.isBossRound) " 🔥보스" else ""
        tvTurn.text = "턴 ${s.turn}"

        // 상태이상
        val statusText = s.statusSummary()
        if (statusText.isNotEmpty()) {
            tvPlayerStatus.text = statusText
            tvPlayerStatus.visibility = View.VISIBLE
        } else {
            tvPlayerStatus.visibility = View.GONE
        }

        // 플레이어 HP
        tvPlayerHp.text = "${s.playerHp} / ${s.playerMaxHp}"
        pbPlayerHp.progress = (s.playerHpPercent * 100).toInt()

        // 버프 표시
        val buffText = s.playerBuffs.joinToString("  ") { e ->
            when (e) {
                is StatusEffect.AttackBuff -> "⬆+${e.bonusDamage}"
                is StatusEffect.Shield -> "🛡×${e.count}"
                else -> ""
            }
        }.trim()
        if (buffText.isNotEmpty()) {
            tvPlayerBuffs.text = buffText
            tvPlayerBuffs.visibility = View.VISIBLE
        } else {
            tvPlayerBuffs.visibility = View.GONE
        }

        // 몬스터
        monsterAdapter.setMonsters(s.monsters)

        // 카드
        val allCards = PveBattleEngine.currentDeck
        val available = PveBattleEngine.getAvailableCards()
        cardAdapter.setCards(allCards, available.map { it.wordId }.toSet())
        tvAvailableCount.text = "${available.size} / ${allCards.size}장 사용 가능"
    }

    private fun onCardSelected(card: CollectedCardManager.CollectedCard) {
        val cd = PveBattleEngine.getCooldown(card.wordId)
        if (cd > 0) {
            Toast.makeText(this, "쿨타임 중! ${cd}턴 후 사용 가능", Toast.LENGTH_SHORT).show()
            return
        }

        pendingCard = card
        val intent = Intent(this, PveQuestionActivity::class.java).apply {
            putExtra(PveQuestionActivity.EXTRA_WORD_ID, card.wordId)
            putExtra(PveQuestionActivity.EXTRA_WORD_TEXT, card.word)
            putExtra(PveQuestionActivity.EXTRA_SKILL_NAME, card.skillName)
            putExtra(PveQuestionActivity.EXTRA_SKILL_GRADE, card.grade)
            putExtra(PveQuestionActivity.EXTRA_SKILL_DAMAGE, card.damage)
            putExtra(PveQuestionActivity.EXTRA_EFFECT_TYPE, PveBattleEngine.getCardEffect(card).name)
        }
        questionLauncher.launch(intent)
    }

    private fun onCardAnsweredCorrectly(card: CollectedCardManager.CollectedCard) {
        val s = PveBattleEngine.state
        val aliveMonsters = s.monsters.filter { it.isAlive }

        val effect = PveBattleEngine.getCardEffect(card)
        val needsTarget = (effect == SkillEffectType.ATTACK || effect == SkillEffectType.POISON
                || effect == SkillEffectType.PARALYSIS) && aliveMonsters.size > 1

        if (needsTarget) {
            showTargetPickerDialog(card, aliveMonsters)
        } else {
            executeCard(card, 0)
        }
    }

    private fun showTargetPickerDialog(
        card: CollectedCardManager.CollectedCard,
        aliveMonsters: List<MonsterState>
    ) {
        val names = aliveMonsters.map { "${it.template.emoji} ${it.template.name}  (HP: ${it.hp})" }
            .toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("공격 대상 선택")
            .setItems(names) { _, which ->
                val realIdx = PveBattleEngine.state.monsters.indexOf(aliveMonsters[which])
                executeCard(card, realIdx)
            }
            .setCancelable(false)
            .show()
    }

    private fun executeCard(card: CollectedCardManager.CollectedCard, targetIdx: Int) {
        val result = PveBattleEngine.useCard(card, targetIdx)
        showTurnResultDialog(result)
    }

    private fun showTurnResultDialog(result: TurnResult) {
        val sb = StringBuilder()

        // 내 행동
        sb.appendLine("🎴 ${result.effectDesc}")
        if (result.damageDealt > 0) {
            sb.appendLine("→ 데미지: ${result.damageDealt}")
        }
        if (result.paralysisBlocked) {
            sb.appendLine("⚡ 마비 효과로 스킬 발동 실패!")
        }

        // 독 피해
        if (result.poisonDamage > 0) {
            sb.appendLine("\n☠ 독 피해: -${result.poisonDamage}")
        }

        // 몬스터 공격
        if (result.monsterAttacks.isNotEmpty()) {
            sb.appendLine("\n[ 몬스터 공격 ]")
            for (atk in result.monsterAttacks) {
                if (atk.shieldBlocked) {
                    sb.appendLine("🛡 ${atk.monsterName}의 공격을 방어!")
                } else {
                    sb.append("💥 ${atk.monsterName}: -${atk.damage}")
                    when (atk.statusApplied) {
                        MonsterStatusAttack.POISON    -> sb.appendLine(" + ☠독 부여!")
                        MonsterStatusAttack.PARALYSIS -> sb.appendLine(" + ⚡마비 부여!")
                        MonsterStatusAttack.NONE      -> sb.appendLine()
                    }
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (result.roundCleared) "🎉 라운드 클리어!" else "턴 결과")
            .setMessage(sb.toString().trim())
            .setPositiveButton("확인") { _, _ ->
                when {
                    result.playerDied  -> onPlayerDied()
                    result.roundCleared -> onRoundCleared()
                    else               -> refreshUI()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun onRoundCleared() {
        val hasNext = PveBattleEngine.advanceToNextRound()
        if (hasNext) {
            val s = PveBattleEngine.state
            val roundLabel = if (s.isBossRound) "🔥 보스 라운드!" else "라운드 ${s.roundIndex + 1}"
            Toast.makeText(this, "다음 라운드 → $roundLabel", Toast.LENGTH_SHORT).show()
            refreshUI()
        } else {
            onStageCleared()
        }
    }

    private fun onStageCleared() {
        val stageId = PveBattleEngine.state.stageId
        val stage = PveStageData.stages.first { it.id == stageId }

        // 클리어 카드 기록 & 스테이지 카운트 증가
        DeckManager.getActiveDeck(this)?.let {
            DeckManager.markDeckCardsAsCleared(this, it)
        }
        DeckManager.incrementClearedStage(this)
        PveBattleEngine.resetBattle()

        AlertDialog.Builder(this)
            .setTitle("🏆 스테이지 클리어!")
            .setMessage("${stage.emoji} ${stage.name} 클리어!\n\n총 클리어 스테이지: ${DeckManager.getClearedStageCount(this)}회\n\n사용한 덱 카드들은 다음 스테이지 클리어 전까지 재사용 불가합니다.")
            .setPositiveButton("던전 나가기") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun onPlayerDied() {
        PveBattleEngine.resetBattle()
        AlertDialog.Builder(this)
            .setTitle("💀 전투 패배")
            .setMessage("HP가 0이 되었습니다.\n던전으로 다시 도전하세요!")
            .setPositiveButton("나가기") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun confirmRetreat() {
        AlertDialog.Builder(this)
            .setTitle("던전 포기")
            .setMessage("던전을 포기하시겠습니까? 진행 상황이 초기화됩니다.")
            .setPositiveButton("포기") { _, _ ->
                PveBattleEngine.resetBattle()
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ── 몬스터 어댑터 ─────────────────────────────────────────────────
    private inner class MonsterAdapter : RecyclerView.Adapter<MonsterAdapter.VH>() {
        private var monsters: List<MonsterState> = emptyList()

        fun setMonsters(list: List<MonsterState>) {
            monsters = list
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvEmoji: TextView = v.findViewById(R.id.tv_monster_emoji)
            val tvName: TextView = v.findViewById(R.id.tv_monster_name)
            val pb: ProgressBar = v.findViewById(R.id.pb_monster_hp)
            val tvHp: TextView = v.findViewById(R.id.tv_monster_hp)
            val tvCountdown: TextView = v.findViewById(R.id.tv_attack_countdown)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_monster, parent, false))

        override fun getItemCount() = monsters.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val m = monsters[pos]
            h.tvEmoji.text = m.template.emoji
            h.tvName.text = m.template.name
            h.pb.progress = (m.hpPercent * 100).toInt()
            h.tvHp.text = "${m.hp} / ${m.template.maxHp}"
            h.tvCountdown.text = if (m.isAlive) "⚡ 공격 ${m.attackCountdown}턴 후" else "💀 격파"
            h.itemView.alpha = if (m.isAlive) 1f else 0.35f
        }
    }

    // ── 덱 카드 어댑터 ───────────────────────────────────────────────
    private inner class DungeonCardAdapter(
        private val onClick: (CollectedCardManager.CollectedCard) -> Unit
    ) : RecyclerView.Adapter<DungeonCardAdapter.VH>() {

        private var allCards: List<CollectedCardManager.CollectedCard> = emptyList()
        private var availableIds: Set<Int> = emptySet()

        fun setCards(cards: List<CollectedCardManager.CollectedCard>, available: Set<Int>) {
            allCards = cards
            availableIds = available
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvGrade: TextView = v.findViewById(R.id.tv_card_grade)
            val tvSkillName: TextView = v.findViewById(R.id.tv_card_skill_name)
            val tvEffectIcon: TextView = v.findViewById(R.id.tv_card_effect_icon)
            val tvDamage: TextView = v.findViewById(R.id.tv_card_damage)
            val tvWord: TextView = v.findViewById(R.id.tv_card_word)
            val cooldownOverlay: FrameLayout = v.findViewById(R.id.cooldown_overlay)
            val tvCooldown: TextView = v.findViewById(R.id.tv_cooldown)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_battle_card, parent, false))

        override fun getItemCount() = allCards.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val card = allCards[pos]
            val effect = PveBattleEngine.getCardEffect(card)
            val cd = PveBattleEngine.getCooldown(card.wordId)
            val available = availableIds.contains(card.wordId)

            h.tvGrade.text = card.grade
            h.tvGrade.backgroundTintList = android.content.res.ColorStateList.valueOf(gradeColor(card.grade))
            h.tvSkillName.text = card.skillName
            h.tvEffectIcon.text = effect.icon
            h.tvDamage.text = "${card.damage}"
            h.tvWord.text = card.word

            val onCooldown = cd > 0
            h.cooldownOverlay.visibility = if (onCooldown) View.VISIBLE else View.GONE
            if (onCooldown) h.tvCooldown.text = "쿨타임\n${cd}턴"
            h.itemView.alpha = if (onCooldown) 0.45f else 1.0f

            h.itemView.setOnClickListener { onClick(card) }
        }

        private fun gradeColor(grade: String): Int = when (grade) {
            "금급" -> Color.parseColor("#FFC107")
            "은급" -> Color.parseColor("#9E9E9E")
            else   -> Color.parseColor("#CD7F32")
        }
    }
}
