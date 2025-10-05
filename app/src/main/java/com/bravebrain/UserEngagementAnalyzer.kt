package com.bravebrain

import android.content.Context
import android.app.usage.UsageStatsManager
import androidx.core.app.NotificationManagerCompat
import java.util.*

/**
 * UserEngagementAnalyzer implements user engagement analysis features
 * including response rate tracking, optimal timing identification, 
 * and content preference learning
 */
class UserEngagementAnalyzer(private val context: Context) {
    
    companion object {
        private const val ENGAGEMENT_PREFS = "user_engagement_prefs"
        private const val NOTIFICATION_RESPONSES = "notification_responses"
        private const val CONTENT_PREFERENCES = "content_preferences"
        private const val OPTIMAL_TIMING = "optimal_timing"
        private const val ENGAGEMENT_HISTORY = "engagement_history"
    }
    
    private val prefs = context.getSharedPreferences(ENGAGEMENT_PREFS, Context.MODE_PRIVATE)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val notificationManager = NotificationManagerCompat.from(context)
    
    data class EngagementMetrics(
        val responseRate: Float,
        val optimalTiming: List<Int>,
        val contentPreferences: Map<String, Float>,
        val engagementTrend: EngagementTrend,
        val notificationFatigueScore: Float,
        val preferredContentTypes: List<String>
    )
    
    enum class EngagementTrend {
        INCREASING, DECREASING, STABLE
    }
    
    /**
     * Tracks notification response rate
     */
    fun trackNotificationResponse(notificationId: Int, wasOpened: Boolean, timestamp: Long = System.currentTimeMillis()) {
        val responses = getNotificationResponses()
        responses[notificationId] = Pair(wasOpened, timestamp)
        saveNotificationResponses(responses)
    }
    
    private fun getNotificationResponses(): MutableMap<Int, Pair<Boolean, Long>> {
        val responses = mutableMapOf<Int, Pair<Boolean, Long>>()
        val responseString = prefs.getString(NOTIFICATION_RESPONSES, "") ?: ""
        
        if (responseString.isNotEmpty()) {
            responseString.split("|").forEach { entry ->
                val parts = entry.split(",")
                if (parts.size == 3) {
                    try {
                        val id = parts[0].toInt()
                        val opened = parts[1].toBoolean()
                        val time = parts[2].toLong()
                        responses[id] = Pair(opened, time)
                    } catch (e: NumberFormatException) {
                        // Skip invalid entries
                    }
                }
            }
        }
        
        return responses
    }
    
    private fun saveNotificationResponses(responses: Map<Int, Pair<Boolean, Long>>) {
        val responseString = responses.entries.joinToString("|") { 
            "${it.key},${it.value.first},${it.value.second}" 
        }
        prefs.edit().putString(NOTIFICATION_RESPONSES, responseString).apply()
    }
    
    /**
     * Calculates the overall notification response rate
     */
    fun getResponseRate(): Float {
        val responses = getNotificationResponses()
        if (responses.isEmpty()) return 0f
        
        val openedCount = responses.count { it.value.first }
        return openedCount.toFloat() / responses.size
    }
    
