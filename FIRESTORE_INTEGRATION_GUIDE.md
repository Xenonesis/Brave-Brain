# Firestore Integration - Quick Developer Guide

## Overview

Firebase Firestore has been integrated into Brave Brain to provide cloud synchronization for user data, analytics, notifications, and gamification features. This guide shows you how to use the integration in your code.

---

## Architecture

The integration follows a **non-invasive extension pattern**:

- **Core services remain unchanged** - existing functionality continues to work
- **Sync extensions** provide Firestore capabilities alongside local storage
- **Gradual adoption** - use Firestore features as needed
- **Offline-first** - all operations work offline and sync automatically

---

## Quick Start

### 1. Initialize Authentication

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var authManager: FirebaseAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = FirebaseAuthManager(this)
        
        // Sign in anonymously if not already signed in
        if (!authManager.isSignedIn()) {
            lifecycleScope.launch {
                val result = authManager.signInAnonymously()
                if (result.isSuccess) {
                    initializeFirestoreSync()
                }
            }
        } else {
            initializeFirestoreSync()
        }
    }
}
```

### 2. Sync Analytics Data

```kotlin
// In your analytics collection code
val analyticsSync = AnalyticsFirestoreSync(context)

// Sync daily analytics
analyticsSync.syncDailyAnalytics(
    totalScreenTimeMs = totalScreenTime,
    productivityScore = productivityScore,
    blockedAttempts = blockedAttempts,
    challengesCompleted = challengesCompleted,
    challengesFailed = challengesFailed,
    usagePatterns = mapOf(
        "peakHour" to peakHour,
        "mostUsedApp" to mostUsedApp
    )
)

// Sync individual app usage
analyticsSync.syncAppUsage(
    packageName = "com.instagram.android",
    appName = "Instagram",
    usageTimeMs = 1800000, // 30 minutes
    dailyLimitMs = 3600000, // 60 minutes
    category = "social_media"
)
```

### 3. Track Notifications

```kotlin
val notificationSync = NotificationFirestoreSync(context)

// When sending a notification
notificationSync.trackNotificationSent(
    type = "usage_warning",
    title = "Screen Time Alert",
    message = "You've been using Instagram for 30 minutes",
    context = mapOf(
        "app" to "Instagram",
        "duration" to 30,
        "limit" to 60
    )
) { notificationId ->
    // Store notificationId for later updates
    if (notificationId != null) {
        saveNotificationId(notificationId)
    }
}

// When user interacts with notification
notificationSync.updateNotificationEffectiveness(
    notificationId = savedNotificationId,
    wasClicked = true,
    wasDismissed = false,
    effectiveness = 0.85
)
```

### 4. Submit User Feedback

```kotlin
val feedbackSync = FeedbackFirestoreSync(context)

feedbackSync.submitFeedback(
    feedbackType = "feature_request",
    rating = 5,
    comment = "Love the new blocking features!",
    context = mapOf(
        "version" to "0.21",
        "feature" to "smart_blocking"
    )
)
```

### 5. Fetch Historical Data

```kotlin
// Get historical analytics
val engagementSync = UserEngagementFirestoreSync(context)

lifecycleScope.launch {
    val analytics = engagementSync.getHistoricalAnalytics(days = 30)
    
    // Process analytics data
    analytics.forEach { data ->
        println("Date: ${data.date}, Score: ${data.productivityScore}")
    }
    
    // Calculate trends
    val trends = engagementSync.calculateEngagementTrends()
    val avgScore = trends["averageProductivityScore"] as? Double
}
```

### 6. Migrate Existing Data

```kotlin
val migrationHelper = FirestoreMigrationHelper(context)

