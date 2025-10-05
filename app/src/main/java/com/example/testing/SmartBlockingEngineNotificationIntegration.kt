package com.example.testing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Integrates notification system with SmartBlockingEngine
 * Handles notifications for progressive blocking, adaptive blocking, and other smart blocking strategies
 */
class SmartBlockingEngineNotificationIntegration(
    private val context: Context,
    private val notificationManager: AdvancedNotificationManager
) {
    companion object {
        private const val TAG = "SmartBlockingNotification"
        private const val TYPE_PROGRESSIVE_BLOCKING_WARNING = "progressive_blocking_warning"
        private const val TYPE_ADAPTIVE_BLOCKING_WARNING = "adaptive_blocking_warning"
        private const val TYPE_STRICT_BLOCKING_WARNING = "strict_blocking_warning"
        private const val TYPE_BLOCKING_CHALLENGE_SUGGESTION = "blocking_challenge_suggestion"
        private const val TYPE_VIOLATION_RECOVERY_SUGGESTION = "violation_recovery_suggestion"
    }

    /**
     * Sends notification when progressive blocking detects a violation
     */
    fun sendProgressiveBlockingNotification(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int,
        violationCount: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "violation_count" to violationCount,
                    "alternative_activities" to getAlternativeActivities()
                )

                val title = "⚠️ $appName progressive limit reached"
                val content = "This is violation #${violationCount + 1} for $appName. Your limit has been adjusted. Consider taking a break."

                notificationManager.scheduleNotification(
                    type = TYPE_PROGRESSIVE_BLOCKING_WARNING,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Progressive blocking notification sent for $packageName, violation #${violationCount + 1}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending progressive blocking notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends notification when adaptive blocking is triggered
     */
    fun sendAdaptiveBlockingNotification(
        packageName: String,
        currentUsage: Int,
        timeLimit: Int,
        userPattern: SmartBlockingEngine.UserPattern
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val additionalData = mapOf(
                    "app_name" to appName,
                    "current_usage" to currentUsage,
                    "time_limit" to timeLimit,
                    "user_pattern" to userPattern,
                    "alternative_activities" to getAlternativeActivities()
                )

                val title = "💡 $appName adaptive limit reached"
                val content = "Based on your usage patterns, $appName has been blocked. This helps maintain healthy habits during your typical high-usage periods."

                notificationManager.scheduleNotification(
                    type = TYPE_ADAPTIVE_BLOCKING_WARNING,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Adaptive blocking notification sent for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending adaptive blocking notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends notification when strict blocking is triggered
     */
    fun sendStrictBlockingNotification(
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

                val title = "🔒 $appName blocked (strict mode)"
                val content = "Strict mode activated for $appName. Your time limit has been exceeded with minimal overtime allowed."

                notificationManager.scheduleNotification(
                    type = TYPE_STRICT_BLOCKING_WARNING,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Strict blocking notification sent for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending strict blocking notification: ${e.message}", e)
            }
        }
    }

    /**
     * Sends challenge suggestion when blocking occurs
     */
    fun sendBlockingChallengeSuggestion(
        packageName: String,
        challengeType: SmartBlockingEngine.ChallengeType,
        coolingOffPeriod: Long
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val challengeDescription = when (challengeType) {
                    SmartBlockingEngine.ChallengeType.MATH -> "Complete a math challenge"
                    SmartBlockingEngine.ChallengeType.COMPLEX_MATH -> "Solve a complex math problem"
                    SmartBlockingEngine.ChallengeType.REFLECTION -> "Answer a reflection question"
                    SmartBlockingEngine.ChallengeType.MINDFULNESS -> "Practice mindfulness exercise"
                    SmartBlockingEngine.ChallengeType.PHYSICAL -> "Do a physical activity"
                    SmartBlockingEngine.ChallengeType.PRODUCTIVITY_QUESTION -> "Answer a productivity question"
                    SmartBlockingEngine.ChallengeType.WAITING -> "Wait for the cooldown period"
                    SmartBlockingEngine.ChallengeType.NONE -> "No challenge required"
                }
                
                val additionalData = mapOf(
                    "app_name" to appName,
                    "challenge_type" to challengeType.name,
                    "challenge_description" to challengeDescription,
                    "cooling_off_period" to coolingOffPeriod,
                    "alternative_activities" to getAlternativeActivities()
                )

                val title = "🎯 $challengeDescription"
                val content = "Complete this challenge to access $appName again: $challengeDescription. Cooling off period: ${coolingOffPeriod / 100 / 60} minutes."

                notificationManager.scheduleNotification(
                    type = TYPE_BLOCKING_CHALLENGE_SUGGESTION,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Blocking challenge suggestion sent for $packageName: $challengeType")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending blocking challenge suggestion: ${e.message}", e)
            }
        }
    }

    /**
     * Sends recovery suggestion after violation
     */
    fun sendViolationRecoverySuggestion(
        packageName: String,
        violationCount: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appName = getAppName(packageName)
                val recoveryActivities = getRecoveryActivities()
                val suggestedActivity = recoveryActivities.random()
                
                val additionalData = mapOf(
                    "app_name" to appName,
                    "violation_count" to violationCount,
                    "suggested_recovery_activity" to suggestedActivity,
                    "recovery_activities" to recoveryActivities
                )

                val title = "💡 Recovery suggestion after violation"
                val content = "After violation #${violationCount + 1} for $appName, try this: $suggestedActivity"

                notificationManager.scheduleNotification(
                    type = TYPE_VIOLATION_RECOVERY_SUGGESTION,
                    title = title,
                    content = content,
                    additionalData = additionalData
                )
                
                Log.d(TAG, "Violation recovery suggestion sent for $packageName: $suggestedActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending violation recovery suggestion: ${e.message}", e)
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