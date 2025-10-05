package com.bravebrain

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SmartNotificationService : Service() {
    companion object {
        private const val TAG = "SmartNotificationService"
        const val CHANNEL_ID = "smart_notification_channel"
        const val NOTIFICATION_ID = 1001
    }

    private lateinit var notificationEngine: ContextAwareNotificationEngine
    private lateinit var advancedNotificationManager: AdvancedNotificationManager
    private lateinit var gamificationAnalyticsIntegration: GamificationAnalyticsNotificationIntegration

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmartNotificationService created")
        notificationEngine = ContextAwareNotificationEngine(this)
        advancedNotificationManager = AdvancedNotificationManager(this)
        gamificationAnalyticsIntegration = GamificationAnalyticsNotificationIntegration(this, advancedNotificationManager)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SmartNotificationService started")
        
        // Start foreground service to avoid being killed by system
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Initialize the notification system with advanced features
        initializeNotificationSystem()
        
        // Handle any incoming intent commands
        handleIntent(intent)
        
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // This service doesn't provide binding
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SmartNotificationService destroyed")
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Notification Service")
            .setContentText("Running in background...")
            .setSmallIcon(R.drawable.ic_check_circle) // Using existing icon
            .setContentIntent(pendingIntent)
            .setTicker("Smart Notification Service Active")
            .build()
    }

    private fun createNotificationChannels() {
        notificationEngine.createNotificationChannels()
    }

    private fun initializeNotificationSystem() {
        // Start the advanced notification manager
        advancedNotificationManager.start()
        
        // Initialize the gamification and analytics integration
        gamificationAnalyticsIntegration.initializeIntegration()
        
        // Schedule daily recurring notifications
        gamificationAnalyticsIntegration.scheduleDailyNotifications()
        
        // Set up periodic analytics processing
        setupAnalyticsProcessing()
        
        Log.d(TAG, "Notification system initialized with gamification and analytics integration")
    }

    private fun setupAnalyticsProcessing() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    // Process analytics data and trigger notifications every hour
                    gamificationAnalyticsIntegration.processAnalyticsDataAndTriggerNotifications()
                    
                    // Wait 1 hour before next check
                    kotlinx.coroutines.delay(60 * 60 * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in analytics processing", e)
                    kotlinx.coroutines.delay(10 * 60 * 100) // Wait 10 minutes on error
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        // Handle any commands passed via intent
        // For example: start specific notification logic based on action
        intent?.let {
            when (it.action) {
                "START_NOTIFICATION_LOGIC" -> {
                    // Start the notification logic
                    notificationEngine.startNotificationLogic()
                }
                "TRIGGER_GAMIFICATION_NOTIFICATION" -> {
                    // Trigger a specific gamification notification
                    val achievementType = it.getStringExtra("achievement_type") ?: ""
                    when (achievementType) {
                        "xp" -> {
                            val amount = it.getIntExtra("xp_amount", 10)
                            val reason = it.getStringExtra("reason") ?: "challenge completion"
                            gamificationAnalyticsIntegration.triggerXPNotification(amount, reason)
                        }
                        "level_up" -> {
                            val newLevel = it.getIntExtra("new_level", 1)
                            gamificationAnalyticsIntegration.triggerLevelUpNotification(newLevel)
                        }
                        "badge" -> {
                            val badgeName = it.getStringExtra("badge_name") ?: "New Badge"
                            gamificationAnalyticsIntegration.triggerBadgeNotification(badgeName)
                        }
                        "streak" -> {
                            val streakType = it.getStringExtra("streak_type") ?: "daily_streak"
                            val streakCount = it.getIntExtra("streak_count", 1)
                            gamificationAnalyticsIntegration.triggerStreakNotification(streakType, streakCount)
                        }
                    }
                }
                "TRIGGER_ANALYTICS_NOTIFICATION" -> {
                    // Trigger a specific analytics notification
                    val insightType = it.getStringExtra("insight_type") ?: "daily"
                    gamificationAnalyticsIntegration.triggerInsightsNotification(insightType)
                }
                else -> {
                    // Default behavior
                    notificationEngine.startNotificationLogic()
                }
            }
        }
    }
}