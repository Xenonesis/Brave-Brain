package com.bravebrain

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Unit tests for ScreentimeStatsHelper
 */
class ScreentimeStatsHelperTest {

    @Test
    fun `test formatTime with hours and minutes`() {
        val result = ScreentimeStatsHelper.formatTime(150) // 2h 30m
        assertEquals("2h 30m", result)
    }

    @Test
    fun `test formatTime with only minutes`() {
        val result = ScreentimeStatsHelper.formatTime(45) // 45m
        assertEquals("45m", result)
    }

    @Test
    fun `test formatTime with zero minutes`() {
        val result = ScreentimeStatsHelper.formatTime(0)
        assertEquals("0m", result)
    }

    @Test
    fun `test formatTime with exact hours`() {
        val result = ScreentimeStatsHelper.formatTime(120) // 2h 0m
        assertEquals("2h 0m", result)
    }

    @Test
    fun `test groupByWeek with empty list`() {
        val emptyList = emptyList<DailyScreenTime>()
        val result = ScreentimeStatsHelper.groupByWeek(emptyList)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test groupByWeek groups same week together`() {
        val data = listOf(
            createDailyScreenTime("2024-01-15", 120), // Monday
            createDailyScreenTime("2024-01-16", 150), // Tuesday
            createDailyScreenTime("2024-01-17", 90)   // Wednesday
        )
        
        val result = ScreentimeStatsHelper.groupByWeek(data)
        assertEquals(1, result.size)
        
        val weekStats = result.values.first()
        assertEquals(3, weekStats.daysCount)
        assertEquals(360, weekStats.totalMinutes) // 120 + 150 + 90
        assertEquals(120, weekStats.averageMinutes) // 360 / 3
    }

    @Test
    fun `test groupByWeek separates different weeks`() {
        val data = listOf(
            createDailyScreenTime("2024-01-15", 120), // Week 1
            createDailyScreenTime("2024-01-22", 150)  // Week 2
        )
        
        val result = ScreentimeStatsHelper.groupByWeek(data)
        assertEquals(2, result.size)
    }

    @Test
    fun `test groupByMonth with empty list`() {
        val emptyList = emptyList<DailyScreenTime>()
        val result = ScreentimeStatsHelper.groupByMonth(emptyList)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test groupByMonth groups same month together`() {
        val data = listOf(
            createDailyScreenTime("2024-01-15", 120),
            createDailyScreenTime("2024-01-20", 150),
            createDailyScreenTime("2024-01-25", 90)
        )
        
        val result = ScreentimeStatsHelper.groupByMonth(data)
        assertEquals(1, result.size)
        
        val monthStats = result.values.first()
        assertEquals(3, monthStats.daysCount)
        assertEquals(360, monthStats.totalMinutes)
        assertEquals(120, monthStats.averageMinutes)
    }

    @Test
    fun `test groupByMonth separates different months`() {
        val data = listOf(
            createDailyScreenTime("2024-01-15", 120),
            createDailyScreenTime("2024-02-15", 150)
        )
        
        val result = ScreentimeStatsHelper.groupByMonth(data)
        assertEquals(2, result.size)
    }

    @Test
    fun `test calculateOverallStats with empty list`() {
        val emptyList = emptyList<DailyScreenTime>()
        val result = ScreentimeStatsHelper.calculateOverallStats(emptyList)
        
        assertEquals(0, result.totalMinutes)
        assertEquals(0, result.averageMinutes)
        assertEquals(0, result.daysCount)
        assertTrue(result.topApps.isEmpty())
    }

    @Test
    fun `test calculateOverallStats with data`() {
        val data = listOf(
            createDailyScreenTime("2024-01-15", 120),
            createDailyScreenTime("2024-01-16", 180),
            createDailyScreenTime("2024-01-17", 60)
        )
        
        val result = ScreentimeStatsHelper.calculateOverallStats(data)
        
        assertEquals(360, result.totalMinutes) // 120 + 180 + 60
        assertEquals(120, result.averageMinutes) // 360 / 3
        assertEquals(180, result.maxDayMinutes)
        assertEquals(60, result.minDayMinutes)
        assertEquals(3, result.daysCount)
    }

    @Test
    fun `test calculateOverallStats aggregates top apps`() {
        val data = listOf(
            createDailyScreenTimeWithApps("2024-01-15", 120, listOf(
                mapOf("appName" to "Instagram", "usageMinutes" to 60),
                mapOf("appName" to "YouTube", "usageMinutes" to 30)
            )),
            createDailyScreenTimeWithApps("2024-01-16", 150, listOf(
                mapOf("appName" to "Instagram", "usageMinutes" to 70),
                mapOf("appName" to "Chrome", "usageMinutes" to 40)
            ))
        )
        
        val result = ScreentimeStatsHelper.calculateOverallStats(data)
        
        assertTrue(result.topApps.containsKey("Instagram"))
        assertEquals(130, result.topApps["Instagram"]) // 60 + 70
        assertTrue(result.topApps.containsKey("YouTube"))
        assertEquals(30, result.topApps["YouTube"])
        assertTrue(result.topApps.containsKey("Chrome"))
        assertEquals(40, result.topApps["Chrome"])
    }

    @Test
    fun `test weekly stats calculates max and min correctly`() {
        val data = listOf(
            createDailyScreenTime("2024-01-15", 120),
            createDailyScreenTime("2024-01-16", 200),
            createDailyScreenTime("2024-01-17", 80)
        )
        
        val result = ScreentimeStatsHelper.groupByWeek(data)
        val weekStats = result.values.first()
        
        assertEquals(200, weekStats.maxDayMinutes)
        assertEquals(80, weekStats.minDayMinutes)
    }

    @Test
    fun `test monthly stats calculates max and min correctly`() {
        val data = listOf(
            createDailyScreenTime("2024-01-15", 120),
            createDailyScreenTime("2024-01-20", 250),
            createDailyScreenTime("2024-01-25", 90)
        )
        
        val result = ScreentimeStatsHelper.groupByMonth(data)
        val monthStats = result.values.first()
        
        assertEquals(250, monthStats.maxDayMinutes)
        assertEquals(90, monthStats.minDayMinutes)
    }

    @Test
    fun `test overall stats limits top apps to 10`() {
        val apps = (1..15).map { i ->
            mapOf("appName" to "App$i", "usageMinutes" to i * 10)
        }
        
        val data = listOf(
            createDailyScreenTimeWithApps("2024-01-15", 300, apps)
        )
        
        val result = ScreentimeStatsHelper.calculateOverallStats(data)
        
        assertEquals(10, result.topApps.size)
    }

    @Test
    fun `test overall stats orders top apps by usage descending`() {
        val data = listOf(
            createDailyScreenTimeWithApps("2024-01-15", 300, listOf(
                mapOf("appName" to "Instagram", "usageMinutes" to 50),
                mapOf("appName" to "YouTube", "usageMinutes" to 100),
                mapOf("appName" to "Chrome", "usageMinutes" to 30)
            ))
        )
        
        val result = ScreentimeStatsHelper.calculateOverallStats(data)
        val topAppsList = result.topApps.entries.toList()
        
        // YouTube should be first with 100 minutes
        assertEquals("YouTube", topAppsList[0].key)
        assertEquals(100, topAppsList[0].value)
    }

    // Helper functions to create test data
    private fun createDailyScreenTime(date: String, minutes: Int): DailyScreenTime {
        return DailyScreenTime(
            id = "test_$date",
            userId = "testUser",
            date = date,
            totalScreenTimeMs = minutes * 60 * 1000L,
            screenTimeMinutes = minutes,
            topApps = emptyList(),
            hourlyBreakdown = emptyMap(),
            timestamp = Timestamp.now()
        )
    }

    private fun createDailyScreenTimeWithApps(
        date: String,
        minutes: Int,
        apps: List<Map<String, Any>>
    ): DailyScreenTime {
        return DailyScreenTime(
            id = "test_$date",
            userId = "testUser",
            date = date,
            totalScreenTimeMs = minutes * 60 * 1000L,
            screenTimeMinutes = minutes,
            topApps = apps,
            hourlyBreakdown = emptyMap(),
            timestamp = Timestamp.now()
        )
    }
}
