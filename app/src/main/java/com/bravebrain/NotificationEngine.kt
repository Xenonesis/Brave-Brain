package com.bravebrain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class NotificationEngine(private val context: Context) {
    companion object {
        private const val TAG = "NotificationEngine"
        
        // Notification channel IDs
        const val PROACTIVE_WARNINGS_CHANNEL_ID = "proactive_warnings_channel"
        const val POSITIVE_REINFORCEMENT_CHANNEL_ID = "positive_reinforcement_channel"
        const val INSIGHTS_ANALYTICS_CHANNEL_ID = "insights_analytics_channel"
        const val GAMIFICATION_UPDATES_CHANNEL_ID = "gamification_updates_channel"
        const val CONTEXTUAL_SUGGESTIONS_CHANNEL_ID = "contextual_suggestions_channel"
        
        // Notification IDs
        const val PROACTIVE_WARNING_ID = 2001
        const val POSITIVE_REINFORCEMENT_ID = 2002
        const val INSIGHTS_ANALYTICS_ID = 2003
        const val GAMIFICATION_UPDATE_ID = 2004
        const val CONTEXTUAL_SUGGESTION_ID = 2005
    }

    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(3)
    private val preferenceManager: NotificationPreferenceManager = NotificationPreferenceManager(context)

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Proactive Warnings Channel
            val proactiveWarningsChannel = NotificationChannel(
                PROACTIVE_WARNINGS_CHANNEL_ID,
                "Proactive Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings about potential negative screen time patterns"
                enableVibration(true)
                setBypassDnd(true)
            }

            // Positive Reinforcement Channel
            val positiveReinforcementChannel = NotificationChannel(
                POSITIVE_REINFORCEMENT_CHANNEL_ID,
                "Positive Reinforcement",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Positive feedback for good screen time habits"
                enableVibration(false)
            }

            // Insights & Analytics Channel
            val insightsAnalyticsChannel = NotificationChannel(
                INSIGHTS_ANALYTICS_CHANNEL_ID,
                "Insights & Analytics",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Weekly and monthly usage insights"
                enableVibration(false)
            }

            // Gamification Updates Channel
            val gamificationUpdatesChannel = NotificationChannel(
                GAMIFICATION_UPDATES_CHANNEL_ID,
                "Gamification Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Achievements, badges, and progress updates"
                enableVibration(false)
            }

            // Contextual Suggestions Channel
            val contextualSuggestionsChannel = NotificationChannel(
                CONTEXTUAL_SUGGESTIONS_CHANNEL_ID,
                "Contextual Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Personalized app usage suggestions"
                enableVibration(false)
            }

            // Register the channels with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(
                listOf(
                    proactiveWarningsChannel,
                    positiveReinforcementChannel,
                    insightsAnalyticsChannel,
                    gamificationUpdatesChannel,
                    contextualSuggestionsChannel
                )
            )
        }
    }

    fun startNotificationLogic() {
        Log.d(TAG, "Starting notification logic")
        
        // Schedule periodic checks for notifications
        executor.scheduleAtFixedRate({
            checkAndSendNotifications()
        }, 0, 15, TimeUnit.MINUTES) // Check every 15 minutes
    }

    private fun checkAndSendNotifications() {
        Log.d(TAG, "Checking for notifications to send")
        
        // Check each type of notification based on preferences
        if (preferenceManager.isProactiveWarningsEnabled()) {
            checkProactiveWarnings()
        }
        
        if (preferenceManager.isPositiveReinforcementEnabled()) {
            checkPositiveReinforcement()
        }
        
        if (preferenceManager.isInsightsAnalyticsEnabled()) {
            checkInsightsAnalytics()
        }
        
        if (preferenceManager.isGamificationUpdatesEnabled()) {
            checkGamificationUpdates()
        }
        
        if (preferenceManager.isContextualSuggestionsEnabled()) {
            checkContextualSuggestions()
        }
    }

    private fun checkProactiveWarnings() {
        // Logic to determine if proactive warnings should be shown
        // This would typically involve checking usage patterns, time limits, etc.
        Log.d(TAG, "Checking for proactive warnings")
        
        // Example: Check if user is approaching time limits
        if (shouldShowProactiveWarning()) {
            sendProactiveWarningNotification()
        }
    }

    private fun shouldShowProactiveWarning(): Boolean {
        // Placeholder logic - in real implementation, this would check actual usage data
        return true // For demonstration purposes
    }

    private fun sendProactiveWarningNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, PROACTIVE_WARNINGS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Approaching Time Limit")
            .setContentText("You're approaching your daily limit for a specific app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(PROACTIVE_WARNING_ID, notification)
    }

    private fun checkPositiveReinforcement() {
        Log.d(TAG, "Checking for positive reinforcement")
        
        if (shouldShowPositiveReinforcement()) {
            sendPositiveReinforcementNotification()
        }
    }

    private fun shouldShowPositiveReinforcement(): Boolean {
        // Placeholder logic - in real implementation, this would check achievements, milestones, etc.
        return true // For demonstration purposes
    }

    private fun sendPositiveReinforcementNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, POSITIVE_REINFORCEMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Great Job!")
            .setContentText("You've maintained good screen time habits today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(POSITIVE_REINFORCEMENT_ID, notification)
    }

    private fun checkInsightsAnalytics() {
        Log.d(TAG, "Checking for insights analytics")
        
        if (shouldShowInsightsAnalytics()) {
            sendInsightsAnalyticsNotification()
        }
    }

    private fun shouldShowInsightsAnalytics(): Boolean {
        // Placeholder logic - in real implementation, this would check if it's time for insights
        return true // For demonstration purposes
    }

    private fun sendInsightsAnalyticsNotification() {
        val intent = Intent(context, InsightsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, INSIGHTS_ANALYTICS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Weekly Insights Ready")
            .setContentText("Your weekly screen time report is ready to view.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(INSIGHTS_ANALYTICS_ID, notification)
    }

    private fun checkGamificationUpdates() {
        Log.d(TAG, "Checking for gamification updates")
        
        if (shouldShowGamificationUpdate()) {
            sendGamificationUpdateNotification()
        }
    }

    private fun shouldShowGamificationUpdate(): Boolean {
        // Placeholder logic - in real implementation, this would check for achievements
        return true // For demonstration purposes
    }

    private fun sendGamificationUpdateNotification() {
        val intent = Intent(context, GamificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, GAMIFICATION_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Achievement Unlocked!")
            .setContentText("You've unlocked a new achievement in your screen time journey.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(GAMIFICATION_UPDATE_ID, notification)
    }

    private fun checkContextualSuggestions() {
        Log.d(TAG, "Checking for contextual suggestions")
        
        if (shouldShowContextualSuggestion()) {
            sendContextualSuggestionNotification()
        }
    }

    private fun shouldShowContextualSuggestion(): Boolean {
        // Placeholder logic - in real implementation, this would check context
        return true // For demonstration purposes
    }

    private fun sendContextualSuggestionNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CONTEXTUAL_SUGGESTIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Suggestion for You")
            .setContentText("Try using a different app that might be more productive right now.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(CONTEXTUAL_SUGGESTION_ID, notification)
    }

    fun stopNotificationLogic() {
        Log.d(TAG, "Stopping notification logic")
        executor.shutdown()
    }

    /**
     * Generic method to send a notification
     */
    fun sendNotification(channelId: String, title: String, content: String, priority: Int) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(mapPriority(priority))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(generateNotificationId(), notification)
    }

    private fun mapPriority(priority: Int): Int {
        return when (priority) {
            android.app.NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
            android.app.NotificationManager.IMPORTANCE_DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            android.app.NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    private fun generateNotificationId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }
}