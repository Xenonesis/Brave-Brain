# Firebase Firestore Integration Setup Guide

## Overview

This guide will help you set up Firebase Firestore for the Brave Brain app, enabling cloud synchronization of user data, analytics, notifications, and gamification features.

---

## Prerequisites

- Android Studio installed
- Google account for Firebase Console
- Brave Brain app source code

---

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select existing project
3. Enter project name: `brave-brain` (or your preferred name)
4. Enable Google Analytics (optional but recommended)
5. Click "Create project"

---

## Step 2: Add Android App to Firebase

1. In Firebase Console, click "Add app" and select Android
2. Enter your package name: `com.bravebrain`
3. Enter app nickname: `Brave Brain`
4. (Optional) Enter SHA-1 certificate fingerprint for authentication
5. Click "Register app"

### Get SHA-1 Certificate (for authentication):

```bash
# Debug certificate
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release certificate (when ready for production)
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-key-alias
```

---

## Step 3: Download google-services.json

1. Download the `google-services.json` file from Firebase Console
2. Place it in your app module directory:
   ```
   brain-rot-app-main/app/google-services.json
   ```
3. **IMPORTANT**: Add to `.gitignore` to avoid committing sensitive data:
   ```
   # Add to .gitignore
   google-services.json
   ```

---

## Step 4: Update build.gradle Files

### Project-level build.gradle.kts

Add the Google Services plugin to the buildscript:

```kotlin
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}
```

### App-level build.gradle.kts

The dependencies are already added. Just apply the plugin at the top:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")  // Add this line
}
```

---

## Step 5: Enable Firestore in Firebase Console

1. In Firebase Console, go to "Firestore Database"
2. Click "Create database"
3. Select "Start in production mode" (we'll add security rules next)
4. Choose a Cloud Firestore location (select closest to your users)
5. Click "Enable"

---

## Step 6: Configure Firestore Security Rules

In Firebase Console, go to Firestore Database > Rules and paste the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the document
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // User profiles - users can only read/write their own profile
    match /users/{userId} {
      allow read, write: if isOwner(userId);
    }
    
    // App usage data - users can only access their own data
    match /appUsage/{usageId} {
      allow read: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // Notifications - users can only access their own notifications
    match /notifications/{notificationId} {
      allow read: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow update: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow delete: if false; // Don't allow deletion, keep for analytics
    }
    
    // Gamification data - users can only access their own data
    match /gamification/{userId} {
      allow read, write: if isOwner(userId);
    }
    
    // Analytics data - users can only access their own analytics
    match /analytics/{analyticsId} {
      allow read: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // User feedback - users can only access their own feedback
    match /feedback/{feedbackId} {
      allow read: if isAuthenticated() && resource.data.userId == request.auth.uid;
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow update, delete: if false; // Feedback is immutable
    }
  }
}
```

Click "Publish" to apply the rules.

---

## Step 7: Create Firestore Indexes

For optimal query performance, create these indexes in Firebase Console > Firestore Database > Indexes:

### Index 1: App Usage by User and Date
- Collection: `appUsage`
- Fields:
  - `userId` (Ascending)
  - `date` (Descending)
- Query scope: Collection

### Index 2: Notifications by User and Time
- Collection: `notifications`
- Fields:
  - `userId` (Ascending)
  - `sentAt` (Descending)
- Query scope: Collection

### Index 3: Analytics by User and Date
- Collection: `analytics`
- Fields:
  - `userId` (Ascending)
  - `date` (Descending)
- Query scope: Collection

### Index 4: Feedback by User and Time
- Collection: `feedback`
- Fields:
  - `userId` (Ascending)
  - `timestamp` (Descending)
- Query scope: Collection

**Note**: Firebase will automatically suggest creating indexes when you run queries that need them.

---

## Step 8: Enable Authentication

1. In Firebase Console, go to "Authentication"
2. Click "Get started"
3. Enable authentication methods:
   - **Anonymous**: Enable for quick user onboarding
   - **Email/Password**: Enable for persistent accounts (optional)
   - **Google Sign-In**: Enable for easy authentication (optional)

---

## Step 9: Initialize Firebase in Your App

The Firebase initialization is already handled in the code. To use it:

### Sign in anonymously (automatic):

```kotlin
val authManager = FirebaseAuthManager(context)
lifecycleScope.launch {
    val result = authManager.signInAnonymously()
    if (result.isSuccess) {
        // User signed in, can now use Firestore
        initializeFirestoreSync()
    }
}
```

