package com.bravebrain

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * This activity is deprecated in favor of TimeLimitBlockingActivity.
 * It's kept for backward compatibility but now redirects users to the proper quiz flow.
 * Users MUST solve a quiz before they can increase time - no bypass is allowed.
 */
class TimeIncreaseActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTextView: TextView? = null
    private var countdownSeconds = 30 // Increased to make bypass harder
    private var appName: String = ""
    private var packageName: String = ""
    private var isActivityActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        super.onCreate(savedInstanceState)
        
        // Setup modern back press handling
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent back button from closing the screen
                Toast.makeText(this@TimeIncreaseActivity, "Solve the quiz to get more time, or go home.", Toast.LENGTH_SHORT).show()
            }
        })
        
        android.util.Log.d("TimeIncreaseActivity", "onCreate started - Intent: ${intent?.action}")
        android.util.Log.d("TimeIncreaseActivity", "onCreate - Intent extras: ${intent?.extras}")
        
        try {
            android.util.Log.d("TimeIncreaseActivity", "onCreate started")
            
            // Get app info from intent
            appName = intent.getStringExtra("app_name") ?: "This app"
            packageName = intent.getStringExtra("package_name") ?: ""
            
            android.util.Log.d("TimeIncreaseActivity", "App: $appName, Package: $packageName")
            
            // Modern full-screen handling
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
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
            
            setContentView(R.layout.activity_time_increase)
            android.util.Log.d("TimeIncreaseActivity", "Layout set successfully")
            
            // Initialize views
            countdownTextView = findViewById(R.id.countdownText)
            android.util.Log.d("TimeIncreaseActivity", "Views initialized")
            
            // Set app name in the question
            findViewById<TextView>(R.id.questionText).text = "Time limit reached for $appName.\n\nSolve a quiz to get more time!"
            
            // Set up buttons - Rename "Yes" to indicate quiz requirement
            val yesButton = findViewById<Button>(R.id.yesButton)
            yesButton.text = "🧠 Solve Quiz for More Time"
            yesButton.setOnClickListener {
                onYesClicked()
            }
            
            // Rename "No" button
            val noButton = findViewById<Button>(R.id.noButton)
            noButton.text = "Go to Home"
            noButton.setOnClickListener {
                onNoClicked()
            }
            
            android.util.Log.d("TimeIncreaseActivity", "Buttons set up successfully")
            
            // Start countdown (for display only - goes to home, not back to app)
            startCountdown()
            
            // Automatically redirect to home after countdown (NOT the blocked app)
            handler.postDelayed({
                if (isActivityActive) {
                    goToHome()
                }
            }, (countdownSeconds * 1000).toLong())
            
            android.util.Log.d("TimeIncreaseActivity", "onCreate completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("TimeIncreaseActivity", "Error in onCreate: ${e.message}")
            e.printStackTrace()
            
            // Emergency fallback: go to home (safe option)
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(homeIntent)
                finish()
            } catch (e2: Exception) {
                android.util.Log.e("TimeIncreaseActivity", "Failed to go to home: ${e2.message}")
                finish()
            }
        }
    }
    
    private fun startCountdown() {
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (!isActivityActive) return
                
                countdownSeconds--
                countdownTextView?.text = "Redirecting to home in $countdownSeconds seconds..."
                
                if (countdownSeconds > 0) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun onYesClicked() {
        android.util.Log.d("TimeIncreaseActivity", "Yes button clicked for $appName - launching quiz")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        try {
            // Start app time increase math challenge - USER MUST SOLVE THIS
            val intent = Intent(this, AppTimeIncreaseMathActivity::class.java)
            intent.putExtra("package_name", packageName)
            intent.putExtra("app_name", appName)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("TimeIncreaseActivity", "Failed to start AppTimeIncreaseMathActivity: ${e.message}")
            goToHome()
        }
    }
    
    private fun onNoClicked() {
        android.util.Log.d("TimeIncreaseActivity", "No button clicked for $appName")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        // Go to home screen (no time increase without quiz)
        goToHome()
    }
    
    private fun goToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(homeIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error going to home: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    
    override fun onResume() {
        super.onResume()
        isActivityActive = true
        android.util.Log.d("TimeIncreaseActivity", "onResume called")
        
        // Ensure we stay in foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }
    
    override fun onPause() {
        super.onPause()
        isActivityActive = false
        android.util.Log.d("TimeIncreaseActivity", "onPause called")
    }
    
    override fun onDestroy() {
        isActivityActive = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 