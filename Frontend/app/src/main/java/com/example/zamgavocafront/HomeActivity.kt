package com.example.zamgavocafront

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.zamgavocafront.fragment.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var bottomNav: BottomNavigationView

    private val tabTitles = mapOf(
        R.id.nav_today to "오늘의 단어",
        R.id.nav_leaderboard to "리더보드",
        R.id.nav_pvp to "PVP 배틀",
        R.id.nav_pve to "PVE",
        R.id.nav_collection to "수집된 카드"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvTitle = findViewById(R.id.tv_title)
        bottomNav = findViewById(R.id.bottom_nav)

        // 설정 버튼
        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 하단 탭 선택
        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_today -> TodayWordFragment()
                R.id.nav_leaderboard -> LeaderboardFragment()
                R.id.nav_pvp -> PvpFragment()
                R.id.nav_pve -> PveFragment()
                R.id.nav_collection -> CollectionFragment()
                else -> return@setOnItemSelectedListener false
            }
            tvTitle.text = tabTitles[item.itemId] ?: ""
            replaceFragment(fragment)
            true
        }

        // 최초 진입: 오늘의 단어 탭
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_today
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
