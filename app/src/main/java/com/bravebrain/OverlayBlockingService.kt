package com.bravebrain

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue

/**
 * OverlayBlockingService - Shows a persistent full-screen overlay when an app is blocked.
 * 
 * IMPORTANT: This overlay enforces app blocking by:
 * 1. Showing a persistent full-screen overlay that cannot be easily dismissed
 * 2. Requiring the user to either complete a quiz OR go to home
 * 3. There is NO way to return to the blocked app without completing the quiz
 */
class OverlayBlockingService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var countdownSeconds = 15  // Longer countdown for user to read and decide
    private var lastBlockedPackage: String? = null
    private var blockedAppName: String = ""
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OverlayBlockingService", "Starting overlay blocking service")
        
        // Check if we have overlay permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            android.util.Log.w("OverlayBlockingService", "Overlay permission not granted, falling back to blocking activity")
            
            // Fallback: Launch blocking activity instead
            try {
                val activityIntent = Intent(this, TimeLimitBlockingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("package_name", intent?.getStringExtra("blocked_package") ?: "")
                    putExtra("app_name", intent?.getStringExtra("blocked_app_name") ?: "")
                }
                startActivity(activityIntent)
            } catch (e: Exception) {
                android.util.Log.e("OverlayBlockingService", "Failed to launch fallback activity: ${e.message}")
            }
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Capture blocked package if provided
        lastBlockedPackage = intent?.getStringExtra("blocked_package")
        blockedAppName = intent?.getStringExtra("blocked_app_name") ?: getAppName(lastBlockedPackage ?: "")
        
        // Show the overlay immediately
        showBlockingOverlay()
        
        // Auto-hide after countdown and go to HOME (not back to app)
        handler.postDelayed({
            goToHome()
        }, (countdownSeconds * 1000).toLong())
        
        return START_NOT_STICKY
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
    
    private fun showBlockingOverlay() {
        try {
            if (overlayView != null) {
                android.util.Log.d("OverlayBlockingService", "Overlay already showing")
                return
            }
            
            // Create the overlay view
            overlayView = createOverlayView()
            
            // Set up window parameters for overlay - MOST AGGRESSIVE SETTINGS
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                // More aggressive flags to prevent dismissal
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                // Set high priority to stay on top
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Use highest importance for overlay windows
                    x = 0
                    y = 0
                }
            }
            
            // Add the overlay to the window
            windowManager.addView(overlayView, params)
            
            android.util.Log.d("OverlayBlockingService", "Overlay blocking screen shown over target app")
            
            // Start countdown
            startCountdown()
            
        } catch (e: Exception) {
            android.util.Log.e("OverlayBlockingService", "Failed to show overlay: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun createOverlayView(): View {
        // Create a full-screen blocking layout
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        layout.setPadding(64, 64, 64, 64)
        
        // Title
        val title = TextView(this)
        title.text = "🚫 App Blocked!"
        title.textSize = 36f
        title.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        title.gravity = android.view.Gravity.CENTER
        title.setPadding(0, 0, 0, 24)
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        layout.addView(title)
        
        // App name
        val appNameView = TextView(this)
        appNameView.text = if (blockedAppName.isNotEmpty()) "⏰ Time limit reached for $blockedAppName" else "⏰ You've reached your daily time limit"
        appNameView.textSize = 18f
        appNameView.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        appNameView.gravity = android.view.Gravity.CENTER
        appNameView.setPadding(0, 0, 0, 32)
        layout.addView(appNameView)
        
        // Message
        val message = TextView(this)
        message.text = "Taking a break is good for you! 😊\n\nTo get more time, you'll need to complete a quick quiz.\nThis helps you stay mindful of your screen time."
        message.textSize = 16f
        message.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        message.gravity = android.view.Gravity.CENTER
        message.setPadding(0, 0, 0, 32)
        message.setLineSpacing(8f, 1.2f)
        layout.addView(message)
        
        // Countdown text
        val countdownTextView = TextView(this)
        countdownTextView.text = "Auto-redirecting to home in $countdownSeconds seconds..."
        countdownTextView.textSize = 16f
        countdownTextView.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        countdownTextView.gravity = android.view.Gravity.CENTER
        countdownTextView.setPadding(0, 0, 0, 48)
        countdownTextView.setTypeface(null, android.graphics.Typeface.BOLD)
        layout.addView(countdownTextView)
        
        // CTA: Solve quiz to get more time (Primary action)
        val challengeButton = MaterialButton(this).apply {
            text = "🧠 Solve Quiz to Get More Time"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@OverlayBlockingService, R.color.colorPrimary))
            isAllCaps = false
            cornerRadius = dpToPx(16)
            elevation = 0f
            
            // Create rounded background
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(ContextCompat.getColor(this@OverlayBlockingService, R.color.colorOnPrimary))
            }
            background = bgDrawable
            
            setPadding(dpToPx(32), dpToPx(18), dpToPx(32), dpToPx(18))
            minimumHeight = dpToPx(56)
            
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, dpToPx(16))
            layoutParams = params
            
            setOnClickListener {
                try {
                    val pkg = lastBlockedPackage ?: ""
                    // Launch AppTimeIncreaseMathActivity which REQUIRES solving quiz before time increase
                    val activityIntent = Intent(this@OverlayBlockingService, AppTimeIncreaseMathActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("package_name", pkg)
                        putExtra("app_name", blockedAppName)
                    }
                    startActivity(activityIntent)
                    hideBlockingOverlay()
                } catch (e: Exception) {
                    android.util.Log.e("OverlayBlockingService", "Failed to launch quiz: ${e.message}")
                    Toast.makeText(this@OverlayBlockingService, "Error launching quiz", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(challengeButton)

        // Home button (Secondary action)
        val homeButton = MaterialButton(this).apply {
            text = "🏠 Accept Limit & Go Home"
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@OverlayBlockingService, R.color.colorOnPrimary))
            isAllCaps = false
            cornerRadius = dpToPx(14)
            elevation = 0f
            
            // Create outlined style background
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(dpToPx(2), ContextCompat.getColor(this@OverlayBlockingService, R.color.colorOnPrimary))
            }
            background = bgDrawable
            
            setPadding(dpToPx(32), dpToPx(14), dpToPx(32), dpToPx(14))
            minimumHeight = dpToPx(52)
            
            setOnClickListener {
                goToHome()
            }
        }
        layout.addView(homeButton)
        
        // Store reference to countdown text view for updates
        layout.tag = countdownTextView
        
        return layout
    }
    
    private fun startCountdown() {
        val countdownRunnable = object : Runnable {
            override fun run() {
                countdownSeconds--
                val countdownTextView = overlayView?.tag as? TextView
                countdownTextView?.text = when {
                    countdownSeconds > 1 -> "Auto-redirecting to home in $countdownSeconds seconds..."
                    countdownSeconds == 1 -> "Redirecting to home in 1 second..."
                    else -> "Redirecting now..."
                }
                
                if (countdownSeconds > 0) {
                    handler.postDelayed(this, 1000)
                } else {
                    goToHome()
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun goToHome() {
        try {
            Toast.makeText(this, "App remains blocked. Take a healthy break! 😊", Toast.LENGTH_SHORT).show()
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(homeIntent)
            hideBlockingOverlay()
        } catch (e: Exception) {
            Toast.makeText(this, "Error going to home: ${e.message}", Toast.LENGTH_SHORT).show()
            hideBlockingOverlay()
        }
    }
    
    private fun hideBlockingOverlay() {
        try {
            if (overlayView != null) {
                windowManager.removeView(overlayView)
                overlayView = null
                android.util.Log.d("OverlayBlockingService", "Overlay blocking screen hidden")
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayBlockingService", "Failed to hide overlay: ${e.message}")
        }
        
        // Stop the service
        stopSelf()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Convert dp to pixels for programmatic UI sizing
     */
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
    
    override fun onDestroy() {
        hideBlockingOverlay()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 