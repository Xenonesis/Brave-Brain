package com.example.testing

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationManagerCompat

/**
 * NotificationContextManager provides a centralized way to manage the context-aware 
 * notification system and coordinate between different components
 */
class NotificationContextManager(private val context: Context) {
    
    companion object {
        private const val MANAGER_PREFS = "notification_context_manager"
        private const val NOTIFICATION_SYSTEM_ENABLED = "notification_system_enabled"
        private const val CONTEXT_ANALYSIS_ENABLED = "context_analysis_enabled"
        private const val PATTERN_RECOGNITION_ENABLED = "pattern_recognition_enabled"
        private const val ENGAGEMENT_TRACKING_ENABLED = "engagement_tracking_enabled"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(MANAGER_PREFS, Context.MODE_PRIVATE)
    private val notificationManager = NotificationManagerCompat.from(context)
    
    // Lazy initialization of components
    val contextAnalyzer: ContextAnalyzer by lazy { ContextAnalyzer(context) }
    val contextDetectionModule: ContextDetectionModule by lazy { ContextDetectionModule(context) }
    val usagePatternRecognition: UsagePatternRecognition by lazy { UsagePatternRecognition(context) }
    val userEngagementAnalyzer: UserEngagementAnalyzer by lazy { UserEngagementAnalyzer(context) }
    val contextAwareEngine: ContextAwareNotificationEngine by lazy { ContextAwareNotificationEngine(context) }
    
    /**
     * Initializes the context-aware notification system
     */
    fun initializeNotificationSystem() {
        if (!isNotificationSystemEnabled()) {
            setNotificationSystemEnabled(true)
        }
        
        // Create notification channels
        contextAwareEngine.createNotificationChannels()
        
        // Start the notification logic
        contextAwareEngine.startNotificationLogic()
    }
    
    /**
     * Starts the context-aware notification system if not already running
     */
    fun startNotificationSystem() {
        if (!isNotificationSystemEnabled()) {
            initializeNotificationSystem()
        } else {
            contextAwareEngine.startNotificationLogic()
        }
    }
    
    /**
     * Stops the context-aware notification system
     */
    fun stopNotificationSystem() {
        contextAwareEngine.stopNotificationLogic()
        setNotificationSystemEnabled(false)
    }
    
    /**
     * Checks if the notification system is currently enabled
     */
    fun isNotificationSystemEnabled(): Boolean {
        return prefs.getBoolean(NOTIFICATION_SYSTEM_ENABLED, false)
    }
    
    private fun setNotificationSystemEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(NOTIFICATION_SYSTEM_ENABLED, enabled).apply()
    }
    
    /**
     * Gets the current user context
     */
    fun getCurrentUserContext(): ContextAnalyzer.UserContext {
        return contextAnalyzer.analyzeContext()
    }
    
    /**
     * Gets the current device context
     */
    fun getCurrentDeviceContext(): ContextDetectionModule.DeviceContext {
        return contextDetectionModule.detectDeviceContext()
    }
    
    /**
     * Gets usage pattern insights
     */
    fun getUsagePatterns(): UsagePatternRecognition {
        return usagePatternRecognition
    }
    
    /**
     * Gets engagement metrics
     */
    fun getEngagementMetrics(): UserEngagementAnalyzer.EngagementMetrics {
        return userEngagementAnalyzer.analyzeEngagement()
    }
    
    /**
     * Tracks when a notification is opened
     */
    fun onNotificationOpened(notificationId: Int) {
        contextAwareEngine.onNotificationOpened(notificationId)
    }
    
    /**
     * Updates user sleep time preferences
     */
    fun setSleepTime(startHour: Int, endHour: Int) {
        contextAnalyzer.setSleepTime(startHour, endHour)
    }
    
    /**
     * Updates user work time preferences
     */
    fun setWorkTime(startHour: Int, endHour: Int) {
        contextAnalyzer.setWorkTime(startHour, endHour)
    }
    
    /**
     * Updates user focus time preferences
     */
    fun setFocusTime(startHour: Int, endHour: Int) {
        contextAnalyzer.setFocusTime(startHour, endHour)
    }
    
    /**
     * Learns from current context and usage patterns
     */
    fun learnFromCurrentContext() {
        contextAnalyzer.learnUserPatterns()
    }
    

    
    /**
     * Resets all engagement tracking (for testing purposes)
     */
    fun resetEngagementTracking() {
        userEngagementAnalyzer.resetEngagementTracking()
    }
    
    /**
     * Gets a summary of the current notification context
     */
    fun getContextSummary(): ContextSummary {
        val userContext = getCurrentUserContext()
        val deviceContext = getCurrentDeviceContext()
        val engagementMetrics = getEngagementMetrics()
        
        return ContextSummary(
            userContext = userContext,
            deviceContext = deviceContext,
            engagementMetrics = engagementMetrics,
            timestamp = System.currentTimeMillis()
        )
    }
    
    data class ContextSummary(
        val userContext: ContextAnalyzer.UserContext,
        val deviceContext: ContextDetectionModule.DeviceContext,
        val engagementMetrics: UserEngagementAnalyzer.EngagementMetrics,
        val timestamp: Long
    )
}