### Use Firestore services:

```kotlin
// Analytics sync
val analyticsSync = AnalyticsFirestoreSync(context)
analyticsSync.syncDailyAnalytics(
    totalScreenTimeMs = 3600000,
    productivityScore = 85,
    blockedAttempts = 3,
    challengesCompleted = 5,
    challengesFailed = 1,
    usagePatterns = mapOf("peakHour" to 14)
)

// Notification tracking
val notificationSync = NotificationFirestoreSync(context)
notificationSync.trackNotificationSent(
    type = "usage_warning",
    title = "Screen Time Alert",
    message = "You've been using Instagram for 30 minutes",
    context = mapOf("app" to "Instagram", "duration" to 30)
)

// User feedback
val feedbackSync = FeedbackFirestoreSync(context)
feedbackSync.submitFeedback(
    feedbackType = "feature_request",
    rating = 5,
    comment = "Love the new blocking features!",
    context = mapOf("version" to "0.21")
)
```

---

## Step 10: Data Migration

To migrate existing local data to Firestore:

```kotlin
val migrationHelper = FirestoreMigrationHelper(context)

// Check if migration is needed
lifecycleScope.launch {
    val status = migrationHelper.checkMigrationStatus()
    
    if (status.needsMigration) {
        // Show migration dialog to user
        showMigrationDialog {
            // Perform migration
            val result = migrationHelper.migrateAllData()
            if (result.isSuccess) {
                migrationHelper.markMigrationComplete()
                Toast.makeText(context, "Data migrated successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

---

## Offline Support

Firestore automatically handles offline support:

- Data is cached locally when offline
- Changes are queued and synced when connection is restored
- Queries work seamlessly with cached data
- No additional code required!

---

## Testing Firestore Integration

### Test in Firebase Console:

1. Go to Firestore Database > Data
2. You should see collections being created as you use the app
3. Click on documents to view data structure

### Test Offline Mode:

1. Enable airplane mode on your device
2. Use the app normally
3. Disable airplane mode
4. Check Firebase Console - data should sync automatically

---

## Security Best Practices

1. **Never commit google-services.json** to version control
2. **Use authentication** - anonymous auth is enabled by default
3. **Validate data** on the client before sending to Firestore
4. **Monitor usage** in Firebase Console to detect anomalies
5. **Set up billing alerts** to avoid unexpected charges
6. **Review security rules** regularly
7. **Use environment-specific projects** (dev, staging, production)

---

## Monitoring and Analytics

### Enable Performance Monitoring:

1. In Firebase Console, go to "Performance"
2. Click "Get started"
3. Add dependency to build.gradle.kts:
   ```kotlin
   implementation("com.google.firebase:firebase-perf-ktx")
   ```

### Enable Crashlytics:

1. In Firebase Console, go to "Crashlytics"
2. Click "Get started"
3. Add dependencies:
   ```kotlin
   implementation("com.google.firebase:firebase-crashlytics-ktx")
   ```

---

## Troubleshooting

### Issue: "google-services.json not found"
**Solution**: Ensure the file is in `app/` directory and sync Gradle

### Issue: "FirebaseApp initialization unsuccessful"
**Solution**: Check that google-services.json matches your package name

### Issue: "Permission denied" errors
**Solution**: Verify security rules and ensure user is authenticated

### Issue: Queries are slow
**Solution**: Create composite indexes as suggested by Firebase

### Issue: Offline data not syncing
**Solution**: Check internet connection and Firebase Console status

---

## Cost Estimation

Firestore pricing (as of 2024):

- **Free tier**:
  - 1 GB storage
  - 50,000 reads/day
  - 20,000 writes/day
  - 20,000 deletes/day

- **Paid tier** (after free tier):
  - $0.18 per GB storage/month
  - $0.06 per 100,000 reads
  - $0.18 per 100,000 writes
  - $0.02 per 100,000 deletes

**Estimated cost for 1,000 active users**: $5-15/month

---

## Next Steps

1. ✅ Complete Firebase setup
2. ✅ Test authentication flow
3. ✅ Verify data is syncing
4. ✅ Test offline functionality
5. ✅ Monitor usage in Firebase Console
6. 🔄 Implement data migration for existing users
7. 🔄 Add error handling and retry logic
8. 🔄 Set up Firebase Analytics for user insights
9. 🔄 Configure Cloud Functions for advanced features (optional)

---

## Support

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Firebase Support](https://firebase.google.com/support)

---

**Your Brave Brain app is now cloud-enabled! 🚀**
