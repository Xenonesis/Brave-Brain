package com.bravebrain

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class to migrate local SharedPreferences data to Firestore
 */
class FirestoreMigrationHelper(private val context: Context) {
    private val firestoreService = FirestoreService(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    suspend fun migrateAllData(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                migrateAppUsageData()
                migrateAnalyticsData()
                migrateGamificationData()
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("FirestoreMigration", "Migration failed: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    private suspend fun migrateAppUsageData() {
        val prefs = context.getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val blockedPackages = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        
        blockedPackages.forEach { packageName ->
            val appName = getAppName(packageName)
            val usageTime = getAppUsageTime(packageName)
            val dailyLimit = prefs.getLong("${packageName}_limit", 0)
            
            firestoreService.saveAppUsage(
                packageName = packageName,
                appName = appName,
                usageTimeMs = usageTime,
                dailyLimitMs = dailyLimit,
                category = "blocked"
            )
        }
    }
    
    private suspend fun migrateAnalyticsData() {
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val today = dateFormat.format(Date())
        
        val productivityScore = prefs.getInt("productivity_score", 0)
        val blockedAttempts = prefs.getInt("blocked_attempts_$today", 0)
        val challengesCompleted = prefs.getInt("challenges_completed_$today", 0)
        val challengesFailed = prefs.getInt("challenges_failed_$today", 0)
        
        val usagePatterns = mapOf(
            "totalScreenTime" to getTotalScreenTime(),
            "peakUsageHour" to getPeakUsageHour()
        )
        
        firestoreService.saveAnalytics(
            date = today,
            totalScreenTimeMs = getTotalScreenTime(),
            productivityScore = productivityScore,
            blockedAttempts = blockedAttempts,
            challengesCompleted = challengesCompleted,
            challengesFailed = challengesFailed,
            usagePatterns = usagePatterns
        )
    }
    
    private suspend fun migrateGamificationData() {
        val prefs = context.getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)
        
        val points = prefs.getInt("total_points", 0)
        val level = prefs.getInt("current_level", 1)
        val badgesJson = prefs.getString("earned_badges", "[]") ?: "[]"
        val badges = parseBadges(badgesJson)
        
        firestoreService.saveGamificationData(
            points = points,
            level = level,
            badges = badges,
            challenges = emptyMap()
        )
    }
    
    suspend fun checkMigrationStatus(): MigrationStatus {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("migration_status", Context.MODE_PRIVATE)
                val lastMigration = prefs.getLong("last_migration_timestamp", 0)
                val migrationCompleted = prefs.getBoolean("migration_completed", false)
                
                MigrationStatus(
                    isCompleted = migrationCompleted,
                    lastMigrationTimestamp = lastMigration,
                    needsMigration = !migrationCompleted && hasLocalData()
                )
            } catch (e: Exception) {
                MigrationStatus(false, 0, false)
            }
        }
    }
    
    suspend fun markMigrationComplete() {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("migration_status", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("migration_completed", true)
                .putLong("last_migration_timestamp", System.currentTimeMillis())
                .apply()
        }
    }
    
    private fun hasLocalData(): Boolean {
        val blockedAppsPrefs = context.getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val analyticsPrefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        
        return blockedAppsPrefs.all.isNotEmpty() || analyticsPrefs.all.isNotEmpty()
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    private fun getAppUsageTime(packageName: String): Long {
        val usage = UsageUtils.getUsage(context)
        return (usage[packageName] ?: 0).toLong() * 60 * 1000
    }
    
    private fun getTotalScreenTime(): Long {
        val usage = UsageUtils.getUsage(context)
        return usage.values.sum().toLong() * 60 * 1000
    }
    
    private fun getPeakUsageHour(): Int {
        // Simplified implementation
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
    
    private fun parseBadges(json: String): List<String> {
        return try {
            json.trim('[', ']').split(",").map { it.trim('"', ' ') }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    data class MigrationStatus(
        val isCompleted: Boolean,
        val lastMigrationTimestamp: Long,
        val needsMigration: Boolean
    )
}
