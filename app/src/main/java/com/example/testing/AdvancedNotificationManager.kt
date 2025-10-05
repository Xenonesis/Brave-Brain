package com.example.testing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * AdvancedNotificationManager integrates all notification components:
 * - NotificationScheduler
 * - NotificationSchedulingAlgorithms
 * - NotificationContentGenerator
 * - NotificationEffectivenessTracker
 */
class AdvancedNotificationManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "AdvancedNotificationMgr"
    }

    private val contextAnalyzer = ContextAnalyzer(context)
    private val userEngagementAnalyzer = UserEngagementAnalyzer(context)
    private val notificationEngine = NotificationEngine(context)
    private val preferenceManager = NotificationPreferenceManager(context)
    private val feedbackManager = UserFeedbackManager(context)
    
    private val scheduler = NotificationScheduler(
        context = context,
        notificationEngine = notificationEngine,
        contextAnalyzer = contextAnalyzer,
        userEngagementAnalyzer = userEngagementAnalyzer
    )
    
    private val schedulingAlgorithms = NotificationSchedulingAlgorithms(
        context = context,
        contextAnalyzer = contextAnalyzer,
        userEngagementAnalyzer = userEngagementAnalyzer
    )
    
    private val contentGenerator = NotificationContentGenerator(
        context = context,
        contextAnalyzer = contextAnalyzer,
        userEngagementAnalyzer = userEngagementAnalyzer
    )
    
    private val effectivenessTracker = NotificationEffectivenessTracker(context)

    /**
     * Starts the advanced notification system
     */
    fun start() {
        Log.d(TAG, "Starting advanced notification system")
        
        // Create notification channels
        notificationEngine.createNotificationChannels()
        
        // Start the scheduler
        scheduler.start()
        
        // Start periodic effectiveness analysis
        startEffectivenessAnalysis()
    }

    /**
     * Stops the advanced notification system
     */
    fun stop() {
        Log.d(TAG, "Stopping advanced notification system")
        
        scheduler.stop()
    }

    /**
     * Schedules a notification with intelligent processing
     */
    suspend fun scheduleNotification(
        type: String,
        title: String? = null,
        content: String? = null,
        additionalData: Map<String, Any> = emptyMap(),
        priority: NotificationScheduler.ScheduledNotification.Priority = NotificationScheduler.ScheduledNotification.Priority.NORMAL,
        contextRequirement: NotificationScheduler.ScheduledNotification.ContextRequirement? = null,
        userId: String? = null
    ): Boolean {
        // Check if notification is allowed based on user preferences and feedback
        if (!preferenceManager.shouldSendNotification(type)) {
            Log.d(TAG, "Notification of type $type is not allowed due to user preferences")
            return false
        }

        // Check if notification is allowed based on feedback learning
        if (!feedbackManager.shouldSendNotificationBasedOnFeedback(type)) {
            Log.d(TAG, "Notification of type $type is not allowed due to feedback analysis")
            return false
        }

        // Check if notification is allowed based on frequency controls
        if (!effectivenessTracker.isNotificationAllowed(type, userId ?: "default")) {
            Log.d(TAG, "Notification of type $type is not allowed due to frequency controls")
            return false
        }

        // Generate content if not provided
        val (generatedTitle, generatedContent) = if (title != null && content != null) {
            Pair(title, content)
        } else {
            contentGenerator.generateNotificationContent(type, null, additionalData)
        }

        // Adapt content to user style
        val userContext = contextAnalyzer.analyzeContext()
        val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()
        val (adaptedTitle, adaptedContent) = contentGenerator.adaptContentToUserStyle(
            generatedTitle, 
            generatedContent, 
            userContext, 
            engagementMetrics
        )

        // Determine optimal time using scheduling algorithms
        val optimalTime = schedulingAlgorithms.calculateOptimalNotificationTime(
            type, 
            System.currentTimeMillis(), 
            contextRequirement
        )

        // Determine priority based on context and engagement
        val calculatedPriority = if (priority == NotificationScheduler.ScheduledNotification.Priority.NORMAL) {
            schedulingAlgorithms.determineNotificationPriority(type, userContext, engagementMetrics)
        } else {
            priority
        }

        // Create scheduled notification
        val scheduledNotification = NotificationScheduler.ScheduledNotification(
            id = "notif_${System.currentTimeMillis()}_${type.hashCode()}",
            type = type,
            title = adaptedTitle,
            content = adaptedContent,
            scheduledTime = optimalTime,
            priority = calculatedPriority,
            contextRequirement = contextRequirement,
            userId = userId,
            metadata = additionalData
        )

        // Track that notification is being scheduled
        effectivenessTracker.trackNotificationSent(scheduledNotification.id, type)

        // Schedule the notification
        return scheduler.scheduleNotification(scheduledNotification)
    }

    /**
     * Schedules a recurring notification
     */
    suspend fun scheduleRecurringNotification(
        type: String,
        interval: Long,
        recurrenceType: NotificationScheduler.RecurringNotification.RecurrenceType,
        title: String? = null,
        content: String? = null,
        additionalData: Map<String, Any> = emptyMap(),
        priority: NotificationScheduler.ScheduledNotification.Priority = NotificationScheduler.ScheduledNotification.Priority.NORMAL,
        contextRequirement: NotificationScheduler.ScheduledNotification.ContextRequirement? = null,
        userId: String? = null
    ): Boolean {
        val enrichedData = additionalData.toMutableMap().apply {
            put("recurrence_type", recurrenceType.name)
            put("interval", interval)
        }

        return scheduleNotification(
            type = type,
            title = title,
            content = content,
            additionalData = enrichedData,
            priority = priority,
            contextRequirement = contextRequirement,
            userId = userId
        )
    }

    /**
     * Cancels a scheduled notification
     */
    fun cancelNotification(notificationId: String): Boolean {
        return scheduler.cancelNotification(notificationId)
    }

    /**
     * Records when a notification was opened by the user
     */
    fun recordNotificationOpened(notificationId: String) {
        effectivenessTracker.trackNotificationOpened(notificationId)
        
        // Update engagement analyzer as well
        userEngagementAnalyzer.trackNotificationResponse(notificationId.hashCode(), true)
    }

    /**
     * Records user feedback for a notification
     */
    fun recordUserFeedback(
        notificationId: String,
        notificationType: String,
        rating: Int,
        helpful: Boolean,
        feedbackText: String? = null,
        actionTaken: String? = null
    ) {
        feedbackManager.recordFeedback(
            notificationId,
            notificationType,
            rating,
            helpful,
            feedbackText,
            actionTaken
        )
    }

    /**
     * Gets feedback statistics for analysis
     */
    fun getFeedbackStats(notificationType: String): UserFeedbackManager.FeedbackStats {
        return feedbackManager.getFeedbackStats(notificationType)
    }

    /**
     * Gets overall feedback analysis
     */
    fun getFeedbackAnalysis(): UserFeedbackManager.FeedbackAnalysis {
        return feedbackManager.analyzeFeedbackTrends()
    }

    /**
     * Checks if a notification type is enabled by user preferences
     */
    fun isNotificationTypeEnabled(type: String): Boolean {
        return preferenceManager.isNotificationTypeEnabled(type)
    }

    /**
     * Snoozes a notification type for a specified duration
     */
    fun snoozeNotificationType(type: String, durationHours: Int) {
        preferenceManager.snoozeNotificationType(type, durationHours)
    }

    /**
     * Disables a notification type permanently
     */
    fun disableNotificationType(type: String) {
        preferenceManager.disableNotificationType(type)
    }

    /**
     * Enables a previously disabled notification type
     */
    fun enableNotificationType(type: String) {
        preferenceManager.enableNotificationType(type)
    }

    /**
     * Gets effectiveness metrics for a notification type
     */
    fun getEffectivenessMetrics(notificationType: String): NotificationEffectivenessTracker.EffectivenessMetrics {
        return effectivenessTracker.getEffectivenessMetrics(notificationType)
    }

    /**
     * Starts periodic effectiveness analysis
     */
    private fun startEffectivenessAnalysis() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    // Adjust frequency limits based on effectiveness
                    effectivenessTracker.adjustFrequencyLimitsBasedOnEffectiveness()
                    
                    // Allow other coroutines to run
                    delay(60 * 60 * 1000) // Run every hour
                } catch (e: Exception) {
                    Log.e(TAG, "Error in effectiveness analysis", e)
                    delay(10 * 60 * 1000) // Retry after 10 minutes on error
                }
            }
        }
    }

    /**
     * Gets the current status of the notification system
     */
    fun getStatus(): NotificationSystemStatus {
        return NotificationSystemStatus(
            schedulerStatus = scheduler.getStatus(),
            effectivenessMetrics = effectivenessTracker.getAllNotificationStats().mapValues { 
                effectivenessTracker.getEffectivenessMetrics(it.key) 
            }
        )
    }

    data class NotificationSystemStatus(
        val schedulerStatus: NotificationScheduler.SchedulerStatus,
        val effectivenessMetrics: Map<String, NotificationEffectivenessTracker.EffectivenessMetrics>
    )
}