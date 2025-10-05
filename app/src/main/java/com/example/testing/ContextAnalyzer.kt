package com.example.testing

import android.content.Context
import android.app.usage.UsageStatsManager
import android.content.SharedPreferences
import java.util.*
import kotlin.math.abs

/**
 * ContextAnalyzer analyzes user context including time of day, day of week, 
 * usage patterns, focus periods, sleep times, and work hours
 */
class ContextAnalyzer(private val context: Context) {
    
    companion object {
        private const val CONTEXT_PREFS = "context_analyzer_prefs"
        private const val LAST_ANALYSIS_TIME = "last_analysis_time"
        private const val USER_PATTERNS_KEY = "user_patterns"
        private const val SLEEP_START_HOUR = "sleep_start_hour"
        private const val SLEEP_END_HOUR = "sleep_end_hour"
        private const val WORK_START_HOUR = "work_start_hour"
        private const val WORK_END_HOUR = "work_end_hour"
        private const val FOCUS_HOURS = "focus_hours"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(CONTEXT_PREFS, Context.MODE_PRIVATE)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    
    data class UserContext(
        val timeOfDay: TimeOfDay,
        val dayOfWeek: DayOfWeek,
        val isSleepTime: Boolean,
        val isWorkTime: Boolean,
        val isFocusTime: Boolean,
        val usageIntensity: UsageIntensity,
        val engagementLevel: EngagementLevel,
        val optimalNotificationTime: Boolean,
        val batteryLevel: Int = 0
    )
    
    enum class TimeOfDay {
        MORNING, AFTERNOON, EVENING, NIGHT
    }
    
    enum class DayOfWeek(val value: Int) {
        MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6), SUNDAY(7)
    }
    
    enum class UsageIntensity {
        LOW, MODERATE, HIGH, PEAK
    }
    
    enum class EngagementLevel {
        LOW, MODERATE, HIGH, VERY_HIGH
    }
    
    /**
     * Analyzes the current user context based on multiple factors
     */
    fun analyzeContext(): UserContext {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val day = currentTime.get(Calendar.DAY_OF_WEEK)
        
        val timeOfDay = determineTimeOfDay(hour)
        val dayOfWeek = determineDayOfWeek(day)
        val isSleepTime = isSleepTime(hour)
        val isWorkTime = isWorkTime(hour)
        val isFocusTime = isFocusTime(hour)
        val usageIntensity = determineUsageIntensity()
        val engagementLevel = determineEngagementLevel()
        val optimalNotificationTime = isOptimalNotificationTime(timeOfDay, isSleepTime, isFocusTime)
        
        return UserContext(
            timeOfDay = timeOfDay,
            dayOfWeek = dayOfWeek,
            isSleepTime = isSleepTime,
            isWorkTime = isWorkTime,
            isFocusTime = isFocusTime,
            usageIntensity = usageIntensity,
            engagementLevel = engagementLevel,
            optimalNotificationTime = optimalNotificationTime
        )
    }
    
