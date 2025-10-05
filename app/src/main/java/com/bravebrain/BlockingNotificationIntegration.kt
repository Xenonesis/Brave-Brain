package com.bravebrain

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Integrates notification system with blocking features
 * Handles pre-blocking warnings, post-blocking encouragement, and repeated access notifications
 */
class BlockingNotificationIntegration(
    private val context: Context,
    private val notificationManager: AdvancedNotificationManager
) {
    companion object {
        private const val TAG = "BlockingNotification"
        private const val TYPE_PRE_BLOCKING_WARNING = "pre_blocking_warning"
        private const val TYPE_POST_BLOCKING_ENCOURAGEMENT = "post_blocking_encouragement"
        private const val TYPE_REPEATED_ACCESS_ATTEMPT = "repeated_access_attempt"
        private const val TYPE_BLOCKING_RECOVERY_SUGGESTION = "blocking_recovery_suggestion"
    }

    /**
     * Sends pre-blocking warning notification 30 seconds before enforced block
     */
    fun sendPreBlockingWarning(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int,
        secondsUntilBlock: Int = 30
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "seconds_until_block" to secondsUntilBlock,
                    "alternative_activities" to getAlternativeActivities()
                )

                val title = "⏰ $appName will be blocked soon!"
                val content = "Your $secondsUntilBlock second break from $appName is starting. Consider trying an alternative activity."

                notificationManager.scheduleNotification(
                    type = TYPE_PRE_BLOCKING_WARNING,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Pre-blocking warning notification sent for $packageName, $secondsUntilBlock seconds until block")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending pre-blocking warning notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends post-blocking encouragement notification after app is blocked
     */
    fun sendPostBlockingEncouragement(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int,
        reason: String = "Time limit reached"
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "reason" to reason,
                    "alternative_activities" to getAlternativeActivities()
                )

                val title = "✅ Good job taking a break from $appName!"
                val content = "You've reached your limit for $appName. This break helps you maintain healthy screen time habits. Try one of these activities instead."

                notificationManager.scheduleNotification(
                    type = TYPE_POST_BLOCKING_ENCOURAGEMENT,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                // Also send recovery suggestion
                sendBlockingRecoverySuggestion(packageName, currentUsage, timeLimit)
                
                Log.d(TAG, "Post-blocking encouragement notification sent for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending post-blocking encouragement notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends notification when user attempts to access a blocked app repeatedly
     */
    fun sendRepeatedAccessNotification(
        packageName: String,
        accessAttempts: Int = 1
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val additionalData = mapOf(
                    "app_name" to appName,
                    "access_attempts" to accessAttempts,
                    "alternative_activities" to getAlternativeActivities()
                )

                val title = "🔄 $appName is still blocked"
                val content = "You've tried to access $appName $accessAttempts times. Remember your commitment to healthy screen time. Try one of these alternatives instead."

                notificationManager.scheduleNotification(
                    type = TYPE_REPEATED_ACCESS_ATTEMPT,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Repeated access notification sent for $packageName, attempt #$accessAttempts")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending repeated access notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends recovery suggestion after blocking occurs
     */
    private fun sendBlockingRecoverySuggestion(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val recoveryActivities = getRecoveryActivities()
                val suggestedActivity = recoveryActivities.random()
                
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "suggested_recovery_activity" to suggestedActivity,
                    "recovery_activities" to recoveryActivities
                )

                val title = "💡 Take care of yourself"
                val content = "After using $appName for $currentUsage minutes, try this: $suggestedActivity"

                notificationManager.scheduleNotification(
                    type = TYPE_BLOCKING_RECOVERY_SUGGESTION,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Blocking recovery suggestion sent for $packageName: $suggestedActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending blocking recovery suggestion: ${e.message}", e)
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
     * Gets a list of recovery activities to suggest after blocking
     */
    private fun getRecoveryActivities(): List<String> {
        return listOf(
            "Take 5 deep breaths",
            "Do 10 push-ups or squats",
            "Step outside for fresh air",
            "Drink a glass of water",
            "Look out the window for 30 seconds",
            "Do a quick meditation",
            "Write down 3 things you're grateful for",
            "Do some light stretching",
            "Listen to calming music",
            "Practice a quick mindfulness exercise",
            "Make a healthy snack",
            "Tidy your workspace",
            "Do a quick decluttering task",
            "Practice progressive muscle relaxation",
            "Do a 2-minute breathing exercise"
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