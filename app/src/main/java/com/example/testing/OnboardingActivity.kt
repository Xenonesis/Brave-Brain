package com.example.testing

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
        private const val PREFS_NAME = "onboarding"
        private const val ONBOARDING_COMPLETE_KEY = "onboarding_complete"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var nextButton: MaterialButton
    private lateinit var skipButton: MaterialButton
    private lateinit var getStartedButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var pageCounter: TextView

    private val onboardingPages = listOf(
        OnboardingPage(
            "Welcome to Brave Brain",
            "🧠 Ready to transform your digital habits?\n\nJoin thousands who've reclaimed their focus and built healthier relationships with technology. Your journey to digital wellness starts here.",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Smart App Blocking",
            "⏱️ Set intelligent time limits for distracting apps\n\n📱 Get gentle nudges when you've spent enough time\n\n🎯 Focus on what truly matters to you",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Real-Time Tracking",
            "📊 Monitor your screen time with beautiful insights\n\n📈 Track your progress and celebrate improvements\n\n🏆 Build lasting digital wellness habits",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Gamified Challenges",
            "🧮 Solve math puzzles to unlock extra time\n\n🎮 Make breaking habits engaging and fun\n\n💪 Train your brain to resist mindless scrolling",
            R.drawable.logo_no_bg
        ),
        OnboardingPage(
            "Get Started",
            "🚀 Ready to take control of your digital life?\n\nWe'll need a few permissions to help you stay focused and track your progress effectively.\n\n✨ Your privacy is protected - everything stays on your device",
            R.drawable.logo_no_bg
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Check if onboarding is already complete
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(ONBOARDING_COMPLETE_KEY, false)) {
            startMainActivity()
            return
        }

        setupViews()
        setupViewPager()
        setupButtons()
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        nextButton = findViewById(R.id.nextButton)
        skipButton = findViewById(R.id.skipButton)
        getStartedButton = findViewById(R.id.getStartedButton)
        progressBar = findViewById(R.id.progressBar)
        pageCounter = findViewById(R.id.pageCounter)
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(onboardingPages)
        viewPager.adapter = adapter

        // Connect ViewPager with TabLayout
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Handle page changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })
    }

    private fun setupButtons() {
        nextButton.setOnClickListener {
            if (viewPager.currentItem < onboardingPages.size - 1) {
                viewPager.currentItem += 1
            }
        }

        skipButton.setOnClickListener {
            startPermissionFlow()
        }

        getStartedButton.setOnClickListener {
            startPermissionFlow()
        }

        // Ensure proper initial state
        nextButton.visibility = View.VISIBLE
        skipButton.visibility = View.VISIBLE
        getStartedButton.visibility = View.GONE
        
        // Set initial button text
        nextButton.text = getString(R.string.continue_button)
        
        // Set initial progress
        progressBar.progress = 20
        pageCounter.text = "1 of ${onboardingPages.size}"
        
        // Update buttons for current position
        updateButtons(0)
    }

    private fun updateButtons(position: Int) {
        // Update progress bar
        val progress = ((position + 1) * 100) / onboardingPages.size
        progressBar.progress = progress
        
        // Update page counter
        pageCounter.text = "${position + 1} of ${onboardingPages.size}"
        
        when (position) {
            onboardingPages.size - 1 -> {
                // Last page - show "Get Started" button only
                nextButton.visibility = View.GONE
                skipButton.visibility = View.GONE
                getStartedButton.visibility = View.VISIBLE
            }
            else -> {
                // Other pages - show "Continue" and "Skip" buttons
                nextButton.visibility = View.VISIBLE
                skipButton.visibility = View.VISIBLE
                getStartedButton.visibility = View.GONE
                
                // Update button text based on position
                if (position == 0) {
                    nextButton.text = getString(R.string.continue_button)
                } else {
                    nextButton.text = getString(R.string.next_button)
                }
            }
        }
    }

    private fun startPermissionFlow() {
        // Start permission request flow
        requestNotificationPermission()
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
            } else {
                requestUsageAccessPermission()
            }
        } else {
            requestUsageAccessPermission()
        }
    }

    private fun requestUsageAccessPermission() {
        if (!hasUsageStatsPermission(this)) {
            showPermissionDialog(
                "Usage Access Permission",
                "To track your app usage and enforce time limits, we need access to your usage statistics. This helps us know when you're using apps and for how long.",
                "Grant Permission"
            ) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showPermissionDialog(
                    "Overlay Permission",
                    "To show blocking screens over other apps when you reach your time limits, we need overlay permission. This allows us to display a gentle reminder directly over the app you're trying to use.",
                    "Grant Permission"
                ) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                }
            } else {
                completeOnboarding()
            }
        } else {
            completeOnboarding()
        }
    }

    private fun showPermissionDialog(title: String, message: String, positiveButtonText: String, onPositiveClick: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                onPositiveClick()
            }
            .setNegativeButton("Skip for now") { _, _ ->
                completeOnboarding()
            }
            .setCancelable(false)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                requestUsageAccessPermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                completeOnboarding()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permissions were granted while in settings
        if (hasUsageStatsPermission(this)) {
            requestOverlayPermission()
        }
    }

    private fun completeOnboarding() {
        // Mark onboarding as complete
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(ONBOARDING_COMPLETE_KEY, true).apply()
        
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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

    data class OnboardingPage(
        val title: String,
        val description: String,
        val iconResId: Int
    )
} 