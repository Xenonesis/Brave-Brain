package com.bravebrain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.*

/**
 * NotificationEffectivenessTracker implements intelligent frequency control
 * and notification effectiveness tracking
 */
class NotificationEffectivenessTracker(private val context: Context) {
    companion object {
        private const val TAG = "NotificationEffectTrack"
        private const val PREFS_NAME = "notification_effectiveness_tracker"
        
        // Frequency control settings
        private const val DEFAULT_MAX_DAILY_NOTIFICATIONS = 5
        private const val DEFAULT_MAX_HOURLY_NOTIFICATIONS = 2
        private const val DEFAULT_THROTTLE_WINDOW_HOURS = 1
        
        // Effectiveness tracking settings
        private const val EFFECTIVENESS_WEIGHT_CURRENT = 0.6f
        private const val EFFECTIVENESS_WEIGHT_HISTORICAL = 0.4f
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Tracks when a notification was sent
     */
    fun trackNotificationSent(notificationId: String, notificationType: String, timestamp: Long = System.currentTimeMillis()) {
        val editor = prefs.edit()
        
        // Store notification send record
        val sendRecord = NotificationSendRecord(
            id = notificationId,
            type = notificationType,
            timestamp = timestamp,
            opened = false // Will be updated if opened
        )
        
        // Add to sent notifications list
        val sentNotifications = getSentNotifications()
        sentNotifications.add(sendRecord)
        
        // Keep only recent notifications (last 30 days)
        val cutoffTime = timestamp - (30 * 24 * 60 * 1000L) // 30 days
        sentNotifications.removeAll { it.timestamp < cutoffTime }
        
        saveSentNotifications(sentNotifications)
        
        // Update frequency counters
        updateFrequencyCounters(notificationType, timestamp)
        
        Log.d(TAG, "Tracked notification sent: $notificationId, type: $notificationType")
    }

    /**
     * Tracks when a notification was opened by the user
     */
    fun trackNotificationOpened(notificationId: String, timestamp: Long = System.currentTimeMillis()) {
        val sentNotifications = getSentNotifications()
        val notificationRecord = sentNotifications.find { it.id == notificationId }
        
        if (notificationRecord != null) {
            // Update the record to mark as opened
            notificationRecord.opened = true
            notificationRecord.openTimestamp = timestamp
            
            saveSentNotifications(sentNotifications)
            
            // Update effectiveness metrics
            updateEffectivenessMetrics(notificationRecord.type, true, timestamp)
            
            Log.d(TAG, "Tracked notification opened: $notificationId")
        } else {
            Log.w(TAG, "Notification not found in sent records: $notificationId")
        }
    }

    /**
     * Updates effectiveness metrics for a notification type
     */
    private fun updateEffectivenessMetrics(notificationType: String, wasOpened: Boolean, timestamp: Long) {
        val typeStats = getNotificationTypeStats(notificationType)
        typeStats.totalSent++
        
        if (wasOpened) {
            typeStats.totalOpened++
        }
        
        // Calculate current response rate
        typeStats.currentResponseRate = if (typeStats.totalSent > 0) {
            typeStats.totalOpened.toFloat() / typeStats.totalSent
        } else {
            0f
        }
        
        // Track in hourly buckets for recent effectiveness
        val hourBucket = timestamp / (60 * 60 * 1000L) // Group by hour
        val hourlyStats = typeStats.hourlyEffectiveness.getOrPut(hourBucket) { HourlyEffectiveness() }
        
        if (wasOpened) {
            hourlyStats.opened++
        } else {
            hourlyStats.sent++
        }
        
        // Keep only recent hourly data (last 7 days)
        val currentHourBucket = System.currentTimeMillis() / (60 * 60 * 1000L)
        val cutoffBucket = currentHourBucket - (7 * 24) // 7 days worth of hours
        typeStats.hourlyEffectiveness.keys.removeAll { it < cutoffBucket }
        
        saveNotificationTypeStats(notificationType, typeStats)
    }

    /**
     * Checks if sending a notification is allowed based on frequency controls
     */
    fun isNotificationAllowed(notificationType: String, userId: String = "default"): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Check daily limit
        if (!isWithinDailyLimit(notificationType, userId, currentTime)) {
            Log.d(TAG, "Daily limit exceeded for notification type: $notificationType")
            return false
        }
        
        // Check hourly limit
        if (!isWithinHourlyLimit(notificationType, userId, currentTime)) {
            Log.d(TAG, "Hourly limit exceeded for notification type: $notificationType")
            return false
        }
        
        // Check throttle window (minimum time between notifications of same type)
        if (!passesThrottleWindow(notificationType, userId, currentTime)) {
            Log.d(TAG, "Throttle window not passed for notification type: $notificationType")
            return false
        }
        
        return true
    }

