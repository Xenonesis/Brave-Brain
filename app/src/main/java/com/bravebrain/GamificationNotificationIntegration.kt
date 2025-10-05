package com.bravebrain

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Integration between the notification system and gamification features
 */
class GamificationNotificationIntegration(
    private val context: Context,
    private val notificationManager: AdvancedNotificationManager
) {
    companion object {
        private const val TAG = "GamificationNotifInt"
    }

    /**
     * Trigger notification when a user achieves an XP milestone
     */
    fun triggerXPNotification(amount: Int, reason: String) {
        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.scheduleNotification(
                type = "positive_reinforcement",
                title = "XP Earned!",
                content = "Congratulations! You earned $amount XP for: $reason",
                additionalData = mapOf(
                    "achievement_type" to "xp_milestone",
                    "xp_earned" to amount,
                    "reason" to reason
                )
            )
        }
    }

    /**
     * Trigger notification when a user levels up
     */
    fun triggerLevelUpNotification(newLevel: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.scheduleNotification(
                type = "positive_reinforcement",
                title = "🎉 Level Up!",
                content = "Amazing! You've reached Level $newLevel! Your dedication to mindful usage is paying off.",
                additionalData = mapOf(
                    "achievement_type" to "level_up",
                    "new_level" to newLevel
                )
            )
        }
    }

    /**
     * Trigger notification when a user earns a badge
     */
    fun triggerBadgeNotification(badgeName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.scheduleNotification(
                type = "gamification_update",
                title = "🏆 Badge Unlocked!",
                content = "Congratulations! You've earned the '$badgeName' badge!",
                additionalData = mapOf(
                    "achievement_type" to "badge",
                    "badge_name" to badgeName
                )
            )
        }
    }

    /**
     * Trigger notification for streak maintenance
     */
    fun triggerStreakNotification(streakType: String, streakCount: Int) {
        val content = when (streakType) {
            "daily_streak" -> "Great job! You've maintained your daily mindful usage streak for $streakCount days!"
            "challenge_streak" -> "Incredible! You've completed $streakCount challenges in a row!"
            "productivity_streak" -> "Fantastic! You've maintained your productivity streak for $streakCount days!"
            else -> "Keep it up! Your $streakType streak is at $streakCount days!"
        }

        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.scheduleNotification(
                type = "positive_reinforcement",
                title = "🔥 Streak Maintained!",
                content = content,
                additionalData = mapOf(
                    "achievement_type" to "streak",
                    "streak_type" to streakType,
                    "streak_count" to streakCount
                )
            )
        }
    }

    /**
     * Trigger daily reminder to maintain streaks
     */
    fun triggerStreakReminderNotification() {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        val dailyStreak = prefs.getInt("daily_streak", 0)
        
        val content = if (dailyStreak > 0) {
            "Don't break your $dailyStreak day streak! Continue your mindful usage journey today."
        } else {
            "Start your mindful usage journey today. Build healthy digital habits one day at a time."
        }

        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.scheduleNotification(
                type = "proactive_warning",
                title = "Daily Streak Reminder",
                content = content,
                priority = NotificationScheduler.ScheduledNotification.Priority.HIGH
            )
        }
    }

    /**
     * Trigger end of day summary notification
     */
    fun triggerEndOfDaySummaryNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
            val currentLevel = prefs.getInt("user_level", 1)
            val currentXP = prefs.getInt("user_xp", 0)
            val dailyStreak = prefs.getInt("daily_streak", 0)
            val totalBadges = prefs.getInt("total_badges", 0)
            
            val xpForNextLevel = currentLevel * 100
            val remainingXP = xpForNextLevel - currentXP
            
            val content = "Daily Summary: Level $currentLevel, $currentXP/$xpForNextLevel XP, $dailyStreak day streak, $totalBadges badges. Great progress today!"
            
            notificationManager.scheduleNotification(
                type = "insights_analytics",
                title = "Daily Progress Summary",
                content = content,
                additionalData = mapOf(
                    "summary_type" to "daily",
                    "level" to currentLevel,
                    "xp" to currentXP,
                    "streak" to dailyStreak,
                    "badges" to totalBadges
                )
            )
        }
    }

    /**
     * Check and trigger any relevant gamification notifications based on current state
     */
    fun checkAndTriggerGamificationNotifications() {
        CoroutineScope(Dispatchers.Main).launch {
            val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
            val currentLevel = prefs.getInt("user_level", 1)
            val currentXP = prefs.getInt("user_xp", 0)
            val dailyStreak = prefs.getInt("daily_streak", 0)
            val challengeStreak = prefs.getInt("challenge_streak", 0)
            val productivityStreak = prefs.getInt("productivity_streak", 0)
            
            // Check for weekly milestone notifications
            if (dailyStreak > 0 && dailyStreak % 7 == 0) {
                triggerStreakNotification("daily_streak", dailyStreak)
            }
            
            // Check for challenge milestones
            if (challengeStreak > 0 && challengeStreak % 10 == 0) {
                triggerStreakNotification("challenge_streak", challengeStreak)
            }
            
            // Check for productivity milestones
            if (productivityStreak > 0 && productivityStreak % 5 == 0) {
                triggerStreakNotification("productivity_streak", productivityStreak)
            }
            
            // Award and notify about badges
            GamificationUtils.checkAndAwardBadges(context)
        }
    }
}