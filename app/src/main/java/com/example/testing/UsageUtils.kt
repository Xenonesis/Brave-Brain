package com.example.testing

import android.content.Context
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStats
import java.util.*

/**
 * UsageUtils provides utility functions for getting and processing usage data
 */
object UsageUtils {
    /**
     * Gets usage statistics for all apps in the last 24 hours
     */
    fun getUsage(context: Context): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Last 24 hours
        val startTime = calendar.timeInMillis

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val usageMap = mutableMapOf<String, Long>()

        for (usageStats in usageStatsList) {
            usageMap[usageStats.packageName] = usageStats.totalTimeInForeground
        }

        return usageMap
    }

    /**
     * Gets usage statistics for a specific app in the last 24 hours
     */
    fun getAppUsage(context: Context, packageName: String): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Last 24 hours
        val startTime = calendar.timeInMillis

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return usageStatsList.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    /**
     * Gets the app with the highest usage in the last 24 hours
     */
    fun getTopApp(context: Context): Pair<String, Long>? {
        val usageMap = getUsage(context)
        if (usageMap.isEmpty()) return null

        val topApp = usageMap.maxByOrNull { it.value }
        return if (topApp != null) Pair(topApp.key, topApp.value) else null
    }

    /**
     * Gets total usage time across all apps in the last 24 hours
     */
    fun getTotalUsage(context: Context): Long {
        val usageMap = getUsage(context)
        return usageMap.values.sum()
    }

    /**
     * Checks if the app has usage access permission
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val appList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 10, // Last 10 seconds
            System.currentTimeMillis()
        )
        return appList != null
    }

    /**
     * Gets app usage time in minutes for a specific app
     */
    fun getAppUsageMinutes(context: Context, packageName: String): Int {
        val usageTime = getAppUsage(context, packageName)
        return (usageTime / (1000 * 60)).toInt() // Convert milliseconds to minutes
    }

    /**
     * Resets usage data if needed (e.g., if it's a new day)
     */
    fun resetIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("usage_data", Context.MODE_PRIVATE)
        val lastResetDate = prefs.getString("last_reset_date", "")
        val currentDate = Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toString()
        
        if (lastResetDate != currentDate) {
            // Reset any daily counters if needed
            prefs.edit().putString("last_reset_date", currentDate).apply()
        }
    }

    /**
     * Increments usage time for a specific app by the given seconds
     */
    fun incrementUsageSeconds(context: Context, packageName: String, seconds: Int) {
        // This is a simplified implementation - in reality you'd need to store this data
        // This function would need to integrate with the usage tracking system
        val prefs = context.getSharedPreferences("app_usage_tracking", Context.MODE_PRIVATE)
        val currentSeconds = prefs.getInt(packageName, 0)
        prefs.edit().putInt(packageName, currentSeconds + seconds).apply()
    }
}
