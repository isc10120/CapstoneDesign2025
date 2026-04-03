package com.example.zamgavocafront

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager

object DndAppsManager {
    private const val PREFS_NAME = "dnd_settings"
    private const val KEY_DND_PACKAGES = "dnd_packages"

    fun getDndPackages(context: Context): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_DND_PACKAGES, emptySet()) ?: emptySet()

    fun setDndPackages(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_DND_PACKAGES, packages)
            .apply()
    }

    /** 현재 포그라운드 앱이 방해금지 목록에 있으면 true */
    fun isForegroundAppBlocked(context: Context): Boolean {
        val dndPackages = getDndPackages(context)
        if (dndPackages.isEmpty()) return false
        val foreground = getForegroundApp(context) ?: return false
        return foreground in dndPackages
    }

    fun getForegroundApp(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10_000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 2000, now)
        return stats != null && stats.isNotEmpty()
    }

    data class AppInfo(val packageName: String, val appName: String)

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.appName }
    }
}
