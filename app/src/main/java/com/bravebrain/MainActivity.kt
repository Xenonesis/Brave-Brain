package com.bravebrain

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.app.usage.UsageStatsManager
import android.graphics.Color
import android.view.Gravity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            setupSwipeRefresh()
            setupUI()
            addTestButton() // Add test functionality
            startBlockerServiceIfNeeded()
            startPeriodicStatsUpdate()
            
            // Check if this activity was opened from a notification and track engagement
            handleNotificationClick()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private var periodicUpdateHandler: android.os.Handler? = null
    private var periodicUpdateRunnable: Runnable? = null

    override fun onResume() {
        super.onResume()
        // Update UI and stats when returning from other activities
        setupUI()
        updateStatsDisplay()
        startPeriodicStatsUpdate()
        
        // Check if this activity was opened from a notification and track engagement
        handleNotificationClick()
    }

    override fun onPause() {
        super.onPause()
        // Stop periodic updates when app is not visible
        stopPeriodicStatsUpdate()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied. Some features may not work properly.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupUI() {
        try {
            // Update date display
            updateDateDisplay()
            
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
            val hasTimeLimits = !prefs.getString("time_limits", null).isNullOrEmpty()

            val accessButton = findViewById<MaterialButton>(R.id.accessButton)
            val selectAppsButton = findViewById<MaterialButton>(R.id.selectAppsButton)
            val setTimeLimitsButton = findViewById<MaterialButton>(R.id.setTimeLimitsButton)
            
            // Add null checks to prevent crashes
            if (accessButton == null || selectAppsButton == null || setTimeLimitsButton == null) {
                android.util.Log.e("MainActivity", "One or more buttons not found in layout")
                Toast.makeText(this, "UI Error: Missing buttons in layout", Toast.LENGTH_LONG).show()
                return
            }

            // Show/hide access button based on permission
        if (hasUsageStatsPermission(this)) {
            accessButton.visibility = View.GONE
        } else {
            accessButton.visibility = View.VISIBLE
            accessButton.setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                Toast.makeText(this, "Please grant usage access in settings", Toast.LENGTH_SHORT).show()
            }
        }

            // Update button states based on app state
            if (hasSelectedApps) {
                selectAppsButton.text = "Select Apps to Block"
                setTimeLimitsButton.isEnabled = true
            } else {
                selectAppsButton.text = "Select Apps to Block"
                setTimeLimitsButton.isEnabled = false
            }

            if (hasTimeLimits) {
                setTimeLimitsButton.text = "Update Time Limits"
            } else {
                setTimeLimitsButton.text = "Set Time Limits"
            }

        selectAppsButton.setOnClickListener {
            if (hasUsageStatsPermission(this@MainActivity)) {
                startActivity(Intent(this@MainActivity, AppSelectionActivity::class.java))
            } else {
                Toast.makeText(this, "Please grant usage access first", Toast.LENGTH_SHORT).show()
            }
        }

        setTimeLimitsButton.setOnClickListener {
            if (hasSelectedApps) {
                startActivity(Intent(this@MainActivity, TimeLimitBlockingActivity::class.java))
            } else {
                Toast.makeText(this, "Please select apps first", Toast.LENGTH_SHORT).show()
            }
        }

        // Gamification button
        val viewGamificationButton = findViewById<MaterialButton>(R.id.viewGamificationButton)
        viewGamificationButton?.setOnClickListener {
            startActivity(Intent(this@MainActivity, GamificationActivity::class.java))
        }

        // Theme Settings button
        val themeSettingsButton = findViewById<MaterialButton>(R.id.themeSettingsButton)
        themeSettingsButton?.setOnClickListener {
            startActivity(Intent(this@MainActivity, ThemeSettingsActivity::class.java))
        }

        // Notification Preferences button - this is added dynamically in addTestButton()
        // No need to set it up here as it's handled separately

            // Update stats display
            updateStatsDisplay()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up UI: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    private fun startBlockerServiceIfNeeded() {
        try {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
            val hasTimeLimits = !prefs.getString("time_limits", null).isNullOrEmpty()

            // Debug logging
            android.util.Log.d("MainActivity", "hasUsageStatsPermission: ${hasUsageStatsPermission(this)}")
            android.util.Log.d("MainActivity", "hasSelectedApps: $hasSelectedApps")
            android.util.Log.d("MainActivity", "hasTimeLimits: $hasTimeLimits")

            if (hasUsageStatsPermission(this) && hasSelectedApps && hasTimeLimits) {
                // Add a small delay to ensure proper user interaction
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        // Stop old service first
                        stopService(Intent(this@MainActivity, BlockerService::class.java))
                        // Start improved service
                        ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, ImprovedBlockerService::class.java))
                        Toast.makeText(this@MainActivity, "Monitoring service started", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to start service: ${e.message}")
                        Toast.makeText(this@MainActivity, "Failed to start monitoring service: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }, 500) // 500ms delay
            } else {
                // Stop service if conditions are not met
                try {
                    stopService(Intent(this, BlockerService::class.java))
                    stopService(Intent(this, ImprovedBlockerService::class.java))
                } catch (e: Exception) {
                    // Ignore errors when stopping service
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error managing service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission needed for blocking screen. Please grant it in settings.", Toast.LENGTH_LONG).show()
                
                // Show a dialog explaining why we need overlay permission
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Required")
                    .setMessage("To show the blocking screen over other apps, we need overlay permission. This allows us to display a blocking message directly over the app you're trying to use.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                    }
                    .setNegativeButton("Later") { _, _ ->
                        Toast.makeText(this, "You can grant overlay permission later in settings", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (android.provider.Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Overlay permission granted! Blocking screen will now work properly.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Overlay permission denied. Blocking screen may not work over other apps.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.applicationInfo.uid,
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Binder.getCallingUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun setupSwipeRefresh() {
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            // Force refresh usage data
            UsageUtils.resetIfNeeded(this)
            
            // Update UI and stats
            setupUI()
            updateStatsDisplay()
            
            // Stop the refresh animation
            swipeRefreshLayout.isRefreshing = false
            
            Toast.makeText(this, "Stats refreshed!", Toast.LENGTH_SHORT).show()
        }
        
        // Set refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorSecondary,
            R.color.colorPrimaryVariant
        )
    }

    private fun updateStatsDisplay() {
        try {
            // Ensure usage data is reset if needed (new day)
            UsageUtils.resetIfNeeded(this)
            
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
            val timeLimits = prefs.getString("time_limits", null)
                ?.split("|")
                ?.mapNotNull {
                    val parts = it.split(",")
                    if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
                }?.toMap() ?: emptyMap()

            // Get total device screen time
            val totalDeviceMinutes = getTotalDeviceScreenTime()
            val totalDeviceHours = totalDeviceMinutes / 60
            val totalDeviceRemainingMinutes = totalDeviceMinutes % 60

            // Get current usage for selected apps with validation
            val selectedAppUsage = blockedApps.associateWith { pkg ->
                val usage = UsageUtils.getAppUsageMinutes(this, pkg)
                // Ensure usage is not negative
                if (usage < 0) 0 else usage
            }
            
            // Get top apps usage for graph (excluding selected apps)
            val topAppsUsage = getTopAppsUsage(selectedAppUsage.keys.toSet())

            // Update UI with null checks
            val totalScreenTimeView = findViewById<TextView>(R.id.totalScreenTime)
            if (totalScreenTimeView != null) {
                totalScreenTimeView.text = "${totalDeviceHours}h ${totalDeviceRemainingMinutes}m"
            } else {
                android.util.Log.e("MainActivity", "totalScreenTime TextView not found")
            }

            // Update usage graph
            updateUsageGraph(topAppsUsage, selectedAppUsage, totalDeviceMinutes)

            // Update selected apps usage list
            updateSelectedAppsUsageList(selectedAppUsage, timeLimits)
            
            // Update enhanced stats
            updateEnhancedStats()
            
            // Log stats for debugging
            android.util.Log.d("MainActivity", "Stats updated - Total Device: ${totalDeviceMinutes}m, Selected Apps: ${selectedAppUsage.values.sum()}m")
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("MainActivity", "Error updating stats: ${e.message}")
        }
    }

    private fun startPeriodicStatsUpdate() {
        // Stop any existing updates
        stopPeriodicStatsUpdate()
        
        periodicUpdateHandler = android.os.Handler(android.os.Looper.getMainLooper())
        periodicUpdateRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing) {
                    updateStatsDisplay()
                    // Schedule next update
                    periodicUpdateHandler?.postDelayed(this, 30000) // 30 seconds
                }
            }
        }
        
        // Start the first update
        periodicUpdateRunnable?.let { runnable ->
            periodicUpdateHandler?.postDelayed(runnable, 30000)
        }
    }

    private fun stopPeriodicStatsUpdate() {
        periodicUpdateRunnable?.let { runnable ->
            periodicUpdateHandler?.removeCallbacks(runnable)
        }
        periodicUpdateHandler = null
        periodicUpdateRunnable = null
    }



    private fun getTotalDeviceScreenTime(): Int {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            
            // Get today's start time (midnight - 12:00 AM)
            val calendar = Calendar.getInstance()
            // Clear all time fields first
            calendar.clear(Calendar.HOUR_OF_DAY)
            calendar.clear(Calendar.HOUR)
            calendar.clear(Calendar.MINUTE)
            calendar.clear(Calendar.SECOND)
            calendar.clear(Calendar.MILLISECOND)
            calendar.clear(Calendar.AM_PM)
            // Set to midnight
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            // Debug: Log the calendar time to verify
            val debugFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            android.util.Log.d("MainActivity", "Calendar set to: ${debugFormat.format(calendar.timeInMillis)}")
            
            // Current time
            val endTime = System.currentTimeMillis()
            
            // Calculate total time from midnight to now (in minutes)
            val totalTimeFromMidnight = (endTime - startTime) / (1000 * 60)
            
            // Query usage stats for the entire day period
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            
            // Calculate actual screen time (not app usage time)
            // We'll use a simpler approach: sum up foreground time but cap it reasonably
            var totalMinutes = 0
            
            for (usageStat in usageStats) {
                val timeInForeground = usageStat.totalTimeInForeground
                if (timeInForeground > 0) {
                    val packageName = usageStat.packageName
                    
                    // Skip system apps, background services, and our own app
                    if (!packageName.startsWith("com.android") &&
                        !packageName.startsWith("android") &&
                        !packageName.startsWith("com.google.android") &&
                        !packageName.startsWith("com.bravebrain") &&
                        !packageName.contains("system") &&
                        !packageName.contains("service") &&
                        !packageName.contains("launcher") &&
                        !packageName.contains("home") &&
                        !packageName.contains("settings") &&
                        !packageName.contains("permission") &&
                        !packageName.contains("manager")) {
                        
                        val minutes = (timeInForeground / (1000 * 60)).toInt()
                        totalMinutes += minutes
                    }
                }
            }
            
            // Cap the result to a reasonable maximum (can't exceed time from midnight)
            // Also apply a reasonable cap to avoid inflated numbers
            val maxPossibleMinutes = totalTimeFromMidnight.toInt()
            val reasonableCap = minOf(maxPossibleMinutes, 24 * 60) // Max 24 hours
            val finalMinutes = minOf(totalMinutes, reasonableCap)
            
            // Debug: Log which apps are being counted as screen time
            android.util.Log.d("MainActivity", "Debug - Apps counted as screen time:")
            for (usageStat in usageStats) {
                val timeInForeground = usageStat.totalTimeInForeground
                if (timeInForeground > 0) {
                    val packageName = usageStat.packageName
                    if (!packageName.startsWith("com.android") &&
                        !packageName.startsWith("android") &&
                        !packageName.startsWith("com.google.android") &&
                        !packageName.startsWith("com.bravebrain") &&
                        !packageName.contains("system") &&
                        !packageName.contains("service") &&
                        !packageName.contains("launcher") &&
                        !packageName.contains("home") &&
                        !packageName.contains("settings") &&
                        !packageName.contains("permission") &&
                        !packageName.contains("manager")) {
                        
                        val appName = getAppName(packageName)
                        val minutes = (timeInForeground / (1000 * 60)).toInt()
                        android.util.Log.d("MainActivity", "  $appName: ${minutes}m")
                    }
                }
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            android.util.Log.d("MainActivity", "Today's screen time: ${finalMinutes}m (from ${dateFormat.format(startTime)} to ${dateFormat.format(endTime)})")
            finalMinutes
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error getting total device screen time: ${e.message}")
            0
        }
    }

    private fun getTopAppsUsage(excludePackages: Set<String>): List<Pair<String, Int>> {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            
            // Get today's start time (midnight - 12:00 AM)
            val calendar = Calendar.getInstance()
            // Clear all time fields first
            calendar.clear(Calendar.HOUR_OF_DAY)
            calendar.clear(Calendar.HOUR)
            calendar.clear(Calendar.MINUTE)
            calendar.clear(Calendar.SECOND)
            calendar.clear(Calendar.MILLISECOND)
            calendar.clear(Calendar.AM_PM)
            // Set to midnight
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            // Current time
            val endTime = System.currentTimeMillis()
            
            // Query usage stats for the entire day period
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            
            val appUsageMap = mutableMapOf<String, Int>()
            
            for (usageStat in usageStats) {
                val packageName = usageStat.packageName
                val timeInForeground = usageStat.totalTimeInForeground
                
                // Skip system apps and excluded packages
                if (timeInForeground > 0 && 
                    !packageName.startsWith("com.android") &&
                    !packageName.startsWith("android") &&
                    !packageName.startsWith("com.google.android") &&
                    !excludePackages.contains(packageName)) {
                    
                    val minutes = (timeInForeground / (1000 * 60)).toInt()
                    if (minutes > 0) {
                        appUsageMap[packageName] = minutes
                    }
                }
            }
            
            val appUsage = appUsageMap.map { it.key to it.value }
            
            // Sort by usage and take top 5
            appUsage.sortedByDescending { it.second }.take(5)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error getting top apps usage: ${e.message}")
            emptyList()
        }
    }

    private fun updateUsageGraph(topAppsUsage: List<Pair<String, Int>>, selectedAppUsage: Map<String, Int>, totalDeviceMinutes: Int) {
        val graphContainer = findViewById<LinearLayout>(R.id.usageGraphContainer)
        graphContainer.removeAllViews()

        if (totalDeviceMinutes == 0) {
            val noDataText = TextView(this).apply {
                text = "No usage data available"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 16)
            }
            graphContainer.addView(noDataText)
            return
        }

        // Create graph for top apps
        topAppsUsage.forEach { (pkg, minutes) ->
            val percentage = (minutes.toFloat() / totalDeviceMinutes * 100).toInt()
            val appName = getAppName(pkg)
            
            val graphRow = createGraphRow(appName, minutes, percentage, false)
            graphContainer.addView(graphRow)
        }

        // Add selected apps to graph with different color
        selectedAppUsage.forEach { (pkg, minutes) ->
            val percentage = (minutes.toFloat() / totalDeviceMinutes * 100).toInt()
            val appName = getAppName(pkg)
            
            val graphRow = createGraphRow(appName, minutes, percentage, true)
            graphContainer.addView(graphRow)
        }
    }

    private fun createGraphRow(appName: String, minutes: Int, percentage: Int, isSelectedApp: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackground(ContextCompat.getDrawable(this@MainActivity, R.drawable.usage_graph_row_background))
            
            // Add margin between rows
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 8
            layoutParams = params
        }

        // Top row with app name and time
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // App name with professional styling
        val nameText = TextView(this).apply {
            text = appName
            textSize = if (isSelectedApp) 15f else 14f
            setTextColor(
                if (isSelectedApp)
                    ContextCompat.getColor(this@MainActivity, R.color.textPrimary)
                else
                    ContextCompat.getColor(this@MainActivity, R.color.textSecondary)
            )
            typeface = if (isSelectedApp) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        // Time and percentage
        val timeText = TextView(this).apply {
            text = "${minutes}m (${percentage}%)"
            textSize = 12f
            setTextColor(
                if (isSelectedApp)
                    ContextCompat.getColor(this@MainActivity, R.color.textHighlight)
                else
                    ContextCompat.getColor(this@MainActivity, R.color.textTertiary)
            )
            gravity = Gravity.END
            typeface = if (isSelectedApp) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }

        topRow.addView(nameText)
        topRow.addView(timeText)

        // Progress bar container
        val progressContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
            setPadding(0, 8, 0, 0)
        }

        // Progress bar background
        val progressBackground = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                12
            )
            setBackgroundResource(R.drawable.progress_bar_background)
        }

        // Progress bar fill
        val progressFill = View(this).apply {
            val fillWidth = (percentage * 0.01 * 300).toInt().coerceAtLeast(4) // Minimum 4dp width
            layoutParams = LinearLayout.LayoutParams(fillWidth, 12)
            
            if (isSelectedApp) {
                setBackgroundResource(R.drawable.progress_bar_fill)
            } else {
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.colorSecondary))
                background.apply {
                    if (this is android.graphics.drawable.GradientDrawable) {
                        cornerRadius = 12f
                    }
                }
            }
        }

        // Create layered progress effect
        val progressLayerContainer = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                12
            )
            addView(progressBackground)
            addView(progressFill)
        }

        progressContainer.addView(progressLayerContainer)

        row.addView(topRow)
        row.addView(progressContainer)

        return row
    }

    private fun updateSelectedAppsUsageList(selectedAppUsage: Map<String, Int>, timeLimits: Map<String, Int>) {
        val selectedAppsList = findViewById<LinearLayout>(R.id.selectedAppsUsageList)
        selectedAppsList.removeAllViews()

        if (selectedAppUsage.isEmpty()) {
            val noAppsText = TextView(this).apply {
                text = "No apps selected for tracking"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
                setPadding(0, 8, 0, 8)
            }
            selectedAppsList.addView(noAppsText)
            return
        }

        // Sort apps by usage (highest first)
        val sortedApps = selectedAppUsage.entries.sortedByDescending { it.value }

        sortedApps.forEach { (pkg, used) ->
            val limit = timeLimits[pkg] ?: 0
            val appName = getAppName(pkg)
            
            val appRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val appNameText = TextView(this).apply {
                text = appName
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textPrimary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val usageText = TextView(this).apply {
                text = if (limit > 0) {
                    val remaining = limit - used
                    if (remaining <= 0) {
                        setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                        "Used: ${used}m (Limit reached)"
                    } else {
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
                        "${used}m / ${limit}m (${remaining}m left)"
                    }
                } else {
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
                    "${used}m"
                }
                textSize = 12f
                gravity = Gravity.END
            }

            appRow.addView(appNameText)
            appRow.addView(usageText)
            selectedAppsList.addView(appRow)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun updateDateDisplay() {
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        if (dateTextView != null) {
            val currentDate = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            val formattedDate = dateFormat.format(currentDate.time)
            dateTextView.text = formattedDate
        }
    }

    private fun updateEnhancedStats() {
        try {
            // Update additional stats views if they exist
            val averageSessionView = findViewById<TextView>(R.id.averageSessionTime)
            val appsUsedCountView = findViewById<TextView>(R.id.appsUsedCount)
            
            if (averageSessionView != null && appsUsedCountView != null) {
                val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
                val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
                
                // Calculate average session time (simplified calculation)
                val totalMinutes = getTotalDeviceScreenTime()
                val appsCount = getActiveAppsCount()
                
                // Update average session display
                if (appsCount > 0) {
                    val avgSession = totalMinutes / appsCount
                    averageSessionView.text = "${avgSession}m"
                } else {
                    averageSessionView.text = "0m"
                }
                
                // Update apps used count
                appsUsedCountView.text = appsCount.toString()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error updating enhanced stats: ${e.message}")
        }
    }
    
    private fun getActiveAppsCount(): Int {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()
            
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            
            var activeAppsCount = 0
            for (usageStat in usageStats) {
                val timeInForeground = usageStat.totalTimeInForeground
                val packageName = usageStat.packageName
                
                if (timeInForeground > 60000 && // More than 1 minute
                    !packageName.startsWith("com.android") &&
                    !packageName.startsWith("android") &&
                    !packageName.startsWith("com.google.android") &&
                    !packageName.startsWith("com.bravebrain")) {
                    activeAppsCount++
                }
            }
            
            activeAppsCount
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error getting active apps count: ${e.message}")
            0
        }
    }

    private fun addTestButton() {
        try {
            val container = findViewById<LinearLayout>(R.id.buttonContainer)
            
            // Create test blocking button
            val testButton = MaterialButton(this).apply {
                text = "🧪 Test Blocking System"
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.warning_orange))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                setPadding(32, 24, 32, 24)
                textSize = 16f
                
                setOnClickListener {
                    testBlockingSystem()
                }
            }
            
            // Add some margin
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            testButton.layoutParams = params
            
            container.addView(testButton)
            
            // Add Analytics Dashboard button
            val analyticsButton = MaterialButton(this).apply {
                text = "📊 View Analytics & Insights"
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary_blue))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                setPadding(32, 24, 32, 24)
                textSize = 16f
                
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, InsightsActivity::class.java))
                }
            }
            
            val analyticsParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            analyticsButton.layoutParams = analyticsParams
            container.addView(analyticsButton)
            
            // Add Smart Blocking Configuration button
            val smartBlockingButton = MaterialButton(this).apply {
                text = "🧠 Smart Blocking Settings"
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.success_green))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                setPadding(32, 24, 32, 24)
                textSize = 16f
                
                setOnClickListener {
                    showSmartBlockingOptions()
                }
            }
            
            val smartBlockingParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            smartBlockingButton.layoutParams = smartBlockingParams
            container.addView(smartBlockingButton)

            // Add Notification Preferences button
            val notificationPrefsButton = MaterialButton(this).apply {
                text = "🔔 Notification Preferences"
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.colorPrimary))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                setPadding(32, 24, 32, 24)
                textSize = 16f
                
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, NotificationPreferenceActivity::class.java))
                }
            }

            val notificationPrefsParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            notificationPrefsButton.layoutParams = notificationPrefsParams
            container.addView(notificationPrefsButton)
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error adding test button: ${e.message}")
        }
    }
    
    private fun testBlockingSystem() {
        try {
            // Check if we have selected apps and time limits
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
            val timeLimits = prefs.getString("time_limits", "")
            
            if (blockedApps.isEmpty()) {
                Toast.makeText(this, "❌ No apps selected for blocking. Please select apps first.", Toast.LENGTH_LONG).show()
                return
            }
            
            if (timeLimits.isNullOrEmpty()) {
                Toast.makeText(this, "❌ No time limits set. Please set time limits first.", Toast.LENGTH_LONG).show()
                return
            }
            
            // Check permissions
            if (!hasUsageStatsPermission(this)) {
                Toast.makeText(this, "❌ Missing usage stats permission. Please grant it.", Toast.LENGTH_LONG).show()
                return
            }
            
            // Test the blocking system
            android.app.AlertDialog.Builder(this)
                .setTitle("🧪 Test Blocking System")
                .setMessage("This will:\n\n1. Temporarily set a very low time limit (1 minute) for all selected apps\n2. Force trigger blocking for testing\n3. Show you how the blocking works\n\nProceed with test?")
                .setPositiveButton("Yes, Test It!") { _, _ ->
                    executeBlockingTest()
                }
                .setNegativeButton("Cancel", null)
                .show()
                
        } catch (e: Exception) {
            Toast.makeText(this, "Error testing blocking: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("MainActivity", "Error in testBlockingSystem: ${e.message}")
        }
    }
    
    private fun executeBlockingTest() {
        try {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
            
            // Backup current time limits
            val originalLimits = prefs.getString("time_limits", "")
            prefs.edit().putString("time_limits_backup", originalLimits).apply()
            
            // Set very low limits for testing (1 minute for all apps)
            val testLimits = blockedApps.joinToString("|") { "$it,1" }
            prefs.edit().putString("time_limits", testLimits).apply()
            
            // Set high usage for all apps to trigger immediate blocking
            blockedApps.forEach { packageName ->
                UsageUtils.incrementUsageSeconds(this, packageName, 120) // 2 minutes worth
            }
            
            Toast.makeText(this, "✅ Test setup complete! Now try opening a blocked app to see blocking in action.", Toast.LENGTH_LONG).show()
            
            // Show instructions
            android.app.AlertDialog.Builder(this)
                .setTitle("🧪 Test Instructions")
                .setMessage("Test setup complete!\n\n📱 Now try opening one of your blocked apps to see the blocking system in action.\n\n⚠️ The app should be blocked immediately and you'll be redirected to home.\n\n🔄 To restore normal limits, tap 'Restore Normal Limits' below.")
                .setPositiveButton("Restore Normal Limits") { _, _ ->
                    restoreNormalLimits()
                }
                .setNegativeButton("Keep Testing", null)
                .setCancelable(false)
                .show()
                
        } catch (e: Exception) {
            Toast.makeText(this, "Error executing test: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("MainActivity", "Error in executeBlockingTest: ${e.message}")
        }
    }
    
    private fun restoreNormalLimits() {
        try {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val backupLimits = prefs.getString("time_limits_backup", "")
            
            if (!backupLimits.isNullOrEmpty()) {
                prefs.edit().putString("time_limits", backupLimits).apply()
                prefs.edit().remove("time_limits_backup").apply()
                
                // Reset usage for today
                UsageUtils.resetIfNeeded(this)
                
                Toast.makeText(this, "✅ Normal time limits restored!", Toast.LENGTH_SHORT).show()
                
                // Refresh the UI
                setupUI()
            } else {
                Toast.makeText(this, "❌ No backup limits found", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error restoring limits: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("MainActivity", "Error in restoreNormalLimits: ${e.message}")
        }
    }
    
    private fun showSmartBlockingOptions() {
        val smartBlockingEngine = SmartBlockingEngine(this)
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        
        if (blockedApps.isEmpty()) {
            Toast.makeText(this, "Please select apps to block first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val strategies = arrayOf(
            "Standard Blocking",
            "Progressive Difficulty", 
            "Smart Adaptive",
            "Strict Mode"
        )
        
        val strategyValues = arrayOf(
            SmartBlockingEngine.STRATEGY_STANDARD,
            SmartBlockingEngine.STRATEGY_PROGRESSIVE,
            SmartBlockingEngine.STRATEGY_ADAPTIVE,
            SmartBlockingEngine.STRATEGY_STRICT
        )
        
        android.app.AlertDialog.Builder(this)
            .setTitle("🧠 Smart Blocking Strategy")
            .setMessage("Choose how you want the blocking system to behave:\n\n" +
                       "• Standard: Block when time limit reached\n" +
                       "• Progressive: Stricter limits with violations\n" +
                       "• Smart Adaptive: Learns your patterns\n" +
                       "• Strict Mode: No mercy, immediate blocking")
            .setItems(strategies) { _, which ->
                val selectedStrategy = strategyValues[which]
                
                // Apply strategy to all blocked apps
                blockedApps.forEach { packageName ->
                    smartBlockingEngine.setBlockingStrategy(packageName, selectedStrategy)
                }
                
                Toast.makeText(this, "Smart blocking strategy updated!", Toast.LENGTH_SHORT).show()
                
                // Show context rules configuration
                showContextRulesConfiguration(smartBlockingEngine)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showContextRulesConfiguration(smartBlockingEngine: SmartBlockingEngine) {
        val contextRules = smartBlockingEngine.getContextRules()
        
        android.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Context-Based Rules")
            .setMessage("Configure when blocking should be more strict:")
            .setMultiChoiceItems(
                arrayOf(
                    "Bedtime Mode (22:00 - 07:00)",
                    "Work Hours (09:00 - 17:00)",
                    "Family Time (18:00 - 20:00)"
                ),
                booleanArrayOf(
                    contextRules.bedtimeBlocking,
                    contextRules.workHoursBlocking,
                    contextRules.familyTimeBlocking
                )
            ) { _, which, isChecked ->
                when (which) {
                    0 -> {
                        val updatedRules = contextRules.copy(bedtimeBlocking = isChecked)
                        smartBlockingEngine.updateContextRules(updatedRules)
                    }
                    1 -> {
                        val updatedRules = contextRules.copy(workHoursBlocking = isChecked)
                        smartBlockingEngine.updateContextRules(updatedRules)
                    }
                    2 -> {
                        val updatedRules = contextRules.copy(familyTimeBlocking = isChecked)
                        smartBlockingEngine.updateContextRules(updatedRules)
                    }
                }
            }
            .setPositiveButton("Save") { _, _ ->
                Toast.makeText(this, "Context rules saved!", Toast.LENGTH_SHORT).show()
                
                // Start analytics service if not already running
                startAnalyticsService()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startAnalyticsService() {
        try {
            val analyticsIntent = Intent(this, AnalyticsService::class.java)
            startService(analyticsIntent)
            Toast.makeText(this, "📊 Analytics tracking started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error starting analytics service: ${e.message}")
        }
    }

    /**
     * Handles notification click tracking when the activity is opened from a notification
     */
    private fun handleNotificationClick() {
        // Check if this activity was opened from a notification
        val notificationId = intent.extras?.getInt("notification_id", -1) ?: -1
        if (notificationId != -1) {
            try {
                val contextAwareEngine = ContextAwareNotificationEngine(this)
                contextAwareEngine.onNotificationOpened(notificationId)
                android.util.Log.d("MainActivity", "Tracked notification click: $notificationId")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error tracking notification click: ${e.message}")
            }
        }
    }
}
