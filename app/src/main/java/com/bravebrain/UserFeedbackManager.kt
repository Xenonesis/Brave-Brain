package com.bravebrain

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*

/**
 * UserFeedbackManager handles user feedback collection, analysis, and integration
 * with the notification system for adaptive learning.
 */
class UserFeedbackManager(private val context: Context) {
    companion object {
        private const val TAG = "UserFeedbackManager"
        
        // Feedback impact weights
        private const val RATING_WEIGHT = 0.6f
        private const val HELPFUL_WEIGHT = 0.4f
        private const val LEARNING_RATE = 0.1f
    }

    private val preferenceManager = NotificationPreferenceManager(context)
    private val effectivenessTracker = NotificationEffectivenessTracker(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Records user feedback for a notification and updates learning models
     */
    fun recordFeedback(
        notificationId: String,
        notificationType: String,
        rating: Int,
        helpful: Boolean,
        feedbackText: String? = null,
        actionTaken: String? = null
    ) {
        scope.launch {
            try {
                // Create feedback object
                val feedback = NotificationPreferenceManager.UserFeedback(
                    notificationId = notificationId,
                    notificationType = notificationType,
                    rating = rating.coerceIn(1, 5),
                    helpful = helpful,
                    feedbackText = feedbackText,
                    actionTaken = actionTaken
                )

                // Store feedback
                preferenceManager.addUserFeedback(feedback)

                // Update effectiveness tracker
                effectivenessTracker.trackNotificationOpened(notificationId)

                // Update learning models if enabled
                if (preferenceManager.isFeedbackLearningEnabled()) {
                    updateLearningModels(feedback)
                }

                Log.d(TAG, "Feedback recorded for notification $notificationId: rating=$rating, helpful=$helpful")

            } catch (e: Exception) {
                Log.e(TAG, "Error recording feedback", e)
            }
        }
    }

    /**
     * Updates learning models based on user feedback
     */
    private fun updateLearningModels(feedback: NotificationPreferenceManager.UserFeedback) {
        // Calculate feedback score (0.0 to 1.0)
        val ratingScore = (feedback.rating - 1) / 4.0f // Convert 1-5 to 0.0-1.0
        val helpfulScore = if (feedback.helpful) 1.0f else 0.0f
        val feedbackScore = (ratingScore * RATING_WEIGHT) + (helpfulScore * HELPFUL_WEIGHT)

        // Get current preference score
        val currentScore = preferenceManager.getUserPreferenceScore(feedback.notificationType)
        
        // Update preference score using learning rate
        val newScore = currentScore + (LEARNING_RATE * (feedbackScore - currentScore))
        preferenceManager.updateUserPreferenceScore(feedback.notificationType, newScore)

        // Handle user actions
        handleUserActions(feedback)

        Log.d(TAG, "Learning updated for ${feedback.notificationType}: $currentScore -> $newScore")
    }

    /**
     * Handles user actions from feedback (snooze, disable, etc.)
     */
    private fun handleUserActions(feedback: NotificationPreferenceManager.UserFeedback) {
        when (feedback.actionTaken?.lowercase()) {
            "snooze" -> {
                // Snooze this notification type for 24 hours
                preferenceManager.snoozeNotificationType(feedback.notificationType, 24)
                Log.d(TAG, "Notification type ${feedback.notificationType} snoozed for 24 hours")
            }
            "disable" -> {
                // Permanently disable this notification type
                preferenceManager.disableNotificationType(feedback.notificationType)
                Log.d(TAG, "Notification type ${feedback.notificationType} disabled permanently")
            }
        }
    }

    /**
     * Gets feedback statistics for a notification type
     */
    fun getFeedbackStats(notificationType: String): FeedbackStats {
        val allFeedback = preferenceManager.getUserFeedbackHistory()
        val typeFeedback = allFeedback.filter { it.notificationType == notificationType }

        return if (typeFeedback.isEmpty()) {
            FeedbackStats(
                totalFeedback = 0,
                averageRating = 0f,
                helpfulPercentage = 0f,
                totalRatings = 0
            )
        } else {
            val total = typeFeedback.size
            val averageRating = typeFeedback.map { it.rating }.average().toFloat()
            val helpfulCount = typeFeedback.count { it.helpful }
            val helpfulPercentage = (helpfulCount.toFloat() / total) * 100

            FeedbackStats(
                totalFeedback = total,
                averageRating = averageRating,
                helpfulPercentage = helpfulPercentage,
                totalRatings = typeFeedback.count { it.rating > 0 }
            )
        }
    }

    /**
     * Gets overall feedback statistics
     */
    fun getOverallFeedbackStats(): Map<String, FeedbackStats> {
        val allFeedback = preferenceManager.getUserFeedbackHistory()
        val statsByType = mutableMapOf<String, FeedbackStats>()

        // Get unique notification types
        val types = allFeedback.map { it.notificationType }.distinct()

        types.forEach { type ->
            statsByType[type] = getFeedbackStats(type)
        }

        return statsByType
    }

    /**
     * Analyzes feedback trends and suggests improvements
     */
    fun analyzeFeedbackTrends(): FeedbackAnalysis {
        val allFeedback = preferenceManager.getUserFeedbackHistory()
        
        if (allFeedback.isEmpty()) {
            return FeedbackAnalysis(
                totalFeedback = 0,
                overallRating = 0f,
                overallHelpfulPercentage = 0f,
                bestPerformingType = null,
                worstPerformingType = null,
                suggestions = emptyList()
            )
        }

        // Calculate overall stats
        val total = allFeedback.size
        val overallRating = allFeedback.map { it.rating }.average().toFloat()
        val helpfulCount = allFeedback.count { it.helpful }
        val overallHelpfulPercentage = (helpfulCount.toFloat() / total) * 100

        // Find best and worst performing types
        val statsByType = getOverallFeedbackStats()
        val bestType = statsByType.maxByOrNull { it.value.averageRating * it.value.helpfulPercentage }?.key
        val worstType = statsByType.minByOrNull { it.value.averageRating * it.value.helpfulPercentage }?.key

        // Generate suggestions
        val suggestions = generateSuggestions(statsByType)

        return FeedbackAnalysis(
            totalFeedback = total,
            overallRating = overallRating,
            overallHelpfulPercentage = overallHelpfulPercentage,
            bestPerformingType = bestType,
            worstPerformingType = worstType,
            suggestions = suggestions
        )
    }

    /**
     * Generates improvement suggestions based on feedback analysis
     */
    private fun generateSuggestions(statsByType: Map<String, FeedbackStats>): List<String> {
        val suggestions = mutableListOf<String>()

        statsByType.forEach { (type, stats) ->
            if (stats.averageRating < 3.0f && stats.helpfulPercentage < 50f) {
                suggestions.add("Consider improving $type notifications (rating: ${stats.averageRating}, helpful: ${stats.helpfulPercentage}%)")
            }
            
            if (stats.totalFeedback < 5) {
                suggestions.add("Need more feedback for $type notifications (only ${stats.totalFeedback} responses)")
            }
        }

        // Add general suggestions
        if (suggestions.isEmpty()) {
            suggestions.add("Feedback looks good! Keep collecting user responses.")
        }

        return suggestions
    }

    /**
     * Clears all feedback data (for testing or reset purposes)
     */
    fun clearAllFeedback() {
        preferenceManager.getAllPreferences().keys
            .filter { it.startsWith("user_feedback") || it.contains("preference_score") }
            .forEach { key ->
                preferenceManager.getAllPreferences()[key]?.let {
                    // This would need proper implementation to clear specific keys
                    // For now, we'll rely on the resetToDefaults in preference manager
                }
            }
    }

    /**
     * Checks if a notification type should be sent based on user preferences and feedback
     */
    fun shouldSendNotificationBasedOnFeedback(type: String): Boolean {
        if (!preferenceManager.shouldSendNotification(type)) {
            return false
        }

        // Check if this type has poor feedback and learning adaptation is enabled
        if (preferenceManager.isLearningAdaptationEnabled()) {
            val preferenceScore = preferenceManager.getUserPreferenceScore(type)
            if (preferenceScore < 0.3f) {
                Log.d(TAG, "Notification type $type has low preference score: $preferenceScore")
                return false
            }
        }

        return true
    }

    // Data classes for feedback analysis
    data class FeedbackStats(
        val totalFeedback: Int,
        val averageRating: Float,
        val helpfulPercentage: Float,
        val totalRatings: Int
    )

    data class FeedbackAnalysis(
        val totalFeedback: Int,
        val overallRating: Float,
        val overallHelpfulPercentage: Float,
        val bestPerformingType: String?,
        val worstPerformingType: String?,
        val suggestions: List<String>
    )
}