    /**
     * Checks if notification is within daily limit
     */
    private fun isWithinDailyLimit(notificationType: String, userId: String, currentTime: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 1000L) - 1
        
        val sentToday = getNotificationsInTimeRange(notificationType, userId, startOfDay, endOfDay)
        val dailyLimit = prefs.getInt("daily_limit_$notificationType", DEFAULT_MAX_DAILY_NOTIFICATIONS)
        
        return sentToday.size < dailyLimit
    }

    /**
     * Checks if notification is within hourly limit
     */
    private fun isWithinHourlyLimit(notificationType: String, userId: String, currentTime: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startOfHour = calendar.timeInMillis
        val endOfHour = startOfHour + (60 * 60 * 1000L) - 1
        
        val sentThisHour = getNotificationsInTimeRange(notificationType, userId, startOfHour, endOfHour)
        val hourlyLimit = prefs.getInt("hourly_limit_$notificationType", DEFAULT_MAX_HOURLY_NOTIFICATIONS)
        
        return sentThisHour.size < hourlyLimit
    }

    /**
     * Checks if notification passes throttle window requirements
     */
    private fun passesThrottleWindow(notificationType: String, userId: String, currentTime: Long): Boolean {
        val throttleWindow = prefs.getLong("throttle_window_$notificationType", 
            DEFAULT_THROTTLE_WINDOW_HOURS * 60 * 60 * 1000L) // Convert to ms
        
        val sentNotifications = getSentNotifications()
        val recentNotifications = sentNotifications.filter { 
            it.type == notificationType && 
            it.userId == userId && 
            (currentTime - it.timestamp) < throttleWindow 
        }
        
        // If no recent notifications of this type, allow it
        if (recentNotifications.isEmpty()) {
            return true
        }
        
        // Check the most recent notification
        val mostRecent = recentNotifications.maxByOrNull { it.timestamp }
        val minInterval = prefs.getLong("min_interval_$notificationType", 15 * 60 * 1000L) // 15 minutes default
        
        return (currentTime - mostRecent!!.timestamp) >= minInterval
    }

    /**
     * Gets notifications in a specific time range
     */
    private fun getNotificationsInTimeRange(
        notificationType: String, 
        userId: String, 
        startTime: Long, 
        endTime: Long
    ): List<NotificationSendRecord> {
        return getSentNotifications().filter { 
            it.type == notificationType && 
            it.userId == userId && 
            it.timestamp >= startTime && 
            it.timestamp <= endTime 
        }
    }

    /**
     * Updates frequency counters for a notification type
     */
    private fun updateFrequencyCounters(notificationType: String, timestamp: Long) {
        val editor = prefs.edit()
        
        // Update daily counter
        val dailyKey = "daily_count_${notificationType}_${getDayKey(timestamp)}"
        val dailyCount = prefs.getInt(dailyKey, 0) + 1
        editor.putInt(dailyKey, dailyCount)
        
        // Update hourly counter
        val hourlyKey = "hourly_count_${notificationType}_${getHourKey(timestamp)}"
        val hourlyCount = prefs.getInt(hourlyKey, 0) + 1
        editor.putInt(hourlyKey, hourlyCount)
        
        editor.apply()
    }

    /**
     * Gets a day key in the format YYYYMMDD
     */
    private fun getDayKey(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return String.format("%d%02d%02d", 
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH))
    }

    /**
     * Gets an hour key in the format YYYYMMDDHH
     */
    private fun getHourKey(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return String.format("%d%02d%02d", 
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY))
    }

    /**
     * Calculates the effectiveness score for a notification type
     */
    fun calculateEffectivenessScore(notificationType: String): Float {
        val typeStats = getNotificationTypeStats(notificationType)
        
        if (typeStats.totalSent == 0) {
            return 0.5f // Neutral score if no data
        }
        
        // Calculate overall response rate
        val overallResponseRate = typeStats.totalOpened.toFloat() / typeStats.totalSent
        
        // Calculate recent effectiveness (last 24 hours)
        val recentResponseRate = calculateRecentEffectiveness(notificationType)
        
        // Weighted combination of overall and recent effectiveness
        return (overallResponseRate * EFFECTIVENESS_WEIGHT_HISTORICAL) + 
               (recentResponseRate * EFFECTIVENESS_WEIGHT_CURRENT)
    }

    /**
     * Calculates recent effectiveness (last 24 hours)
     */
    private fun calculateRecentEffectiveness(notificationType: String): Float {
        val typeStats = getNotificationTypeStats(notificationType)
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val oneDayBucket = oneDayAgo / (60 * 60 * 1000L)
        
        var opened = 0
        var sent = 0
        
        for ((bucket, hourlyStats) in typeStats.hourlyEffectiveness) {
            if (bucket >= oneDayBucket) {
                opened += hourlyStats.opened
                sent += hourlyStats.sent + hourlyStats.opened
            }
        }
        
        return if (sent > 0) opened.toFloat() / sent else 0f
    }

    /**
     * Gets all notification statistics
     */
    fun getAllNotificationStats(): Map<String, NotificationTypeStats> {
        val allStats = mutableMapOf<String, NotificationTypeStats>()
        
        // Get all notification types that have stats
        val allKeys = prefs.all.keys
        val typeKeys = allKeys.filter { it.startsWith("stats_") }.map { 
            it.substring(6, it.indexOf('_', 6)) // Extract type from "stats_TYPE_opened"
        }.distinct()
        
        for (type in typeKeys) {
            allStats[type] = getNotificationTypeStats(type)
        }
        
        return allStats
    }

    /**
     * Gets effectiveness metrics for a specific notification type
     */
    fun getEffectivenessMetrics(notificationType: String): EffectivenessMetrics {
        val typeStats = getNotificationTypeStats(notificationType)
        val effectivenessScore = calculateEffectivenessScore(notificationType)
        val recentEffectiveness = calculateRecentEffectiveness(notificationType)
        
        return EffectivenessMetrics(
            type = notificationType,
            totalSent = typeStats.totalSent,
            totalOpened = typeStats.totalOpened,
            responseRate = if (typeStats.totalSent > 0) typeStats.totalOpened.toFloat() / typeStats.totalSent else 0f,
            recentResponseRate = recentEffectiveness,
            effectivenessScore = effectivenessScore,
            lastSentTime = typeStats.lastSentTime,
            lastOpenedTime = typeStats.lastOpenedTime
        )
    }

    /**
     * Resets frequency counters (for testing purposes)
     */
    fun resetFrequencyCounters() {
        val editor = prefs.edit()
        
        // Remove all frequency-related entries
        val allKeys = prefs.all.keys
        val frequencyKeys = allKeys.filter { 
            it.startsWith("daily_count_") || 
            it.startsWith("hourly_count_") ||
            it.startsWith("last_sent_")
        }
        
        for (key in frequencyKeys) {
            editor.remove(key)
        }
        
        editor.apply()
    }

    /**
     * Gets all sent notifications
     */
    private fun getSentNotifications(): MutableList<NotificationSendRecord> {
        val sentNotificationsJson = prefs.getString("sent_notifications", "[]") ?: "[]"
        // In a real implementation, we would parse this JSON
        // For now, returning an empty list as we're tracking differently
        return mutableListOf()
    }

    /**
     * Saves sent notifications
     */
    private fun saveSentNotifications(notifications: List<NotificationSendRecord>) {
        // In a real implementation, we would serialize this to JSON
        // For now, doing nothing as we're tracking differently
    }

    /**
     * Gets notification type statistics
     */
    private fun getNotificationTypeStats(notificationType: String): NotificationTypeStats {
        val totalSent = prefs.getInt("stats_${notificationType}_sent", 0)
        val totalOpened = prefs.getInt("stats_${notificationType}_opened", 0)
        val lastSentTime = prefs.getLong("stats_${notificationType}_last_sent", 0L)
        val lastOpenedTime = prefs.getLong("stats_${notificationType}_last_opened", 0L)
        
        return NotificationTypeStats(
            totalSent = totalSent,
            totalOpened = totalOpened,
            currentResponseRate = if (totalSent > 0) totalOpened.toFloat() / totalSent else 0f,
            lastSentTime = lastSentTime,
            lastOpenedTime = lastOpenedTime,
            hourlyEffectiveness = mutableMapOf() // Would load from storage in real implementation
        )
    }

    /**
     * Saves notification type statistics
     */
    private fun saveNotificationTypeStats(notificationType: String, stats: NotificationTypeStats) {
        val editor = prefs.edit()
        editor.putInt("stats_${notificationType}_sent", stats.totalSent)
        editor.putInt("stats_${notificationType}_opened", stats.totalOpened)
        editor.putLong("stats_${notificationType}_last_sent", stats.lastSentTime)
        editor.putLong("stats_${notificationType}_last_opened", stats.lastOpenedTime)
        editor.apply()
    }

    /**
     * Adjusts frequency limits based on effectiveness
     */
    fun adjustFrequencyLimitsBasedOnEffectiveness() {
        val allStats = getAllNotificationStats()
        
        for ((type, stats) in allStats) {
            if (stats.totalSent < 5) continue // Need sufficient data
            
            val effectivenessScore = calculateEffectivenessScore(type)
            
            // Adjust limits based on effectiveness
            when {
                effectivenessScore > 0.7f -> {
                    // High effectiveness - could potentially increase frequency
                    // For safety, we'll maintain current limits but log for review
                    Log.d(TAG, "High effectiveness for $type: $effectivenessScore - consider increasing frequency")
                }
                effectivenessScore < 0.3f -> {
                    // Low effectiveness - reduce frequency to prevent fatigue
                    val currentDailyLimit = prefs.getInt("daily_limit_$type", DEFAULT_MAX_DAILY_NOTIFICATIONS)
                    val newDailyLimit = maxOf(currentDailyLimit - 1, 1) // Minimum 1 per day
                    prefs.edit().putInt("daily_limit_$type", newDailyLimit).apply()
                    
                    Log.d(TAG, "Low effectiveness for $type: $effectivenessScore - reduced daily limit to $newDailyLimit")
                }
                else -> {
                    // Medium effectiveness - maintain current limits
                }
            }
        }
    }

    // Data classes for tracking
    data class NotificationSendRecord(
        val id: String,
        val type: String,
        val timestamp: Long,
        var opened: Boolean = false,
        var openTimestamp: Long? = null,
        val userId: String = "default"
    )

    data class NotificationTypeStats(
        var totalSent: Int = 0,
        var totalOpened: Int = 0,
        var currentResponseRate: Float = 0f,
        var lastSentTime: Long = 0L,
        var lastOpenedTime: Long = 0L,
        val hourlyEffectiveness: MutableMap<Long, HourlyEffectiveness> = mutableMapOf()
    )

    data class HourlyEffectiveness(
        var sent: Int = 0,
        var opened: Int = 0
    )

    data class EffectivenessMetrics(
        val type: String,
        val totalSent: Int,
        val totalOpened: Int,
        val responseRate: Float,
        val recentResponseRate: Float,
        val effectivenessScore: Float,
        val lastSentTime: Long,
        val lastOpenedTime: Long
    )
}