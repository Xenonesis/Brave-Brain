package com.example.testing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Integrates notification system with time limit features
 * Handles progressive warnings and positive reinforcement
 */
class TimeLimitNotificationIntegration(
    private val context: Context,
    private val notificationManager: AdvancedNotificationManager
) {
    companion object {
        private const val TAG = "TimeLimitNotification"
        private const val TYPE_PROGRESSIVE_WARNING = "progressive_warning"
        private const val TYPE_POSITIVE_REINFORCEMENT = "positive_reinforcement"
        private const val TYPE_TIME_LIMIT_REACHED = "time_limit_reached"
        private const val TYPE_ALTERNATIVE_SUGGESTION = "alternative_suggestion"
    }

    /**
     * Checks app usage and sends appropriate notifications based on time limits
     */
    fun checkAndSendTimeLimitNotifications(packageName: String, currentUsageMinutes: Int, timeLimitMinutes: Int) {
        val percentage = if (timeLimitMinutes > 0) {
            (currentUsageMinutes.toFloat() / timeLimitMinutes.toFloat()) * 100
        } else 0f

        when {
            // 80% threshold warning
            percentage >= 80 && percentage < 90 && currentUsageMinutes % 5 == 0 -> {
                sendProgressiveWarningNotification(packageName, currentUsageMinutes, timeLimitMinutes, 80)
            }
            // 90% threshold warning
            percentage >= 90 && percentage < 100 && currentUsageMinutes % 5 == 0 -> {
                sendProgressiveWarningNotification(packageName, currentUsageMinutes, timeLimitMinutes, 90)
            }
            // Time limit reached
            percentage >= 10 -> {
                sendTimeLimitReachedNotification(packageName, currentUsageMinutes, timeLimitMinutes)
            }
            // Positive reinforcement when staying within limits (for apps that were previously over-limit)
            else -> {
                checkPositiveReinforcement(packageName, currentUsageMinutes, timeLimitMinutes)
            }
        }
    }

    /**
     * Sends progressive warning notifications at 80% and 90% thresholds
     */
    private fun sendProgressiveWarningNotification(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int,
        threshold: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "threshold" to threshold,
                    "usage_percentage" to ((currentUsage.toFloat() / timeLimit.toFloat()) * 100).toInt()
                )

                val title = when (threshold) {
                    80 -> "⚠️ $appName approaching limit"
                    90 -> "⚠️ $appName nearly at limit!"
                    else -> "⚠️ $appName usage warning"
                }

                val content = when (threshold) {
                    80 -> "You've used $appName for $currentUsage of $timeLimit minutes (${(currentUsage.toFloat() / timeLimit.toFloat()) * 100}%)"
                    90 -> "Approaching your limit! $appName usage: $currentUsage of $timeLimit minutes (${(currentUsage.toFloat() / timeLimit.toFloat()) * 100}%)"
                    else -> "Time limit approaching for $appName"
                }

                notificationManager.scheduleNotification(
                    type = TYPE_PROGRESSIVE_WARNING,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Progressive warning notification sent for $packageName at $threshold% threshold")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending progressive warning notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends notification when time limit is reached
     */
    private fun sendTimeLimitReachedNotification(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "alternative_activities" to getAlternativeActivities()
                )

                val title = "⏰ $appName time limit reached!"
                val content = "You've reached your daily limit of $timeLimit minutes for $appName. Consider taking a break or trying an alternative activity."

                notificationManager.scheduleNotification(
                    type = TYPE_TIME_LIMIT_REACHED,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                // Also send alternative activity suggestion
                sendAlternativeActivitySuggestion(packageName, currentUsage, timeLimit)
                
                Log.d(TAG, "Time limit reached notification sent for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending time limit reached notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends positive reinforcement when users stay within limits
     */
    private fun checkPositiveReinforcement(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int
    ) {
        // Only send positive reinforcement if usage is significantly below limit
        val percentage = if (timeLimit > 0) {
            (currentUsage.toFloat() / timeLimit.toFloat()) * 100
        } else 0f

        if (percentage <= 50 && currentUsage % 10 == 0) { // Every 10 minutes when under 50% of limit
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appName = getAppName(packageName)
                    val additionalData = mapOf(
                        "app_name" to appName,
                        "current_usage" to currentUsage,
                        "time_limit" to timeLimit,
                        "usage_percentage" to percentage.toInt()
                    )

                    val title = "👍 Great job with $appName!"
                    val content = "You're managing your $appName usage well - only $currentUsage of $timeLimit minutes used today!"

                    notificationManager.scheduleNotification(
                        type = TYPE_POSITIVE_REINFORCEMENT,
                        title = title,
                        content = content,
                        additionalData = additionalData
                    )
                    
                    Log.d(TAG, "Positive reinforcement notification sent for $packageName")
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending positive reinforcement notification: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Sends alternative activity suggestions when limits are reached
     */
    private fun sendAlternativeActivitySuggestion(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val alternativeActivities = getAlternativeActivities()
                val suggestedActivity = alternativeActivities.random()
                
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "suggested_activity" to suggestedActivity,
                    "alternative_activities" to alternativeActivities
                )

                val title = "💡 Try something different"
                val content = "Since you've reached your limit for $appName, how about trying: $suggestedActivity"

                notificationManager.scheduleNotification(
                    type = TYPE_ALTERNATIVE_SUGGESTION,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Alternative activity suggestion sent for $packageName: $suggestedActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending alternative activity suggestion: ${e.message}", e)
            }
        }
    }

    /**
     * Gets a list of alternative activities to suggest
     */
    private fun getAlternativeActivities(): List<String> {
        return listOf(
            "Go for a walk outside",
            "Do some stretching exercises",
            "Read a book",
            "Call a friend or family member",
            "Practice meditation or mindfulness",
            "Do a quick workout or yoga",
            "Listen to music or a podcast",
            "Write in a journal",
            "Do a puzzle or brain game",
            "Cook a healthy meal",
            "Tidy up your space",
            "Work on a hobby",
            "Learn something new online",
            "Take a power nap",
            "Practice a musical instrument"
        )
    }

    /**
     * Gets app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}