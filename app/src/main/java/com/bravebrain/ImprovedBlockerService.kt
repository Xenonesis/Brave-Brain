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

        // Respect any active cooling-off period
        val smartPrefs = getSharedPreferences("smart_blocking", Context.MODE_PRIVATE)
        val coolingUntil = smartPrefs.getLong("cooling_off_until_$packageName", 0L)
        if (System.currentTimeMillis() < coolingUntil) {
            android.util.Log.w("ImprovedBlockerService", "Cooling-off active for $packageName, enforcing block")
            showBlockingNotification(packageName)
            executeBlocking(packageName)
            return
        }

        // Track time for this app while in foreground
        trackAppUsage(packageName)

        // Re-evaluate usage after tracking increment
        val newUsageMinutes = UsageUtils.getAppUsageMinutes(this, packageName)

        // Use SmartBlockingEngine to decide action based on strategy and context
        val engine = SmartBlockingEngine(this)
        val decision = engine.shouldBlockApp(packageName, newUsageMinutes, timeLimitMinutes)
        android.util.Log.d("ImprovedBlockerService", "Decision for $packageName => shouldBlock=${decision.shouldBlock}, reason='${decision.reason}', challenge=${decision.challengeType}, cooling=${decision.coolingOffPeriod}, overtime=${decision.allowedOvertime}")

        if (decision.shouldBlock) {
            // Launch the appropriate challenge UI
            launchAdvancedChallenge(decision, packageName)

            // If a cooling off period is recommended, persist it
            if (decision.coolingOffPeriod > 0) {
                smartPrefs.edit().putLong("cooling_off_until_$packageName", System.currentTimeMillis() + decision.coolingOffPeriod).apply()
            }

            // For strict/waiting cases, also enforce immediate blocking behavior
            if (decision.challengeType == SmartBlockingEngine.ChallengeType.WAITING ||
                decision.challengeType == SmartBlockingEngine.ChallengeType.COMPLEX_MATH ||
                decision.allowedOvertime <= 0) {
                showBlockingNotification(packageName)
                executeBlocking(packageName)
            }
        } else {
            // Not blocking yet, but send progressive warnings if needed
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
            
            // Record blocked attempt for analytics
            try {
                val analyticsPrefs = getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val attempts = analyticsPrefs.getInt("blocked_attempts_$today", 0)
                analyticsPrefs.edit().putInt("blocked_attempts_$today", attempts + 1).apply()
            } catch (e: Exception) {
                android.util.Log.w("ImprovedBlockerService", "Failed to record blocked attempt: ${e.message}")
            }

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
            android.util.Log.w("ImprovedBlockerService", "=== EXECUTING AGGRESSIVE MULTI-LAYER BLOCKING for $packageName ===")
            
            // Method 1: AGGRESSIVE APP TERMINATION FIRST (enhanced approach)
            try {
                android.util.Log.w("ImprovedBlockerService", "🔥 PHASE 1: AGGRESSIVE APP TERMINATION")
                forceKillApp(packageName)
                android.util.Log.d("ImprovedBlockerService", "✓ Executed enhanced app termination")
                
                // Wait briefly to see if termination was effective
                Thread.sleep(500)
                
                // Check if app is still running after termination attempt
                val stillRunning = isAppStillRunning(packageName)
                if (!stillRunning) {
                    android.util.Log.i("ImprovedBlockerService", "🎯 SUCCESS: App $packageName was terminated!")
                    success = true
                } else {
                    android.util.Log.w("ImprovedBlockerService", "⚠️ App $packageName still running after termination attempt")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ImprovedBlockerService", "✗ Enhanced termination failed: ${e.message}")
            }
            
            // Method 2: Redirect to home screen (immediate disruption)
            try {
                android.util.Log.w("ImprovedBlockerService", "🏠 PHASE 2: AGGRESSIVE HOME REDIRECT")
                redirectToHome()
                android.util.Log.d("ImprovedBlockerService", "✓ Redirected to home screen")
                success = true
                
                // Double redirect with delay for stubborn apps
                Thread.sleep(200)
                redirectToHome()
                android.util.Log.d("ImprovedBlockerService", "✓ Double home redirect executed")
                
            } catch (e: Exception) {
                android.util.Log.e("ImprovedBlockerService", "✗ Failed to redirect to home: ${e.message}")
            }
            
            // Method 3: Show blocking activity (full screen interruption)
            try {
                android.util.Log.w("ImprovedBlockerService", "🚫 PHASE 3: BLOCKING ACTIVITY")
                showBlockingActivity(packageName)
                android.util.Log.d("ImprovedBlockerService", "✓ Launched blocking activity")
                success = true
            } catch (e: Exception) {
                android.util.Log.e("ImprovedBlockerService", "✗ Failed to show blocking activity: ${e.message}")
            }
            
            // Method 4: Show overlay (persistent visual block)
            try {
                android.util.Log.w("ImprovedBlockerService", "📱 PHASE 4: OVERLAY BLOCKING")
                if (canDrawOverlays()) {
                    showOverlayBlock(packageName)
                    android.util.Log.d("ImprovedBlockerService", "✓ Activated overlay blocking")
                    success = true
                } else {
                    android.util.Log.w("ImprovedBlockerService", "✗ Overlay permission not granted")
                }
            } catch (e: Exception) {
                android.util.Log.e("ImprovedBlockerService", "✗ Failed overlay blocking: ${e.message}")
            }
            
            // Method 5: FINAL TERMINATION ATTEMPT (if app somehow survived)
            try {
                android.util.Log.w("ImprovedBlockerService", "💀 PHASE 5: FINAL TERMINATION ATTEMPT")
                val stillAlive = isAppStillRunning(packageName)
                if (stillAlive) {
                    android.util.Log.w("ImprovedBlockerService", "App $packageName survived initial termination - executing final kill sequence")
                    
                    // Execute termination again with more aggressive approach
                    forceKillApp(packageName)
                    
                    // Force multiple home redirects
                    repeat(3) {
                        redirectToHome()
                        Thread.sleep(100)
                    }
                    
                    android.util.Log.d("ImprovedBlockerService", "✓ Final termination sequence completed")
                } else {
                    android.util.Log.i("ImprovedBlockerService", "✓ App $packageName successfully terminated - no final kill needed")
                }
            } catch (e: Exception) {
                android.util.Log.e("ImprovedBlockerService", "✗ Final termination failed: ${e.message}")
            }
            
            android.util.Log.w("ImprovedBlockerService", "=== AGGRESSIVE BLOCKING EXECUTION COMPLETE (success=$success) ===")
            
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Error in executeBlocking: ${e.message}")
        }
        
        return success
    }
    
    private fun isAppStillRunning(packageName: String): Boolean {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = activityManager.runningAppProcesses ?: return false
            
            for (process in runningProcesses) {
                if (process.processName == packageName || process.processName.startsWith("$packageName:")) {
                    android.util.Log.d("ImprovedBlockerService", "Found running process: ${process.processName} (importance: ${process.importance})")
                    return true
                }
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Error checking if app is running: ${e.message}")
            true // Assume it's running if we can't check
        }
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
            
            android.util.Log.w("ImprovedBlockerService", "=== ENHANCED APP TERMINATION for $packageName ===")
            
            // Method 1: Kill background processes (enhanced with retry logic)
            repeat(3) { attempt ->
                try {
                    activityManager.killBackgroundProcesses(packageName)
                    android.util.Log.d("ImprovedBlockerService", "Attempt ${attempt + 1}: Killed background processes for $packageName")
                    Thread.sleep(200) // Brief delay between attempts
                } catch (e: Exception) {
                    android.util.Log.w("ImprovedBlockerService", "Kill attempt ${attempt + 1} failed: ${e.message}")
                }
            }
            
            // Method 2: Enhanced task removal with forced finalization
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val appTasks = activityManager.appTasks
                    var tasksRemoved = 0
                    
                    for (task in appTasks) {
                        try {
                            val taskInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                task.taskInfo
                            } else {
                                null
                            }
                            
                            if (taskInfo != null) {
                                val taskPackage = taskInfo.baseIntent?.component?.packageName
                                if (taskPackage == packageName) {
                                    // Try multiple removal methods
                                    task.finishAndRemoveTask()
                                    task.moveToFront() // Move to front first
                                    task.finishAndRemoveTask() // Then remove again
                                    tasksRemoved++
                                    android.util.Log.d("ImprovedBlockerService", "Force-removed task for $packageName from recents")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ImprovedBlockerService", "Enhanced task removal failed: ${e.message}")
                        }
                    }
                    
                    if (tasksRemoved > 0) {
                        android.util.Log.i("ImprovedBlockerService", "Successfully removed $tasksRemoved tasks for $packageName")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ImprovedBlockerService", "Enhanced task removal error: ${e.message}")
            }
            
            // Method 3: Force app state disruption (aggressive approach)
            try {
                // Send CLOSE_SYSTEM_DIALOGS broadcast to disrupt app state
                val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                sendBroadcast(closeIntent)
                android.util.Log.d("ImprovedBlockerService", "Sent close system dialogs broadcast")
                
                // Force home launcher with aggressive flags
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                           Intent.FLAG_ACTIVITY_CLEAR_TASK or
                           Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                           Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(homeIntent)
                android.util.Log.d("ImprovedBlockerService", "Executed enhanced home redirect")
                
            } catch (e: Exception) {
                android.util.Log.w("ImprovedBlockerService", "App state disruption failed: ${e.message}")
            }
            
            // Method 4: Memory pressure simulation (advanced technique)
            try {
                // Trigger garbage collection and memory pressure
                System.gc()
                Runtime.getRuntime().gc()
                
                // Request low memory simulation (may cause apps to release resources)
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                if (memoryInfo.availMem < memoryInfo.threshold) {
                    android.util.Log.d("ImprovedBlockerService", "System under memory pressure - may help terminate $packageName")
                }
                
            } catch (e: Exception) {
                android.util.Log.w("ImprovedBlockerService", "Memory pressure simulation failed: ${e.message}")
            }
            
            // Method 5: Advanced process monitoring and termination
            try {
                val runningProcesses = activityManager.runningAppProcesses
                runningProcesses?.forEach { process ->
                    if (process.processName == packageName || process.processName.startsWith("$packageName:")) {
                        android.util.Log.w("ImprovedBlockerService", "Found running process: ${process.processName} (PID: ${process.pid}, Importance: ${process.importance})")
                        
                        // For background or cached processes, attempt additional termination
                        if (process.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                            try {
                                android.os.Process.killProcess(process.pid)
                                android.util.Log.w("ImprovedBlockerService", "Attempted to kill PID ${process.pid} for $packageName")
                            } catch (e: SecurityException) {
                                android.util.Log.w("ImprovedBlockerService", "Cannot kill PID ${process.pid}: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ImprovedBlockerService", "Process monitoring failed: ${e.message}")
            }
            
            android.util.Log.w("ImprovedBlockerService", "=== ENHANCED TERMINATION COMPLETE for $packageName ===")
            
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Enhanced force kill failed for $packageName: ${e.message}")
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
        // Aggressively monitor for the next 60 seconds to catch re-access attempts
        val monitoringRunnable = object : Runnable {
            private var iterations = 0
            private val maxIterations = 60  // Monitor for 60 seconds
            private val checkInterval = 500L  // Check every 500ms (more aggressive)
            
            override fun run() {
                try {
                    val currentApp = getCurrentForegroundApp()
                    if (currentApp == blockedPackage) {
                        android.util.Log.w("ImprovedBlockerService", "⚠️ RE-ACCESS DETECTED for $blockedPackage - BLOCKING AGAIN! (attempt ${iterations + 1})")
                        
                        // Send repeated access notification
                        blockingNotificationIntegration?.sendRepeatedAccessNotification(blockedPackage, iterations + 1)
                        
                        // Immediately block again with all methods
                        executeBlocking(blockedPackage)
                        
                        // Reset iteration counter to continue monitoring
                        iterations = 0
                    }
                    
                    iterations++
                    if (iterations < maxIterations) {
                        handler.postDelayed(this, checkInterval)
                    } else {
                        android.util.Log.d("ImprovedBlockerService", "Continuous monitoring ended for $blockedPackage after ${maxIterations/2} seconds")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImprovedBlockerService", "Error in continuous monitoring: ${e.message}")
                }
            }
        }
        
        handler.postDelayed(monitoringRunnable, 500)
        android.util.Log.d("ImprovedBlockerService", "Started AGGRESSIVE continuous monitoring for $blockedPackage (500ms interval)")
    }

    private fun launchAdvancedChallenge(decision: SmartBlockingEngine.BlockingDecision, packageName: String) {
        try {
            val intent = Intent(this, AdvancedChallengeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AdvancedChallengeActivity.EXTRA_CHALLENGE_TYPE, decision.challengeType.name)
                putExtra(AdvancedChallengeActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AdvancedChallengeActivity.EXTRA_COOLING_OFF_PERIOD, decision.coolingOffPeriod)
                putExtra(AdvancedChallengeActivity.EXTRA_REASON, decision.reason)
            }
            startActivity(intent)
            android.util.Log.d("ImprovedBlockerService", "Launched AdvancedChallengeActivity for $packageName")
        } catch (e: Exception) {
            android.util.Log.e("ImprovedBlockerService", "Failed to launch challenge: ${e.message}")
        }
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