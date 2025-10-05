package com.bravebrain

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DataSyncManager(private val context: Context) {
    private val firestoreService = FirestoreService(context)
    private val authManager = FirebaseAuthManager(context)
    
    fun syncAllData() {
        if (!authManager.isSignedIn()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            syncAppSettings()
            syncAnalytics()
            syncGamification()
        }
    }
    
    private suspend fun syncAppSettings() {
        val prefs = context.getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        val timeLimits = prefs.getString("time_limits", "") ?: ""
        
        blockedApps.forEach { packageName ->
            val limit = extractTimeLimit(timeLimits, packageName)
            firestoreService.saveAppUsage(
                packageName = packageName,
                appName = packageName,
                usageTimeMs = UsageUtils.getAppUsage(context, packageName),
                dailyLimitMs = limit,
                category = "blocked"
            )
        }
    }
    
    private suspend fun syncAnalytics() {
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        val totalScreenTime = prefs.getLong("total_screen_time_$today", 0L)
        val productivityScore = prefs.getInt("productivity_score_$today", 0)
        val blockedAttempts = prefs.getInt("blocked_attempts_$today", 0)
        val challengesCompleted = prefs.getInt("challenges_completed_$today", 0)
        val challengesFailed = prefs.getInt("challenges_failed_$today", 0)
        
        firestoreService.saveAnalytics(
            date = today,
            totalScreenTimeMs = totalScreenTime,
            productivityScore = productivityScore,
            blockedAttempts = blockedAttempts,
            challengesCompleted = challengesCompleted,
            challengesFailed = challengesFailed,
            usagePatterns = emptyMap()
        )
    }
    
    private suspend fun syncGamification() {
        val prefs = context.getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)
        val points = prefs.getInt("total_points", 0)
        val level = prefs.getInt("current_level", 1)
        val badges = prefs.getStringSet("earned_badges", emptySet())?.toList() ?: emptyList()
        
        firestoreService.saveGamificationData(
            points = points,
            level = level,
            badges = badges,
            challenges = emptyMap()
        )
    }
    
    private fun extractTimeLimit(timeLimits: String, packageName: String): Long {
        timeLimits.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2 && parts[0] == packageName) {
                return parts[1].toLongOrNull() ?: 0L
            }
        }
        return 0L
    }
}
