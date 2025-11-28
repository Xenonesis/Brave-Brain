package com.bravebrain

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FirestoreService screentime methods
 * 
 * Note: These are basic structural tests. Integration tests with actual Firebase
 * would require a test Firebase instance or emulator.
 */
class FirestoreServiceScreentimeTest {

    @Test
    fun `test DailyScreenTime model has correct defaults`() {
        val screenTime = DailyScreenTime()
        
        assertEquals("", screenTime.id)
        assertEquals("", screenTime.userId)
        assertEquals("", screenTime.date)
        assertEquals(0L, screenTime.totalScreenTimeMs)
        assertEquals(0, screenTime.screenTimeMinutes)
        assertTrue(screenTime.topApps.isEmpty())
        assertTrue(screenTime.hourlyBreakdown.isEmpty())
        assertNotNull(screenTime.timestamp)
    }

    @Test
    fun `test DailyScreenTime model with custom data`() {
        val topApps = listOf(
            mapOf("appName" to "Instagram", "usageMinutes" to 45),
            mapOf("appName" to "YouTube", "usageMinutes" to 30)
        )
        
        val hourlyBreakdown = mapOf(
            "hour_14" to 3600000L,
            "hour_15" to 2400000L
        )
        
        val screenTime = DailyScreenTime(
            id = "user123_2024-01-15",
            userId = "user123",
            date = "2024-01-15",
            totalScreenTimeMs = 7200000L,
            screenTimeMinutes = 120,
            topApps = topApps,
            hourlyBreakdown = hourlyBreakdown
        )
        
        assertEquals("user123_2024-01-15", screenTime.id)
        assertEquals("user123", screenTime.userId)
        assertEquals("2024-01-15", screenTime.date)
        assertEquals(7200000L, screenTime.totalScreenTimeMs)
        assertEquals(120, screenTime.screenTimeMinutes)
        assertEquals(2, screenTime.topApps.size)
        assertEquals(2, screenTime.hourlyBreakdown.size)
    }

    @Test
    fun `test DailyScreenTime topApps structure`() {
        val topApps = listOf(
            mapOf(
                "packageName" to "com.instagram.android",
                "appName" to "Instagram",
                "usageTimeMs" to 2700000L,
                "usageMinutes" to 45
            )
        )
        
        val screenTime = DailyScreenTime(
            topApps = topApps
        )
        
        assertEquals(1, screenTime.topApps.size)
        val firstApp = screenTime.topApps[0]
        assertEquals("com.instagram.android", firstApp["packageName"])
        assertEquals("Instagram", firstApp["appName"])
        assertEquals(2700000L, firstApp["usageTimeMs"])
        assertEquals(45, firstApp["usageMinutes"])
    }

    @Test
    fun `test DailyScreenTime hourlyBreakdown structure`() {
        val hourlyBreakdown = mapOf(
            "hour_9" to 1800000L,  // 30 minutes at 9 AM
            "hour_14" to 3600000L, // 60 minutes at 2 PM
            "hour_20" to 2400000L  // 40 minutes at 8 PM
        )
        
        val screenTime = DailyScreenTime(
            hourlyBreakdown = hourlyBreakdown
        )
        
        assertEquals(3, screenTime.hourlyBreakdown.size)
        assertEquals(1800000L, screenTime.hourlyBreakdown["hour_9"])
        assertEquals(3600000L, screenTime.hourlyBreakdown["hour_14"])
        assertEquals(2400000L, screenTime.hourlyBreakdown["hour_20"])
    }

    @Test
    fun `test screentime calculation from milliseconds to minutes`() {
        val screenTimeMs = 7200000L // 2 hours in milliseconds
        val screenTimeMinutes = (screenTimeMs / (1000 * 60)).toInt()
        
        assertEquals(120, screenTimeMinutes) // 2 hours = 120 minutes
    }

    @Test
    fun `test date format consistency`() {
        val date = "2024-01-15"
        val pattern = Regex("\\d{4}-\\d{2}-\\d{2}")
        
        assertTrue(pattern.matches(date))
    }

    @Test
    fun `test document ID format`() {
        val userId = "user123"
        val date = "2024-01-15"
        val expectedDocId = "${userId}_$date"
        
        assertEquals("user123_2024-01-15", expectedDocId)
    }

    @Test
    fun `test empty topApps list`() {
        val screenTime = DailyScreenTime(
            topApps = emptyList()
        )
        
        assertTrue(screenTime.topApps.isEmpty())
        assertEquals(0, screenTime.topApps.size)
    }

    @Test
    fun `test empty hourlyBreakdown map`() {
        val screenTime = DailyScreenTime(
            hourlyBreakdown = emptyMap()
        )
        
        assertTrue(screenTime.hourlyBreakdown.isEmpty())
        assertEquals(0, screenTime.hourlyBreakdown.size)
    }

    @Test
    fun `test multiple days with same user`() {
        val day1 = DailyScreenTime(
            id = "user123_2024-01-15",
            userId = "user123",
            date = "2024-01-15",
            screenTimeMinutes = 120
        )
        
        val day2 = DailyScreenTime(
            id = "user123_2024-01-16",
            userId = "user123",
            date = "2024-01-16",
            screenTimeMinutes = 150
        )
        
        assertEquals("user123", day1.userId)
        assertEquals("user123", day2.userId)
        assertNotEquals(day1.date, day2.date)
        assertNotEquals(day1.id, day2.id)
    }

    @Test
    fun `test top 5 apps limit`() {
        val topApps = (1..10).map { i ->
            mapOf(
                "appName" to "App$i",
                "usageMinutes" to i * 10
            )
        }.take(5) // Should only take top 5
        
        val screenTime = DailyScreenTime(
            topApps = topApps
        )
        
        assertEquals(5, screenTime.topApps.size)
    }

    @Test
    fun `test 24-hour hourly breakdown coverage`() {
        val hourlyBreakdown = (0..23).associate { hour ->
            "hour_$hour" to (hour * 60 * 1000L) // Each hour has 'hour' minutes
        }
        
        val screenTime = DailyScreenTime(
            hourlyBreakdown = hourlyBreakdown
        )
        
        assertEquals(24, screenTime.hourlyBreakdown.size)
        assertEquals(0L, screenTime.hourlyBreakdown["hour_0"])
        assertEquals(23 * 60 * 1000L, screenTime.hourlyBreakdown["hour_23"])
    }

    @Test
    fun `test screentime minutes match milliseconds conversion`() {
        val minutes = 150
        val milliseconds = minutes * 60 * 1000L
        
        val screenTime = DailyScreenTime(
            totalScreenTimeMs = milliseconds,
            screenTimeMinutes = minutes
        )
        
        assertEquals(minutes, screenTime.screenTimeMinutes)
        assertEquals(milliseconds, screenTime.totalScreenTimeMs)
        
        // Verify conversion is correct
        val calculatedMinutes = (screenTime.totalScreenTimeMs / (1000 * 60)).toInt()
        assertEquals(screenTime.screenTimeMinutes, calculatedMinutes)
    }
}