lifecycleScope.launch {
    // Check migration status
    val status = migrationHelper.checkMigrationStatus()
    
    if (status.needsMigration) {
        // Show dialog to user
        AlertDialog.Builder(context)
            .setTitle("Cloud Sync Available")
            .setMessage("Would you like to sync your data to the cloud?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    val result = migrationHelper.migrateAllData()
                    if (result.isSuccess) {
                        migrationHelper.markMigrationComplete()
                        Toast.makeText(context, "Data synced!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
```

---

## Available Services

### FirebaseAuthManager
- `signInAnonymously()` - Quick anonymous authentication
- `signInWithEmail(email, password)` - Email/password authentication
- `createUserWithEmail(email, password)` - Create new account
- `getCurrentUser()` - Get current Firebase user
- `isSignedIn()` - Check authentication status
- `signOut()` - Sign out user

### FirestoreService
Core service for all Firestore operations:
- User profile management
- App usage tracking
- Notification storage
- Gamification data
- Analytics storage
- User feedback

### AnalyticsFirestoreSync
- `syncDailyAnalytics()` - Sync daily analytics data
- `syncAppUsage()` - Sync individual app usage
- `getHistoricalAnalytics()` - Fetch historical analytics

### NotificationFirestoreSync
- `trackNotificationSent()` - Track sent notifications
- `updateNotificationEffectiveness()` - Update interaction data
- `getNotificationHistory()` - Fetch notification history
- `calculateNotificationMetrics()` - Calculate effectiveness metrics

### UserEngagementFirestoreSync
- `getHistoricalUsageData()` - Fetch usage history
- `getHistoricalAnalytics()` - Fetch analytics history
- `calculateEngagementTrends()` - Calculate engagement trends

### FeedbackFirestoreSync
- `submitFeedback()` - Submit user feedback
- `getFeedbackHistory()` - Fetch feedback history
- `calculateFeedbackMetrics()` - Calculate feedback metrics

### FirestoreMigrationHelper
- `migrateAllData()` - Migrate all local data to Firestore
- `checkMigrationStatus()` - Check if migration is needed
- `markMigrationComplete()` - Mark migration as complete

---

## Data Models

### UserProfile
```kotlin
data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val createdAt: Timestamp,
    val lastSyncAt: Timestamp,
    val preferences: Map<String, Any>
)
```

### AppUsageData
```kotlin
data class AppUsageData(
    val userId: String,
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val dailyLimitMs: Long,
    val category: String,
    val date: String,
    val timestamp: Timestamp
)
```

### NotificationData
```kotlin
data class NotificationData(
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val sentAt: Timestamp,
    val wasClicked: Boolean,
    val wasDismissed: Boolean,
    val effectiveness: Double,
    val context: Map<String, Any>
)
```

### GamificationData
```kotlin
data class GamificationData(
    val userId: String,
    val points: Int,
    val level: Int,
    val badges: List<String>,
    val challenges: Map<String, Any>,
    val achievements: List<Map<String, Any>>,
    val lastUpdated: Timestamp
)
```

### AnalyticsData
```kotlin
data class AnalyticsData(
    val userId: String,
    val date: String,
    val totalScreenTimeMs: Long,
    val productivityScore: Int,
    val blockedAttempts: Int,
    val challengesCompleted: Int,
    val challengesFailed: Int,
    val usagePatterns: Map<String, Any>,
    val timestamp: Timestamp
)
```

---

## Best Practices

### 1. Error Handling
```kotlin
lifecycleScope.launch {
    try {
        val result = firestoreService.saveAnalytics(...)
        if (result.isSuccess) {
            // Success
        } else {
            // Handle failure
            Log.e("Firestore", "Failed: ${result.exceptionOrNull()}")
        }
    } catch (e: Exception) {
        Log.e("Firestore", "Error: ${e.message}")
    }
}
```

### 2. Offline Support
Firestore automatically handles offline mode - no special code needed:
```kotlin
// This works both online and offline
analyticsSync.syncDailyAnalytics(...)
// Data is cached locally and synced when online
```

### 3. Batch Operations
For multiple operations, use coroutines:
```kotlin
lifecycleScope.launch {
    // All operations run in parallel
    launch { analyticsSync.syncDailyAnalytics(...) }
    launch { notificationSync.trackNotificationSent(...) }
    launch { feedbackSync.submitFeedback(...) }
}
```

### 4. Background Sync
Sync data periodically in background:
```kotlin
class BackgroundSyncWorker(context: Context, params: WorkerParameters) 
    : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val analyticsSync = AnalyticsFirestoreSync(applicationContext)
        analyticsSync.syncDailyAnalytics(...)
        return Result.success()
    }
}

// Schedule periodic sync
val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
    15, TimeUnit.MINUTES
).build()

WorkManager.getInstance(context).enqueue(syncRequest)
```

---

## Testing

### Test Firestore Integration
```kotlin
@Test
fun testFirestoreSync() = runTest {
    val authManager = FirebaseAuthManager(context)
    val result = authManager.signInAnonymously()
    
    assertTrue(result.isSuccess)
    assertNotNull(authManager.getCurrentUserId())
}
```

### Test Offline Mode
1. Enable airplane mode
2. Use app normally
3. Disable airplane mode
4. Verify data syncs automatically

---

## Troubleshooting

### Issue: Authentication fails
```kotlin
// Check if user is signed in
if (!authManager.isSignedIn()) {
    lifecycleScope.launch {
        authManager.signInAnonymously()
    }
}
```

### Issue: Data not syncing
```kotlin
// Check internet connection
val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
val isConnected = connectivityManager.activeNetworkInfo?.isConnected == true

if (!isConnected) {
    // Data will sync when connection is restored
    Toast.makeText(context, "Offline - data will sync later", Toast.LENGTH_SHORT).show()
}
```

---

## Performance Tips

1. **Batch writes** - Group multiple operations together
2. **Use indexes** - Create indexes for frequently queried fields
3. **Limit query results** - Use pagination for large datasets
4. **Cache data** - Firestore automatically caches, but you can also cache in memory
5. **Monitor usage** - Check Firebase Console for performance metrics

---

## Next Steps

1. ✅ Follow FIREBASE_SETUP.md to configure Firebase
2. ✅ Test authentication flow
3. ✅ Integrate sync calls in existing code
4. ✅ Test offline functionality
5. ✅ Monitor data in Firebase Console
6. 🔄 Implement data migration for existing users
7. 🔄 Add analytics dashboards using cloud data
8. 🔄 Set up Cloud Functions for advanced features

---

## Support

For Firebase setup instructions, see [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

For Firebase documentation, visit [Firebase Docs](https://firebase.google.com/docs/firestore)

---

**Happy coding! 🚀**
