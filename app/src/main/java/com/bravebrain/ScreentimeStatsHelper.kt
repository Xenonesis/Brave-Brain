package com.bravebrain

import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for calculating screentime statistics
 */
object ScreentimeStatsHelper {
    
    /**
     * Groups daily screentime data by week
     */
    fun groupByWeek(dailyData: List<DailyScreenTime>): Map<String, WeeklyStats> {
        val weeklyMap = mutableMapOf<String, MutableList<DailyScreenTime>>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        dailyData.forEach { day ->
            try {
                val date = dateFormat.parse(day.date) ?: return@forEach
                calendar.time = date
                
                // Get week of year
                val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                val year = calendar.get(Calendar.YEAR)
                val weekKey = "$year-W$weekOfYear"
                
                weeklyMap.getOrPut(weekKey) { mutableListOf() }.add(day)
            } catch (e: Exception) {
                // Skip invalid dates
            }
        }
        
        return weeklyMap.mapValues { (weekKey, days) ->
            calculateWeeklyStats(weekKey, days)
        }
    }
    
    /**
     * Groups daily screentime data by month
     */
    fun groupByMonth(dailyData: List<DailyScreenTime>): Map<String, MonthlyStats> {
        val monthlyMap = mutableMapOf<String, MutableList<DailyScreenTime>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        dailyData.forEach { day ->
            try {
                val date = dateFormat.parse(day.date) ?: return@forEach
                val monthKey = monthFormat.format(date)
                
                monthlyMap.getOrPut(monthKey) { mutableListOf() }.add(day)
            } catch (e: Exception) {
                // Skip invalid dates
            }
        }
        
        return monthlyMap.mapValues { (monthKey, days) ->
            calculateMonthlyStats(monthKey, days)
        }
    }
    
    /**
     * Calculates weekly statistics
     */
    private fun calculateWeeklyStats(weekKey: String, days: List<DailyScreenTime>): WeeklyStats {
        val totalMinutes = days.sumOf { it.screenTimeMinutes }
        val avgMinutes = if (days.isNotEmpty()) totalMinutes / days.size else 0
        val maxDay = days.maxByOrNull { it.screenTimeMinutes }
        val minDay = days.minByOrNull { it.screenTimeMinutes }
        
        // Get date range for the week
        val sortedDays = days.sortedBy { it.date }
        val startDate = sortedDays.firstOrNull()?.date ?: ""
        val endDate = sortedDays.lastOrNull()?.date ?: ""
        
        return WeeklyStats(
            weekKey = weekKey,
            startDate = startDate,
            endDate = endDate,
            totalMinutes = totalMinutes,
            averageMinutes = avgMinutes,
            daysCount = days.size,
            maxDayMinutes = maxDay?.screenTimeMinutes ?: 0,
            minDayMinutes = minDay?.screenTimeMinutes ?: 0,
            dailyData = days
        )
    }
    
    /**
     * Calculates monthly statistics
     */
    private fun calculateMonthlyStats(monthKey: String, days: List<DailyScreenTime>): MonthlyStats {
        val totalMinutes = days.sumOf { it.screenTimeMinutes }
        val avgMinutes = if (days.isNotEmpty()) totalMinutes / days.size else 0
        val maxDay = days.maxByOrNull { it.screenTimeMinutes }
        val minDay = days.minByOrNull { it.screenTimeMinutes }
        
        // Parse month for display
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val displayName = try {
            val date = monthFormat.parse(monthKey)
            displayFormat.format(date ?: Date())
        } catch (e: Exception) {
            monthKey
        }
        
        return MonthlyStats(
            monthKey = monthKey,
            monthName = displayName,
            totalMinutes = totalMinutes,
            averageMinutes = avgMinutes,
            daysCount = days.size,
            maxDayMinutes = maxDay?.screenTimeMinutes ?: 0,
            minDayMinutes = minDay?.screenTimeMinutes ?: 0,
            dailyData = days
        )
    }
    
    /**
     * Calculates overall statistics
     */
    fun calculateOverallStats(dailyData: List<DailyScreenTime>): OverallStats {
        if (dailyData.isEmpty()) {
            return OverallStats(0, 0, 0, 0, 0, emptyMap())
        }
        
        val totalMinutes = dailyData.sumOf { it.screenTimeMinutes }
        val avgMinutes = totalMinutes / dailyData.size
        val maxDay = dailyData.maxByOrNull { it.screenTimeMinutes }
        val minDay = dailyData.minByOrNull { it.screenTimeMinutes }
        
        // Calculate top apps across all days
        val appUsageMap = mutableMapOf<String, Int>()
        dailyData.forEach { day ->
            day.topApps.forEach { app ->
                val appName = app["appName"] as? String ?: "Unknown"
                val minutes = (app["usageMinutes"] as? Number)?.toInt() ?: 0
                appUsageMap[appName] = (appUsageMap[appName] ?: 0) + minutes
            }
        }
        
        val topApps = appUsageMap.entries
            .sortedByDescending { it.value }
            .take(10)
            .associate { it.key to it.value }
        
        return OverallStats(
            totalMinutes = totalMinutes,
            averageMinutes = avgMinutes,
            maxDayMinutes = maxDay?.screenTimeMinutes ?: 0,
            minDayMinutes = minDay?.screenTimeMinutes ?: 0,
            daysCount = dailyData.size,
            topApps = topApps
        )
    }
    
    /**
     * Gets formatted time string
     */
    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }
}

/**
 * Data class for weekly statistics
 */
data class WeeklyStats(
    val weekKey: String,
    val startDate: String,
    val endDate: String,
    val totalMinutes: Int,
    val averageMinutes: Int,
    val daysCount: Int,
    val maxDayMinutes: Int,
    val minDayMinutes: Int,
    val dailyData: List<DailyScreenTime>
)

/**
 * Data class for monthly statistics
 */
data class MonthlyStats(
    val monthKey: String,
    val monthName: String,
    val totalMinutes: Int,
    val averageMinutes: Int,
    val daysCount: Int,
    val maxDayMinutes: Int,
    val minDayMinutes: Int,
    val dailyData: List<DailyScreenTime>
)

/**
 * Data class for overall statistics
 */
data class OverallStats(
    val totalMinutes: Int,
    val averageMinutes: Int,
    val maxDayMinutes: Int,
    val minDayMinutes: Int,
    val daysCount: Int,
    val topApps: Map<String, Int>
)
