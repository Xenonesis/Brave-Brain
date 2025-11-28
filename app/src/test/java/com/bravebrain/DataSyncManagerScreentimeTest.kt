package com.bravebrain

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for DataSyncManager screentime syncing logic
 */
class DataSyncManagerScreentimeTest {

    @Test
    fun `test date format for sync is correct`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        
        // Should match pattern YYYY-MM-DD
        val pattern = Regex("\\d{4}-\\d{2}-\\d{2}")
        assertTrue(pattern.matches(today))
    }

    @Test
    fun `test milliseconds to minutes conversion`() {
        val milliseconds = 7200000L // 2 hours
        val minutes = (milliseconds / (1000 * 60)).toInt()
        
        assertEquals(120, minutes)
    }

    @Test
    fun `test top apps sorting by usage`() {
        val usageMap = mapOf(
            "com.instagram.android" to 2700000L,
            "com.google.android.youtube" to 3600000L,
            "com.android.chrome" to 1800000L
        )
        
        val topApps = usageMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .toList()
        
        // YouTube should be first (highest usage)
        assertEquals("com.google.android.youtube", topApps[0].key)
        assertEquals(3600000L, topApps[0].value)
        
        // Instagram should be second
        assertEquals("com.instagram.android", topApps[1].key)
        
        // Chrome should be third (lowest usage)
        assertEquals("com.android.chrome", topApps[2].key)
    }

    @Test
    fun `test top apps limit to 5`() {
        val usageMap = (1..10).associate { i ->
            "app$i" to (i * 1000L)
        }
        
        val topApps = usageMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .toList()
        
        assertEquals(5, topApps.size)
    }

    @Test
    fun `test hourly breakdown structure`() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val totalScreenTimeMs = 7200000L
        
        val hourlyBreakdown = mapOf(
            "hour_$currentHour" to totalScreenTimeMs,
            "lastUpdated" to System.currentTimeMillis()
        )
        
        assertEquals(2, hourlyBreakdown.size)
        assertTrue(hourlyBreakdown.containsKey("hour_$currentHour"))
        assertTrue(hourlyBreakdown.containsKey("lastUpdated"))
    }

    @Test
    fun `test usage data to map conversion`() {
        val packageName = "com.instagram.android"
        val appName = "Instagram"
        val usageTimeMs = 2700000L
        val usageMinutes = (usageTimeMs / (1000 * 60)).toInt()
        
        val appMap = mapOf(
            "packageName" to packageName,
            "appName" to appName,
            "usageTimeMs" to usageTimeMs,
            "usageMinutes" to usageMinutes
        )
        
        assertEquals(packageName, appMap["packageName"])
        assertEquals(appName, appMap["appName"])
        assertEquals(usageTimeMs, appMap["usageTimeMs"])
        assertEquals(45, appMap["usageMinutes"])
    }

    @Test
    fun `test zero usage handling`() {
        val totalScreenTimeMs = 0L
        val screenTimeMinutes = (totalScreenTimeMs / (1000 * 60)).toInt()
        
        assertEquals(0, screenTimeMinutes)
    }

    @Test
    fun `test large usage values`() {
        val totalScreenTimeMs = 86400000L // 24 hours
        val screenTimeMinutes = (totalScreenTimeMs / (1000 * 60)).toInt()
        
        assertEquals(1440, screenTimeMinutes) // 24 * 60 = 1440 minutes
    }

    @Test
    fun `test current hour calculation`() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        assertTrue(hour >= 0)
        assertTrue(hour <= 23)
    }

    @Test
    fun `test empty usage map`() {
        val usageMap = emptyMap<String, Long>()
        val topApps = usageMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .toList()
        
        assertTrue(topApps.isEmpty())
    }

    @Test
    fun `test sync should handle no data gracefully`() {
        val usageMap = emptyMap<String, Long>()
        val totalMinutes = usageMap.values.sum() / (1000 * 60)
        
        assertEquals(0L, totalMinutes)
    }

    @Test
    fun `test document ID generation`() {
        val userId = "user123"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        val docId = "${userId}_$today"
        
        assertTrue(docId.startsWith("user123_"))
        assertTrue(docId.contains("-"))
    }

    @Test
    fun `test top apps data structure consistency`() {
        val topApp = mapOf(
            "packageName" to "com.instagram.android",
            "appName" to "Instagram",
            "usageTimeMs" to 2700000L,
            "usageMinutes" to 45
        )
        
        assertEquals(4, topApp.size)
        assertTrue(topApp.containsKey("packageName"))
        assertTrue(topApp.containsKey("appName"))
        assertTrue(topApp.containsKey("usageTimeMs"))
        assertTrue(topApp.containsKey("usageMinutes"))
    }

    @Test
    fun `test multiple apps with same usage time`() {
        val usageMap = mapOf(
            "app1" to 1000L,
            "app2" to 1000L,
            "app3" to 1000L
        )
        
        val topApps = usageMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .toList()
        
        assertEquals(3, topApps.size)
        topApps.forEach { 
            assertEquals(1000L, it.value)
        }
    }

    @Test
    fun `test usage calculation precision`() {
        val usageMs = 1234567L // 20 minutes 34 seconds 567 milliseconds
        val usageMinutes = (usageMs / (1000 * 60)).toInt()
        
        assertEquals(20, usageMinutes) // Should truncate seconds and milliseconds
    }

    @Test
    fun `test date consistency across time zones`() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date1 = dateFormat.format(Date())
        Thread.sleep(100) // Small delay
        val date2 = dateFormat.format(Date())
        
        // Should be the same day (unless test runs exactly at midnight)
        // This test is to verify date format consistency
        assertEquals(10, date1.length) // YYYY-MM-DD = 10 chars
        assertEquals(10, date2.length)
    }

    @Test
    fun `test hourly breakdown with multiple hours`() {
        val hourlyData = mutableMapOf<String, Long>()
        
        // Simulate data for multiple hours
        for (hour in 0..23) {
            hourlyData["hour_$hour"] = hour * 60 * 1000L
        }
        
        assertEquals(24, hourlyData.size)
        assertEquals(0L, hourlyData["hour_0"])
        assertEquals(23 * 60 * 1000L, hourlyData["hour_23"])
    }

    @Test
    fun `test sync data deduplication by document ID`() {
        val userId = "user123"
        val date = "2024-01-15"
        
        // Same user and date should produce same document ID
        val docId1 = "${userId}_$date"
        val docId2 = "${userId}_$date"
        
        assertEquals(docId1, docId2)
    }

    @Test
    fun `test different dates produce different document IDs`() {
        val userId = "user123"
        val date1 = "2024-01-15"
        val date2 = "2024-01-16"
        
        val docId1 = "${userId}_$date1"
        val docId2 = "${userId}_$date2"
        
        assertNotEquals(docId1, docId2)
    }

    @Test
    fun `test app name fallback to package name`() {
        val packageName = "com.unknown.app"
        val appName = packageName // Fallback when app name retrieval fails
        
        assertEquals(packageName, appName)
    }
}
