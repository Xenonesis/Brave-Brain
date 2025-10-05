package com.bravebrain

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main integration class that connects the notification system with both
 * gamification and analytics features
 */
class GamificationAnalyticsNotificationIntegration(
    private val context: Context,
    private val notificationManager: AdvancedNotificationManager
) {
    companion object {
        private const val TAG = "GAMificationAnalyticsInt"
    }

    private val gamificationIntegration: GamificationNotificationIntegration
    private val analyticsIntegration: AnalyticsNotificationIntegration

    init {
        gamificationIntegration = GamificationNotificationIntegration(context, notificationManager)
        analyticsIntegration = AnalyticsNotificationIntegration(context, notificationManager)
    }

    /**
     * Initialize all integration points and set up periodic triggers
     */
    fun initializeIntegration() {
        Log.d(TAG, "Initializing gamification and analytics notification integration")
        
        // Set up periodic checks for notifications
        setupPeriodicNotificationTriggers()
    }

    /**
     * Set up periodic triggers for various notification types
     */
    private fun setupPeriodicNotificationTriggers() {
        CoroutineScope(Dispatchers.IO).launch {
            // Check for gamification notifications every 30 minutes during active hours
            while (true) {
                try {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    
                    // Only trigger notifications during reasonable hours (8 AM to 10 PM)
                    if (currentHour in 8..22) {
                        gamificationIntegration.checkAndTriggerGamificationNotifications()
                        analyticsIntegration.triggerPersonalizedRecommendationNotification()
                    }
                    
                    // Wait 30 minutes before next check
                    kotlinx.coroutines.delay(30 * 60 * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic notification triggers", e)
                    kotlinx.coroutines.delay(5 * 60 * 1000) // Wait 5 minutes on error
                }
            }
        }
    }

    /**
     * Trigger notification when a user achieves an XP milestone
     */
    fun triggerXPNotification(amount: Int, reason: String) {
        gamificationIntegration.triggerXPNotification(amount, reason)
    }

    /**
     * Trigger notification when a user levels up
     */
    fun triggerLevelUpNotification(newLevel: Int) {
        gamificationIntegration.triggerLevelUpNotification(newLevel)
    }

    /**
     * Trigger notification when a user earns a badge
     */
    fun triggerBadgeNotification(badgeName: String) {
        gamificationIntegration.triggerBadgeNotification(badgeName)
    }

    /**
     * Trigger notification for streak maintenance
     */
    fun triggerStreakNotification(streakType: String, streakCount: Int) {
        gamificationIntegration.triggerStreakNotification(streakType, streakCount)
    }

    /**
     * Trigger daily reminder to maintain streaks
     */
    fun triggerStreakReminderNotification() {
        gamificationIntegration.triggerStreakReminderNotification()
    }

    /**
     * Trigger end of day summary notification
     */
    fun triggerEndOfDaySummaryNotification() {
        gamificationIntegration.triggerEndOfDaySummaryNotification()
    }

    /**
     * Trigger notification based on usage patterns and insights
     */
    fun triggerInsightsNotification(insightType: String = "daily") {
        analyticsIntegration.triggerInsightsNotification(insightType)
    }

    /**
     * Trigger weekly progress report notification
     */
    fun triggerWeeklyReportNotification() {
        analyticsIntegration.triggerWeeklyReportNotification()
    }

    /**
     * Trigger monthly progress report notification
     */
    fun triggerMonthlyReportNotification() {
        analyticsIntegration.triggerMonthlyReportNotification()
    }

    /**
     * Trigger notification for pattern detection
     */
    fun triggerPatternDetectionNotification(patternType: String, details: String) {
        analyticsIntegration.triggerPatternDetectionNotification(patternType, details)
    }

    /**
     * Provide personalized recommendations based on analytics data
     */
    fun triggerPersonalizedRecommendationNotification() {
        analyticsIntegration.triggerPersonalizedRecommendationNotification()
    }

    /**
     * Trigger challenge completion notification
     */
    fun triggerChallengeCompletionNotification(challengeName: String, xpReward: Int = 0) {
        CoroutineScope(Dispatchers.Main).launch {
            val content = if (xpReward > 0) {
                "Congratulations! You've completed the '$challengeName' challenge and earned $xpReward XP!"
            } else {
                "Congratulations! You've completed the '$challengeName' challenge!"
            }

            notificationManager.scheduleNotification(
                type = "positive_reinforcement",
                title = "Challenge Completed! 🏆",
                content = content,
                additionalData = mapOf(
                    "achievement_type" to "challenge",
                    "challenge_name" to challengeName,
                    "xp_reward" to xpReward
                )
            )
        }
    }

    /**
     * Trigger milestone notification for significant achievements
     */
    fun triggerMilestoneNotification(milestoneType: String, milestoneValue: String) {
        val title = when (milestoneType.lowercase()) {
            "screen_time_reduction" -> "Reduction Milestone! 📉"
            "productivity_score" -> "Productivity Milestone! 📈"
            "streak_achievement" -> "Streak Milestone! 🔥"
            "challenge_completion" -> "Challenge Milestone! 🏅"
            else -> "Achievement Milestone! 🎉"
        }

        val content = when (milestoneType.lowercase()) {
            "screen_time_reduction" -> "Fantastic! You've achieved your screen time reduction goal of $milestoneValue. Your mindful habits are paying off!"
            "productivity_score" -> "Impressive! You've reached a productivity score of $milestoneValue. Your focus and discipline are remarkable!"
            "streak_achievement" -> "Outstanding! You've achieved a streak of $milestoneValue. Consistency is key to building healthy habits!"
            "challenge_completion" -> "Incredible! You've completed $milestoneValue challenges. You're truly committed to your digital wellness!"
            else -> "Congratulations! You've reached the milestone: $milestoneValue. Keep up the great work!"
        }

        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.scheduleNotification(
                type = "positive_reinforcement",
                title = title,
                content = content,
                additionalData = mapOf(
                    "achievement_type" to "milestone",
                    "milestone_type" to milestoneType,
                    "milestone_value" to milestoneValue
                )
            )
        }
    }

    /**
     * Register for gamification events (like when XP is awarded)
     */
    fun registerForGamificationEvents() {
        // This would typically involve setting up listeners or observers
        // For now, we'll just log that we're ready to handle events
        Log.d(TAG, "Registered for gamification events")
    }

    /**
     * Register for analytics events (like when patterns are detected)
     */
    fun registerForAnalyticsEvents() {
        // This would typically involve setting up listeners or observers
        // For now, we'll just log that we're ready to handle events
        Log.d(TAG, "Registered for analytics events")
    }

    /**
     * Process analytics data to detect patterns and trigger appropriate notifications
     */
    fun processAnalyticsDataAndTriggerNotifications() {
        // Analyze current analytics data to identify patterns
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val currentProductivityScore = prefs.getInt("productivity_score", 50)
        
        // Check for significant changes in productivity
        val lastProductivityScore = prefs.getInt("last_productivity_score", 50)
        val productivityChange = currentProductivityScore - lastProductivityScore
        
        if (kotlin.math.abs(productivityChange) >= 15) { // Significant change threshold
            val patternType = if (productivityChange > 0) "improvement" else "decline"
            val details = if (productivityChange > 0) {
                "Productivity improved by ${kotlin.math.abs(productivityChange)} points"
            } else {
                "Productivity decreased by ${kotlin.math.abs(productivityChange)} points"
            }
            
            triggerPatternDetectionNotification(patternType, details)
            
            // Update last score
            prefs.edit().putInt("last_productivity_score", currentProductivityScore).apply()
        }
        
        // Check for usage pattern changes
        checkUsagePatternChanges()
    }

    /**
     * Check for usage pattern changes that warrant notifications
     */
    private fun checkUsagePatternChanges() {
        val currentUsage = UsageUtils.getUsage(context)
        
        // Get yesterday's usage for comparison
        val yesterdayUsage = getYesterdayUsage()
        
        // Compare today's usage with yesterday's
        for ((packageName, todayTime) in currentUsage) {
            val yesterdayTime = yesterdayUsage[packageName] ?: 0
            val changePercentage = if (yesterdayTime > 0) {
                ((todayTime - yesterdayTime).toDouble() / yesterdayTime) * 100
            } else if (todayTime > 0 && yesterdayTime == 0) {
                10.0 // New app usage
            } else {
                0.0
            }
            
            // Trigger notification if usage changed significantly (more than 50%)
            if (kotlin.math.abs(changePercentage) > 50) {
                val patternType = if (changePercentage > 0) "usage_spike" else "usage_drop"
                val details = if (changePercentage > 0) {
                    "$packageName usage increased by ${changePercentage.toInt()}% compared to yesterday"
                } else {
                    "$packageName usage decreased by ${kotlin.math.abs(changePercentage.toInt())}% compared to yesterday"
                }
                
                triggerPatternDetectionNotification(patternType, details)
            }
        }
    }

    /**
     * Get usage data from yesterday for comparison
     */
    private fun getYesterdayUsage(): Map<String, Int> {
        val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1) // Yesterday
        val yesterday = dateFormat.format(calendar.time)
        
        // In a real implementation, we would store daily usage data
        // For now, return an empty map
        return emptyMap()
    }

    /**
     * Schedule all daily recurring notifications
     */
    fun scheduleDailyNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Schedule streak reminder for morning
                notificationManager.scheduleRecurringNotification(
                    type = "streak_reminder",
                    interval = 24 * 60 * 1000L, // 24 hours
                    recurrenceType = NotificationScheduler.RecurringNotification.RecurrenceType.DAILY,
                    title = "Daily Streak Reminder",
                    content = "Don't forget to maintain your mindful usage streak today!",
                    priority = NotificationScheduler.ScheduledNotification.Priority.HIGH
                )
                
                // Schedule end-of-day summary
                notificationManager.scheduleRecurringNotification(
                    type = "end_of_day_summary",
                    interval = 24 * 60 * 60 * 1000L, // 24 hours
                    recurrenceType = NotificationScheduler.RecurringNotification.RecurrenceType.DAILY,
                    title = "Daily Progress Summary",
                    content = "Here's your daily summary of mindful usage achievements.",
                    priority = NotificationScheduler.ScheduledNotification.Priority.NORMAL
                )
                
                // Schedule weekly report
                notificationManager.scheduleRecurringNotification(
                    type = "weekly_report",
                    interval = 7 * 24 * 60 * 1000L, // 7 days
                    recurrenceType = NotificationScheduler.RecurringNotification.RecurrenceType.WEEKLY,
                    title = "Weekly Progress Report",
                    content = "Your weekly digital wellness progress report is ready.",
                    priority = NotificationScheduler.ScheduledNotification.Priority.HIGH
                )
                
                Log.d(TAG, "Scheduled daily recurring notifications")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling daily notifications", e)
            }
        }
    }
}