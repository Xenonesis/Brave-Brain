package com.bravebrain

import android.content.Context
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * ContextAwareNotificationEngine extends the existing notification engine
 * with context-aware capabilities using the new context detection components
 */
class ContextAwareNotificationEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "ContextAwareNotificationEngine"
        
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
    
    // Context detection components
    private val contextAnalyzer = ContextAnalyzer(context)
    private val contextDetectionModule = ContextDetectionModule(context)
    private val usagePatternRecognition = UsagePatternRecognition(context)
    private val userEngagementAnalyzer = UserEngagementAnalyzer(context)
    
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Proactive Warnings Channel
            val proactiveWarningsChannel = NotificationChannel(
                PROACTIVE_WARNINGS_CHANNEL_ID,
                "Proactive Warnings",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings about potential negative screen time patterns"
                enableVibration(true)
                setBypassDnd(true)
            }

            // Positive Reinforcement Channel
            val positiveReinforcementChannel = NotificationChannel(
                POSITIVE_REINFORCEMENT_CHANNEL_ID,
                "Positive Reinforcement",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Positive feedback for good screen time habits"
                enableVibration(false)
            }

            // Insights & Analytics Channel
            val insightsAnalyticsChannel = NotificationChannel(
                INSIGHTS_ANALYTICS_CHANNEL_ID,
                "Insights & Analytics",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Weekly and monthly usage insights"
                enableVibration(false)
            }

            // Gamification Updates Channel
            val gamificationUpdatesChannel = NotificationChannel(
                GAMIFICATION_UPDATES_CHANNEL_ID,
                "Gamification Updates",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Achievements, badges, and progress updates"
                enableVibration(false)
            }

            // Contextual Suggestions Channel
            val contextualSuggestionsChannel = NotificationChannel(
                CONTEXTUAL_SUGGESTIONS_CHANNEL_ID,
                "Contextual Suggestions",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Personalized app usage suggestions based on context"
                enableVibration(false)
            }

            // Register the channels with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
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
        android.util.Log.d(TAG, "Starting context-aware notification logic")
        
        // Schedule periodic checks for notifications
        executor.scheduleAtFixedRate({
            checkAndSendContextualNotifications()
        }, 0, 15, TimeUnit.MINUTES) // Check every 15 minutes
    }

    private fun checkAndSendContextualNotifications() {
        android.util.Log.d(TAG, "Checking for contextual notifications to send")
        
        // Analyze current context
        val userContext = contextAnalyzer.analyzeContext()
        val deviceContext = contextDetectionModule.detectDeviceContext()
        
        // Only send notifications if context is appropriate
        if (userContext.isSleepTime || userContext.isFocusTime || !deviceContext.screenOn) {
            android.util.Log.d(TAG, "Skipping notifications due to context: sleep=${userContext.isSleepTime}, focus=${userContext.isFocusTime}, screenOn=${deviceContext.screenOn}")
            return
        }
        
        // Check each type of notification based on preferences and context
        if (preferenceManager.isProactiveWarningsEnabled()) {
            checkProactiveWarnings(userContext, deviceContext)
        }
        
        if (preferenceManager.isPositiveReinforcementEnabled()) {
            checkPositiveReinforcement(userContext, deviceContext)
        }
        
        if (preferenceManager.isInsightsAnalyticsEnabled()) {
            checkInsightsAnalytics(userContext, deviceContext)
        }
        
        if (preferenceManager.isGamificationUpdatesEnabled()) {
            checkGamificationUpdates(userContext, deviceContext)
        }
        
        if (preferenceManager.isContextualSuggestionsEnabled()) {
            checkContextualSuggestions(userContext, deviceContext)
        }
        
        // Update context patterns for learning
        contextAnalyzer.learnUserPatterns()
    }

    private fun checkProactiveWarnings(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext) {
        android.util.Log.d(TAG, "Checking for proactive warnings with context")
        
        // Use context and usage patterns to determine if warning should be shown
        if (shouldShowProactiveWarning(userContext, deviceContext)) {
            // Check if this time is optimal based on engagement patterns
            val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            
            if (engagementMetrics.responseRate > 0.3f || currentHour in engagementMetrics.optimalTiming) {
                sendProactiveWarningNotification()
            } else {
                android.util.Log.d(TAG, "Skipping proactive warning due to low engagement at this time")
            }
        }
    }

    private fun shouldShowProactiveWarning(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext): Boolean {
        // Check usage patterns to see if user is approaching problematic behavior
        val bingePatterns = usagePatternRecognition.detectBingeBehavior()
        val peakUsageTimes = usagePatternRecognition.detectPeakUsageTimes()
        
        // Check if currently in a high-risk time based on patterns
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isPeakUsageTime = peakUsageTimes.any { it.hour == currentHour }
        
        // Check if user is showing binge behavior patterns
        val hasBingePatterns = bingePatterns.any { it.severity >= UsagePatternRecognition.BingeSeverity.HIGH }
        
        // Check if user is in a recovery pattern (don't show warnings during recovery)
        val recoveryPatterns = usagePatternRecognition.detectRecoveryPatterns()
        val isInRecovery = recoveryPatterns.any { 
            val recoveryDate = java.util.Calendar.getInstance().apply { time = it.date }
            val today = java.util.Calendar.getInstance()
            recoveryDate.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
        }
        
        return isPeakUsageTime && hasBingePatterns && !isInRecovery
    }

    private fun sendProactiveWarningNotification() {
        val notificationId = PROACTIVE_WARNING_ID
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, PROACTIVE_WARNINGS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Approaching Time Limit")
            .setContentText("You're approaching your daily limit for a specific app. Consider taking a break.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Track notification for engagement analysis
        notificationManager.notify(notificationId, notification)
        
        // Track that notification was sent
        userEngagementAnalyzer.trackNotificationResponse(notificationId, false) // Not opened initially
    }

    private fun checkPositiveReinforcement(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext) {
        android.util.Log.d(TAG, "Checking for positive reinforcement with context")
        
        if (shouldShowPositiveReinforcement(userContext, deviceContext)) {
            // Check engagement patterns to determine optimal timing
            val bestTimes = userEngagementAnalyzer.predictBestNotificationTime("positive_reinforcement")
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            
            if (currentHour in bestTimes) {
                sendPositiveReinforcementNotification()
            }
        }
    }

    private fun shouldShowPositiveReinforcement(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext): Boolean {
        // Check if user has shown improvement or reached positive milestones
        val recoveryPatterns = usagePatternRecognition.detectRecoveryPatterns()
        val isShowingImprovement = recoveryPatterns.any { 
            val recoveryDate = java.util.Calendar.getInstance().apply { time = it.date }
            val today = java.util.Calendar.getInstance()
            recoveryDate.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
        }
        
        // Check circadian rhythm patterns to see if sleep patterns are improving
        val circadianPattern = usagePatternRecognition.detectCircadianRhythm()
        val isHealthyRhythm = circadianPattern.dayNightRatio > 1.0f && circadianPattern.rhythmConsistency > 70.0f
        
        return isShowingImprovement || isHealthyRhythm
    }

    private fun sendPositiveReinforcementNotification() {
        val notificationId = POSITIVE_REINFORCEMENT_ID
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, POSITIVE_REINFORCEMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Great Job!")
            .setContentText("You've maintained good screen time habits today. Keep up the good work!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Track notification for engagement analysis
        notificationManager.notify(notificationId, notification)
        
        // Track that notification was sent
        userEngagementAnalyzer.trackNotificationResponse(notificationId, false)
    }

    private fun checkInsightsAnalytics(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext) {
        android.util.Log.d(TAG, "Checking for insights analytics with context")
        
        if (shouldShowInsightsAnalytics(userContext, deviceContext)) {
            // Check if user is likely to engage with insights
            val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()
            val personalizationScore = userEngagementAnalyzer.calculatePersonalizationScore("insights", 
                java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY))
            
            if (engagementMetrics.responseRate > 0.2f && personalizationScore > 0.5f) {
                sendInsightsAnalyticsNotification()
            }
        }
    }

    private fun shouldShowInsightsAnalytics(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext): Boolean {
        // Show insights when user is not in a rush and has shown interest in analytics
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isCalmTime = currentHour !in 7..9 && currentHour !in 17..19 // Avoid rush hours
        
        // Check if it's been a while since last insights
        val lastInsights = getLastInsightsNotificationTime()
        val timeSinceLastInsights = System.currentTimeMillis() - lastInsights
        val shouldShow = timeSinceLastInsights > TimeUnit.DAYS.toMillis(1) // At least daily
        
        return isCalmTime && shouldShow
    }

    private fun sendInsightsAnalyticsNotification() {
        val notificationId = INSIGHTS_ANALYTICS_ID
        val intent = Intent(context, InsightsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        // Generate personalized insights based on usage patterns
        val insightsText = generatePersonalizedInsights()
        
        val notification = NotificationCompat.Builder(context, INSIGHTS_ANALYTICS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Your Personal Insights")
            .setContentText(insightsText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Track notification for engagement analysis
        notificationManager.notify(notificationId, notification)
        
        // Track that notification was sent
        userEngagementAnalyzer.trackNotificationResponse(notificationId, false)
        
        // Update last insights time
        setLastInsightsNotificationTime(System.currentTimeMillis())
    }

    private fun generatePersonalizedInsights(): String {
        val circadianPattern = usagePatternRecognition.detectCircadianRhythm()
        val bingePatterns = usagePatternRecognition.detectBingeBehavior()
        val recoveryPatterns = usagePatternRecognition.detectRecoveryPatterns()
        
        val insights = mutableListOf<String>()
        
        // Add circadian rhythm insight
        if (circadianPattern.rhythmConsistency > 80.0f) {
            insights.add("Your usage rhythm is very consistent! This is great for maintaining healthy habits.")
        } else if (circadianPattern.rhythmConsistency < 50.0f) {
            insights.add("Your usage times vary quite a bit. Try setting consistent usage hours.")
        }
        
        // Add binge behavior insight if detected
        if (bingePatterns.isNotEmpty()) {
            val severeBinge = bingePatterns.any { it.severity >= UsagePatternRecognition.BingeSeverity.HIGH }
            if (severeBinge) {
                insights.add("We noticed some extended usage sessions. Consider taking more frequent breaks.")
            }
        }
        
        // Add recovery insight
        if (recoveryPatterns.isNotEmpty()) {
            val recentRecovery = recoveryPatterns.lastOrNull()
            if (recentRecovery != null) {
                val daysAgo = ((System.currentTimeMillis() - recentRecovery.date.time) / (24L * 60L * 60L * 1000L)).toInt()
                if (daysAgo <= 1) {
                    insights.add("Great work on reducing usage yesterday!")
                }
            }
        }
        
        // Return first insight or default message
        return if (insights.isNotEmpty()) insights[0] else "Your weekly screen time report is ready to view."
    }

    private fun checkGamificationUpdates(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext) {
        android.util.Log.d(TAG, "Checking for gamification updates with context")
        
        if (shouldShowGamificationUpdate(userContext, deviceContext)) {
            // Check engagement patterns for optimal timing
            val bestTimes = userEngagementAnalyzer.predictBestNotificationTime("gamification")
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            
            if (currentHour in bestTimes) {
                sendGamificationUpdateNotification()
            }
        }
    }

    private fun shouldShowGamificationUpdate(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext): Boolean {
        // Show gamification when user is engaged but not overwhelmed
        val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()
        val shouldShow = engagementMetrics.notificationFatigueScore < 0.7f // Not too fatigued
        
        return shouldShow
    }

    private fun sendGamificationUpdateNotification() {
        val notificationId = GAMIFICATION_UPDATE_ID
        val intent = Intent(context, GamificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, GAMIFICATION_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Achievement Unlocked!")
            .setContentText("You've made progress on your screen time goals. Keep it up!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Track notification for engagement analysis
        notificationManager.notify(notificationId, notification)
        
        // Track that notification was sent
        userEngagementAnalyzer.trackNotificationResponse(notificationId, false)
    }

    private fun checkContextualSuggestions(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext) {
        android.util.Log.d(TAG, "Checking for contextual suggestions with context")
        
        if (shouldShowContextualSuggestion(userContext, deviceContext)) {
            // Use engagement patterns to personalize and time appropriately
            val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val personalizationScore = userEngagementAnalyzer.calculatePersonalizationScore("suggestion", currentHour)
            
            if (engagementMetrics.responseRate > 0.1f && personalizationScore > 0.3f) {
                sendContextualSuggestionNotification()
            }
        }
    }

    private fun shouldShowContextualSuggestion(userContext: ContextAnalyzer.UserContext, deviceContext: ContextDetectionModule.DeviceContext): Boolean {
        // Check if user is in a context-appropriate time for suggestions
        val isAppropriateTime = userContext.optimalNotificationTime && 
                               !userContext.isWorkTime && 
                               !deviceContext.isMoving // Not while moving
        
        // Check if user has shown interest in suggestions
        val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()
        val suggestionsPreference = userEngagementAnalyzer.getContentPreferencesByType().getOrDefault("suggestion", 0.5f)
        
        return isAppropriateTime && (engagementMetrics.responseRate > 0.1f || suggestionsPreference > 0.5f)
    }

    private fun sendContextualSuggestionNotification() {
        val notificationId = CONTEXTUAL_SUGGESTION_ID
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        // Generate contextual suggestion based on current context and usage
        val suggestionText = generateContextualSuggestion()
        
        val notification = NotificationCompat.Builder(context, CONTEXTUAL_SUGGESTIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("Suggestion for You")
            .setContentText(suggestionText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Track notification for engagement analysis
        notificationManager.notify(notificationId, notification)
        
        // Track that notification was sent
        userEngagementAnalyzer.trackNotificationResponse(notificationId, false)
    }

    private fun generateContextualSuggestion(): String {
        // Analyze current usage and suggest alternatives based on context
        val currentUsage = UsageUtils.getUsage(context)
        val peakHours = usagePatternRecognition.getPeakUsageHours()
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        // Check if currently using an app during peak hours
        var peakUsageApp = ""
        var maxUsage = 0
        for ((app, usage) in currentUsage) {
            if (usage > maxUsage) {
                maxUsage = usage.toInt() // Convert Long to Int to match maxUsage type
                peakUsageApp = app
            }
        }
        
        // If using a potentially problematic app during peak time, suggest an alternative
        if (maxUsage > 30 * 60 * 1000) { // Using app for >30 minutes (in milliseconds)
            return "You've been using $peakUsageApp for a while. Consider switching to a more productive app."
        }
        
        // Default suggestion
        return "Try using a different app that might be more productive right now."
    }

    /**
     * Gets peak usage hours from usage pattern recognition
     */
    private fun getPeakUsageHours(): List<Int> {
        val peakPatterns = usagePatternRecognition.detectPeakUsageTimes()
        return peakPatterns.map { it.hour }.distinct()
    }

    private fun getLastInsightsNotificationTime(): Long {
        val prefs = context.getSharedPreferences("notification_timing", Context.MODE_PRIVATE)
        return prefs.getLong("last_insights_time", 0L)
    }

    private fun setLastInsightsNotificationTime(time: Long) {
        val prefs = context.getSharedPreferences("notification_timing", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_insights_time", time).apply()
    }

    fun stopNotificationLogic() {
        android.util.Log.d(TAG, "Stopping context-aware notification logic")
        executor.shutdown()
    }
    
    /**
     * Updates engagement when a notification is opened
     */
    fun onNotificationOpened(notificationId: Int) {
        userEngagementAnalyzer.trackNotificationResponse(notificationId, true)
    }
    
    /**
     * Gets the context analyzer for external use
     */
    fun getContextAnalyzer(): ContextAnalyzer {
        return contextAnalyzer
    }
    
    /**
     * Gets the usage pattern recognition for external use
     */
    fun getUsagePatternRecognition(): UsagePatternRecognition {
        return usagePatternRecognition
    }
    
    /**
     * Gets the user engagement analyzer for external use
     */
    fun getUserEngagementAnalyzer(): UserEngagementAnalyzer {
        return userEngagementAnalyzer
    }
}
