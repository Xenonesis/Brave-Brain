package com.bravebrain

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

/**
 * FeedbackManager - Centralized utility for providing consistent user feedback
 * across the app through Toast messages, Snackbars, and haptic feedback.
 */
object FeedbackManager {
    
    // Feedback Types
    enum class FeedbackType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        NEUTRAL
    }
    
    // Duration Types
    enum class Duration {
        SHORT,
        LONG
    }
    
    /**
     * Show a styled toast message with icon
     */
    fun showToast(context: Context, message: String, type: FeedbackType = FeedbackType.NEUTRAL, duration: Duration = Duration.SHORT) {
        val icon = when (type) {
            FeedbackType.SUCCESS -> "✓"
            FeedbackType.ERROR -> "✗"
            FeedbackType.WARNING -> "⚠"
            FeedbackType.INFO -> "ℹ"
            FeedbackType.NEUTRAL -> ""
        }
        
        val fullMessage = if (icon.isNotEmpty()) "$icon $message" else message
        
        Toast.makeText(
            context, 
            fullMessage, 
            if (duration == Duration.SHORT) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        ).show()
        
        // Provide haptic feedback based on type
        provideHapticFeedback(context, type)
    }
    
    /**
     * Show a Snackbar with action support
     */
    fun showSnackbar(
        view: View,
        message: String,
        type: FeedbackType = FeedbackType.NEUTRAL,
        duration: Duration = Duration.SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbarDuration = if (duration == Duration.SHORT) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
        
        val snackbar = Snackbar.make(view, message, snackbarDuration)
        
        // Style based on type
        val snackbarView = snackbar.view
        val backgroundColor = when (type) {
            FeedbackType.SUCCESS -> R.color.colorSuccess
            FeedbackType.ERROR -> R.color.colorError
            FeedbackType.WARNING -> R.color.colorWarning
            FeedbackType.INFO -> R.color.colorInfo
            FeedbackType.NEUTRAL -> R.color.textPrimary
        }
        
        snackbarView.setBackgroundColor(ContextCompat.getColor(view.context, backgroundColor))
        
        // Set action if provided
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
            snackbar.setActionTextColor(ContextCompat.getColor(view.context, R.color.colorOnPrimary))
        }
        
        snackbar.show()
        
        // Provide haptic feedback
        provideHapticFeedback(view.context, type)
    }
    
    /**
     * Provide haptic feedback based on feedback type
     */
    fun provideHapticFeedback(context: Context, type: FeedbackType) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    FeedbackType.SUCCESS -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    FeedbackType.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                    FeedbackType.WARNING -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    FeedbackType.INFO -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    FeedbackType.NEUTRAL -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    FeedbackType.SUCCESS -> vibrator.vibrate(50)
                    FeedbackType.ERROR -> vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
                    FeedbackType.WARNING -> vibrator.vibrate(100)
                    FeedbackType.INFO -> vibrator.vibrate(30)
                    FeedbackType.NEUTRAL -> vibrator.vibrate(20)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
    
    // ============ Specific Feedback Messages ============
    
    // App Selection Feedback
    fun showAppSelected(context: Context, appName: String) {
        showToast(context, "$appName added to block list", FeedbackType.SUCCESS)
    }
    
    fun showAppDeselected(context: Context, appName: String) {
        showToast(context, "$appName removed from block list", FeedbackType.INFO)
    }
    
    fun showAppsSelectionSaved(context: Context, count: Int) {
        showToast(context, "$count app${if (count == 1) "" else "s"} saved for blocking", FeedbackType.SUCCESS, Duration.LONG)
    }
    
    fun showNoAppsSelected(context: Context) {
        showToast(context, "Please select at least one app to block", FeedbackType.WARNING)
    }
    
    // Time Limit Feedback
    fun showTimeLimitSaved(context: Context, appName: String, minutes: Int) {
        showToast(context, "Time limit set: $appName - ${minutes}min", FeedbackType.SUCCESS)
    }
    
    fun showTimeLimitUpdated(context: Context) {
        showToast(context, "Time limits updated successfully", FeedbackType.SUCCESS)
    }
    
    fun showInvalidTimeLimit(context: Context) {
        showToast(context, "Please enter a valid time limit (1-1440 minutes)", FeedbackType.ERROR)
    }
    
    // Blocking Feedback
    fun showAppBlocked(context: Context, appName: String) {
        showToast(context, "$appName is blocked. Time limit reached!", FeedbackType.WARNING, Duration.LONG)
    }
    
    fun showTimeExtended(context: Context, minutes: Int) {
        showToast(context, "Time extended by $minutes minute${if (minutes == 1) "" else "s"}", FeedbackType.SUCCESS)
    }
    
    fun showBlockingBypassDenied(context: Context) {
        showToast(context, "Complete the challenge to get more time", FeedbackType.WARNING)
    }
    
    // Challenge/Quiz Feedback
    fun showChallengeCorrect(context: Context) {
        showToast(context, "Correct answer! Well done!", FeedbackType.SUCCESS)
    }
    
    fun showChallengeIncorrect(context: Context, attempts: Int) {
        showToast(context, "Incorrect. $attempts attempt${if (attempts == 1) "" else "s"} remaining", FeedbackType.ERROR)
    }
    
    fun showChallengeCompleted(context: Context) {
        showToast(context, "Challenge completed! You can now choose extra time", FeedbackType.SUCCESS, Duration.LONG)
    }
    
    fun showChallengeFailed(context: Context) {
        showToast(context, "Challenge failed. Try again or go back to home", FeedbackType.ERROR, Duration.LONG)
    }
    
    // Auth Feedback
    fun showLoginSuccess(context: Context, name: String? = null) {
        val message = if (name != null) "Welcome back, $name!" else "Welcome back!"
        showToast(context, message, FeedbackType.SUCCESS)
    }
    
    fun showLoginError(context: Context, error: String) {
        showToast(context, "Login failed: $error", FeedbackType.ERROR, Duration.LONG)
    }
    
    fun showSignupSuccess(context: Context) {
        showToast(context, "Account created successfully!", FeedbackType.SUCCESS)
    }
    
    fun showSignupError(context: Context, error: String) {
        showToast(context, "Signup failed: $error", FeedbackType.ERROR, Duration.LONG)
    }
    
    fun showLogoutSuccess(context: Context) {
        showToast(context, "Logged out successfully", FeedbackType.SUCCESS)
    }
    
    // Permission Feedback
    fun showPermissionGranted(context: Context, permissionName: String) {
        showToast(context, "$permissionName permission granted", FeedbackType.SUCCESS)
    }
    
    fun showPermissionDenied(context: Context, permissionName: String) {
        showToast(context, "$permissionName permission denied. Some features may not work", FeedbackType.WARNING, Duration.LONG)
    }
    
    fun showPermissionRequired(context: Context, permissionName: String) {
        showToast(context, "Please grant $permissionName permission", FeedbackType.INFO)
    }
    
    // Service Feedback
    fun showServiceStarted(context: Context) {
        showToast(context, "Monitoring service started", FeedbackType.SUCCESS)
    }
    
    fun showServiceStopped(context: Context) {
        showToast(context, "Monitoring service stopped", FeedbackType.INFO)
    }
    
    fun showServiceError(context: Context, error: String) {
        showToast(context, "Service error: $error", FeedbackType.ERROR, Duration.LONG)
    }
    
    // Data Sync Feedback
    fun showSyncStarted(context: Context) {
        showToast(context, "Syncing data...", FeedbackType.INFO)
    }
    
    fun showSyncComplete(context: Context) {
        showToast(context, "Data synced successfully", FeedbackType.SUCCESS)
    }
    
    fun showSyncError(context: Context, error: String) {
        showToast(context, "Sync failed: $error", FeedbackType.ERROR)
    }
    
    // Stats Refresh Feedback
    fun showStatsRefreshed(context: Context) {
        showToast(context, "Stats refreshed", FeedbackType.SUCCESS)
    }
    
    // Settings Feedback
    fun showSettingsSaved(context: Context) {
        showToast(context, "Settings saved", FeedbackType.SUCCESS)
    }
    
    fun showThemeChanged(context: Context, themeName: String) {
        showToast(context, "Theme changed to $themeName", FeedbackType.SUCCESS)
    }
    
    fun showNotificationPreferencesSaved(context: Context) {
        showToast(context, "Notification preferences saved", FeedbackType.SUCCESS)
    }
    
    // Gamification Feedback
    fun showLevelUp(context: Context, newLevel: Int) {
        showToast(context, "Congratulations! You reached Level $newLevel!", FeedbackType.SUCCESS, Duration.LONG)
    }
    
    fun showXPEarned(context: Context, xp: Int) {
        showToast(context, "+$xp XP earned!", FeedbackType.SUCCESS)
    }
    
    fun showBadgeUnlocked(context: Context, badgeName: String) {
        showToast(context, "Badge unlocked: $badgeName", FeedbackType.SUCCESS, Duration.LONG)
    }
    
    fun showStreakMaintained(context: Context, days: Int) {
        showToast(context, "$days day streak! Keep it up!", FeedbackType.SUCCESS)
    }
    
    fun showStreakLost(context: Context) {
        showToast(context, "Streak lost. Start again!", FeedbackType.WARNING)
    }
    
    // General Error Feedback
    fun showGenericError(context: Context, error: String? = null) {
        val message = if (error != null) "Error: $error" else "An error occurred. Please try again"
        showToast(context, message, FeedbackType.ERROR, Duration.LONG)
    }
    
    // Navigation Feedback
    fun showNavigatingTo(context: Context, destination: String) {
        showToast(context, "Opening $destination...", FeedbackType.INFO)
    }
    
    // Action Confirmations
    fun showActionCompleted(context: Context, action: String) {
        showToast(context, "$action completed", FeedbackType.SUCCESS)
    }
    
    fun showActionCancelled(context: Context) {
        showToast(context, "Action cancelled", FeedbackType.INFO)
    }
    
    // Loading States (for when Toast isn't appropriate, use with UI updates)
    fun showLoading(context: Context, message: String = "Loading...") {
        showToast(context, message, FeedbackType.INFO)
    }
}
