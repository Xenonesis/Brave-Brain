package com.example.testing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * NotificationScheduler manages timing and delivery of notifications with features for
 * intelligent throttling, recurring notifications, and priority-based queuing
 */
class NotificationScheduler(
    private val context: Context,
    private val notificationEngine: NotificationEngine,
    private val contextAnalyzer: ContextAnalyzer,
    private val userEngagementAnalyzer: UserEngagementAnalyzer
) {
    companion object {
        private const val TAG = "NotificationScheduler"
        private const val DEFAULT_THROTTLE_WINDOW_MS = 60 * 60 * 1000L // 1 hour
        private const val MAX_NOTIFICATIONS_PER_WINDOW = 5
        private const val MIN_INTERVAL_BETWEEN_NOTIFICATIONS = 15 * 60 * 100L // 15 minutes
    }

    // Priority queue for scheduled notifications
    private val notificationQueue = PriorityBlockingQueue<ScheduledNotification>(11) { n1, n2 ->
        n1.scheduledTime.compareTo(n2.scheduledTime)
    }

    // Track notification history for throttling
    private val notificationHistory = ConcurrentHashMap<String, MutableList<Long>>()
    
    // Track last notification time for frequency control
    private val lastNotificationTime = ConcurrentHashMap<String, Long>()
    
    // Coroutines for background processing
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    
    // Recurring notifications storage
    private val recurringNotifications = ConcurrentHashMap<String, RecurringNotification>()
    
    data class ScheduledNotification(
        val id: String,
        val type: String,
        val title: String,
        val content: String,
        val scheduledTime: Long,
        val priority: Priority = Priority.NORMAL,
        val contextRequirement: ContextRequirement? = null,
        val userId: String? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) {
        enum class Priority {
            LOW, NORMAL, HIGH, CRITICAL
        }
        
        data class ContextRequirement(
            val timeOfDay: ContextAnalyzer.TimeOfDay? = null,
            val dayOfWeek: ContextAnalyzer.DayOfWeek? = null,
            val isSleepTime: Boolean? = null,
            val isWorkTime: Boolean? = null,
            val isFocusTime: Boolean? = null,
            val usageIntensity: ContextAnalyzer.UsageIntensity? = null,
            val engagementLevel: ContextAnalyzer.EngagementLevel? = null
        )
    }
    
    data class RecurringNotification(
        val id: String,
        val baseNotification: ScheduledNotification,
        val interval: Long, // in milliseconds
        val recurrenceType: RecurrenceType,
        val enabled: Boolean = true,
        val nextScheduledTime: Long = System.currentTimeMillis()
    ) {
        enum class RecurrenceType {
            MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY
        }
    }

    /**
     * Starts the notification scheduler
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Starting notification scheduler")
            
            // Start the processing loop
            scope.launch {
                processNotificationQueue()
            }
            
            // Schedule recurring notifications
            scheduleRecurringNotifications()
        }
    }

    /**
     * Stops the notification scheduler
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            Log.d(TAG, "Stopping notification scheduler")
            scope.cancel()
        }
    }

    /**
     * Schedules a notification for delivery
     */
    fun scheduleNotification(notification: ScheduledNotification): Boolean {
        if (!isEligibleForScheduling(notification)) {
            Log.d(TAG, "Notification ${notification.id} is not eligible for scheduling")
            return false
        }

        // Check if notification is recurring
        if (notification.metadata.containsKey("recurrence_type")) {
            val recurrenceType = RecurringNotification.RecurrenceType.valueOf(
                notification.metadata["recurrence_type"] as String
            )
            val interval = notification.metadata["interval"] as? Long ?: 0L
            
            val recurringNotification = RecurringNotification(
                id = notification.id,
                baseNotification = notification,
                interval = interval,
                recurrenceType = recurrenceType,
                enabled = true
            )
            
            recurringNotifications[notification.id] = recurringNotification
            Log.d(TAG, "Scheduled recurring notification: ${notification.id}")
        } else {
            notificationQueue.offer(notification)
            Log.d(TAG, "Scheduled one-time notification: ${notification.id}")
        }

        return true
    }

    /**
     * Cancels a scheduled notification
     */
    fun cancelNotification(notificationId: String): Boolean {
        // Remove from queue
        val queueCopy = notificationQueue.toList()
        val notificationToRemove = queueCopy.find { it.id == notificationId }
        
        if (notificationToRemove != null) {
            notificationQueue.remove(notificationToRemove)
        }
        
        // Remove from recurring notifications
        val recurringNotification = recurringNotifications.remove(notificationId)
        
        return notificationToRemove != null || recurringNotification != null
    }

    /**
     * Processes the notification queue and delivers notifications
     */
    private suspend fun processNotificationQueue() {
        while (isRunning.get()) {
            try {
                // Check for scheduled notifications
                val currentTime = System.currentTimeMillis()
                
                // Process all notifications that are ready to be sent
                val readyNotifications = mutableListOf<ScheduledNotification>()
                val queueCopy = notificationQueue.toList()
                
                for (notification in queueCopy) {
                    if (notification.scheduledTime <= currentTime) {
                        readyNotifications.add(notification)
                    }
                }
                
                // Remove processed notifications from queue
                for (notification in readyNotifications) {
                    notificationQueue.remove(notification)
                    
                    // Check if the notification should be delivered based on context and throttling
                    if (shouldDeliverNotification(notification)) {
                        deliverNotification(notification)
                        
                        // Track this notification for throttling
                        trackNotificationDelivery(notification)
                    } else {
                        Log.d(TAG, "Notification ${notification.id} was not delivered due to context/throttling rules")
                    }
                }
                
                // Process recurring notifications
                processRecurringNotifications(currentTime)
                
                // Small delay to prevent busy waiting
                delay(5000) // 5 seconds
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification queue", e)
                delay(10000) // 10 seconds on error
            }
        }
    }

    /**
     * Processes recurring notifications and schedules next occurrences
     */
    private fun processRecurringNotifications(currentTime: Long) {
        for ((id, recurringNotification) in recurringNotifications) {
            if (recurringNotification.enabled && recurringNotification.nextScheduledTime <= currentTime) {
                // Create a new instance of the notification with updated time
                val nextNotification = ScheduledNotification(
                    id = "${id}_${System.currentTimeMillis()}",
                    type = recurringNotification.baseNotification.type,
                    title = recurringNotification.baseNotification.title,
                    content = recurringNotification.baseNotification.content,
                    scheduledTime = currentTime,
                    priority = recurringNotification.baseNotification.priority,
                    contextRequirement = recurringNotification.baseNotification.contextRequirement,
                    userId = recurringNotification.baseNotification.userId,
                    metadata = recurringNotification.baseNotification.metadata
                )
                
                // Schedule the next occurrence
                val nextTime = calculateNextOccurrence(
                    recurringNotification.nextScheduledTime,
                    recurringNotification.recurrenceType,
                    recurringNotification.interval
                )
                
                val updatedRecurring = recurringNotification.copy(
                    nextScheduledTime = nextTime
                )
                recurringNotifications[id] = updatedRecurring
                
                // Add to queue for delivery
                notificationQueue.offer(nextNotification)
            }
        }
    }

    /**
     * Calculates the next occurrence time for a recurring notification
     */
    private fun calculateNextOccurrence(
        lastTime: Long,
        recurrenceType: RecurringNotification.RecurrenceType,
        interval: Long
    ): Long {
        return when (recurrenceType) {
            RecurringNotification.RecurrenceType.MINUTELY -> lastTime + (interval * 60 * 1000L)
            RecurringNotification.RecurrenceType.HOURLY -> lastTime + (interval * 60 * 1000L)
            RecurringNotification.RecurrenceType.DAILY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = lastTime
                calendar.add(Calendar.DAY_OF_MONTH, interval.toInt())
                calendar.timeInMillis
            }
            RecurringNotification.RecurrenceType.WEEKLY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = lastTime
                calendar.add(Calendar.WEEK_OF_YEAR, interval.toInt())
                calendar.timeInMillis
            }
            RecurringNotification.RecurrenceType.MONTHLY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = lastTime
                calendar.add(Calendar.MONTH, interval.toInt())
                calendar.timeInMillis
            }
        }
    }

    /**
     * Determines if a notification is eligible for scheduling
     */
    private fun isEligibleForScheduling(notification: ScheduledNotification): Boolean {
        // Check if notification is in the past
        if (notification.scheduledTime < System.currentTimeMillis()) {
            Log.d(TAG, "Notification ${notification.id} is in the past")
            return false
        }

        // Check context requirements if specified
        if (notification.contextRequirement != null) {
            val userContext = contextAnalyzer.analyzeContext()
            if (!matchesContextRequirement(notification.contextRequirement, userContext)) {
                Log.d(TAG, "Notification ${notification.id} does not match context requirement")
                return false
            }
        }

        return true
    }

    /**
     * Determines if a notification should be delivered based on context and throttling
     */
    private fun shouldDeliverNotification(notification: ScheduledNotification): Boolean {
        val userContext = contextAnalyzer.analyzeContext()
        val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()

        // Check context requirements
        if (notification.contextRequirement != null &&
            !matchesContextRequirement(notification.contextRequirement, userContext)) {
            Log.d(TAG, "Notification ${notification.id} does not match context requirement")
            return false
        }

        // Check if user is in sleep time and notification is not critical
        if (userContext.isSleepTime && notification.priority != ScheduledNotification.Priority.CRITICAL) {
            Log.d(TAG, "User is in sleep time, skipping non-critical notification")
            return false
        }

        // Check notification fatigue
        if (engagementMetrics.notificationFatigueScore > 0.7f) {
            Log.d(TAG, "High notification fatigue detected, skipping notification")
            return false
        }

        // Check if notification type has low preference
        val typePreference = engagementMetrics.contentPreferences.getOrDefault(notification.type, 0.5f)
        if (typePreference < 0.2f && notification.priority != ScheduledNotification.Priority.CRITICAL) {
            Log.d(TAG, "Low preference for notification type, skipping")
            return false
        }

        // Apply intelligent throttling
        if (!passesThrottling(notification)) {
            Log.d(TAG, "Notification ${notification.id} failed throttling check")
            return false
        }

        return true
    }

    /**
     * Checks if notification matches context requirements
     */
    private fun matchesContextRequirement(
        requirement: ScheduledNotification.ContextRequirement,
        context: ContextAnalyzer.UserContext
    ): Boolean {
        return (requirement.timeOfDay == null || requirement.timeOfDay == context.timeOfDay) &&
               (requirement.dayOfWeek == null || requirement.dayOfWeek == context.dayOfWeek) &&
               (requirement.isSleepTime == null || requirement.isSleepTime == context.isSleepTime) &&
               (requirement.isWorkTime == null || requirement.isWorkTime == context.isWorkTime) &&
               (requirement.isFocusTime == null || requirement.isFocusTime == context.isFocusTime) &&
               (requirement.usageIntensity == null || requirement.usageIntensity == context.usageIntensity) &&
               (requirement.engagementLevel == null || requirement.engagementLevel == context.engagementLevel)
    }

    /**
     * Checks if notification passes throttling requirements
     */
    private fun passesThrottling(notification: ScheduledNotification): Boolean {
        val now = System.currentTimeMillis()
        val type = notification.type
        val userId = notification.userId ?: "default"
        
        // Check frequency control - minimum interval between notifications of same type
        val lastTime = lastNotificationTime.getOrDefault("$userId:$type", 0L)
        if (now - lastTime < MIN_INTERVAL_BETWEEN_NOTIFICATIONS) {
            Log.d(TAG, "Too frequent - minimum interval not met for type: $type")
            return false
        }

        // Check throttle window - maximum notifications per time window
        val throttleKey = "$userId:$type:throttle"
        val history = notificationHistory.getOrPut(throttleKey) { mutableListOf() }
        
        // Remove old entries outside the throttle window
        val cutoffTime = now - DEFAULT_THROTTLE_WINDOW_MS
        history.removeAll { it < cutoffTime }
        
        if (history.size >= MAX_NOTIFICATIONS_PER_WINDOW) {
            Log.d(TAG, "Throttle limit reached for type: $type")
            return false
        }

        return true
    }

    /**
     * Delivers a notification using the notification engine
     */
    private fun deliverNotification(notification: ScheduledNotification) {
        // Determine notification channel based on type
        val channelId = when (notification.type.lowercase()) {
            "proactive_warning" -> NotificationEngine.PROACTIVE_WARNINGS_CHANNEL_ID
            "positive_reinforcement" -> NotificationEngine.POSITIVE_REINFORCEMENT_CHANNEL_ID
            "insights_analytics" -> NotificationEngine.INSIGHTS_ANALYTICS_CHANNEL_ID
            "gamification_update" -> NotificationEngine.GAMIFICATION_UPDATES_CHANNEL_ID
            "contextual_suggestion" -> NotificationEngine.CONTEXTUAL_SUGGESTIONS_CHANNEL_ID
            else -> NotificationEngine.PROACTIVE_WARNINGS_CHANNEL_ID
        }

        // Create notification using the existing engine
        // For now, we'll call the appropriate method based on type
        when (notification.type.lowercase()) {
            "proactive_warning" -> {
                // Create and send proactive warning notification
                notificationEngine.sendNotification(
                    channelId,
                    notification.title,
                    notification.content,
                    getPriorityValue(notification.priority)
                )
            }
            "positive_reinforcement" -> {
                // Create and send positive reinforcement notification
                notificationEngine.sendNotification(
                    channelId,
                    notification.title,
                    notification.content,
                    getPriorityValue(notification.priority)
                )
            }
            "insights_analytics" -> {
                // Create and send insights notification
                notificationEngine.sendNotification(
                    channelId,
                    notification.title,
                    notification.content,
                    getPriorityValue(notification.priority)
                )
            }
            "gamification_update" -> {
                // Create and send gamification notification
                notificationEngine.sendNotification(
                    channelId,
                    notification.title,
                    notification.content,
                    getPriorityValue(notification.priority)
                )
            }
            "contextual_suggestion" -> {
                // Create and send contextual suggestion notification
                notificationEngine.sendNotification(
                    channelId,
                    notification.title,
                    notification.content,
                    getPriorityValue(notification.priority)
                )
            }
            else -> {
                // Default notification
                notificationEngine.sendNotification(
                    channelId,
                    notification.title,
                    notification.content,
                    getPriorityValue(notification.priority)
                )
            }
        }

        Log.d(TAG, "Delivered notification: ${notification.id}")

        // Track engagement for this notification
        scope.launch {
            delay(5000) // Wait a bit for user to potentially interact
            // In a real implementation, we'd track if the user opened the notification
            userEngagementAnalyzer.trackNotificationResponse(
                notification.id.hashCode(),
                false // We don't know if it was opened yet
            )
        }
    }

    /**
     * Maps scheduler priority to notification priority value
     */
    private fun getPriorityValue(priority: ScheduledNotification.Priority): Int {
        return when (priority) {
            ScheduledNotification.Priority.LOW -> android.app.NotificationManager.IMPORTANCE_LOW
            ScheduledNotification.Priority.NORMAL -> android.app.NotificationManager.IMPORTANCE_DEFAULT
            ScheduledNotification.Priority.HIGH -> android.app.NotificationManager.IMPORTANCE_HIGH
            ScheduledNotification.Priority.CRITICAL -> android.app.NotificationManager.IMPORTANCE_HIGH
        }
    }

    /**
     * Tracks notification delivery for throttling purposes
     */
    private fun trackNotificationDelivery(notification: ScheduledNotification) {
        val now = System.currentTimeMillis()
        val userId = notification.userId ?: "default"
        val type = notification.type
        
        // Update last notification time
        lastNotificationTime["$userId:$type"] = now
        
        // Add to throttle history
        val throttleKey = "$userId:$type:throttle"
        val history = notificationHistory.getOrPut(throttleKey) { mutableListOf() }
        history.add(now)
    }

    /**
     * Schedules recurring notifications based on user patterns
     */
    private fun scheduleRecurringNotifications() {
        // Example: Schedule daily check-ins based on optimal timing
        val optimalTimes = userEngagementAnalyzer.identifyOptimalTiming()
        
        for ((index, hour) in optimalTimes.withIndex()) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            
            // If the time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val recurringNotification = ScheduledNotification(
                id = "daily_checkin_$index",
                type = "positive_reinforcement",
                title = "Daily Check-in",
                content = "How are you feeling about your screen time today?",
                scheduledTime = calendar.timeInMillis,
                priority = ScheduledNotification.Priority.NORMAL,
                metadata = mapOf(
                    "recurrence_type" to RecurringNotification.RecurrenceType.DAILY.name,
                    "interval" to 1L
                )
            )
            
            scheduleNotification(recurringNotification)
        }
    }

    /**
     * Gets the current status of the scheduler
     */
    fun getStatus(): SchedulerStatus {
        return SchedulerStatus(
            isRunning = isRunning.get(),
            queuedNotifications = notificationQueue.size,
            recurringNotifications = recurringNotifications.size,
            activeThrottleWindows = notificationHistory.size
        )
    }

    data class SchedulerStatus(
        val isRunning: Boolean,
        val queuedNotifications: Int,
        val recurringNotifications: Int,
        val activeThrottleWindows: Int
    )
}