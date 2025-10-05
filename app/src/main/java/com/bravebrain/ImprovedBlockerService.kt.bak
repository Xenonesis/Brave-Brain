package com.bravebrain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.provider.Settings
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.widget.Toast
import android.app.ActivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImprovedBlockerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMs = 1000L // Check every second for better responsiveness
    private var lastCheckedPackage: String? = null
    private var lastForegroundTimestamp: Long = 0L
    private var blockingInProgress = false
    private var isServiceRunning = false
    
    // Notification integration components
    private var advancedNotificationManager: AdvancedNotificationManager? = null
    private var timeLimitNotificationIntegration: TimeLimitNotificationIntegration? = null
    private var blockingNotificationIntegration: BlockingNotificationIntegration? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "blocker_service_channel"
        private const val BLOCKING_CHANNEL_ID = "blocking_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("ImprovedBlockerService", "Service created")
        
        // Initialize notification components
        advancedNotificationManager = AdvancedNotificationManager(this)
        timeLimitNotificationIntegration = TimeLimitNotificationIntegration(this, advancedNotificationManager!!)
        blockingNotificationIntegration = BlockingNotificationIntegration(this, advancedNotificationManager!!)
        
        // Start notification system
        advancedNotificationManager?.start()
        
        // Start foreground service immediately
        startForegroundServiceWithNotification()
        isServiceRunning = true
        
        // Start monitoring
        handler.post(monitoringRunnable)
    }

    private val monitoringRunnable = object : Runnable {
        override fun run() {
            if (isServiceRunning) {
                try {
                    UsageUtils.resetIfNeeded(this@ImprovedBlockerService)
                    checkAndBlockApps()
                } catch (e: Exception) {
                    android.util.Log.e("ImprovedBlockerService", "Error in monitoring: ${e.message}")
                }
                handler.postDelayed(this, checkIntervalMs)
            }
        }
    }

    private fun checkAndBlockApps() {
        // Check permissions first
        if (!hasUsageStatsPermission(this)) {
            android.util.Log.e("ImprovedBlockerService", "Missing usage stats permission")
            return
        }

        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        val timeLimitsString = prefs.getString("time_limits", "")
        
        if (blockedApps.isEmpty() || timeLimitsString.isNullOrEmpty()) {
            android.util.Log.d("ImprovedBlockerService", "No apps to monitor")
            return
        }

        // Parse time limits
        val timeLimits = parseTimeLimits(timeLimitsString)
        
        // Get current foreground app
        val currentApp = getCurrentForegroundApp()
        
        if (currentApp == null) {
            android.util.Log.d("ImprovedBlockerService", "No foreground app detected")
            return
        }

        // Don't block our own app
        if (currentApp == packageName) {
            resetTracking()
            return
        }

        android.util.Log.d("ImprovedBlockerService", "Current app: $currentApp, Blocked apps: $blockedApps")

        // Check if current app is in blocked list
        if (blockedApps.contains(currentApp)) {
            val timeLimit = timeLimits[currentApp] ?: 0
            
            if (timeLimit > 0) {
                handleBlockedApp(currentApp, timeLimit)
            }
        } else {
            // Reset tracking when switching to non-blocked app
            if (lastCheckedPackage != null && blockedApps.contains(lastCheckedPackage)) {
                android.util.Log.d("ImprovedBlockerService", "Switched from blocked app to safe app: $currentApp")
            }
            resetTracking()
        }
    }

    private fun handleBlockedApp(packageName: String, timeLimitMinutes: Int) {
        val currentUsageMinutes = UsageUtils.getAppUsageMinutes(this, packageName)
        
        android.util.Log.d("ImprovedBlockerService", "App: $packageName, Usage: $currentUsageMinutes/$timeLimitMinutes minutes")

        // If already over limit, block immediately
        if (currentUsageMinutes >= timeLimitMinutes) {
            android.util.Log.w("ImprovedBlockerService", "BLOCKING: $packageName already over limit!")
            blockAppImmediately(packageName)
            return
        }

        // Track time for this app
        trackAppUsage(packageName)

        // Check if limit reached after tracking
        val newUsageMinutes = UsageUtils.getAppUsageMinutes(this, packageName)
        if (newUsageMinutes >= timeLimitMinutes) {
            android.util.Log.w("ImprovedBlockerService", "BLOCKING: $packageName just reached limit!")
            blockAppImmediately(packageName)
        } else {
            // Use new notification integration instead of old showUsageWarning method
            timeLimitNotificationIntegration?.checkAndSendTimeLimitNotifications(packageName, newUsageMinutes, timeLimitMinutes)
        }
    }

    private fun trackAppUsage(packageName: String) {
        val now = System.currentTimeMillis()
        
        if (packageName == lastCheckedPackage && lastForegroundTimestamp > 0) {
            val elapsedSeconds = ((now - lastForegroundTimestamp) / 1000).toInt()
            if (elapsedSeconds > 0 && elapsedSeconds < 60) { // Sanity check
                UsageUtils.incrementUsageSeconds(this, packageName, elapsedSeconds)
                android.util.Log.d("ImprovedBlockerService", "Tracked $elapsedSeconds seconds for $packageName")
            }
        }
        
        lastCheckedPackage = packageName
        lastForegroundTimestamp = now
    }

    private fun blockAppImmediately(packageName: String) {
        if (blockingInProgress) {
            android.util.Log.d("ImprovedBlockerService", "Blocking already in progress")
            return
        }

        blockingInProgress = true
        
        try {
            android.util.Log.w("ImprovedBlockerService", "EXECUTING BLOCK for $packageName")
            
            // Send post-blocking encouragement notification
            val currentUsage = UsageUtils.getAppUsageMinutes(this, packageName)
            val timeLimit = parseTimeLimits(getSharedPreferences("blocked_apps", Context.MODE_PRIVATE).getString("time_limits", "") ?: "").getOrDefault(packageName, 0)
            blockingNotificationIntegration?.sendPostBlockingEncouragement(packageName, currentUsage, timeLimit)
            
            // Show blocking notification
            showBlockingNotification(packageName)
            
            // Try multiple blocking methods
            val blocked = executeBlocking(packageName)
            
            if (blocked) {
                android.util.Log.i("ImprovedBlockerService", "Successfully blocked $packageName")
                // Start monitoring for re-access attempts
                startContinuousMonitoring(packageName)
            } else {
                android.util.Log.e("ImprovedBlockerService", "Failed to block $packageName")
            }
            
        } finally {
            // Reset blocking flag after delay
            handler.postDelayed({
                blockingInProgress = false
            }, 3000)
        }
    }

    private fun executeBlocking(packageName: String): Boolean {
        var success = false
        
        try {
            // Method 1: Try overlay blocking first (most effective)
            if (canDrawOverlays()) {
                android.util.Log.d("ImprovedBlockerService", "Attempting overlay blocking")
                showOverlayBlock(packageName)
                success = true
            }
            
            // Method 2: Force kill the app (if possible)
            try {
                forceKillApp(packageName)
                android.util.Log.d("ImprovedBlockerService", "Attempted to kill $packageName")
            } catch (e: Exception) {
                android.util.Log.w("ImprovedBlockerService", "Could not kill app: ${e.message}")
            }
            
            // Method 3: Redirect to home screen
            redirectToHome()
            success = true
            
            // Method 4: Show blocking activity as fallback
            if (!success) {
                showBlockingActivity(packageName)
                success = true
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Error in executeBlocking: ${e.message}")
        }
        
        return success
    }

    private fun getCurrentForegroundApp(): String? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val queryTime = currentTime - 5000 // Look back 5 seconds
            
            // Method 1: Try usage events (most reliable)
            val events = usageStatsManager.queryEvents(queryTime, currentTime)
            var lastResumedApp: String? = null
            var lastResumedTime = 0L
            
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (event.timeStamp > lastResumedTime) {
                        lastResumedApp = event.packageName
                        lastResumedTime = event.timeStamp
                    }
                }
            }
            
            if (lastResumedApp != null) {
                android.util.Log.d("ImprovedBlockerService", "Detected via events: $lastResumedApp")
                return lastResumedApp
            }
            
            // Method 2: Fallback to usage stats
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                queryTime,
                currentTime
            )
            
            var mostRecentApp: String? = null
            var mostRecentTime = 0L
            
            for (stat in usageStats) {
                if (stat.lastTimeUsed > mostRecentTime) {
                    mostRecentApp = stat.packageName
                    mostRecentTime = stat.lastTimeUsed
                }
            }
            
            android.util.Log.d("ImprovedBlockerService", "Detected via stats: $mostRecentApp")
            return mostRecentApp
            
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Error detecting foreground app: ${e.message}")
            null
        }
    }

    private fun showOverlayBlock(packageName: String) {
        try {
            val intent = Intent(this, OverlayBlockingService::class.java)
            intent.putExtra("blocked_package", packageName)
            startService(intent)
            android.util.Log.d("ImprovedBlockerService", "Started overlay blocking for $packageName")
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Failed to start overlay: ${e.message}")
        }
    }

    private fun forceKillApp(packageName: String) {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
            android.util.Log.d("ImprovedBlockerService", "Killed background processes for $packageName")
        } catch (e: Exception) {
            android.util.Log.w("ImprovedBlockerService", "Could not kill $packageName: ${e.message}")
        }
    }

    private fun redirectToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)
            android.util.Log.d("ImprovedBlockerService", "Redirected to home")
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Failed to redirect to home: ${e.message}")
        }
    }

    private fun showBlockingActivity(packageName: String) {
        try {
            val intent = Intent(this, BlockingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("blocked_package", packageName)
            }
            startActivity(intent)
            android.util.Log.d("ImprovedBlockerService", "Showed blocking activity for $packageName")
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Failed to show blocking activity: ${e.message}")
        }
    }

    private fun startContinuousMonitoring(blockedPackage: String) {
        // Monitor for the next 30 seconds to catch re-access attempts
        val monitoringRunnable = object : Runnable {
            private var iterations = 0
            private val maxIterations = 30
            
            override fun run() {
                try {
                    val currentApp = getCurrentForegroundApp()
                    if (currentApp == blockedPackage) {
                        android.util.Log.w("ImprovedBlockerService", "Re-access detected for $blockedPackage - blocking again!")
                        
                        // Send repeated access notification
                        blockingNotificationIntegration?.sendRepeatedAccessNotification(blockedPackage, iterations + 1)
                        
                        executeBlocking(blockedPackage)
                    }
                    
                    iterations++
                    if (iterations < maxIterations) {
                        handler.postDelayed(this, 1000)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImprovedBlockerService", "Error in continuous monitoring: ${e.message}")
                }
            }
        }
        
        handler.postDelayed(monitoringRunnable, 1000)
        android.util.Log.d("ImprovedBlockerService", "Started continuous monitoring for $blockedPackage")
    }

    // Old method replaced by TimeLimitNotificationIntegration
    private fun showUsageWarning(packageName: String, currentMinutes: Int, limitMinutes: Int) {
        // This method is now deprecated in favor of the new notification integration
        // The TimeLimitNotificationIntegration handles all progressive warnings now
    }

    private fun showBlockingNotification(packageName: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create blocking channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    BLOCKING_CHANNEL_ID,
                    "App Blocking Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when apps are blocked"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val appName = getAppName(packageName)
            val notification = NotificationCompat.Builder(this, BLOCKING_CHANNEL_ID)
                .setContentTitle("🚫 App Blocked!")
                .setContentText("$appName has been blocked - time limit reached")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("🚫 $appName has been blocked!\n\nYou've reached your daily time limit. Taking breaks is healthy! 😊"))
                .setSmallIcon(android.R.drawable.ic_delete)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt() + 2000, notification)
            android.util.Log.d("ImprovedBlockerService", "Blocking notification shown for $appName")
            
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Failed to show blocking notification: ${e.message}")
        }
    }

    private fun parseTimeLimits(timeLimitsString: String): Map<String, Int> {
        return try {
            timeLimitsString.split("|")
                .mapNotNull { entry ->
                    val parts = entry.split(",")
                    if (parts.size == 2) {
                        parts[0] to (parts[1].toIntOrNull() ?: 0)
                    } else null
                }
                .toMap()
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Error parsing time limits: ${e.message}")
            emptyMap()
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun resetTracking() {
        lastCheckedPackage = null
        lastForegroundTimestamp = 0L
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.applicationInfo.uid,
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Binder.getCallingUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun startForegroundServiceWithNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "App Blocker Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Monitors app usage and enforces time limits"
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🧠 Brave Brain Active")
                .setContentText("Monitoring app usage and enforcing time limits")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            
            startForeground(NOTIFICATION_ID, notification)
            android.util.Log.d("ImprovedBlockerService", "Foreground service started")
            
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Failed to start foreground service: ${e.message}")
            throw e
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("ImprovedBlockerService", "onStartCommand called")
        
        // Check if we have the necessary conditions to run
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
        val hasTimeLimits = !prefs.getString("time_limits", "").isNullOrEmpty()
        val hasPermission = hasUsageStatsPermission(this)
        
        android.util.Log.d("ImprovedBlockerService", "Conditions - Permission: $hasPermission, Apps: $hasSelectedApps, Limits: $hasTimeLimits")
        
        if (!hasPermission || !hasSelectedApps || !hasTimeLimits) {
            android.util.Log.w("ImprovedBlockerService", "Stopping service - conditions not met")
            stopSelf()
            return START_NOT_STICKY
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        android.util.Log.d("ImprovedBlockerService", "Service destroyed")
        isServiceRunning = false
        handler.removeCallbacks(monitoringRunnable)
        
        // Stop notification system
        advancedNotificationManager?.stop()
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}