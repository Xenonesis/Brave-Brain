package com.bravebrain

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Extension class to sync notification data with Firestore
 */
class NotificationFirestoreSync(private val context: Context) {
    private val firestoreService = FirestoreService(context)
    
    fun trackNotificationSent(
        type: String,
        title: String,
        message: String,
        context: Map<String, Any> = emptyMap(),
        callback: (String?) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firestoreService.saveNotification(type, title, message, context)
                callback(result.getOrNull())
            } catch (e: Exception) {
                android.util.Log.e("NotificationFirestoreSync", "Failed to track notification: ${e.message}")
                callback(null)
            }
        }
    }
    
    fun updateNotificationEffectiveness(
        notificationId: String,
        wasClicked: Boolean,
        wasDismissed: Boolean,
        effectiveness: Double
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestoreService.updateNotificationInteraction(
                    notificationId,
                    wasClicked,
                    wasDismissed,
                    effectiveness
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationFirestoreSync", "Failed to update effectiveness: ${e.message}")
            }
        }
    }
    
    suspend fun getNotificationHistory(days: Int = 30): List<NotificationData> {
        return withContext(Dispatchers.IO) {
            try {
                val result = firestoreService.getNotificationHistory(days)
                result.getOrNull() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("NotificationFirestoreSync", "Failed to fetch history: ${e.message}")
                emptyList()
            }
        }
    }
    
    suspend fun calculateNotificationMetrics(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val history = getNotificationHistory(30)
                val metrics = mutableMapOf<String, Any>()
                
                if (history.isNotEmpty()) {
                    metrics["totalSent"] = history.size
                    metrics["clickRate"] = history.count { it.wasClicked }.toDouble() / history.size
                    metrics["dismissRate"] = history.count { it.wasDismissed }.toDouble() / history.size
                    metrics["averageEffectiveness"] = history.map { it.effectiveness }.average()
                    
                    // Group by type
                    val byType = history.groupBy { it.type }
                    metrics["typeBreakdown"] = byType.mapValues { (_, notifications) ->
                        mapOf(
                            "count" to notifications.size,
                            "clickRate" to notifications.count { it.wasClicked }.toDouble() / notifications.size,
                            "avgEffectiveness" to notifications.map { it.effectiveness }.average()
                        )
                    }
                }
                
                metrics
            } catch (e: Exception) {
                android.util.Log.e("NotificationFirestoreSync", "Failed to calculate metrics: ${e.message}")
                emptyMap()
            }
        }
    }
}
