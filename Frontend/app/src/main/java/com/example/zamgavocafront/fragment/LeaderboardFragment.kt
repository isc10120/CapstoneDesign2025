package com.example.zamgavocafront.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.RankingEntry
import kotlinx.coroutines.launch

class LeaderboardFragment : Fragment() {

    private lateinit var rvRanking: RecyclerView
    private lateinit var tvValueHeader: TextView
    private lateinit var btnTabExp: Button
    private lateinit var btnTabSkills: Button
    private lateinit var tvMyRank: TextView
    private lateinit var tvMyNickname: TextView
    private lateinit var tvMyLevel: TextView
    private lateinit var tvMyValue: TextView

    private val adapter = RankingAdapter()
    private var currentTab = Tab.EXP

    enum class Tab { EXP, SKILLS }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_leaderboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvRanking = view.findViewById(R.id.rv_ranking)
        tvValueHeader = view.findViewById(R.id.tv_value_header)
        btnTabExp = view.findViewById(R.id.btn_tab_exp)
        btnTabSkills = view.findViewById(R.id.btn_tab_skills)
        tvMyRank = view.findViewById(R.id.tv_my_rank)
        tvMyNickname = view.findViewById(R.id.tv_my_nickname)
        tvMyLevel = view.findViewById(R.id.tv_my_level)
        tvMyValue = view.findViewById(R.id.tv_my_value)

        rvRanking.layoutManager = LinearLayoutManager(requireContext())
        rvRanking.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        rvRanking.adapter = adapter

        btnTabExp.setOnClickListener { switchTab(Tab.EXP) }
        btnTabSkills.setOnClickListener { switchTab(Tab.SKILLS) }

        loadRanking(Tab.EXP)
    }

    override fun onResume() {
        super.onResume()
        loadRanking(currentTab)
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        val navyColor = resources.getColor(R.color.main_navy, null)
        val grayColor = android.graphics.Color.parseColor("#CCCCCC")
        btnTabExp.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (tab == Tab.EXP) navyColor else grayColor
        )
        btnTabSkills.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (tab == Tab.SKILLS) navyColor else grayColor
        )
        tvValueHeader.text = if (tab == Tab.EXP) "경험치" else "스킬 수"
        loadRanking(tab)
    }

    private fun loadRanking(tab: Tab) {
        lifecycleScope.launch {
            try {
                val resp = if (tab == Tab.EXP) {
                    ApiClient.api.getExpRanking()
                } else {
                    ApiClient.api.getSkillsRanking()
                }
                if (resp.success && resp.data != null) {
                    adapter.submitList(resp.data.entries)
                    val my = resp.data.myEntry
                    tvMyRank.text = my.rank.toString()
                    tvMyNickname.text = my.nickname
                    tvMyLevel.text = "Lv.${my.level}"
                    tvMyValue.text = my.value.toString()
                }
            } catch (_: Exception) {}
        }
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────

    class RankingAdapter : RecyclerView.Adapter<RankingAdapter.VH>() {

        private val items = mutableListOf<RankingEntry>()

        fun submitList(list: List<RankingEntry>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ranking, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvRank: TextView = view.findViewById(R.id.tv_rank)
            private val tvNickname: TextView = view.findViewById(R.id.tv_nickname)
            private val tvLevel: TextView = view.findViewById(R.id.tv_level)
            private val tvValue: TextView = view.findViewById(R.id.tv_value)

            fun bind(entry: RankingEntry) {
                tvRank.text = when (entry.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> entry.rank.toString()
                }
                tvNickname.text = entry.nickname
                tvLevel.text = "Lv.${entry.level}"
                tvValue.text = entry.value.toString()
            }
        }
    }
}
