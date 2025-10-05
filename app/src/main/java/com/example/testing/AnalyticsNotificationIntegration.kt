package com.example.testing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.testing.UsageUtils

/**
 * Integration between the notification system and analytics features
 */
class AnalyticsNotificationIntegration(
    private val context: Context,
    private val notificationManager: AdvancedNotificationManager
) {
    companion object {
        private const val TAG = "AnalyticsNotifInt"
    }

    /**
     * Trigger notification based on usage patterns and insights
     */
    fun triggerInsightsNotification(insightType: String = "daily") {
        CoroutineScope(Dispatchers.Main).launch {
            val analyticsService = AnalyticsService()
            val insights = getInsightsFromAnalytics()
            
            if (insights.isNotEmpty()) {
                val content = insights.joinToString("\n")
                
                notificationManager.scheduleNotification(
                    type = "insights_analytics",
                    title = when (insightType) {
                        "weekly" -> "Weekly Insights Ready"
                        "monthly" -> "Monthly Insights Ready"
                        else -> "Daily Insights Ready"
                    },
                    content = content,
                    additionalData = mapOf(
                        "insight_type" to insightType,
                        "insights" to insights
                    )
                )
            }
        }
    }

    /**
     * Get insights from analytics data
     */
    private fun getInsightsFromAnalytics(): List<String> {
        val insights = mutableListOf<String>()
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        
        // Get current productivity score
        val currentScore = prefs.getInt("productivity_score", 50)
        val averageScore = getAverageProductivityScoreThisWeek()
        
        when {
            currentScore > averageScore + 10 -> insights.add("Great job! Your productivity is above average today.")
            currentScore < averageScore - 10 -> insights.add("Your productivity is below average. Consider taking a break from distracting apps.")
            else -> insights.add("Your productivity is steady. Keep up the good work!")
        }
        
        // Add other insights based on available data
        val weeklyStats = getWeeklyStats()
        if (weeklyStats.isNotEmpty()) {
            weeklyStats.forEach { stat ->
                insights.add(stat)
            }
        }
        
        return insights
    }

    /**
     * Trigger weekly progress report notification
     */
    fun triggerWeeklyReportNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            val weeklyStats = getWeeklyStats()
            val content = if (weeklyStats.isNotEmpty()) {
                "Weekly Report: ${weeklyStats.joinToString(". ")}"
            } else {
                "Weekly Report: Here's your weekly usage summary and progress toward goals."
            }
            
            notificationManager.scheduleNotification(
                type = "insights_analytics",
                title = "Weekly Progress Report",
                content = content,
                additionalData = mapOf(
                    "report_type" to "weekly",
                    "stats" to weeklyStats
                )
            )
        }
    }

    /**
     * Trigger monthly progress report notification
     */
    fun triggerMonthlyReportNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            val monthlyStats = getMonthlyStats()
            val content = if (monthlyStats.isNotEmpty()) {
                "Monthly Report: ${monthlyStats.joinToString(". ")}"
            } else {
                "Monthly Report: Here's your monthly usage summary and progress toward goals."
            }
            
            notificationManager.scheduleNotification(
                type = "insights_analytics",
                title = "Monthly Progress Report",
                content = content,
                additionalData = mapOf(
                    "report_type" to "monthly",
                    "stats" to monthlyStats
                )
            )
        }
    }

    /**
     * Trigger notification for pattern detection
     */
    fun triggerPatternDetectionNotification(patternType: String, details: String) {
        val title = when (patternType) {
            "usage_spike" -> "Usage Pattern Detected"
            "time_waste" -> "Time Waste Pattern"
            "improvement" -> "Positive Pattern Detected"
            "productivity" -> "Productivity Pattern"
            else -> "Usage Pattern Detected"
        }
        
        val content = when (patternType) {
            "usage_spike" -> "We noticed a significant increase in your app usage. Consider taking a mindful break."
            "time_waste" -> "Pattern detected: You're spending more time on potentially distracting apps. Maybe try a focus session?"
            "improvement" -> "Great progress! We're seeing positive changes in your usage patterns."
            "productivity" -> "You're in a productive flow! Keep up the good work with your mindful usage."
            else -> "We've detected a pattern in your usage. Here's a personalized insight: $details"
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.scheduleNotification(
                type = "insights_analytics",
                title = title,
                content = content,
                additionalData = mapOf(
                    "pattern_type" to patternType,
                    "pattern_details" to details
                )
            )
        }
    }

    /**
     * Provide personalized recommendations based on analytics data
     */
    fun triggerPersonalizedRecommendationNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            val recommendations = generatePersonalizedRecommendations()
            
            if (recommendations.isNotEmpty()) {
                val content = recommendations.joinToString("\n• ", prefix = "• ")
                
                notificationManager.scheduleNotification(
                    type = "contextual_suggestion",
                    title = "Personalized Recommendations",
                    content = content,
                    additionalData = mapOf(
                        "recommendation_type" to "personalized",
                        "recommendations" to recommendations
                    )
                )
            }
        }
    }

    /**
     * Generate personalized recommendations based on usage analytics
     */
    private fun generatePersonalizedRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val usageData = UsageUtils.getUsage(context)
        
        // Find most used app
        val mostUsedApp = usageData.maxByOrNull { it.value }?.key ?: ""
        val mostUsedTime = usageData.maxByOrNull { it.value }?.value ?: 0
        
        if (mostUsedApp.isNotEmpty() && mostUsedTime > 120) { // More than 2 hours
            recommendations.add("You spent $mostUsedTime minutes on $mostUsedApp today. Consider setting a time limit for tomorrow.")
        }
        
        // Check for time of day patterns
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour in 22..23) { // Late night
            recommendations.add("It's getting late. Consider winding down from screen time for better sleep.")
        } else if (currentHour in 6..9 && mostUsedTime > 60) { // Morning with high usage
            recommendations.add("Starting your day with significant screen time? Maybe try a mindful morning routine instead.")
        }
        
        // Check productivity score
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val productivityScore = prefs.getInt("productivity_score", 50)
        
        if (productivityScore < 40) {
            recommendations.add("Your productivity score is low. Consider taking a break from screens and engaging in a different activity.")
        } else if (productivityScore > 80) {
            recommendations.add("Great productivity score! Keep up the mindful usage habits.")
        }
        
        // Add recommendation if user hasn't opened the app in a while
        val lastOpenTime = prefs.getLong("last_app_open_time", 0)
        val currentTime = System.currentTimeMillis()
        val daysSinceLastOpen = (currentTime - lastOpenTime) / (1000 * 60 * 24)
        
        if (daysSinceLastOpen >= 2) {
            recommendations.add("We haven't seen you in a while! Check in on your digital wellness goals.")
        }
        
        return recommendations
    }

    /**
     * Get weekly stats for report generation
     */
    private fun getWeeklyStats(): List<String> {
        val stats = mutableListOf<String>()
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        
        // Get a week's worth of daily stats
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        
        for (i in 0..6) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val date = dateFormat.format(calendar.time)
            val dailyStat = prefs.getString("daily_stats-$date", "")
            
            if (!dailyStat.isNullOrEmpty()) {
                // Parse the daily stat data
                val parts = dailyStat.split(",")
                if (parts.size >= 5) {
                    val totalScreenTime = parts[1].toLongOrNull() ?: 0
                    val productivityScore = parts[6].toIntOrNull() ?: 0
                    val blockedAttempts = parts[7].toIntOrNull() ?: 0
                    
                    if (totalScreenTime > 0) {
                        stats.add("On $date: Screen time ${totalScreenTime / (1000 * 60)}h, Productivity $productivityScore%, Blocked attempts $blockedAttempts")
                    }
                }
            }
        }
        
        return stats
    }

    /**
     * Get monthly stats for report generation
     */
    private fun getMonthlyStats(): List<String> {
        val stats = mutableListOf<String>()
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        
        // For now, just return a placeholder - in a real implementation, we would aggregate monthly data
        stats.add("Monthly usage trends show your commitment to mindful digital habits.")
        stats.add("Your productivity scores have improved by X% this month.")
        stats.add("You've maintained Y day streaks multiple times this month.")
        
        return stats
    }

    /**
     * Get average productivity score for the week
     */
    private fun getAverageProductivityScoreThisWeek(): Int {
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val weeklyStats = getWeeklyStats()
        
        if (weeklyStats.isEmpty()) return 50
        
        var totalScore = 0
        var count = 0
        
        weeklyStats.forEach { stat ->
            // Extract productivity score from the stat string
            val scoreRegex = Regex("Productivity (\\d+)%")
            val match = scoreRegex.find(stat)
            if (match != null) {
                val score = match.groupValues[1].toIntOrNull()
                if (score != null) {
                    totalScore += score
                    count++
                }
            }
        }
        
        return if (count > 0) totalScore / count else 50
    }
}