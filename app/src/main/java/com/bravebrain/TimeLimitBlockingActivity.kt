package com.bravebrain

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class TimeLimitBlockingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTextView: TextView? = null
    private var progressBar: ProgressBar? = null
    private var countdownProgress: ProgressBar? = null
    private var continueButton: MaterialButton? = null
    private var cancelButton: MaterialButton? = null
    private var countdownSeconds = 8
    private var isActivityActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup modern back press handling
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent back button from closing the screen
                Toast.makeText(this@TimeLimitBlockingActivity, "Please make a choice or wait for the timer.", Toast.LENGTH_SHORT).show()
            }
        })
        
        android.util.Log.d("TimeLimitBlockingActivity", "onCreate started - Intent: ${intent?.action}")
        android.util.Log.d("TimeLimitBlockingActivity", "onCreate - Intent extras: ${intent?.extras}")
        
        try {
            android.util.Log.d("TimeLimitBlockingActivity", "onCreate started")
            
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
            
            setContentView(R.layout.activity_time_limit_blocking)
            android.util.Log.d("TimeLimitBlockingActivity", "Layout set successfully")
            
            // Initialize views
            countdownTextView = findViewById(R.id.countdownText)
            progressBar = findViewById(R.id.progressBar)
            countdownProgress = findViewById(R.id.countdownProgress)
            continueButton = findViewById(R.id.continueButton)
            cancelButton = findViewById(R.id.cancelButton)
            
            // Initialize progress bar
            progressBar?.max = countdownSeconds
            progressBar?.progress = countdownSeconds
            
            android.util.Log.d("TimeLimitBlockingActivity", "Views initialized")
            
            // Set up buttons
            continueButton?.setOnClickListener {
                onContinueClicked()
            }
            
            cancelButton?.setOnClickListener {
                onCancelClicked()
            }
            
            // Add button animations
            setupButtonAnimations()
            
            android.util.Log.d("TimeLimitBlockingActivity", "Buttons set up successfully")
            
            // Start countdown
            startCountdown()
            
            // Automatically redirect to home after countdown
            handler.postDelayed({
                if (isActivityActive) {
                    goToHome()
                }
            }, (countdownSeconds * 1000).toLong())
            
            android.util.Log.d("TimeLimitBlockingActivity", "onCreate completed successfully")
            
            // Show a toast to confirm the activity is working
            Toast.makeText(this, "Time limit blocking screen shown", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.util.Log.e("TimeLimitBlockingActivity", "Error in onCreate: ${e.message}")
            e.printStackTrace()
            
            // Emergency fallback: go to home
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(homeIntent)
                finish()
            } catch (e2: Exception) {
                android.util.Log.e("TimeLimitBlockingActivity", "Failed to go to home: ${e2.message}")
                finish()
            }
        }
    }
    
    private fun setupButtonAnimations() {
        // Subtle pulse animation for continue button to draw attention
        val pulseAnimator = ObjectAnimator.ofFloat(continueButton, "scaleX", 1.0f, 1.05f, 1.0f)
        pulseAnimator.duration = 2000
        pulseAnimator.repeatCount = ValueAnimator.INFINITE
        pulseAnimator.interpolator = AccelerateDecelerateInterpolator()
        pulseAnimator.start()
        
        val pulseAnimatorY = ObjectAnimator.ofFloat(continueButton, "scaleY", 1.0f, 1.05f, 1.0f)
        pulseAnimatorY.duration = 2000
        pulseAnimatorY.repeatCount = ValueAnimator.INFINITE
        pulseAnimatorY.interpolator = AccelerateDecelerateInterpolator()
        pulseAnimatorY.start()
    }
    
    private fun startCountdown() {
        val totalCountdown = countdownSeconds
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (!isActivityActive) return
                
                countdownSeconds--
                updateCountdownUI()
                
                // Animate progress bar
                progressBar?.let { pb ->
                    val animator = ObjectAnimator.ofInt(pb, "progress", pb.progress, countdownSeconds)
                    animator.duration = 800
                    animator.interpolator = AccelerateDecelerateInterpolator()
                    animator.start()
                }
                
                // Add urgency effects when countdown is low
                if (countdownSeconds <= 3 && countdownSeconds > 0) {
                    addUrgencyEffects()
                }
                
                if (countdownSeconds > 0) {
                    handler.postDelayed(this, 1000)
                } else {
                    // Countdown finished, redirect
                    onCountdownFinished()
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun updateCountdownUI() {
        countdownTextView?.text = when {
            countdownSeconds > 1 -> getString(R.string.redirecting_in_seconds, countdownSeconds)
            countdownSeconds == 1 -> getString(R.string.redirecting_in_second)
            else -> getString(R.string.redirecting_now)
        }
    }
    
    private fun addUrgencyEffects() {
        // Flash the countdown text
        countdownTextView?.let { textView ->
            val flashAnimator = ObjectAnimator.ofFloat(textView, "alpha", 1.0f, 0.3f, 1.0f)
            flashAnimator.duration = 500
            flashAnimator.start()
        }
        
        // Slightly shake the main card
        findViewById<View>(R.id.mainCard)?.let { card ->
            val shakeAnimator = ObjectAnimator.ofFloat(card, "translationX", 0f, -10f, 10f, 0f)
            shakeAnimator.duration = 300
            shakeAnimator.start()
        }
    }
    
    private fun onCountdownFinished() {
        // Show final message briefly
        countdownTextView?.text = "Redirecting to home..."
        handler.postDelayed({
            goToHome()
        }, 500)
    }
    
    private fun onContinueClicked() {
        android.util.Log.d("TimeLimitBlockingActivity", "Continue button clicked")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        try {
            // Start math challenge activity instead of directly going to time limit activity
            val intent = Intent(this, MathChallengeActivity::class.java)
            intent.putExtra("package_name", "time_limit_access")
            intent.putExtra("app_name", "Time Limit Access")
            intent.putExtra("is_time_limit_access", true)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("TimeLimitBlockingActivity", "Failed to start MathChallengeActivity: ${e.message}")
            goToHome()
        }
    }
    
    private fun onCancelClicked() {
        android.util.Log.d("TimeLimitBlockingActivity", "Cancel button clicked")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        // Go to home screen
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
        android.util.Log.d("TimeLimitBlockingActivity", "onResume called")
        
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
        android.util.Log.d("TimeLimitBlockingActivity", "onPause called")
    }
    
    override fun onDestroy() {
        isActivityActive = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
} 