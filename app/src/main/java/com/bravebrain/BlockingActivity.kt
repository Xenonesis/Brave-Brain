package com.bravebrain

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue

class BlockingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTextView: TextView? = null
    private var blockedPackage: String = ""
    private var countdownSeconds = 30 // Increased countdown time
    private val monitoringRunnable = object : Runnable {
        override fun run() {
            // Check if we're still in foreground, if not bring ourselves back
            if (!isTaskRoot) {
                android.util.Log.d("BlockingActivity", "Not task root, bringing to front")
                val intent = Intent(this@BlockingActivity, BlockingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("blocked_package", blockedPackage)
                startActivity(intent)
            }
            
            // Continue monitoring
            handler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        super.onCreate(savedInstanceState)
        
        // Get the blocked package name
        blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        
        // Setup modern back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent back button from closing the blocking screen
                FeedbackManager.showBlockingBypassDenied(this@BlockingActivity)
            }
        })
        
        // Keep screen on and show when locked with modern APIs when available
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        // Hide system UI using WindowInsets on API 30+, fallback for older devices
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val controller = window.insetsController
            controller?.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            run {
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        }
        
        // Create a simple blocking layout
        setContentView(createBlockingLayout())
        
        android.util.Log.d("BlockingActivity", "Blocking screen shown for package: $blockedPackage")
        
        // Start countdown
        startCountdown()
        
        // Start monitoring to prevent dismissal
        handler.post(monitoringRunnable)
        
        // Automatically go to home after countdown (not to the app!)
        handler.postDelayed({
            goToHome()
        }, (countdownSeconds * 1000).toLong())
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("BlockingActivity", "Activity paused")
        // Don't try to restart - let overlay and service handle persistence
    }
    
    override fun onStop() {
        super.onStop()
        android.util.Log.d("BlockingActivity", "Activity stopped")
        // Don't try to restart - causes issues with task management
    }
    
    private fun startCountdown() {
        val countdownRunnable = object : Runnable {
            override fun run() {
                countdownSeconds--
                countdownTextView?.text = "Redirecting to home in $countdownSeconds seconds..."
                
                if (countdownSeconds > 0) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun createBlockingLayout(): View {
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        layout.setPadding(64, 64, 64, 64)
        
        // Title
        val title = TextView(this)
        title.text = "🚫 App Blocked!"
        title.textSize = 32f
        title.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        title.gravity = android.view.Gravity.CENTER
        title.setPadding(0, 0, 0, 32)
        layout.addView(title)
        
        // App name if available
        if (blockedPackage.isNotEmpty()) {
            val appNameView = TextView(this)
            appNameView.text = getAppName(blockedPackage)
            appNameView.textSize = 20f
            appNameView.setTextColor(ContextCompat.getColor(this, R.color.textSecondary))
            appNameView.gravity = android.view.Gravity.CENTER
            appNameView.setPadding(0, 0, 0, 16)
            layout.addView(appNameView)
        }
        
        // Message
        val message = TextView(this)
        message.text = "You've reached your daily time limit.\nTaking a break is good for you! 😊\n\nSolve a quiz to get more time."
        message.textSize = 18f
        message.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        message.gravity = android.view.Gravity.CENTER
        message.setPadding(0, 0, 0, 32)
        layout.addView(message)
        
        // Countdown text
        countdownTextView = TextView(this)
        countdownTextView?.text = "Redirecting to home in $countdownSeconds seconds..."
        countdownTextView?.textSize = 16f
        countdownTextView?.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
        countdownTextView?.gravity = android.view.Gravity.CENTER
        countdownTextView?.setPadding(0, 0, 0, 64)
        layout.addView(countdownTextView)
        
        // Button container
        val buttonLayout = android.widget.LinearLayout(this)
        buttonLayout.orientation = android.widget.LinearLayout.VERTICAL
        buttonLayout.gravity = android.view.Gravity.CENTER
        
        // Unlock with Challenge button (primary action - requires quiz)
        val unlockButton = MaterialButton(this).apply {
            text = "🧠 Unlock with Challenge"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@BlockingActivity, R.color.colorPrimary))
            isAllCaps = false
            cornerRadius = dpToPx(16)
            elevation = 0f
            
            // Create rounded white background
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(ContextCompat.getColor(this@BlockingActivity, R.color.colorOnPrimary))
            }
            background = bgDrawable
            
            setPadding(dpToPx(32), dpToPx(18), dpToPx(32), dpToPx(18))
            minimumHeight = dpToPx(56)
            minimumWidth = dpToPx(240)
            
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, dpToPx(16))
            layoutParams = params
            
            setOnClickListener {
                launchQuizForTimeIncrease()
            }
        }
        buttonLayout.addView(unlockButton)
        
        // Home button (secondary action - just goes home, no time increase)
        val homeButton = MaterialButton(this).apply {
            text = "🏠 Go to Home"
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@BlockingActivity, R.color.colorOnPrimary))
            isAllCaps = false
            cornerRadius = dpToPx(14)
            elevation = 0f
            
            // Create outlined style background
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(dpToPx(2), ContextCompat.getColor(this@BlockingActivity, R.color.colorOnPrimary))
            }
            background = bgDrawable
            
            setPadding(dpToPx(32), dpToPx(14), dpToPx(32), dpToPx(14))
            minimumHeight = dpToPx(52)
            minimumWidth = dpToPx(200)
            
            setOnClickListener {
                goToHome()
            }
        }
        buttonLayout.addView(homeButton)
        
        layout.addView(buttonLayout)
        
        return layout
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
    
    private fun launchQuizForTimeIncrease() {
        try {
            // User must solve quiz to get more time
            val intent = Intent(this, AppTimeIncreaseMathActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("package_name", blockedPackage)
                putExtra("app_name", getAppName(blockedPackage))
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("BlockingActivity", "Failed to launch quiz: ${e.message}")
            FeedbackManager.showGenericError(this, "Error launching challenge")
        }
    }
    
    private fun goToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(homeIntent)
            finish()
        } catch (e: Exception) {
            FeedbackManager.showGenericError(this, "Error going to home")
            finish()
        }
    }
    
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
        android.util.Log.d("BlockingActivity", "Activity destroyed")
        handler.removeCallbacks(monitoringRunnable)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 