    private fun determineTimeOfDay(hour: Int): TimeOfDay {
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
    
    private fun determineDayOfWeek(day: Int): DayOfWeek {
        return when (day) {
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
    }
    
    private fun isSleepTime(hour: Int): Boolean {
        val sleepStart = prefs.getInt(SLEEP_START_HOUR, 22) // Default 10 PM
        val sleepEnd = prefs.getInt(SLEEP_END_HOUR, 7)     // Default 7 AM
        
        if (sleepStart > sleepEnd) {
            // Sleep time crosses midnight (e.g., 22:00 to 07:00)
            return hour >= sleepStart || hour < sleepEnd
        } else {
            // Sleep time within same day (e.g., 02:00 to 07:00)
            return hour in sleepStart until sleepEnd
        }
    }
    
    private fun isWorkTime(hour: Int): Boolean {
        val workStart = prefs.getInt(WORK_START_HOUR, 9)  // Default 9 AM
        val workEnd = prefs.getInt(WORK_END_HOUR, 18)     // Default 6 PM
        
        return if (workStart <= workEnd) {
            hour in workStart until workEnd
        } else {
            // Work time crosses midnight
            hour >= workStart || hour < workEnd
        }
    }
    
    private fun isFocusTime(hour: Int): Boolean {
        val focusHours = prefs.getString(FOCUS_HOURS, "22-7") ?: "22-7"
        val hours = focusHours.split("-").mapNotNull { it.toIntOrNull() }
        
        if (hours.size == 2) {
            val start = hours[0]
            val end = hours[1]
            
            if (start > end) {
                // Focus time crosses midnight
                return hour >= start || hour < end
            } else {
                // Focus time within same day
                return hour in start until end
            }
        }
        
        return false
    }
    
    private fun determineUsageIntensity(): UsageIntensity {
        // Analyze usage in the last hour
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.HOUR, -1)
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        if (usageStats.isEmpty()) return UsageIntensity.LOW
        
        // Calculate total usage time in the last hour
        var totalTime = 0L
        for (stats in usageStats) {
            totalTime += stats.totalTimeInForeground
        }
        
        // Convert to minutes
        val totalMinutes = totalTime / (1000 * 60)
        
        return when {
            totalMinutes < 5 -> UsageIntensity.LOW
            totalMinutes < 15 -> UsageIntensity.MODERATE
            totalMinutes < 30 -> UsageIntensity.HIGH
            else -> UsageIntensity.PEAK
        }
    }
    
    private fun determineEngagementLevel(): EngagementLevel {
        // Analyze usage patterns over the last 24 hours
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        if (usageStats.isEmpty()) return EngagementLevel.LOW
        
        // Count number of unique apps used
        val uniqueApps = usageStats.size
        
        // Calculate total usage time
        var totalTime = 0L
        for (stats in usageStats) {
            totalTime += stats.totalTimeInForeground
        }
        
        // Calculate engagement score based on app diversity and usage time
        val engagementScore = calculateEngagementScore(uniqueApps, totalTime)
        
        return when {
            engagementScore < 20 -> EngagementLevel.LOW
            engagementScore < 50 -> EngagementLevel.MODERATE
            engagementScore < 80 -> EngagementLevel.HIGH
            else -> EngagementLevel.VERY_HIGH
        }
    }
    
    private fun calculateEngagementScore(uniqueApps: Int, totalTime: Long): Int {
        // Normalize the values to a 0-10 scale
        val appScore = minOf(uniqueApps * 5, 50) // Max 50 for app diversity
        val timeScore = minOf((totalTime / (1000 * 60 * 10)).toInt(), 50) // Max 50 for time
        
        return appScore + timeScore
    }
    
    private fun isOptimalNotificationTime(
        timeOfDay: TimeOfDay,
        isSleepTime: Boolean,
        isFocusTime: Boolean
    ): Boolean {
        // Notifications are optimal when:
        // - Not during sleep time
        // - Not during focus time
        // - During moderate engagement periods
        return !isSleepTime && !isFocusTime && timeOfDay != TimeOfDay.NIGHT
    }
    
    /**
     * Learns user patterns and updates internal models
     */
    fun learnUserPatterns() {
        val currentContext = analyzeContext()
        
        // Store pattern data for analysis
        val patternData = getUserPatternData()
        patternData.add(currentContext)
        
        // Keep only recent patterns (last 30 days worth of data)
        if (patternData.size > 30 * 24) { // Approximate hours in 30 days
            patternData.subList(0, patternData.size - 30 * 24).clear()
        }
        
        saveUserPatternData(patternData)
    }
    
    private fun getUserPatternData(): MutableList<UserContext> {
        // In a real implementation, this would load from persistent storage
        // For now, returning an empty list
        return mutableListOf()
    }
    
    private fun saveUserPatternData(patternData: List<UserContext>) {
        // In a real implementation, this would save to persistent storage
    }
    
    /**
     * Gets user's sleep time preferences
     */
    fun getSleepTime(): Pair<Int, Int> {
        val sleepStart = prefs.getInt(SLEEP_START_HOUR, 22)
        val sleepEnd = prefs.getInt(SLEEP_END_HOUR, 7)
        return Pair(sleepStart, sleepEnd)
    }
    
    /**
     * Sets user's sleep time preferences
     */
    fun setSleepTime(startHour: Int, endHour: Int) {
        prefs.edit()
            .putInt(SLEEP_START_HOUR, startHour)
            .putInt(SLEEP_END_HOUR, endHour)
            .apply()
    }
    
    /**
     * Gets user's work time preferences
     */
    fun getWorkTime(): Pair<Int, Int> {
        val workStart = prefs.getInt(WORK_START_HOUR, 9)
        val workEnd = prefs.getInt(WORK_END_HOUR, 18)
        return Pair(workStart, workEnd)
    }
    
    /**
     * Sets user's work time preferences
     */
    fun setWorkTime(startHour: Int, endHour: Int) {
        prefs.edit()
            .putInt(WORK_START_HOUR, startHour)
            .putInt(WORK_END_HOUR, endHour)
            .apply()
    }
    
    /**
     * Gets user's focus time preferences
     */
    fun getFocusTime(): Pair<Int, Int> {
        val focusHours = prefs.getString(FOCUS_HOURS, "22-7") ?: "22-7"
        val hours = focusHours.split("-").mapNotNull { it.toIntOrNull() }
        
        if (hours.size == 2) {
            return Pair(hours[0], hours[1])
        }
        
        return Pair(22, 7) // Default: 10 PM to 7 AM
    }
    
    /**
     * Sets user's focus time preferences
     */
    fun setFocusTime(startHour: Int, endHour: Int) {
        prefs.edit()
            .putString(FOCUS_HOURS, "$startHour-$endHour")
            .apply()
    }
}