    /**
     * Identifies optimal notification timing based on user behavior
     */
    fun identifyOptimalTiming(): List<Int> {
        val responses = getNotificationResponses()
        if (responses.isEmpty()) return listOf(10, 12, 15, 19) // Default optimal times
        
        // Group responses by hour
        val responsesByHour = mutableMapOf<Int, MutableList<Boolean>>()
        
        for ((id, response) in responses) {
            val hour = Calendar.getInstance().apply { timeInMillis = response.second }.get(Calendar.HOUR_OF_DAY)
            if (!responsesByHour.containsKey(hour)) {
                responsesByHour[hour] = mutableListOf()
            }
            responsesByHour[hour]?.add(response.first)
        }
        
        // Calculate response rate by hour
        val hourRates = mutableMapOf<Int, Float>()
        for ((hour, hourResponses) in responsesByHour) {
            val openedCount = hourResponses.count { it }
            hourRates[hour] = openedCount.toFloat() / hourResponses.size
        }
        
        // Return top 4 hours with highest response rates
        return hourRates.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }
            .sorted()
    }
    
    /**
     * Learns content preferences based on user interactions
     */
    fun learnContentPreferences(notificationType: String, wasEngaged: Boolean) {
        val preferences = getContentPreferences()
        val currentScore = preferences.getOrDefault(notificationType, 0.5f) // Default to neutral
        
        // Adjust score based on engagement (0.0 to 1.0 scale)
        val adjustment = if (wasEngaged) 0.1f else -0.05f
        val newScore = (currentScore + adjustment).coerceIn(0f, 1f)
        
        preferences[notificationType] = newScore
        saveContentPreferences(preferences)
    }
    
    private fun getContentPreferences(): MutableMap<String, Float> {
        val preferences = mutableMapOf<String, Float>()
        val prefString = prefs.getString(CONTENT_PREFERENCES, "") ?: ""
        
        if (prefString.isNotEmpty()) {
            prefString.split("|").forEach { entry ->
                val parts = entry.split(",")
                if (parts.size == 2) {
                    try {
                        val type = parts[0]
                        val score = parts[1].toFloat()
                        preferences[type] = score
                    } catch (e: NumberFormatException) {
                        // Skip invalid entries
                    }
                }
            }
        }
        
        return preferences
    }
    
    private fun saveContentPreferences(preferences: Map<String, Float>) {
        val prefString = preferences.entries.joinToString("|") { 
            "${it.key},${it.value}" 
        }
        prefs.edit().putString(CONTENT_PREFERENCES, prefString).apply()
    }
    
    /**
     * Gets content preferences for different notification types
     */
    fun getContentPreferencesByType(): Map<String, Float> {
        return getContentPreferences().toMap()
    }
    
    /**
     * Analyzes user engagement patterns
     */
    fun analyzeEngagement(): EngagementMetrics {
        val responseRate = getResponseRate()
        val optimalTiming = identifyOptimalTiming()
        val contentPreferences = getContentPreferences()
        val engagementTrend = calculateEngagementTrend()
        val notificationFatigueScore = calculateNotificationFatigueScore()
        val preferredContentTypes = getPreferredContentTypes(contentPreferences)
        
        return EngagementMetrics(
            responseRate = responseRate,
            optimalTiming = optimalTiming,
            contentPreferences = contentPreferences,
            engagementTrend = engagementTrend,
            notificationFatigueScore = notificationFatigueScore,
            preferredContentTypes = preferredContentTypes
        )
    }
    
    private fun calculateEngagementTrend(): EngagementTrend {
        val responses = getNotificationResponses()
        if (responses.size < 10) return EngagementTrend.STABLE // Not enough data
        
        // Get the most recent responses (last 20)
        val recentResponses = responses.values
            .sortedBy { it.second }
            .takeLast(20)
        
        if (recentResponses.size < 10) return EngagementTrend.STABLE // Still not enough data
        
        // Split into first and second half
        val midPoint = recentResponses.size / 2
        val firstHalf = recentResponses.take(midPoint).count { it.first }
        val secondHalf = recentResponses.takeLast(recentResponses.size - midPoint).count { it.first }
        
        val firstHalfRate = firstHalf.toFloat() / midPoint
        val secondHalfRate = secondHalf.toFloat() / (recentResponses.size - midPoint)
        
        return when {
            secondHalfRate > firstHalfRate * 1.2 -> EngagementTrend.INCREASING
            secondHalfRate < firstHalfRate * 0.8 -> EngagementTrend.DECREASING
            else -> EngagementTrend.STABLE
        }
    }
    
    private fun calculateNotificationFatigueScore(): Float {
        val responses = getNotificationResponses()
        if (responses.size < 5) return 0.0f // No fatigue if not many notifications
        
        // Look at recent response patterns (last 24 hours)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -24)
        val timeThreshold = calendar.timeInMillis
        
        val recentResponses = responses.values.filter { it.second > timeThreshold }
        
        if (recentResponses.isEmpty()) return 0.0f
        
        // Calculate fatigue as the inverse of response rate in recent period
        val recentResponseRate = recentResponses.count { it.first }.toFloat() / recentResponses.size
        val fatigueScore = 1.0f - recentResponseRate // Higher non-response = higher fatigue
        
        // Also consider notification density
        val notificationDensity = recentResponses.size.toFloat() / 24 // Per hour
        val densityFactor = minOf(notificationDensity / 5.0f, 1.0f) // Cap at 1.0 for >5 per hour
        
        return (fatigueScore * 0.7f + densityFactor * 0.3f).coerceIn(0f, 1f)
    }
    
    private fun getPreferredContentTypes(preferences: Map<String, Float>): List<String> {
        return preferences.entries
            .sortedByDescending { it.value }
            .take(5) // Top 5 preferred types
            .map { it.key }
    }
    
    /**
     * Predicts the best time to send a notification based on user patterns
     */
    fun predictBestNotificationTime(notificationType: String): List<Int> {
        val optimalTiming = identifyOptimalTiming()
        val preferences = getContentPreferences()
        val userContext = ContextAnalyzer(context).analyzeContext()
        
        // Filter out times that conflict with user context
        val filteredTimes = optimalTiming.filter { hour ->
            // Don't send during sleep time
            val isSleepTime = if (userContext.timeOfDay == ContextAnalyzer.TimeOfDay.NIGHT) {
                hour >= 22 || hour < 7
            } else {
                false
            }
            
            // Don't send during focus time
            val isFocusTime = hour in 22..6 // Simplified focus time check
            
            !isSleepTime && !isFocusTime
        }
        
        // If the notification type has low preference, adjust timing to more active periods
        val typePreference = preferences.getOrDefault(notificationType, 0.5f)
        if (typePreference < 0.3f) {
            // For low-preference content, use only peak engagement hours
            return filteredTimes.filter { it in 10..14 || it in 18..20 }
        }
        
        return filteredTimes
    }
    
    /**
     * Calculates personalization score for a notification
     */
    fun calculatePersonalizationScore(notificationType: String, hour: Int): Float {
        val preferences = getContentPreferences()
        val typePreference = preferences.getOrDefault(notificationType, 0.5f)
        
        val optimalTiming = identifyOptimalTiming()
        val timeMatch = if (hour in optimalTiming) 1.0f else 0.5f
        
        // Combine type preference and timing match
        return (typePreference * 0.6f + timeMatch * 0.4f).coerceIn(0f, 1f)
    }
    
    /**
     * Gets notification effectiveness by time of day
     */
    fun getEffectivenessByTime(): Map<Int, Float> {
        val responses = getNotificationResponses()
        val effectiveness = mutableMapOf<Int, Float>()
        
        if (responses.isEmpty()) return effectiveness
        
        // Group responses by hour
        val responsesByHour = mutableMapOf<Int, MutableList<Boolean>>()
        
        for ((id, response) in responses) {
            val hour = Calendar.getInstance().apply { timeInMillis = response.second }.get(Calendar.HOUR_OF_DAY)
            if (!responsesByHour.containsKey(hour)) {
                responsesByHour[hour] = mutableListOf()
            }
            responsesByHour[hour]?.add(response.first)
        }
        
        // Calculate effectiveness for each hour
        for ((hour, hourResponses) in responsesByHour) {
            val openedCount = hourResponses.count { it }
            effectiveness[hour] = openedCount.toFloat() / hourResponses.size
        }
        
        return effectiveness
    }
    
    /**
     * Resets engagement tracking (for testing purposes)
     */
    fun resetEngagementTracking() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Gets engagement history for analytics
     */
    fun getEngagementHistory(): List<EngagementRecord> {
        val responses = getNotificationResponses()
        val records = mutableListOf<EngagementRecord>()
        
        for ((id, response) in responses) {
            val hour = Calendar.getInstance().apply { timeInMillis = response.second }.get(Calendar.HOUR_OF_DAY)
            records.add(EngagementRecord(
                notificationId = id,
                wasOpened = response.first,
                timestamp = response.second,
                hourOfDay = hour
            ))
        }
        
        return records
    }
    
    data class EngagementRecord(
        val notificationId: Int,
        val wasOpened: Boolean,
        val timestamp: Long,
        val hourOfDay: Int
    )
}