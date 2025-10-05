package com.example.testing

import android.content.Context
import android.util.Log
import java.util.*

/**
 * NotificationSchedulingAlgorithms implements intelligent scheduling algorithms
 * that consider user context, optimal timing, and engagement patterns
 */
class NotificationSchedulingAlgorithms(
    private val context: Context,
    private val contextAnalyzer: ContextAnalyzer,
    private val userEngagementAnalyzer: UserEngagementAnalyzer
) {
    companion object {
        private const val TAG = "NotificationSchedAlgo"
    }

    /**
     * Calculates the optimal time to send a notification based on user patterns
     */
    fun calculateOptimalNotificationTime(
        notificationType: String,
        baseTime: Long = System.currentTimeMillis(),
        contextRequirement: NotificationScheduler.ScheduledNotification.ContextRequirement? = null
    ): Long {
        val userContext = contextAnalyzer.analyzeContext()
        val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()

        // Start with the base time
        var scheduledTime = baseTime

        // Adjust based on optimal timing patterns
        val optimalHours = userEngagementAnalyzer.identifyOptimalTiming()
        if (optimalHours.isNotEmpty()) {
            // Find the next optimal hour from now
            scheduledTime = getNextOptimalTime(scheduledTime, optimalHours, userContext)
        }

        // Consider context requirements
        if (contextRequirement != null) {
            scheduledTime = adjustForContextRequirements(scheduledTime, contextRequirement)
        }

        // Avoid sleep time
        scheduledTime = avoidSleepTime(scheduledTime, userContext)

        // Consider engagement trends
        scheduledTime = adjustForEngagementTrend(scheduledTime, engagementMetrics, notificationType)

        // Apply personalization score to further refine timing
        scheduledTime = applyPersonalizationAdjustment(scheduledTime, notificationType, engagementMetrics)

        return scheduledTime
    }

    /**
     * Finds the next optimal time based on user's historical engagement patterns
     */
    private fun getNextOptimalTime(
        baseTime: Long,
        optimalHours: List<Int>,
        userContext: ContextAnalyzer.UserContext
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = baseTime

        // Get current hour
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Find the next optimal hour
        val nextOptimalHour = optimalHours.find { it > currentHour } ?: 
            if (optimalHours.isNotEmpty()) optimalHours.first() else currentHour

        // Set the time to the next optimal hour
        calendar.set(Calendar.HOUR_OF_DAY, nextOptimalHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If the calculated time is earlier than current time, add a day
        if (calendar.timeInMillis <= baseTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return calendar.timeInMillis
    }

    /**
     * Adjusts the scheduled time to meet context requirements
     */
    private fun adjustForContextRequirements(
        baseTime: Long,
        contextRequirement: NotificationScheduler.ScheduledNotification.ContextRequirement
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = baseTime

        // Adjust for time of day requirement
        if (contextRequirement.timeOfDay != null) {
            val hour = when (contextRequirement.timeOfDay) {
                ContextAnalyzer.TimeOfDay.MORNING -> 8
                ContextAnalyzer.TimeOfDay.AFTERNOON -> 14
                ContextAnalyzer.TimeOfDay.EVENING -> 19
                ContextAnalyzer.TimeOfDay.NIGHT -> 21
            }
            calendar.set(Calendar.HOUR_OF_DAY, hour)
        }

        // Adjust for day of week requirement
        if (contextRequirement.dayOfWeek != null) {
            // This is a simplification - in a real implementation, we'd find the next occurrence
            val targetDay = when (contextRequirement.dayOfWeek) {
                ContextAnalyzer.DayOfWeek.MONDAY -> Calendar.MONDAY
                ContextAnalyzer.DayOfWeek.TUESDAY -> Calendar.TUESDAY
                ContextAnalyzer.DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                ContextAnalyzer.DayOfWeek.THURSDAY -> Calendar.THURSDAY
                ContextAnalyzer.DayOfWeek.FRIDAY -> Calendar.FRIDAY
                ContextAnalyzer.DayOfWeek.SATURDAY -> Calendar.SATURDAY
                ContextAnalyzer.DayOfWeek.SUNDAY -> Calendar.SUNDAY
            }
            calendar.set(Calendar.DAY_OF_WEEK, targetDay)
        }

        return calendar.timeInMillis
    }

    /**
     * Avoids scheduling notifications during sleep time
     */
    private fun avoidSleepTime(baseTime: Long, userContext: ContextAnalyzer.UserContext): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = baseTime

        // Check if the scheduled time falls during sleep time
        val sleepTime = contextAnalyzer.getSleepTime()
        val sleepStart = sleepTime.first
        val sleepEnd = sleepTime.second

        val scheduledHour = calendar.get(Calendar.HOUR_OF_DAY)

        if (sleepStart > sleepEnd) {
            // Sleep time crosses midnight (e.g., 22:00 to 07:00)
            if (scheduledHour >= sleepStart || scheduledHour < sleepEnd) {
                // Adjust to after sleep time
                calendar.set(Calendar.HOUR_OF_DAY, sleepEnd)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                
                if (scheduledHour >= sleepStart) {
                    // If we're in the evening sleep period, schedule for tomorrow
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        } else {
            // Sleep time within same day (e.g., 23:00 to 07:00)
            if (scheduledHour in sleepStart until sleepEnd) {
                // Adjust to after sleep time
                calendar.set(Calendar.HOUR_OF_DAY, sleepEnd)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * Adjusts notification timing based on user's engagement trend
     */
    private fun adjustForEngagementTrend(
        baseTime: Long,
        engagementMetrics: UserEngagementAnalyzer.EngagementMetrics,
        notificationType: String
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = baseTime

        return when (engagementMetrics.engagementTrend) {
            UserEngagementAnalyzer.EngagementTrend.DECREASING -> {
                // If engagement is decreasing, try more engaging times or content
                val optimalHours = userEngagementAnalyzer.identifyOptimalTiming()
                if (optimalHours.isNotEmpty()) {
                    // Find the most engaging hour based on historical data
                    val mostEngagingHour = findMostEngagingHour(engagementMetrics)
                    calendar.set(Calendar.HOUR_OF_DAY, mostEngagingHour)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                }
                calendar.timeInMillis
            }
            UserEngagementAnalyzer.EngagementTrend.INCREASING -> {
                // If engagement is increasing, maintain current timing but consider sending more
                baseTime
            }
            UserEngagementAnalyzer.EngagementTrend.STABLE -> {
                // If engagement is stable, use standard timing
                baseTime
            }
        }
    }

    /**
     * Finds the most engaging hour based on effectiveness data
     */
    private fun findMostEngagingHour(engagementMetrics: UserEngagementAnalyzer.EngagementMetrics): Int {
        val effectivenessByTime = userEngagementAnalyzer.getEffectivenessByTime()
        if (effectivenessByTime.isEmpty()) {
            return 12 // Default to noon if no data
        }

        return effectivenessByTime.maxByOrNull { it.value }?.key ?: 12
    }

    /**
     * Applies personalization adjustment based on notification type and user preferences
     */
    private fun applyPersonalizationAdjustment(
        baseTime: Long,
        notificationType: String,
        engagementMetrics: UserEngagementAnalyzer.EngagementMetrics
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = baseTime

        // Get content preference for this notification type
        val typePreference = engagementMetrics.contentPreferences.getOrDefault(notificationType, 0.5f)

        if (typePreference < 0.3f) {
            // For low-preference content, schedule during peak engagement times
            val peakHours = listOf(10, 11, 14, 15, 19, 20) // Peak engagement hours
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            
            val closestPeakHour = peakHours.minByOrNull { 
                val diff = if (it > currentHour) it - currentHour else (24 - currentHour) + it
                diff
            } ?: currentHour
            
            calendar.set(Calendar.HOUR_OF_DAY, closestPeakHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        } else if (typePreference > 0.7f) {
            // For high-preference content, can be more flexible with timing
            // No adjustment needed, keep the calculated time
        }

        return calendar.timeInMillis
    }

    /**
     * Determines the appropriate priority for a notification based on context and engagement
     */
    fun determineNotificationPriority(
        notificationType: String,
        userContext: ContextAnalyzer.UserContext,
        engagementMetrics: UserEngagementAnalyzer.EngagementMetrics
    ): NotificationScheduler.ScheduledNotification.Priority {
        // Check if user is in a sensitive context (sleep, focus time)
        if (userContext.isSleepTime) {
            return if (notificationType.lowercase().contains("urgent") || 
                      notificationType.lowercase().contains("critical")) {
                NotificationScheduler.ScheduledNotification.Priority.HIGH
            } else {
                NotificationScheduler.ScheduledNotification.Priority.LOW
            }
        }

        if (userContext.isFocusTime) {
            return if (notificationType.lowercase().contains("break") || 
                      notificationType.lowercase().contains("reminder")) {
                NotificationScheduler.ScheduledNotification.Priority.NORMAL
            } else {
                NotificationScheduler.ScheduledNotification.Priority.LOW
            }
        }

        // Check content preferences
        val typePreference = engagementMetrics.contentPreferences.getOrDefault(notificationType, 0.5f)
        
        return when {
            typePreference >= 0.8f -> NotificationScheduler.ScheduledNotification.Priority.HIGH
            typePreference >= 0.5f -> NotificationScheduler.ScheduledNotification.Priority.NORMAL
            else -> NotificationScheduler.ScheduledNotification.Priority.LOW
        }
    }

    /**
     * Evaluates if it's an appropriate time to send a notification based on multiple factors
     */
    fun isAppropriateTimeToSend(
        notificationType: String,
        scheduledTime: Long = System.currentTimeMillis(),
        userContext: ContextAnalyzer.UserContext? = null,
        engagementMetrics: UserEngagementAnalyzer.EngagementMetrics? = null
    ): Boolean {
        val context = userContext ?: contextAnalyzer.analyzeContext()
        val metrics = engagementMetrics ?: userEngagementAnalyzer.analyzeEngagement()

        // Don't send during sleep time unless critical
        if (context.isSleepTime && !isCriticalNotification(notificationType)) {
            Log.d(TAG, "Not appropriate time: user is sleeping")
            return false
        }

        // Don't send during focus time unless relevant
        if (context.isFocusTime && !isFocusTimeRelevant(notificationType)) {
            Log.d(TAG, "Not appropriate time: user is in focus mode")
            return false
        }

        // Check notification fatigue
        if (metrics.notificationFatigueScore > 0.8f) {
            Log.d(TAG, "Not appropriate time: high notification fatigue")
            return false
        }

        // Check if it's during optimal timing windows
        val hour = Calendar.getInstance().apply { timeInMillis = scheduledTime }.get(Calendar.HOUR_OF_DAY)
        val optimalTiming = userEngagementAnalyzer.identifyOptimalTiming()
        if (hour !in optimalTiming && !isCriticalNotification(notificationType)) {
            Log.d(TAG, "Not optimal timing for notification type: $notificationType")
        }

        return true
    }

    /**
     * Determines if a notification is critical and should be sent regardless of context
     */
    private fun isCriticalNotification(notificationType: String): Boolean {
        return notificationType.lowercase().contains("urgent") ||
               notificationType.lowercase().contains("critical") ||
               notificationType.lowercase().contains("emergency") ||
               notificationType.lowercase().contains("limit") && 
               notificationType.lowercase().contains("approaching")
    }

    /**
     * Determines if a notification is relevant during focus time
     */
    private fun isFocusTimeRelevant(notificationType: String): Boolean {
        return notificationType.lowercase().contains("break") ||
               notificationType.lowercase().contains("reminder") ||
               notificationType.lowercase().contains("time")
    }

    /**
     * Adjusts scheduling based on usage intensity
     */
    fun adjustForUsageIntensity(
        baseTime: Long,
        userContext: ContextAnalyzer.UserContext,
        notificationType: String
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = baseTime

        return when (userContext.usageIntensity) {
            ContextAnalyzer.UsageIntensity.LOW -> {
                // During low usage, user might be more receptive to notifications
                // No adjustment needed
                baseTime
            }
            ContextAnalyzer.UsageIntensity.MODERATE -> {
                // Standard timing is appropriate
                baseTime
            }
            ContextAnalyzer.UsageIntensity.HIGH -> {
                // During high usage, delay notifications to avoid interruption
                calendar.add(Calendar.MINUTE, 15) // Delay by 15 minutes
                calendar.timeInMillis
            }
            ContextAnalyzer.UsageIntensity.PEAK -> {
                // During peak usage, delay significantly or find alternative time
                calendar.add(Calendar.MINUTE, 30) // Delay by 30 minutes
                calendar.timeInMillis
            }
        }
    }

    /**
     * Calculates the effectiveness score for a potential notification
     */
    fun calculateEffectivenessScore(
        notificationType: String,
        scheduledTime: Long,
        userContext: ContextAnalyzer.UserContext? = null,
        engagementMetrics: UserEngagementAnalyzer.EngagementMetrics? = null
    ): Float {
        val context = userContext ?: contextAnalyzer.analyzeContext()
        val metrics = engagementMetrics ?: userEngagementAnalyzer.analyzeEngagement()

        // Start with base effectiveness
        var score = 0.5f

        // Adjust based on content preference
        val typePreference = metrics.contentPreferences.getOrDefault(notificationType, 0.5f)
        score += (typePreference - 0.5f) * 0.3f

        // Adjust based on timing
        val hour = Calendar.getInstance().apply { timeInMillis = scheduledTime }.get(Calendar.HOUR_OF_DAY)
        val timeEffectiveness = userEngagementAnalyzer.calculatePersonalizationScore(notificationType, hour)
        score += (timeEffectiveness - 0.5f) * 0.3f

        // Adjust based on context appropriateness
        if (isAppropriateTimeToSend(notificationType, scheduledTime, context, metrics)) {
            score += 0.2f
        } else {
            score -= 0.3f
        }

        // Adjust based on engagement trend
        score += when (metrics.engagementTrend) {
            UserEngagementAnalyzer.EngagementTrend.INCREASING -> 0.1f
            UserEngagementAnalyzer.EngagementTrend.DECREASING -> -0.1f
            UserEngagementAnalyzer.EngagementTrend.STABLE -> 0.0f
        }

        // Ensure score is within bounds
        return score.coerceIn(0f, 1f)
    }
}