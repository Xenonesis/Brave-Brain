package com.bravebrain

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Extension class to sync user feedback with Firestore
 */
class FeedbackFirestoreSync(private val context: Context) {
    private val firestoreService = FirestoreService(context)
    
    fun submitFeedback(
        feedbackType: String,
        rating: Int,
        comment: String,
        context: Map<String, Any> = emptyMap()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestoreService.saveFeedback(feedbackType, rating, comment, context)
                android.util.Log.d("FeedbackFirestoreSync", "Feedback submitted successfully")
            } catch (e: Exception) {
                android.util.Log.e("FeedbackFirestoreSync", "Failed to submit feedback: ${e.message}")
            }
        }
    }
    
    suspend fun getFeedbackHistory(): List<UserFeedback> {
        return withContext(Dispatchers.IO) {
            try {
                val result = firestoreService.getFeedbackHistory()
                result.getOrNull() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("FeedbackFirestoreSync", "Failed to fetch feedback: ${e.message}")
                emptyList()
            }
        }
    }
    
    suspend fun calculateFeedbackMetrics(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val feedback = getFeedbackHistory()
                val metrics = mutableMapOf<String, Any>()
                
                if (feedback.isNotEmpty()) {
                    metrics["totalFeedback"] = feedback.size
                    metrics["averageRating"] = feedback.map { it.rating }.average()
                    
                    // Group by type
                    val byType = feedback.groupBy { it.feedbackType }
                    metrics["typeBreakdown"] = byType.mapValues { (_, items) ->
                        mapOf(
                            "count" to items.size,
                            "avgRating" to items.map { it.rating }.average()
                        )
                    }
                    
                    // Rating distribution
                    metrics["ratingDistribution"] = feedback.groupBy { it.rating }
                        .mapValues { it.value.size }
                }
                
                metrics
            } catch (e: Exception) {
                android.util.Log.e("FeedbackFirestoreSync", "Failed to calculate metrics: ${e.message}")
                emptyMap()
            }
        }
    }
}
