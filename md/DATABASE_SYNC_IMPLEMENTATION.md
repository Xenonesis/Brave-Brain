# Database Sync Implementation

## Overview
All user data is now being saved to Firebase Firestore database per user. The implementation uses a centralized `DataSyncManager` that syncs local SharedPreferences data to Firestore.

## What Data is Being Saved

### 1. App Settings
- **Blocked apps list**: Package names of apps selected for blocking
- **Time limits**: Daily time limits set for each blocked app
- **Usage data**: Current usage time for each app

### 2. Analytics Data
- **Total screen time**: Daily screen time in milliseconds
- **Productivity score**: Daily productivity score (0-100)
- **Blocked attempts**: Number of times apps were blocked
- **Challenges completed**: Number of challenges successfully completed
- **Challenges failed**: Number of challenges failed
- **Usage patterns**: Behavioral patterns and trends

### 3. Gamification Data
- **Points**: Total points earned
- **Level**: Current user level
- **Badges**: List of earned badges
- **Challenges**: Challenge progress and history

### 4. User Profile
- **User ID**: Firebase authentication user ID
- **Email**: User's email address
- **Display name**: User's display name
- **Last sync timestamp**: When data was last synced

## Implementation Details

### DataSyncManager.kt
Created a new class that handles all data synchronization:
- `syncAllData()`: Main method that syncs all data types
- `syncAppSettings()`: Syncs blocked apps and time limits
- `syncAnalytics()`: Syncs daily analytics data
- `syncGamification()`: Syncs gamification progress

### Integration Points

#### 1. MainActivity.kt
- **Location**: `onResume()` method
- **Trigger**: When app resumes or returns from other activities
- **Purpose**: Ensures data is synced whenever user returns to main screen

#### 2. AnalyticsService.kt
- **Location**: `updateInsights()` method
- **Trigger**: When analytics are updated (periodically)
- **Purpose**: Syncs analytics data after calculations

#### 3. LoginActivity.kt
- **Location**: `saveLoginState()` method
- **Trigger**: Immediately after successful login/signup
- **Purpose**: Initial sync of all local data to Firestore for new sessions

## How It Works

1. **Authentication Check**: All sync operations check if user is authenticated before proceeding
2. **Background Sync**: Data is synced in background using Kotlin Coroutines (Dispatchers.IO)
3. **Non-blocking**: Sync operations don't block the UI thread
4. **Automatic**: Sync happens automatically at key points in the app lifecycle

## Data Flow

```
User Action → Local Storage (SharedPreferences) → DataSyncManager → FirestoreService → Firebase Firestore
```

## Privacy & Security

- All data is associated with the authenticated user's ID
- Data is only synced when user is logged in
- Firestore security rules should be configured to ensure users can only access their own data
- No data is synced if user is not authenticated

## Firestore Collections Structure

### users
```
{
  userId: string,
  email: string,
  displayName: string,
  lastSyncAt: timestamp
}
```

### appUsage
```
{
  userId: string,
  packageName: string,
  appName: string,
  usageTimeMs: number,
  dailyLimitMs: number,
  category: string,
  date: string (yyyy-MM-dd)
}
```

### analytics
```
{
  userId: string,
  date: string (yyyy-MM-dd),
  totalScreenTimeMs: number,
  productivityScore: number,
  blockedAttempts: number,
  challengesCompleted: number,
  challengesFailed: number,
  usagePatterns: map
}
```

### gamification
```
{
  userId: string,
  points: number,
  level: number,
  badges: array,
  challenges: map,
  updatedAt: timestamp
}
```

## Testing

To verify data is being saved:
1. Login to the app
2. Select apps and set time limits
3. Use the app normally for a day
4. Check Firebase Console → Firestore Database
5. Look for your user ID in the collections
6. Verify data is present and up-to-date

## Future Enhancements

- Add data restore functionality (sync from Firestore to local)
- Implement conflict resolution for offline changes
- Add periodic background sync service
- Implement data export/import features
- Add data deletion on account removal
