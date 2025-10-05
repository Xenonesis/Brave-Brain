package com.example.testing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.app.usage.UsageStatsManager
import java.util.*
import kotlin.math.sqrt

/**
 * ContextDetectionModule integrates with device sensors and usage stats
 * to provide comprehensive context detection
 */
class ContextDetectionModule(private val context: Context) {
    
    companion object {
        private const val CONTEXT_DETECTION_PREFS = "context_detection_prefs"
        private const val LAST_SENSOR_UPDATE = "last_sensor_update"
        private const val MOTION_THRESHOLD = 0.5f
    }
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    
    private var accelerometerSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private var motionDetector: MotionDetector? = null
    
    data class DeviceContext(
        val isMoving: Boolean,
        val isPocket: Boolean,
        val ambientLightLevel: Float,
        val batteryLevel: Int,
        val isCharging: Boolean,
        val screenOn: Boolean,
        val proximityNear: Boolean,
        val motionIntensity: MotionIntensity
    )
    
    enum class MotionIntensity {
        STATIONARY, LIGHT_MOVEMENT, MODERATE_MOVEMENT, HIGH_MOVEMENT
    }
    
    init {
        initializeSensors()
        motionDetector = MotionDetector()
    }
    
    private fun initializeSensors() {
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }
    
    /**
     * Detects the current device context using sensor data
     */
    fun detectDeviceContext(): DeviceContext {
        val isMoving = isDeviceMoving()
        val isPocket = isDeviceInPocket()
        val ambientLightLevel = getAmbientLightLevel()
        val batteryLevel = getBatteryLevel()
        val isCharging = isDeviceCharging()
        val screenOn = isScreenOn()
        val proximityNear = isProximityNear()
        val motionIntensity = getMotionIntensity()
        
        return DeviceContext(
            isMoving = isMoving,
            isPocket = isPocket,
            ambientLightLevel = ambientLightLevel,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            screenOn = screenOn,
            proximityNear = proximityNear,
            motionIntensity = motionIntensity
        )
    }
    
    private fun isDeviceMoving(): Boolean {
        // In a real implementation, this would use real-time accelerometer data
        // For now, we'll simulate based on recent usage patterns
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MINUTE, -5) // Last 5 minutes
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // If there's been recent interaction, assume device is moving
        return usageStats.isNotEmpty()
    }
    
    private fun isDeviceInPocket(): Boolean {
        // This would normally use proximity sensor data
        // For now, using a simulated approach
        return false // Default assumption
    }
    
    private fun getAmbientLightLevel(): Float {
        // This would normally use light sensor data
        // For now, returning a default value
        return 100f // Default ambient light level
    }
    
    private fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level == -1 || scale == -1) 50 else (level * 100 / scale)
    }
    
    private fun isDeviceCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
               status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }
    
    private fun isScreenOn(): Boolean {
        return powerManager.isInteractive
    }
    
    private fun isProximityNear(): Boolean {
        // This would normally use proximity sensor data
        // For now, returning a default value
        return false
    }
    
    private fun getMotionIntensity(): MotionIntensity {
        // Analyze recent usage to determine motion intensity
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MINUTE, -10) // Last 10 minutes
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Calculate motion based on interaction frequency
        val interactionCount = usageStats.size
        return when {
            interactionCount == 0 -> MotionIntensity.STATIONARY
            interactionCount < 3 -> MotionIntensity.LIGHT_MOVEMENT
            interactionCount < 10 -> MotionIntensity.MODERATE_MOVEMENT
            else -> MotionIntensity.HIGH_MOVEMENT
        }
    }
    
    /**
     * Registers sensor listeners for real-time context detection
     */
    fun registerSensorListeners(listener: SensorEventListener) {
        accelerometerSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        proximitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        lightSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    /**
     * Unregisters sensor listeners
     */
    fun unregisterSensorListeners(listener: SensorEventListener) {
        sensorManager.unregisterListener(listener)
    }
    
    /**
     * Gets user's usage patterns from the last week
     */
    fun getWeeklyUsagePatterns(): List<UsagePattern> {
        val patterns = mutableListOf<UsagePattern>()
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Last 7 days
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        for (stats in usageStats) {
            val packageName = stats.packageName
            val totalTime = stats.totalTimeInForeground
            val lastTimeUsed = stats.lastTimeUsed
            
            patterns.add(UsagePattern(
                packageName = packageName,
                totalTime = totalTime,
                lastTimeUsed = Date(lastTimeUsed),
                dailyAverage = totalTime / 7 // Rough average over 7 days
            ))
        }
        
        return patterns
    }
    
    /**
     * Detects if the user is in a meeting based on usage patterns
     */
    fun isUserInMeeting(): Boolean {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MINUTE, -30) // Last 30 minutes
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // If there's been minimal interaction in the last 30 minutes, 
        // user might be in a meeting
        return usageStats.isEmpty()
    }
    
    /**
     * Detects if the user is commuting based on movement patterns
     */
    fun isUserCommuting(): Boolean {
        // This would use real-time motion data
        // For now, simulating based on usage patterns during commute times
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Typical commute times: 7-9 AM and 5-7 PM
        return (hour in 7..9) || (hour in 17..19)
    }
    
    /**
     * Gets the user's peak usage hours
     */
    fun getPeakUsageHours(): List<Int> {
        val peakHours = mutableListOf<Int>()
        val usageByHour = mutableMapOf<Int, Long>()
        
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Last 7 days
        val startTime = calendar.timeInMillis
        
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        for (stats in usageStats) {
            val lastUsed = Date(stats.lastTimeUsed)
            val hour = Calendar.getInstance().apply { time = lastUsed }.get(Calendar.HOUR_OF_DAY)
            
            usageByHour[hour] = usageByHour.getOrDefault(hour, 0L) + stats.totalTimeInForeground
        }
        
        // Find top 3 hours with highest usage
        val sortedHours = usageByHour.entries.sortedByDescending { it.value }
        for (i in 0 until minOf(3, sortedHours.size)) {
            peakHours.add(sortedHours[i].key)
        }
        
        return peakHours
    }
    
    /**
     * Inner class for motion detection using accelerometer
     */
    inner class MotionDetector : SensorEventListener {
        private var lastX = 0f
        private var lastY = 0f
        private var lastZ = 0f
        private var lastTimestamp = 0L
        private var motionDetected = false
        
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val timestamp = System.currentTimeMillis()
                
                if (lastTimestamp != 0L) {
                    val deltaX = kotlin.math.abs(x - lastX)
                    val deltaY = kotlin.math.abs(y - lastY)
                    val deltaZ = kotlin.math.abs(z - lastZ)
                    val totalDelta = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
                    
                    if (totalDelta > MOTION_THRESHOLD) {
                        motionDetected = true
                    }
                }
                
                lastX = x
                lastY = y
                lastZ = z
                lastTimestamp = timestamp
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
        
        fun isMoving(): Boolean {
            val result = motionDetected
            motionDetected = false // Reset after reading
            return result
        }
    }
    
    data class UsagePattern(
        val packageName: String,
        val totalTime: Long,
        val lastTimeUsed: Date,
        val dailyAverage: Long
    )
}