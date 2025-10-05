package com.example.testing

import android.content.Context
import android.app.usage.UsageStatsManager
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * UsagePatternRecognition contains algorithms for usage pattern recognition
 * including peak usage detection, binge behavior recognition, 
 * recovery pattern detection, and circadian rhythm integration
 */
class UsagePatternRecognition(private val context: Context) {
    
    companion object {
        private const val PATTERN_PREFS = "usage_pattern_prefs"
        private const val PEAK_USAGE_TIMES = "peak_usage_times"
        private const val BINGE_PATTERNS = "binge_patterns"
        private const val RECOVERY_PATTERNS = "recovery_patterns"
        private const val CIRCADIAN_PATTERNS = "circadian_patterns"
    }
    
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val prefs = context.getSharedPreferences(PATTERN_PREFS, Context.MODE_PRIVATE)
    
    data class PeakUsagePattern(
        val hour: Int,
        val usageDuration: Long,
        val appPackage: String,
        val dayOfWeek: Int
    )
    
    data class BingeBehaviorPattern(
        val packageName: String,
        val sessionDuration: Long,
        val frequency: Int,
        val timeOfDay: String,
        val severity: BingeSeverity
    )
    
    data class RecoveryPattern(
        val date: Date,
        val reducedUsageDuration: Long,
        val improvedAppUsage: Map<String, Long>,
        val recoveryType: RecoveryType
    )
    
    data class CircadianRhythmPattern(
        val peakActivityHour: Int,
        val lowActivityHour: Int,
        val rhythmConsistency: Float,
        val dayNightRatio: Float
    )
    
    enum class BingeSeverity {
        LOW, MODERATE, HIGH, EXTREME
    }
    
    enum class RecoveryType {
        SHORT_TERM, MEDIUM_TERM, LONG_TERM
    }
    
    /**
     * Detects peak usage times for the user
     */
    fun detectPeakUsageTimes(): List<PeakUsagePattern> {
        val peakPatterns = mutableListOf<PeakUsagePattern>()
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -30) // Last 30 days
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Group usage by hour of day
        val usageByHour = mutableMapOf<Int, MutableMap<String, Long>>()
        
        for (stats in usageStats) {
            val lastUsed = Date(stats.lastTimeUsed)
            val hour = Calendar.getInstance().apply { time = lastUsed }.get(Calendar.HOUR_OF_DAY)
            val packageName = stats.packageName
            val totalTime = stats.totalTimeInForeground
            
            if (!usageByHour.containsKey(hour)) {
                usageByHour[hour] = mutableMapOf()
            }
            
            usageByHour[hour]?.let { hourMap ->
                val currentTotal = hourMap.getOrDefault(packageName, 0L)
                hourMap[packageName] = currentTotal + totalTime
            }
        }
        
        // Find top usage times for each hour
        for ((hour, packageUsage) in usageByHour) {
            val topPackage = packageUsage.maxByOrNull { it.value }
            if (topPackage != null) {
                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                peakPatterns.add(PeakUsagePattern(
                    hour = hour,
                    usageDuration = topPackage.value,
                    appPackage = topPackage.key,
                    dayOfWeek = dayOfWeek
                ))
            }
        }
        
