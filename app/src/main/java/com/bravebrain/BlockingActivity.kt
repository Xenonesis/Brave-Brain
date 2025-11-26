package com.bravebrain

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
import androidx.activity.OnBackPressedCallback

class BlockingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTextView: TextView? = null
    private var countdownSeconds = 8
    private val monitoringRunnable = object : Runnable {
        override fun run() {
            // Check if we're still in foreground, if not bring ourselves back
            if (!isTaskRoot) {
                android.util.Log.d("BlockingActivity", "Not task root, bringing to front")
                val intent = Intent(this@BlockingActivity, BlockingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
            
            // Continue monitoring
            handler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup modern back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent back button from closing the blocking screen
                Toast.makeText(this@BlockingActivity, "App is blocked. Please wait or tap 'Go to Home'.", Toast.LENGTH_SHORT).show()
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
        
        android.util.Log.d("BlockingActivity", "Blocking screen shown")
        
        // Start countdown
        startCountdown()
        
        // Start monitoring to prevent dismissal
        handler.post(monitoringRunnable)
        
        // Automatically go to home after countdown
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
                countdownTextView?.text = "Redirecting in $countdownSeconds seconds..."
                
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
        layout.setBackgroundColor(0xFF1976D2.toInt()) // Blue background
        layout.setPadding(64, 64, 64, 64)
        
        // Title
        val title = TextView(this)
        title.text = "🚫 App Blocked!"
        title.textSize = 32f
        title.setTextColor(0xFFFFFFFF.toInt())
        title.gravity = android.view.Gravity.CENTER
        title.setPadding(0, 0, 0, 32)
        layout.addView(title)
        
        // Message
        val message = TextView(this)
        message.text = "🚫 APP BLOCKED! 🚫\n\nYou've reached your daily time limit.\nTaking a break is good for you! 😊"
        message.textSize = 18f
        message.setTextColor(0xFFFFFFFF.toInt())
        message.gravity = android.view.Gravity.CENTER
        message.setPadding(0, 0, 0, 32)
        layout.addView(message)
        
        // Countdown text
        countdownTextView = TextView(this)
        countdownTextView?.text = "Redirecting in $countdownSeconds seconds..."
        countdownTextView?.textSize = 16f
        countdownTextView?.setTextColor(0xFFFFFFFF.toInt())
        countdownTextView?.gravity = android.view.Gravity.CENTER
        countdownTextView?.setPadding(0, 0, 0, 64)
        layout.addView(countdownTextView)
        
        // Home button
        val homeButton = Button(this)
        homeButton.text = "Go to Home"
        homeButton.textSize = 18f
        homeButton.setOnClickListener {
            goToHome()
        }
        layout.addView(homeButton)
        
        return layout
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
    
    
    override fun onDestroy() {
        android.util.Log.d("BlockingActivity", "Activity destroyed")
        handler.removeCallbacks(monitoringRunnable)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 