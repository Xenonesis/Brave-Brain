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

/**
 * TimeLimitBlockingActivity - A blocking screen shown when a user reaches their time limit.
 * 
 * IMPORTANT: This activity enforces that users MUST complete a quiz/challenge
 * before they can increase their time limit. There is NO way to bypass this requirement.
 */
class TimeLimitBlockingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTextView: TextView? = null
    private var appNameTextView: TextView? = null
    private var messageTextView: TextView? = null
    private var progressBar: ProgressBar? = null
    private var countdownProgress: ProgressBar? = null
    private var continueButton: MaterialButton? = null
    private var cancelButton: MaterialButton? = null
    private var countdownSeconds = 10  // Slightly longer countdown to give user time to read
    private var isActivityActive = false
    
    // Track which app was blocked (for time increase)
    private var blockedPackageName: String = ""
    private var blockedAppName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        super.onCreate(savedInstanceState)
        
        // Setup modern back press handling - CANNOT be bypassed
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent back button from closing the screen - user MUST make a choice
                Toast.makeText(this@TimeLimitBlockingActivity, "⛔ You must choose an option. Complete quiz to get more time.", Toast.LENGTH_SHORT).show()
            }
        })
        
        android.util.Log.d("TimeLimitBlockingActivity", "onCreate started - Intent: ${intent?.action}")
        android.util.Log.d("TimeLimitBlockingActivity", "onCreate - Intent extras: ${intent?.extras}")
        
        // Get blocked app info from intent
        blockedPackageName = intent.getStringExtra("package_name") ?: ""
        blockedAppName = intent.getStringExtra("app_name") ?: "This app"
        
        android.util.Log.d("TimeLimitBlockingActivity", "Blocking app: $blockedAppName ($blockedPackageName)")
        
        try {
            // Modern full-screen handling - make this a truly blocking experience
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
            appNameTextView = findViewById(R.id.appNameText)
            messageTextView = findViewById(R.id.messageText)
            progressBar = findViewById(R.id.progressBar)
            countdownProgress = findViewById(R.id.countdownProgress)
            continueButton = findViewById(R.id.continueButton)
            cancelButton = findViewById(R.id.cancelButton)
            
            // Update UI with blocked app info
            appNameTextView?.text = "⏰ Time limit reached for $blockedAppName"
            messageTextView?.text = "You've used up your allotted time for this app.\n\nTo get more time, you must complete a short quiz.\nThis helps you stay mindful of your screen time."
            continueButton?.text = "🧠 Solve Quiz to Get More Time"
            cancelButton?.text = "🏠 Accept Limit & Go Home"
            
            // Initialize progress bar
            progressBar?.max = countdownSeconds
            progressBar?.progress = countdownSeconds
            
            android.util.Log.d("TimeLimitBlockingActivity", "Views initialized")
            
            // Set up buttons - the continue button ALWAYS requires quiz
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
            
            // Automatically redirect to home after countdown (NOT to the app)
            handler.postDelayed({
                if (isActivityActive) {
                    android.util.Log.d("TimeLimitBlockingActivity", "Countdown finished - going to home (app stays blocked)")
                    goToHome()
                }
            }, (countdownSeconds * 1000).toLong())
            
            android.util.Log.d("TimeLimitBlockingActivity", "onCreate completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("TimeLimitBlockingActivity", "Error in onCreate: ${e.message}")
            e.printStackTrace()
            
            // Emergency fallback: go to home (keeps app blocked)
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
                    // Countdown finished, redirect to HOME (not app)
                    onCountdownFinished()
                }
            }
        }
        handler.post(countdownRunnable)
    }
    
    private fun updateCountdownUI() {
        countdownTextView?.text = when {
            countdownSeconds > 1 -> "Auto-redirecting to home in $countdownSeconds seconds..."
            countdownSeconds == 1 -> "Redirecting to home in 1 second..."
            else -> "Redirecting now..."
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
        countdownTextView?.text = "Redirecting to home... (app stays blocked)"
        handler.postDelayed({
            goToHome()
        }, 500)
    }
    
    /**
     * When user clicks "Continue" button, they MUST complete a quiz to increase time.
     * This is the ONLY way to get more time for the blocked app.
     */
    private fun onContinueClicked() {
        android.util.Log.d("TimeLimitBlockingActivity", "Continue button clicked - starting quiz for $blockedAppName")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        try {
            // IMPORTANT: User must complete AppTimeIncreaseMathActivity to increase time
            // This activity requires solving math problems before showing time increase options
            val intent = Intent(this, AppTimeIncreaseMathActivity::class.java)
            intent.putExtra("package_name", blockedPackageName)
            intent.putExtra("app_name", blockedAppName)
            // Note: We don't need "is_time_limit_access" - AppTimeIncreaseMathActivity
            // always requires quiz completion before allowing time increase
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("TimeLimitBlockingActivity", "Failed to start AppTimeIncreaseMathActivity: ${e.message}")
            Toast.makeText(this, "Error starting quiz. Please try again.", Toast.LENGTH_SHORT).show()
            // Don't go home - keep blocking screen up so user can try again
        }
    }
    
    /**
     * User accepts the time limit and goes home. App stays blocked.
     */
    private fun onCancelClicked() {
        android.util.Log.d("TimeLimitBlockingActivity", "Cancel button clicked - accepting limit, going to home")
        
        // Stop the countdown
        handler.removeCallbacksAndMessages(null)
        
        // Go to home screen - app stays blocked
        goToHome()
    }
    
    private fun goToHome() {
        try {
            Toast.makeText(this, "App remains blocked. Take a healthy break! 😊", Toast.LENGTH_SHORT).show()
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
        
        // Ensure we stay in foreground - this is a blocking experience
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