        // Sort by usage duration (descending)
        return peakPatterns.sortedByDescending { it.usageDuration }
    }
    
    /**
     * Detects binge behavior patterns
     */
    fun detectBingeBehavior(): List<BingeBehaviorPattern> {
        val bingePatterns = mutableListOf<BingeBehaviorPattern>()
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -14) // Last 14 days
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Group by package name and analyze session lengths
        val packageSessions = mutableMapOf<String, MutableList<Long>>()
        
        for (stats in usageStats) {
            val packageName = stats.packageName
            val totalTime = stats.totalTimeInForeground
            
            // Consider a "binge session" if usage is above a threshold (e.g., 30 minutes)
            if (totalTime > 30 * 60 * 1000) { // 30 minutes in milliseconds
                if (!packageSessions.containsKey(packageName)) {
                    packageSessions[packageName] = mutableListOf()
                }
                packageSessions[packageName]?.add(totalTime)
            }
        }
        
        // Analyze binge patterns for each package
        for ((packageName, sessions) in packageSessions) {
            if (sessions.isNotEmpty()) {
                val avgSessionDuration = sessions.average().toLong()
                val frequency = sessions.size
                val timeOfDay = determineTimeOfDay(avgSessionDuration)
                
                val severity = when {
                    avgSessionDuration > 2 * 60 * 60 * 1000 -> BingeSeverity.EXTREME // More than 2 hours
                    avgSessionDuration > 60 * 1000 -> BingeSeverity.HIGH       // More than 1 hour
                    avgSessionDuration > 30 * 60 * 1000 -> BingeSeverity.MODERATE  // More than 30 minutes
                    else -> BingeSeverity.LOW
                }
                
                bingePatterns.add(BingeBehaviorPattern(
                    packageName = packageName,
                    sessionDuration = avgSessionDuration,
                    frequency = frequency,
                    timeOfDay = timeOfDay,
                    severity = severity
                ))
            }
        }
        
        return bingePatterns
    }
    
    private fun determineTimeOfDay(duration: Long): String {
        val hour = (duration / (1000 * 60)).toInt()
        return when {
            hour < 6 -> "Night"
            hour < 12 -> "Morning"
            hour < 18 -> "Afternoon"
            else -> "Evening"
        }
    }
    
    /**
     * Detects recovery patterns (periods of reduced usage)
     */
    fun detectRecoveryPatterns(): List<RecoveryPattern> {
        val recoveryPatterns = mutableListOf<RecoveryPattern>()
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -60) // Last 60 days
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Group by date and calculate daily usage
        val dailyUsage = mutableMapOf<Date, MutableMap<String, Long>>()
        
        for (stats in usageStats) {
            val date = Date(stats.lastTimeUsed)
            val dayKey = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val packageName = stats.packageName
            val totalTime = stats.totalTimeInForeground
            
            if (!dailyUsage.containsKey(dayKey)) {
                dailyUsage[dayKey] = mutableMapOf()
            }
            
            dailyUsage[dayKey]?.set(packageName, totalTime)
        }
        
        // Find days with significantly lower usage than average
        val allDailyTotals = dailyUsage.map { entry: Map.Entry<Date, MutableMap<String, Long>> ->
            entry.value.values.sum()
        }.toList()
        
        if (allDailyTotals.isNotEmpty()) {
            val avgDailyUsage = allDailyTotals.average()
            val threshold = avgDailyUsage * 0.7 // 70% of average is considered "recovery"
            
            for ((date, appUsage) in dailyUsage) {
                val dailyTotal = appUsage.values.sum()
                
                if (dailyTotal < threshold) {
                    val recoveryType = when {
                        dailyTotal < avgDailyUsage * 0.3 -> RecoveryType.LONG_TERM
                        dailyTotal < avgDailyUsage * 0.5 -> RecoveryType.MEDIUM_TERM
                        else -> RecoveryType.SHORT_TERM
                    }
                    
                    recoveryPatterns.add(RecoveryPattern(
                        date = date,
                        reducedUsageDuration = dailyTotal,
                        improvedAppUsage = appUsage,
                        recoveryType = recoveryType
                    ))
                }
            }
        }
        
        return recoveryPatterns
    }
    
    /**
     * Integrates with circadian rhythm patterns
     */
    fun detectCircadianRhythm(): CircadianRhythmPattern {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -90) // Last 90 days for better circadian analysis
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Group usage by hour across all days
        val hourlyUsage = mutableMapOf<Int, Long>()
        
        for (stats in usageStats) {
            val lastUsed = Date(stats.lastTimeUsed)
            val hour = Calendar.getInstance().apply { time = lastUsed }.get(Calendar.HOUR_OF_DAY)
            val totalTime = stats.totalTimeInForeground
            
            hourlyUsage[hour] = hourlyUsage.getOrDefault(hour, 0L) + totalTime
        }
        
        // Find peak and low activity hours
        var peakHour = 0
        var peakUsage = 0L
        var lowHour = 0
        var lowUsage = Long.MAX_VALUE
        
        for ((hour, usage) in hourlyUsage) {
            if (usage > peakUsage) {
                peakUsage = usage
                peakHour = hour
            }
            if (usage < lowUsage) {
                lowUsage = usage
                lowHour = hour
            }
        }
        
        // Calculate rhythm consistency (how regular the pattern is)
        val consistency = calculateRhythmConsistency(hourlyUsage)
        
        // Calculate day/night usage ratio
        val dayUsage = (6..18).sumOf { hourlyUsage.getOrDefault(it, 0L) } // 6 AM to 6 PM
        val nightUsage = (0..5).sumOf { hourlyUsage.getOrDefault(it, 0L) } + 
                        (19..23).sumOf { hourlyUsage.getOrDefault(it, 0L) } // Night hours
        
        val dayNightRatio = if (nightUsage > 0) dayUsage.toFloat() / nightUsage else Float.MAX_VALUE
        
        return CircadianRhythmPattern(
            peakActivityHour = peakHour,
            lowActivityHour = lowHour,
            rhythmConsistency = consistency,
            dayNightRatio = dayNightRatio
        )
    }
    
    private fun calculateRhythmConsistency(hourlyUsage: Map<Int, Long>): Float {
        if (hourlyUsage.isEmpty()) return 0f
        
        val values = hourlyUsage.values.toList()
        val mean = values.average()
        val variance = if (values.size > 0) values.sumOf { (it - mean) * (it - mean) } / values.size.toDouble() else 0.0
        
        // Convert variance to a consistency score (lower variance = higher consistency)
        return (1.0f / (1.0f + variance.toFloat())) * 100
    }
    
    /**
     * Predicts the next likely usage spike based on patterns
     */
    fun predictNextUsageSpike(): Date? {
        val peakPatterns = detectPeakUsageTimes()
        if (peakPatterns.isEmpty()) return null
        
        // Get the most common peak hour
        val peakHourCounts = mutableMapOf<Int, Int>()
        for (pattern in peakPatterns) {
            peakHourCounts[pattern.hour] = peakHourCounts.getOrDefault(pattern.hour, 0) + 1
        }
        
        val mostCommonPeakHour = peakHourCounts.maxByOrNull { it.value }?.key ?: return null
        
        // Predict next occurrence based on the most common peak hour
        val nextOccurrence = Calendar.getInstance()
        val currentHour = nextOccurrence.get(Calendar.HOUR_OF_DAY)
        
        if (currentHour >= mostCommonPeakHour) {
            // If we've passed the peak hour today, predict for tomorrow
            nextOccurrence.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        nextOccurrence.set(Calendar.HOUR_OF_DAY, mostCommonPeakHour)
        nextOccurrence.set(Calendar.MINUTE, 0)
        nextOccurrence.set(Calendar.SECOND, 0)
        nextOccurrence.set(Calendar.MILLISECOND, 0)
        
        return nextOccurrence.time
    }
    
    /**
     * Identifies app-specific usage patterns
     */
    fun getAppUsagePatterns(packageName: String): AppUsagePattern {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -30) // Last 30 days
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        val appStats = usageStats.find { it.packageName == packageName }
        if (appStats == null) {
            // Return default pattern if no stats found
            return AppUsagePattern(
                packageName = packageName,
                averageDailyUsage = 0L,
                averageWeeklyUsage = 0L,
                totalUsage = 0L,
                lastUsed = Date(0),
                usageConsistency = 0f,
                trend = UsageTrend.STABLE
            )
        }
        
        val dailyUsage = mutableListOf<Long>()
        val weeklyUsage = mutableMapOf<Int, Long>() // Week number to total usage
        
        // Calculate daily usage patterns
        for (stats in usageStats) {
            if (stats.packageName == packageName) {
                dailyUsage.add(stats.totalTimeInForeground)
                
                // Group by week
                val week = Calendar.getInstance().apply { time = Date(stats.lastTimeUsed) }.get(Calendar.WEEK_OF_YEAR)
                weeklyUsage[week] = weeklyUsage.getOrDefault(week, 0L) + stats.totalTimeInForeground
            }
        }
        
        val avgDailyUsage = if (dailyUsage.isNotEmpty()) dailyUsage.average().toLong() else 0L
        val avgWeeklyUsage = if (weeklyUsage.isNotEmpty()) weeklyUsage.values.average().toLong() else 0L
        
        return AppUsagePattern(
            packageName = packageName,
            averageDailyUsage = avgDailyUsage,
            averageWeeklyUsage = avgWeeklyUsage,
            totalUsage = appStats?.totalTimeInForeground ?: 0L,
            lastUsed = Date(appStats?.lastTimeUsed ?: 0L),
            usageConsistency = calculateUsageConsistency(dailyUsage),
            trend = calculateUsageTrend(dailyUsage)
        )
    }
    
    private fun calculateUsageConsistency(dailyUsage: List<Long>): Float {
        if (dailyUsage.size < 2) return 0f
        
        val mean = dailyUsage.average()
        val variance = dailyUsage.sumOf { (it - mean) * (it - mean) } / dailyUsage.size
        val stdDev = sqrt(variance)
        
        // Coefficient of variation (lower is more consistent)
        return if (mean != 0.0) 1.0f - (stdDev / mean).toFloat() else 1.0f
    }
    
    private fun calculateUsageTrend(dailyUsage: List<Long>): UsageTrend {
        if (dailyUsage.size < 2) return UsageTrend.STABLE
        
        // Simple trend analysis: compare first half vs second half
        val midPoint = dailyUsage.size / 2
        val firstHalf = dailyUsage.take(midPoint)
        val secondHalf = dailyUsage.takeLast(dailyUsage.size - midPoint)
        
        if (firstHalf.isNotEmpty() && secondHalf.isNotEmpty()) {
            val firstAvg = firstHalf.average()
            val secondAvg = secondHalf.average()
            
            return when {
                secondAvg > firstAvg * 1.2 -> UsageTrend.INCREASING
                secondAvg < firstAvg * 0.8 -> UsageTrend.DECREASING
                else -> UsageTrend.STABLE
            }
        }
        
        return UsageTrend.STABLE
    }
    
    enum class UsageTrend {
        INCREASING, DECREASING, STABLE
    }
    
    data class AppUsagePattern(
        val packageName: String,
        val averageDailyUsage: Long,
        val averageWeeklyUsage: Long,
        val totalUsage: Long,
        val lastUsed: Date,
        val usageConsistency: Float,
        val trend: UsageTrend
    )

    /**
     * Gets peak usage hours from detected peak usage patterns
     */
    fun getPeakUsageHours(): List<Int> {
        val peakPatterns = detectPeakUsageTimes()
        return peakPatterns.map { it.hour }.distinct()
    }
}
