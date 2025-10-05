package com.bravebrain

import android.content.Context
import android.content.Intent

/**
 * Helper class to start the SmartNotificationService
 */
object NotificationStarter {
    
    /**
     * Starts the SmartNotificationService
     */
    fun startNotificationService(context: Context) {
        val intent = Intent(context, SmartNotificationService::class.java)
        intent.action = "START_NOTIFICATION_LOGIC"
        context.startForegroundService(intent)
    }
    
    /**
     * Starts the context-aware notification system
     */
    fun startContextAwareNotificationSystem(context: Context) {
        val manager = NotificationContextManager(context)
        manager.startNotificationSystem()
    }
    
    /**
     * Stops the context-aware notification system
     */
    fun stopContextAwareNotificationSystem(context: Context) {
        val manager = NotificationContextManager(context)
        manager.stopNotificationSystem()
    }
    
    /**
     * Stops the SmartNotificationService
     */
    fun stopNotificationService(context: Context) {
        val intent = Intent(context, SmartNotificationService::class.java)
        context.stopService(intent)
    }
}