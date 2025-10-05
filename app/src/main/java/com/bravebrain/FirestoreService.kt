package com.bravebrain

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirestoreService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val authManager = FirebaseAuthManager(context)
    
    init {
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }
    
    private fun getUserId(): String? = authManager.getCurrentUserId()
    
    // User Profile Operations
    suspend fun createOrUpdateUserProfile(email: String, displayName: String): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val profile = UserProfile(
                userId = userId,
                email = email,
                displayName = displayName,
                lastSyncAt = Timestamp.now()
            )
            db.collection("users").document(userId).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfile(): Result<UserProfile?> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val doc = db.collection("users").document(userId).get().await()
            Result.success(doc.toObject(UserProfile::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // App Usage Operations
    suspend fun saveAppUsage(packageName: String, appName: String, usageTimeMs: Long, dailyLimitMs: Long, category: String): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            
            val usageData = AppUsageData(
                userId = userId,
                packageName = packageName,
                appName = appName,
                usageTimeMs = usageTimeMs,
                dailyLimitMs = dailyLimitMs,
                category = category,
                date = today
            )
            db.collection("appUsage").add(usageData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAppUsageHistory(days: Int = 7): Result<List<AppUsageData>> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = dateFormat.format(calendar.time)
            
            val snapshot = db.collection("appUsage")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Result.success(snapshot.toObjects(AppUsageData::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Notification Operations
    suspend fun saveNotification(type: String, title: String, message: String, context: Map<String, Any> = emptyMap()): Result<String> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val notification = NotificationData(
                userId = userId,
                type = type,
                title = title,
                message = message,
                context = context
            )
            val docRef = db.collection("notifications").add(notification).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateNotificationInteraction(notificationId: String, clicked: Boolean, dismissed: Boolean, effectiveness: Double): Result<Unit> {
        return try {
            db.collection("notifications").document(notificationId)
                .update(mapOf(
                    "wasClicked" to clicked,
                    "wasDismissed" to dismissed,
                    "effectiveness" to effectiveness
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getNotificationHistory(days: Int = 30): Result<List<NotificationData>> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startTimestamp = Timestamp(calendar.time)
            
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("sentAt", startTimestamp)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Result.success(snapshot.toObjects(NotificationData::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Gamification Operations
    suspend fun saveGamificationData(points: Int, level: Int, badges: List<String>, challenges: Map<String, Any>): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val gamification = GamificationData(
                userId = userId,
                points = points,
                level = level,
                badges = badges,
                challenges = challenges
            )
            db.collection("gamification").document(userId).set(gamification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGamificationData(): Result<GamificationData?> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val doc = db.collection("gamification").document(userId).get().await()
            Result.success(doc.toObject(GamificationData::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Analytics Operations
    suspend fun saveAnalytics(date: String, totalScreenTimeMs: Long, productivityScore: Int, 
                             blockedAttempts: Int, challengesCompleted: Int, challengesFailed: Int,
                             usagePatterns: Map<String, Any>): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val analytics = AnalyticsData(
                userId = userId,
                date = date,
                totalScreenTimeMs = totalScreenTimeMs,
                productivityScore = productivityScore,
                blockedAttempts = blockedAttempts,
                challengesCompleted = challengesCompleted,
                challengesFailed = challengesFailed,
                usagePatterns = usagePatterns
            )
            db.collection("analytics").add(analytics).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAnalyticsHistory(days: Int = 30): Result<List<AnalyticsData>> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = dateFormat.format(calendar.time)
            
            val snapshot = db.collection("analytics")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Result.success(snapshot.toObjects(AnalyticsData::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // User Feedback Operations
    suspend fun saveFeedback(feedbackType: String, rating: Int, comment: String, context: Map<String, Any> = emptyMap()): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val feedback = UserFeedback(
                userId = userId,
                feedbackType = feedbackType,
                rating = rating,
                comment = comment,
                context = context
            )
            db.collection("feedback").add(feedback).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFeedbackHistory(): Result<List<UserFeedback>> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val snapshot = db.collection("feedback")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Result.success(snapshot.toObjects(UserFeedback::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
