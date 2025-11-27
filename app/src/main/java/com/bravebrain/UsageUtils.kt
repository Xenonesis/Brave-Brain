package com.bravebrain

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
        return appList != null && appList.isNotEmpty()
    }

    /**
     * Gets app usage time in minutes for a specific app TODAY (from midnight)
     */
    fun getAppUsageMinutes(context: Context, packageName: String): Int {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Get today's start time (midnight - 12:00 AM)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        // Current time
        val endTime = System.currentTimeMillis()
        
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )
        
        // Find the usage for the specific package and add any tracked usage from preferences
        val systemUsage = usageStatsList.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
        
        // Add any manually tracked usage from preferences (for testing and incremental tracking)
        val prefs = context.getSharedPreferences("app_usage_tracking", Context.MODE_PRIVATE)
        val trackedSeconds = prefs.getInt(packageName, 0)
        val trackedMillis = trackedSeconds * 1000L
        
        val totalUsageMillis = systemUsage + trackedMillis
        val usageMinutes = (totalUsageMillis / (1000 * 60)).toInt()
        
        // Log for debugging
        android.util.Log.d("UsageUtils", "Usage for $packageName: system=${systemUsage/1000/60}m, tracked=${trackedSeconds/60}m, total=${usageMinutes}m")
        
        return usageMinutes
    }

    /**
     * Resets usage data if needed (e.g., if it's a new day)
     * Uses full date format to handle year changes correctly
     */
    fun resetIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("usage_data", Context.MODE_PRIVATE)
        val trackingPrefs = context.getSharedPreferences("app_usage_tracking", Context.MODE_PRIVATE)
        val lastResetDate = prefs.getString("last_reset_date", "")
        
        // Use full date format (YYYY-MM-DD) to handle year changes
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val currentDate = dateFormat.format(Date())
        
        if (lastResetDate != currentDate) {
            android.util.Log.d("UsageUtils", "New day detected ($lastResetDate -> $currentDate), resetting tracked usage")
            
            // Clear all tracked usage for the new day
            trackingPrefs.edit().clear().apply()
            
            // Update last reset date
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
