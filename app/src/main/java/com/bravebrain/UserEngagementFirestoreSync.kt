package com.bravebrain

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extension class to fetch historical engagement data from Firestore
 */
class UserEngagementFirestoreSync(private val context: Context) {
    private val firestoreService = FirestoreService(context)
    
    suspend fun getHistoricalUsageData(days: Int = 30): List<AppUsageData> {
        return withContext(Dispatchers.IO) {
            try {
                val result = firestoreService.getAppUsageHistory(days)
                result.getOrNull() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("UserEngagementFirestoreSync", "Failed to fetch usage data: ${e.message}")
                emptyList()
            }
        }
    }
    
    suspend fun getHistoricalAnalytics(days: Int = 30): List<AnalyticsData> {
        return withContext(Dispatchers.IO) {
            try {
                val result = firestoreService.getAnalyticsHistory(days)
                result.getOrNull() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("UserEngagementFirestoreSync", "Failed to fetch analytics: ${e.message}")
                emptyList()
            }
        }
    }
    
    suspend fun calculateEngagementTrends(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val analytics = getHistoricalAnalytics(30)
                val trends = mutableMapOf<String, Any>()
                
                if (analytics.isNotEmpty()) {
                    trends["averageProductivityScore"] = analytics.map { it.productivityScore }.average()
                    trends["totalBlockedAttempts"] = analytics.sumOf { it.blockedAttempts }
                    trends["totalChallengesCompleted"] = analytics.sumOf { it.challengesCompleted }
                    trends["averageScreenTime"] = analytics.map { it.totalScreenTimeMs }.average()
                }
                
                trends
            } catch (e: Exception) {
                android.util.Log.e("UserEngagementFirestoreSync", "Failed to calculate trends: ${e.message}")
                emptyMap()
            }
        }
    }
}
