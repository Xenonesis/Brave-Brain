package com.bravebrain

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension class to sync AnalyticsService data with Firestore
 */
class AnalyticsFirestoreSync(private val context: Context) {
    private val firestoreService = FirestoreService(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    fun syncDailyAnalytics(
        totalScreenTimeMs: Long,
        productivityScore: Int,
        blockedAttempts: Int,
        challengesCompleted: Int,
        challengesFailed: Int,
        usagePatterns: Map<String, Any>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = dateFormat.format(Date())
                firestoreService.saveAnalytics(
                    date = today,
                    totalScreenTimeMs = totalScreenTimeMs,
                    productivityScore = productivityScore,
                    blockedAttempts = blockedAttempts,
                    challengesCompleted = challengesCompleted,
                    challengesFailed = challengesFailed,
                    usagePatterns = usagePatterns
                )
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsFirestoreSync", "Failed to sync analytics: ${e.message}")
            }
        }
    }
    
    fun syncAppUsage(packageName: String, appName: String, usageTimeMs: Long, dailyLimitMs: Long, category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestoreService.saveAppUsage(packageName, appName, usageTimeMs, dailyLimitMs, category)
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsFirestoreSync", "Failed to sync app usage: ${e.message}")
            }
        }
    }
    
    suspend fun getHistoricalAnalytics(days: Int = 30): List<AnalyticsData> {
        return try {
            val result = firestoreService.getAnalyticsHistory(days)
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsFirestoreSync", "Failed to fetch analytics: ${e.message}")
            emptyList()
        }
    }
}
