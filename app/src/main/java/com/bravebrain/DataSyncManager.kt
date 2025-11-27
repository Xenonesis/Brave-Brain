package com.bravebrain

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * DataSyncManager handles synchronization of all app data to Firestore.
 * This ensures all user data is persisted to the cloud database for backup and cross-device sync.
 */
class DataSyncManager(private val context: Context) {
    private val firestoreService = FirestoreService(context)
    private val authManager = FirebaseAuthManager(context)
    private val db = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "DataSyncManager"
        private const val PREFS_NAME = "data_sync_prefs"
        private const val KEY_COLLECTIONS_INITIALIZED = "collections_initialized"
    }
    
    /**
     * Initializes all Firestore collections with initial data for a new user.
     * This ensures collections are visible in Firebase Console and the database is ready for use.
     * Should be called after successful login/signup.
     */
    suspend fun initializeCollections(): Boolean {
        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Log.e(TAG, "Cannot initialize collections: User not authenticated")
            return false
        }
        
        // Check if already initialized
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean("${KEY_COLLECTIONS_INITIALIZED}_$userId", false)) {
            Log.d(TAG, "Collections already initialized for user $userId")
            return true
        }
        
        Log.d(TAG, "Initializing Firestore collections for user $userId...")
        
        return withContext(Dispatchers.IO) {
            try {
                val user = authManager.getCurrentUser()
                val email = user?.email ?: ""
                val displayName = user?.displayName ?: "Brave Brain User"
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                
                // 1. Create user profile in 'users' collection
                val userProfile = hashMapOf(
                    "userId" to userId,
                    "email" to email,
                    "displayName" to displayName,
                    "createdAt" to Timestamp.now(),
                    "lastSyncAt" to Timestamp.now(),
                    "preferences" to mapOf(
                        "theme" to "system",
                        "notifications" to true
                    )
                )
                db.collection("users").document(userId).set(userProfile).await()
                Log.d(TAG, "✓ Created 'users' collection with profile")
                
                // 2. Create initial analytics entry in 'analytics' collection
                val analyticsData = hashMapOf(
                    "userId" to userId,
                    "date" to today,
                    "totalScreenTimeMs" to 0L,
                    "productivityScore" to 0,
                    "blockedAttempts" to 0,
                    "challengesCompleted" to 0,
                    "challengesFailed" to 0,
                    "usagePatterns" to mapOf(
                        "initialized" to true,
                        "lastSyncTime" to System.currentTimeMillis()
                    ),
                    "timestamp" to Timestamp.now()
                )
                db.collection("analytics").document("${userId}_$today").set(analyticsData).await()
                Log.d(TAG, "✓ Created 'analytics' collection with initial entry")
                
                // 3. Create initial gamification entry in 'gamification' collection
                val gamificationData = hashMapOf(
                    "userId" to userId,
                    "points" to 0,
                    "level" to 1,
                    "badges" to listOf<String>(),
                    "challenges" to mapOf(
                        "dailyStreak" to 0,
                        "challengeStreak" to 0,
                        "productivityStreak" to 0,
                        "initialized" to true
                    ),
                    "achievements" to listOf<Map<String, Any>>(),
                    "lastUpdated" to Timestamp.now()
                )
                db.collection("gamification").document(userId).set(gamificationData).await()
                Log.d(TAG, "✓ Created 'gamification' collection with initial entry")
                
                // 4. Create initial notification entry in 'notifications' collection
                val notificationData = hashMapOf(
                    "userId" to userId,
                    "type" to "welcome",
                    "title" to "Welcome to Brave Brain!",
                    "message" to "Your account has been set up successfully. Start tracking your app usage!",
                    "sentAt" to Timestamp.now(),
                    "wasClicked" to false,
                    "wasDismissed" to false,
                    "effectiveness" to 0.0,
                    "context" to mapOf("source" to "account_creation")
                )
                db.collection("notifications").add(notificationData).await()
                Log.d(TAG, "✓ Created 'notifications' collection with welcome notification")
                
                // 5. Create initial appUsage entry in 'appUsage' collection
                val appUsageData = hashMapOf(
                    "userId" to userId,
                    "packageName" to "com.bravebrain",
                    "appName" to "Brave Brain",
                    "usageTimeMs" to 0L,
                    "dailyLimitMs" to 0L,
                    "category" to "productivity",
                    "date" to today,
                    "timestamp" to Timestamp.now()
                )
                db.collection("appUsage").document("${userId}_com.bravebrain_$today").set(appUsageData).await()
                Log.d(TAG, "✓ Created 'appUsage' collection with initial entry")
                
                // 6. Create initial feedback entry in 'feedback' collection (optional - for structure)
                val feedbackData = hashMapOf(
                    "userId" to userId,
                    "feedbackType" to "account_created",
                    "rating" to 5,
                    "comment" to "Account initialized",
                    "context" to mapOf("source" to "auto_init"),
                    "timestamp" to Timestamp.now()
                )
                db.collection("feedback").add(feedbackData).await()
                Log.d(TAG, "✓ Created 'feedback' collection with initial entry")
                
                // Mark as initialized
                prefs.edit().putBoolean("${KEY_COLLECTIONS_INITIALIZED}_$userId", true).apply()
                
                Log.d(TAG, "✅ All Firestore collections initialized successfully!")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing collections: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Syncs all app data to Firestore. Should be called:
     * - On app resume
     * - After login
     * - After settings changes
     * - Periodically in background
     */
    fun syncAllData() {
        if (!authManager.isSignedIn()) {
            Log.d(TAG, "User not signed in, skipping sync")
            return
        }
        
        Log.d(TAG, "Starting data sync to Firestore...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure collections are initialized first
                initializeCollections()
                
                syncAppSettings()
                syncAnalytics()
                syncGamification()
                syncUserProfile()
                Log.d(TAG, "Data sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during data sync: ${e.message}", e)
            }
        }
    }
    
    /**
     * Syncs app settings and usage data for blocked apps
     */
    private suspend fun syncAppSettings() {
        try {
            val prefs = context.getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val blockedApps = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
            val timeLimits = prefs.getString("time_limits", "") ?: ""
            
            if (blockedApps.isEmpty()) {
                Log.d(TAG, "No blocked apps to sync")
                return
            }
            
            Log.d(TAG, "Syncing ${blockedApps.size} blocked apps")
            
            blockedApps.forEach { packageName ->
                try {
                    val limit = extractTimeLimit(timeLimits, packageName)
                    val usageMs = UsageUtils.getAppUsage(context, packageName)
                    val appName = getAppName(packageName)
                    
                    firestoreService.saveAppUsage(
                        packageName = packageName,
                        appName = appName,
                        usageTimeMs = usageMs,
                        dailyLimitMs = limit,
                        category = "blocked"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing app $packageName: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing app settings: ${e.message}", e)
        }
    }
    
    /**
     * Syncs analytics data including productivity scores and usage stats
     */
    private suspend fun syncAnalytics() {
        try {
            val prefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            
            // Get total screen time from UsageUtils for accurate data
            val totalScreenTimeMs = UsageUtils.getTotalUsage(context)
            
            // Get productivity score - check both possible keys
            val productivityScore = prefs.getInt("productivity_score", 0).takeIf { it > 0 }
                ?: prefs.getInt("productivity_score_$today", 0)
            
            val blockedAttempts = prefs.getInt("blocked_attempts_$today", 0)
            val challengesCompleted = prefs.getInt("challenges_completed_$today", 0)
            val challengesFailed = prefs.getInt("challenges_failed_$today", 0)
            val peakHour = prefs.getInt("peak_usage_hour", -1)
            
            // Build usage patterns map
            val usagePatterns = mutableMapOf<String, Any>(
                "peakUsageHour" to peakHour,
                "lastSyncTime" to System.currentTimeMillis()
            )
            
            // Add daily stats if available
            val dailyStatsRaw = prefs.getString("daily_stats-$today", null)
            if (dailyStatsRaw != null) {
                val parts = dailyStatsRaw.split(",")
                if (parts.size >= 9) {
                    usagePatterns["appSessions"] = parts[2].toIntOrNull() ?: 0
                    usagePatterns["averageSessionLength"] = parts[3].toLongOrNull() ?: 0L
                    usagePatterns["longestSession"] = parts[4].toLongOrNull() ?: 0L
                    usagePatterns["mostUsedApp"] = parts[5]
                }
            }
            
            Log.d(TAG, "Syncing analytics - Score: $productivityScore, Blocked: $blockedAttempts, Challenges: $challengesCompleted")
            
            firestoreService.saveAnalytics(
                date = today,
                totalScreenTimeMs = totalScreenTimeMs,
                productivityScore = productivityScore,
                blockedAttempts = blockedAttempts,
                challengesCompleted = challengesCompleted,
                challengesFailed = challengesFailed,
                usagePatterns = usagePatterns
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing analytics: ${e.message}", e)
        }
    }
    
    /**
     * Syncs gamification data - uses the correct SharedPreferences name "gamification_data"
     */
    private suspend fun syncGamification() {
        try {
            // Use "gamification_data" - this is what GamificationActivity and GamificationUtils use
            val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
            
            val level = prefs.getInt("user_level", 1)
            val xp = prefs.getInt("user_xp", 0)
            val dailyStreak = prefs.getInt("daily_streak", 0)
            val challengeStreak = prefs.getInt("challenge_streak", 0)
            val productivityStreak = prefs.getInt("productivity_streak", 0)
            val badges = prefs.getStringSet("earned_badges", emptySet())?.toList() ?: emptyList()
            val totalBadges = prefs.getInt("total_badges", 0)
            
            // Build challenges map with streak data
            val challengesMap = mapOf<String, Any>(
                "xp" to xp,
                "dailyStreak" to dailyStreak,
                "challengeStreak" to challengeStreak,
                "productivityStreak" to productivityStreak,
                "totalBadges" to totalBadges,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            Log.d(TAG, "Syncing gamification - Level: $level, XP: $xp, Badges: ${badges.size}")
            
            firestoreService.saveGamificationData(
                points = xp,
                level = level,
                badges = badges,
                challenges = challengesMap
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing gamification: ${e.message}", e)
        }
    }
    
    /**
     * Syncs user profile information
     */
    private suspend fun syncUserProfile() {
        try {
            val user = authManager.getCurrentUser()
            if (user != null) {
                val email = user.email ?: ""
                val displayName = user.displayName ?: "Brave Brain User"
                
                firestoreService.createOrUpdateUserProfile(
                    email = email,
                    displayName = displayName
                )
                Log.d(TAG, "User profile synced: $displayName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user profile: ${e.message}", e)
        }
    }
    
    /**
     * Extracts time limit in milliseconds for a specific package
     */
    private fun extractTimeLimit(timeLimits: String, packageName: String): Long {
        timeLimits.split("|").forEach { entry ->
            val parts = entry.split(",")
            if (parts.size == 2 && parts[0] == packageName) {
                // Convert minutes to milliseconds
                return (parts[1].toLongOrNull() ?: 0L) * 60 * 1000
            }
        }
        return 0L
    }
    
    /**
     * Gets the human-readable app name from a package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    /**
     * Restore data from Firestore to local storage (for new device/reinstall)
     */
    suspend fun restoreFromCloud(): Boolean {
        if (!authManager.isSignedIn()) return false
        
        return withContext(Dispatchers.IO) {
            try {
                // Restore gamification data
                val gamificationResult = firestoreService.getGamificationData()
                gamificationResult.getOrNull()?.let { data ->
                    val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putInt("user_level", data.level)
                        putInt("user_xp", data.points)
                        putStringSet("earned_badges", data.badges.toSet())
                        putInt("total_badges", data.badges.size)
                        
                        // Restore challenges data
                        val challenges = data.challenges
                        putInt("daily_streak", (challenges["dailyStreak"] as? Number)?.toInt() ?: 0)
                        putInt("challenge_streak", (challenges["challengeStreak"] as? Number)?.toInt() ?: 0)
                        putInt("productivity_streak", (challenges["productivityStreak"] as? Number)?.toInt() ?: 0)
                        
                        apply()
                    }
                    Log.d(TAG, "Gamification data restored from cloud")
                }
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring from cloud: ${e.message}", e)
                false
            }
        }
    }
}
