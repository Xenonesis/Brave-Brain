package com.example.testing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class NotificationPreferenceManager(context: Context) {
    companion object {
        private const val TAG = "NotificationPrefMgr"
        private const val PREF_NAME = "notification_preferences"
        
        // Preference keys
        private const val PROACTIVE_WARNINGS_ENABLED = "proactive_warnings_enabled"
        private const val POSITIVE_REINFORCEMENT_ENABLED = "positive_reinforcement_enabled"
        private const val INSIGHTS_ANALYTICS_ENABLED = "insights_analytics_enabled"
        private const val GAMIFICATION_UPDATES_ENABLED = "gamification_updates_enabled"
        private const val CONTEXTUAL_SUGGESTIONS_ENABLED = "contextual_suggestions_enabled"
        private const val NOTIFICATION_FREQUENCY = "notification_frequency"
        private const val SILENT_HOURS_START = "silent_hours_start"
        private const val SILENT_HOURS_END = "silent_hours_end"
        private const val SILENT_HOURS_ENABLED = "silent_hours_enabled"
        
        // New preference keys for enhanced features
        private const val PREFERRED_NOTIFICATION_WINDOW_START = "preferred_window_start"
        private const val PREFERRED_NOTIFICATION_WINDOW_END = "preferred_window_end"
        private const val LEARNING_ADAPTATION_ENABLED = "learning_adaptation_enabled"
        private const val FEEDBACK_LEARNING_ENABLED = "feedback_learning_enabled"
        private const val MESSAGE_CATEGORIES = "message_categories"
        private const val USER_FEEDBACK_HISTORY = "user_feedback_history"
        private const val SNOOZED_NOTIFICATIONS = "snoozed_notifications"
        private const val DISABLED_NOTIFICATION_TYPES = "disabled_notification_types"
        private const val USER_PREFERENCE_SCORE = "user_preference_score"
    }

    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Proactive Warnings
    fun isProactiveWarningsEnabled(): Boolean {
        return sharedPreferences.getBoolean(PROACTIVE_WARNINGS_ENABLED, true)
    }

    fun setProactiveWarningsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(PROACTIVE_WARNINGS_ENABLED, enabled).apply()
    }

    // Positive Reinforcement
    fun isPositiveReinforcementEnabled(): Boolean {
        return sharedPreferences.getBoolean(POSITIVE_REINFORCEMENT_ENABLED, true)
    }

    fun setPositiveReinforcementEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(POSITIVE_REINFORCEMENT_ENABLED, enabled).apply()
    }

    // Insights & Analytics
    fun isInsightsAnalyticsEnabled(): Boolean {
        return sharedPreferences.getBoolean(INSIGHTS_ANALYTICS_ENABLED, true)
    }

    fun setInsightsAnalyticsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(INSIGHTS_ANALYTICS_ENABLED, enabled).apply()
    }

    // Gamification Updates
    fun isGamificationUpdatesEnabled(): Boolean {
        return sharedPreferences.getBoolean(GAMIFICATION_UPDATES_ENABLED, true)
    }

    fun setGamificationUpdatesEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(GAMIFICATION_UPDATES_ENABLED, enabled).apply()
    }

    // Contextual Suggestions
    fun isContextualSuggestionsEnabled(): Boolean {
        return sharedPreferences.getBoolean(CONTEXTUAL_SUGGESTIONS_ENABLED, true)
    }

    fun setContextualSuggestionsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(CONTEXTUAL_SUGGESTIONS_ENABLED, enabled).apply()
    }

    // Notification Frequency
    fun getNotificationFrequency(): Int {
        return sharedPreferences.getInt(NOTIFICATION_FREQUENCY, 15) // Default to 15 minutes
    }

    fun setNotificationFrequency(minutes: Int) {
        sharedPreferences.edit().putInt(NOTIFICATION_FREQUENCY, minutes).apply()
    }

    // Silent Hours - Start Time (in 24-hour format, e.g., 22 for 10 PM)
    fun getSilentHoursStart(): Int {
        return sharedPreferences.getInt(SILENT_HOURS_START, 2) // Default to 10 PM
    }

    fun setSilentHoursStart(hour: Int) {
        sharedPreferences.edit().putInt(SILENT_HOURS_START, hour).apply()
    }

    // Silent Hours - End Time (in 24-hour format, e.g., 7 for 7 AM)
    fun getSilentHoursEnd(): Int {
        return sharedPreferences.getInt(SILENT_HOURS_END, 7) // Default to 7 AM
    }

    fun setSilentHoursEnd(hour: Int) {
        sharedPreferences.edit().putInt(SILENT_HOURS_END, hour).apply()
    }

    // Silent Hours Enabled
    fun isSilentHoursEnabled(): Boolean {
        return sharedPreferences.getBoolean(SILENT_HOURS_ENABLED, false)
    }

    fun setSilentHoursEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(SILENT_HOURS_ENABLED, enabled).apply()
    }

    // Preferred Notification Window - Start Time (in 24-hour format)
    fun getPreferredNotificationWindowStart(): Int {
        return sharedPreferences.getInt(PREFERRED_NOTIFICATION_WINDOW_START, 9) // Default to 9 AM
    }

    fun setPreferredNotificationWindowStart(hour: Int) {
        sharedPreferences.edit().putInt(PREFERRED_NOTIFICATION_WINDOW_START, hour).apply()
    }

    // Preferred Notification Window - End Time (in 24-hour format)
    fun getPreferredNotificationWindowEnd(): Int {
        return sharedPreferences.getInt(PREFERRED_NOTIFICATION_WINDOW_END, 21) // Default to 9 PM
    }

    fun setPreferredNotificationWindowEnd(hour: Int) {
        sharedPreferences.edit().putInt(PREFERRED_NOTIFICATION_WINDOW_END, hour).apply()
    }

    // Learning Adaptation
    fun isLearningAdaptationEnabled(): Boolean {
        return sharedPreferences.getBoolean(LEARNING_ADAPTATION_ENABLED, true)
    }

    fun setLearningAdaptationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(LEARNING_ADAPTATION_ENABLED, enabled).apply()
    }

    // Feedback Learning
    fun isFeedbackLearningEnabled(): Boolean {
        return sharedPreferences.getBoolean(FEEDBACK_LEARNING_ENABLED, true)
    }

    fun setFeedbackLearningEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(FEEDBACK_LEARNING_ENABLED, enabled).apply()
    }

    // Message Categories
    fun getMessageCategories(): Set<String> {
        val categoriesJson = sharedPreferences.getString(MESSAGE_CATEGORIES, "[]") ?: "[]"
        return try {
            Gson().fromJson(categoriesJson, object : TypeToken<Set<String>>() {}.type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message categories", e)
            setOf("motivational", "informative", "warning", "suggestion", "achievement")
        }
    }

    fun setMessageCategories(categories: Set<String>) {
        val categoriesJson = Gson().toJson(categories)
        sharedPreferences.edit().putString(MESSAGE_CATEGORIES, categoriesJson).apply()
    }

    // User Feedback History
    fun addUserFeedback(feedback: UserFeedback) {
        val feedbackList = getUserFeedbackHistory().toMutableList()
        feedbackList.add(feedback)
        val feedbackJson = Gson().toJson(feedbackList)
        sharedPreferences.edit().putString(USER_FEEDBACK_HISTORY, feedbackJson).apply()
    }

    fun getUserFeedbackHistory(): List<UserFeedback> {
        val feedbackJson = sharedPreferences.getString(USER_FEEDBACK_HISTORY, "[]") ?: "[]"
        return try {
            Gson().fromJson(feedbackJson, object : TypeToken<List<UserFeedback>>() {}.type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user feedback history", e)
            emptyList()
        }
    }

    // Snoozed Notifications
    fun snoozeNotificationType(type: String, durationHours: Int) {
        val snoozedMap = getSnoozedNotifications().toMutableMap()
        val snoozeUntil = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000L)
        snoozedMap[type] = snoozeUntil
        val snoozedJson = Gson().toJson(snoozedMap)
        sharedPreferences.edit().putString(SNOOZED_NOTIFICATIONS, snoozedJson).apply()
    }

    fun getSnoozedNotifications(): Map<String, Long> {
        val snoozedJson = sharedPreferences.getString(SNOOZED_NOTIFICATIONS, "{}") ?: "{}"
        return try {
            Gson().fromJson(snoozedJson, object : TypeToken<Map<String, Long>>() {}.type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing snoozed notifications", e)
            emptyMap()
        }
    }

    fun isNotificationTypeSnoozed(type: String): Boolean {
        val snoozedUntil = getSnoozedNotifications()[type] ?: return false
        return System.currentTimeMillis() < snoozedUntil
    }

    // Disabled Notification Types
    fun disableNotificationType(type: String) {
        val disabledTypes = getDisabledNotificationTypes().toMutableSet()
        disabledTypes.add(type)
        val disabledJson = Gson().toJson(disabledTypes)
        sharedPreferences.edit().putString(DISABLED_NOTIFICATION_TYPES, disabledJson).apply()
    }

    fun enableNotificationType(type: String) {
        val disabledTypes = getDisabledNotificationTypes().toMutableSet()
        disabledTypes.remove(type)
        val disabledJson = Gson().toJson(disabledTypes)
        sharedPreferences.edit().putString(DISABLED_NOTIFICATION_TYPES, disabledJson).apply()
    }

    fun getDisabledNotificationTypes(): Set<String> {
        val disabledJson = sharedPreferences.getString(DISABLED_NOTIFICATION_TYPES, "[]") ?: "[]"
        return try {
            Gson().fromJson(disabledJson, object : TypeToken<Set<String>>() {}.type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing disabled notification types", e)
            emptySet()
        }
    }

    fun isNotificationTypeEnabled(type: String): Boolean {
        return !getDisabledNotificationTypes().contains(type)
    }

    // User Preference Score (for learning adaptation)
    fun getUserPreferenceScore(type: String): Float {
        return sharedPreferences.getFloat("${USER_PREFERENCE_SCORE}_$type", 0.5f)
    }

    fun updateUserPreferenceScore(type: String, score: Float) {
        sharedPreferences.edit().putFloat("${USER_PREFERENCE_SCORE}_$type", score.coerceIn(0f, 1f)).apply()
    }

    // Check if notification should be sent based on all preferences
    fun shouldSendNotification(type: String): Boolean {
        if (!isNotificationTypeEnabled(type)) {
            Log.d(TAG, "Notification type $type is disabled by user")
            return false
        }

        if (isNotificationTypeSnoozed(type)) {
            Log.d(TAG, "Notification type $type is snoozed")
            return false
        }

        if (!isNotificationTypeAllowedByTime(type)) {
            Log.d(TAG, "Notification type $type not allowed by time preferences")
            return false
        }

        return true
    }

    private fun isNotificationTypeAllowedByTime(type: String): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // Check silent hours
        if (isSilentHoursEnabled()) {
            val silentStart = getSilentHoursStart()
            val silentEnd = getSilentHoursEnd()
            if (isTimeInRange(currentHour, silentStart, silentEnd)) {
                return false
            }
        }

        // Check preferred notification window
        val windowStart = getPreferredNotificationWindowStart()
        val windowEnd = getPreferredNotificationWindowEnd()
        if (!isTimeInRange(currentHour, windowStart, windowEnd)) {
            return false
        }

        return true
    }

    private fun isTimeInRange(hour: Int, start: Int, end: Int): Boolean {
        return if (start <= end) {
            hour in start..end
        } else {
            hour >= start || hour <= end
        }
    }

    // Reset all preferences to default values
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
    }

    // Get all preferences as a map (useful for debugging or exporting settings)
    fun getAllPreferences(): Map<String, *> {
        return sharedPreferences.all
    }

    // Data class for user feedback
    data class UserFeedback(
        val notificationId: String,
        val notificationType: String,
        val rating: Int, // 1-5 stars
        val helpful: Boolean,
        val feedbackText: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val actionTaken: String? = null // e.g., "snooze", "disable", "none"
    )
}
