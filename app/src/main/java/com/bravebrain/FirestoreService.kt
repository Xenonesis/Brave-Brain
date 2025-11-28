package com.bravebrain

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * FirestoreService handles all Firestore database operations.
 * This is the central service for cloud data persistence.
 */
class FirestoreService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val authManager = FirebaseAuthManager(context)
    
    companion object {
        private const val TAG = "FirestoreService"
    }
    
    init {
        try {
            db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            Log.d(TAG, "Firestore initialized with offline persistence enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore settings: ${e.message}")
        }
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
            db.collection("users").document(userId).set(profile, SetOptions.merge()).await()
            Log.d(TAG, "User profile saved for $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile: ${e.message}", e)
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
            // Use document ID based on user+package+date to allow upserts
            val docId = "${userId}_${packageName}_$today"
            db.collection("appUsage").document(docId).set(usageData, SetOptions.merge()).await()
            Log.d(TAG, "App usage saved for $appName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving app usage: ${e.message}", e)
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
            db.collection("gamification").document(userId).set(gamification, SetOptions.merge()).await()
            Log.d(TAG, "Gamification data saved - Level: $level, Points: $points")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving gamification data: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getGamificationData(): Result<GamificationData?> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val doc = db.collection("gamification").document(userId).get().await()
            val data = doc.toObject(GamificationData::class.java)
            Log.d(TAG, "Gamification data retrieved: ${data?.level ?: "null"}")
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting gamification data: ${e.message}", e)
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
            // Use document ID based on user+date to allow upserts and avoid duplicates
            val docId = "${userId}_$date"
            db.collection("analytics").document(docId).set(analytics, SetOptions.merge()).await()
            Log.d(TAG, "Analytics saved for date $date")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving analytics: ${e.message}", e)
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
    
    // Daily ScreenTime Operations
    suspend fun saveDailyScreenTime(
        date: String, 
        totalScreenTimeMs: Long, 
        screenTimeMinutes: Int,
        topApps: List<Map<String, Any>> = emptyList(),
        hourlyBreakdown: Map<String, Long> = emptyMap()
    ): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val screenTimeData = DailyScreenTime(
                userId = userId,
                date = date,
                totalScreenTimeMs = totalScreenTimeMs,
                screenTimeMinutes = screenTimeMinutes,
                topApps = topApps,
                hourlyBreakdown = hourlyBreakdown
            )
            // Use document ID based on user+date to allow upserts
            val docId = "${userId}_$date"
            db.collection("dailyScreenTime").document(docId).set(screenTimeData, SetOptions.merge()).await()
            Log.d(TAG, "Daily screentime saved for date $date: ${screenTimeMinutes}m")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving daily screentime: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getScreenTimeHistory(days: Int = 30): Result<List<DailyScreenTime>> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = dateFormat.format(calendar.time)
            
            val snapshot = db.collection("dailyScreenTime")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Result.success(snapshot.toObjects(DailyScreenTime::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screentime history: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getScreenTimeForDate(date: String): Result<DailyScreenTime?> {
        val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val docId = "${userId}_$date"
            val doc = db.collection("dailyScreenTime").document(docId).get().await()
            Result.success(doc.toObject(DailyScreenTime::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screentime for date: ${e.message}", e)
            Result.failure(e)
        }
    }
}
