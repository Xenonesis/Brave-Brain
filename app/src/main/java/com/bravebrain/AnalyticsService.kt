package com.bravebrain

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import java.util.*
import java.text.SimpleDateFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Enhanced Analytics Service for comprehensive usage tracking and insights
 */
class AnalyticsService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private val analyticsInterval = 30000L // Collect analytics every 30 seconds
    private var isRunning = false
    
    companion object {
        private const val ANALYTICS_PREFS = "analytics_data"
        private const val USAGE_EVENTS_KEY = "usage_events"
        private const val DAILY_STATS_KEY = "daily_stats"
        private const val WEEKLY_STATS_KEY = "weekly_stats"
        private const val PRODUCTIVITY_SCORE_KEY = "productivity_score"
    }
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startAnalyticsCollection()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(analyticsRunnable)
    }
    
    private val analyticsRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                try {
                    collectUsageAnalytics()
                    calculateProductivityScore()
                    updateInsights()
                } catch (e: Exception) {
                    android.util.Log.e("AnalyticsService", "Error collecting analytics: ${e.message}")
                }
                handler.postDelayed(this, analyticsInterval)
            }
        }
    }
    
    private fun startAnalyticsCollection() {
        handler.post(analyticsRunnable)
    }
    
    private fun collectUsageAnalytics() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MINUTE, -30) // Last 30 minutes
        val startTime = calendar.timeInMillis
        
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val usageEvents = mutableListOf<UsageEvent>()
            
            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)
                
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                    
                    usageEvents.add(UsageEvent(
                        packageName = event.packageName,
                        timestamp = event.timeStamp,
                        eventType = event.eventType,
                        className = event.className
                    ))
                }
            }
            
            storeUsageEvents(usageEvents)
            updateDailyStats(usageEvents)
            
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsService", "Error querying usage events: ${e.message}")
        }
    }
    
    private fun storeUsageEvents(events: List<UsageEvent>) {
        val prefs = getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH", Locale.US)
        val currentHour = dateFormat.format(Date())
        
        val existingEvents = prefs.getString("$USAGE_EVENTS_KEY-$currentHour", "") ?: ""
        val newEventsJson = events.joinToString("|") { event ->
            "${event.packageName},${event.timestamp},${event.eventType},${event.className}"
        }
        
        val updatedEvents = if (existingEvents.isEmpty()) newEventsJson 
                           else "$existingEvents|$newEventsJson"
        
        prefs.edit().putString("$USAGE_EVENTS_KEY-$currentHour", updatedEvents).apply()
    }
    
    private fun updateDailyStats(events: List<UsageEvent>) {
        val prefs = getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        
        // Calculate session data
        val sessionData = calculateSessionData(events)
        val dailyStats = DailyStats(
            date = today,
            totalScreenTime = getTotalScreenTimeToday(),
            appSessions = sessionData.sessions,
            averageSessionLength = sessionData.averageLength,
            longestSession = sessionData.longestSession,
            mostUsedApp = sessionData.mostUsedApp,
            productivityScore = calculateDailyProductivityScore(),
            blockedAttempts = getBlockedAttemptsToday(),
            challengesCompleted = getChallengesCompletedToday()
        )
        
        val statsJson = "${dailyStats.date},${dailyStats.totalScreenTime},${dailyStats.appSessions}," +
                       "${dailyStats.averageSessionLength},${dailyStats.longestSession}," +
                       "${dailyStats.mostUsedApp},${dailyStats.productivityScore}," +
                       "${dailyStats.blockedAttempts},${dailyStats.challengesCompleted}"
        
        prefs.edit().putString("$DAILY_STATS_KEY-$today", statsJson).apply()
    }
    
    private fun calculateSessionData(events: List<UsageEvent>): SessionData {
        val sessions = mutableMapOf<String, MutableList<Long>>()
        val currentSessions = mutableMapOf<String, Long>()
        
        events.sortedBy { it.timestamp }.forEach { event ->
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    currentSessions[event.packageName] = event.timestamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val startTime = currentSessions[event.packageName]
                    if (startTime != null) {
                        val sessionLength = event.timestamp - startTime
                        if (sessionLength > 0) {
                            sessions.getOrPut(event.packageName) { mutableListOf() }.add(sessionLength)
                        }
                        currentSessions.remove(event.packageName)
                    }
                }
            }
        }
        
        val allSessions = sessions.values.flatten()
        val totalSessions = allSessions.size
        val averageLength = if (totalSessions > 0) allSessions.average().toLong() else 0L
        val longestSession = allSessions.maxOrNull() ?: 0L
        val mostUsedApp = sessions.maxByOrNull { it.value.sum() }?.key ?: ""
        
        return SessionData(totalSessions, averageLength, longestSession, mostUsedApp)
    }
    
    private fun calculateProductivityScore(): Int {
        // Calculate productivity score based on various factors
        val blockedAppsUsage = getBlockedAppsUsageToday()
        val totalUsage = getTotalScreenTimeToday()
        val challengesCompleted = getChallengesCompletedToday()
        val blockedAttempts = getBlockedAttemptsToday()
        
        var score = 100
        
        // Deduct points for excessive blocked app usage
        if (totalUsage > 0) {
            val blockedPercentage = (blockedAppsUsage.toFloat() / totalUsage) * 100
            score -= (blockedPercentage * 0.5).toInt()
        }
        
        // Add points for completing challenges
        score += challengesCompleted * 5
        
        // Deduct points for blocking attempts (shows lack of self-control)
        score -= blockedAttempts * 2
        
        // Ensure score is between 0 and 100
        return score.coerceIn(0, 100)
    }
    
    private fun calculateDailyProductivityScore(): Int {
        return calculateProductivityScore()
    }
    
    private fun updateInsights() {
        val prefs = getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
        
        // Store current productivity score
        val currentScore = calculateProductivityScore()
        prefs.edit().putInt(PRODUCTIVITY_SCORE_KEY, currentScore).apply()
        
        // Update weekly aggregates
        updateWeeklyStats()
        
        // Generate insights and recommendations
        generateInsights()
        
        
        // Sync analytics to Firestore
        DataSyncManager(this).syncAllData()        // Trigger notifications based on insights
        triggerAnalyticsNotifications(currentScore)
    }
    
    private fun triggerAnalyticsNotifications(currentScore: Int) {
        val averageScore = getAverageProductivityScoreThisWeek()
        val scoreChange = currentScore - averageScore
        
        // Trigger notification if there's a significant change in productivity
        if (kotlin.math.abs(scoreChange) >= 15) {
            val patternType = if (scoreChange > 0) "improvement" else "decline"
            val details = if (scoreChange > 0) {
                "Productivity improved by ${kotlin.math.abs(scoreChange)} points"
            } else {
                "Productivity decreased by ${kotlin.math.abs(scoreChange)} points"
            }
            
            triggerPatternDetectionNotification(patternType, details)
        }
    }
    
    private fun triggerPatternDetectionNotification(patternType: String, details: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent("TRIGGER_ANALYTICS_NOTIFICATION").apply {
                setPackage(packageName)
                putExtra("pattern_type", patternType)
                putExtra("pattern_details", details)
            }
            sendBroadcast(intent)
        }
    }
    
    private fun updateWeeklyStats() {
        val prefs = getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val weekStart = getWeekStart(calendar)
        val weekKey = SimpleDateFormat("yyyy-'W'ww", Locale.US).format(weekStart.time)
        
        val weeklyStats = WeeklyStats(
            weekStart = weekKey,
            averageScreenTime = getAverageScreenTimeThisWeek(),
            totalBlockedAttempts = getTotalBlockedAttemptsThisWeek(),
            averageProductivityScore = getAverageProductivityScoreThisWeek(),
            mostProblematicApp = getMostProblematicAppThisWeek(),
            bestDay = getBestDayThisWeek(),
            improvement = getWeeklyImprovement()
        )
        
        val weeklyStatsJson = "${weeklyStats.weekStart},${weeklyStats.averageScreenTime}," +
                             "${weeklyStats.totalBlockedAttempts},${weeklyStats.averageProductivityScore}," +
                             "${weeklyStats.mostProblematicApp},${weeklyStats.bestDay},${weeklyStats.improvement}"
        
        prefs.edit().putString("$WEEKLY_STATS_KEY-$weekKey", weeklyStatsJson).apply()
    }
    
    private fun generateInsights() {
        val insights = mutableListOf<String>()
        
        // Analyze usage patterns
        val currentScore = calculateProductivityScore()
        val averageScore = getAverageProductivityScoreThisWeek()
        
        when {
            currentScore > averageScore + 10 -> insights.add("Great job! Your productivity is above average today.")
            currentScore < averageScore - 10 -> insights.add("Your productivity is below average. Consider taking a break from distracting apps.")
            else -> insights.add("Your productivity is steady. Keep up the good work!")
        }
        
        // Peak usage time analysis
        val peakHour = getPeakUsageHour()
        if (peakHour != -1) {
            insights.add("Your peak usage time is around ${peakHour}:00. Consider setting stricter limits during this time.")
        }
        
        // Store insights
        val prefs = getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("insights", insights.joinToString("|")).apply()
    }
    
    // Helper methods for data retrieval
    private fun getTotalScreenTimeToday(): Long {
        return UsageUtils.getUsage(this).values.sum().toLong() * 60 * 1000 // Convert minutes to milliseconds
    }
    
    private fun getBlockedAppsUsageToday(): Long {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        val usage = UsageUtils.getUsage(this)
        return blockedApps.sumOf { usage[it] ?: 0 }.toLong() * 60 * 1000
    }
    
    private fun getBlockedAttemptsToday(): Int {
        val prefs = getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return prefs.getInt("blocked_attempts_$today", 0)
    }
    
    private fun getChallengesCompletedToday(): Int {
        val prefs = getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return prefs.getInt("challenges_completed_$today", 0)
    }
    
    private fun getAverageScreenTimeThisWeek(): Long {
        // Implementation for weekly average calculation
        return 0L // Placeholder
    }
    
    private fun getTotalBlockedAttemptsThisWeek(): Int {
        // Implementation for weekly blocked attempts
        return 0 // Placeholder
    }
    
    private fun getAverageProductivityScoreThisWeek(): Int {
        // Implementation for weekly productivity score average
        return 50 // Placeholder
    }
    
    private fun getMostProblematicAppThisWeek(): String {
        // Implementation to find most problematic app
        return "" // Placeholder
    }
    
    private fun getBestDayThisWeek(): String {
        // Implementation to find best day
        return "" // Placeholder
    }
    
    private fun getWeeklyImprovement(): Float {
        // Implementation to calculate improvement percentage
        return 0.0f // Placeholder
    }
    
    private fun getPeakUsageHour(): Int {
        // Implementation to find peak usage hour
        return -1 // Placeholder
    }
    
    private fun getWeekStart(calendar: Calendar): Calendar {
        val weekStart = calendar.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        weekStart.set(Calendar.HOUR_OF_DAY, 0)
        weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0)
        weekStart.set(Calendar.MILLISECOND, 0)
        return weekStart
    }
    
    // Data classes for analytics
    data class UsageEvent(
        val packageName: String,
        val timestamp: Long,
        val eventType: Int,
        val className: String?
    )
    
    data class SessionData(
        val sessions: Int,
        val averageLength: Long,
        val longestSession: Long,
        val mostUsedApp: String
    )
    
    data class DailyStats(
        val date: String,
        val totalScreenTime: Long,
        val appSessions: Int,
        val averageSessionLength: Long,
        val longestSession: Long,
        val mostUsedApp: String,
        val productivityScore: Int,
        val blockedAttempts: Int,
        val challengesCompleted: Int
    )
    
    data class WeeklyStats(
        val weekStart: String,
        val averageScreenTime: Long,
        val totalBlockedAttempts: Int,
        val averageProductivityScore: Int,
        val mostProblematicApp: String,
        val bestDay: String,
        val improvement: Float
    )
}
