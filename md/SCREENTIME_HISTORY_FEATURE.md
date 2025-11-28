# Screentime History Feature Implementation

## Overview
This document describes the implementation of the Screentime History feature that saves all user screentime data to Firebase Firestore and provides a dedicated page to view historical screentime data by date.

## Features Implemented

### 1. Data Model (`FirestoreModels.kt`)
Added a new `DailyScreenTime` data model:
- `userId`: User identifier
- `date`: Date in YYYY-MM-DD format
- `totalScreenTimeMs`: Total screentime in milliseconds
- `screenTimeMinutes`: Total screentime in minutes (for easy display)
- `topApps`: List of top 5 apps used that day with usage stats
- `hourlyBreakdown`: Map of hourly usage data
- `timestamp`: When the data was recorded

### 2. Firestore Service Methods (`FirestoreService.kt`)
Added three new methods:

#### `saveDailyScreenTime()`
Saves daily screentime data to Firestore with:
- Automatic upsert based on userId + date (prevents duplicates)
- Stores total screentime, top apps, and hourly breakdown
- Uses document ID pattern: `{userId}_{date}`

#### `getScreenTimeHistory(days: Int)`
Retrieves screentime history for the specified number of days:
- Default: Last 30 days
- Returns data sorted by date (descending)
- Filters by current user

#### `getScreenTimeForDate(date: String)`
Retrieves screentime data for a specific date:
- Useful for detailed daily views
- Returns null if no data exists for that date

### 3. Data Sync Manager Integration (`DataSyncManager.kt`)
Added `syncDailyScreenTime()` method that:
- Automatically syncs screentime data during regular sync operations
- Calculates total screentime from UsageStats
- Extracts top 5 apps with usage details
- Tracks hourly breakdown
- Called automatically by `syncAllData()`

### 4. Screentime History Activity (`ScreentimeHistoryActivity.kt`)
New Activity that displays screentime history:

#### Features:
- **RecyclerView** with custom adapter for displaying history
- **Swipe-to-refresh** for manual data reload
- **Color-coded screentime values**:
  - Green: < 2 hours (good)
  - Orange: 2-4 hours (moderate)
  - Red: > 4 hours (high usage)
- **Date formatting**: Shows friendly dates (e.g., "Monday, Dec 25, 2023")
- **Top apps display**: Shows top 3 apps per day
- **Empty state**: Friendly message when no data is available
- **Loading state**: Progress indicator while fetching data

#### UI Components:
- Header card with feature description
- Individual cards for each day
- Shows total screentime and top apps per day
- Back button in toolbar

### 5. UI Integration (`MainActivity.kt`)
Added a new button in the "YOUR PROGRESS" section:
- **"View Screentime History"** button
- Located between the stats card and gamification button
- Opens the ScreentimeHistoryActivity
- Uses trending up icon for visual appeal

### 6. Layout Files

#### `activity_screentime_history.xml`
Main layout with:
- SwipeRefreshLayout for pull-to-refresh
- Header card explaining the feature
- RecyclerView for history items
- Progress bar for loading state
- Empty state text view

#### `item_screentime_history.xml`
Individual history item card showing:
- Date in friendly format
- Total screentime in hours and minutes
- Top 3 apps with usage time
- Color-coded screentime value
- Material card design with elevation

### 7. Manifest Registration (`AndroidManifest.xml`)
Registered the new activity:
```xml
<activity android:name=".ScreentimeHistoryActivity"
    android:exported="false"
    android:theme="@style/Theme.Testing" />
```

## Data Sync Flow

```
User Activity
    ↓
MainActivity (calls DataSyncManager.syncAllData())
    ↓
DataSyncManager.syncDailyScreenTime()
    ↓
UsageUtils (gets device screentime data)
    ↓
FirestoreService.saveDailyScreenTime()
    ↓
Firebase Firestore (dailyScreenTime collection)
```

## Viewing History Flow

```
User clicks "View Screentime History"
    ↓
ScreentimeHistoryActivity opens
    ↓
Loads data via FirestoreService.getScreenTimeHistory(30)
    ↓
Displays data in RecyclerView
    ↓
User can swipe to refresh for latest data
```

## Key Benefits

1. **Automatic Sync**: Screentime data is automatically saved whenever `DataSyncManager.syncAllData()` is called
2. **Historical Tracking**: Users can see their screentime trends over the last 30 days
3. **Visual Feedback**: Color coding helps users quickly identify high-usage days
4. **Top Apps Insight**: Shows which apps consumed the most time each day
5. **Cloud Backup**: All data is stored in Firestore for cross-device access
6. **Duplicate Prevention**: Uses unique document IDs to prevent duplicate entries
7. **Offline Support**: Firestore persistence allows viewing history offline

## Database Structure

### Collection: `dailyScreenTime`
```
dailyScreenTime/
  {userId}_{date}/
    userId: "abc123"
    date: "2024-01-15"
    totalScreenTimeMs: 7200000
    screenTimeMinutes: 120
    topApps: [
      {
        packageName: "com.instagram.android"
        appName: "Instagram"
        usageTimeMs: 2700000
        usageMinutes: 45
      },
      ...
    ]
    hourlyBreakdown: {
      hour_14: 3600000
      hour_15: 2400000
      lastUpdated: 1705334400000
    }
    timestamp: Timestamp
```

## Usage

### For Users:
1. Use the app normally - screentime is tracked automatically
2. Go to MainActivity
3. Scroll to "YOUR PROGRESS" section
4. Click "View Screentime History"
5. See your daily screentime for the last 30 days
6. Pull down to refresh data

### For Developers:
```kotlin
// Save screentime manually
val firestoreService = FirestoreService(context)
lifecycleScope.launch {
    firestoreService.saveDailyScreenTime(
        date = "2024-01-15",
        totalScreenTimeMs = 7200000,
        screenTimeMinutes = 120,
        topApps = listOf(...),
        hourlyBreakdown = mapOf(...)
    )
}

// Retrieve history
lifecycleScope.launch {
    val result = firestoreService.getScreenTimeHistory(30)
    result.fold(
        onSuccess = { history -> /* Display data */ },
        onFailure = { error -> /* Handle error */ }
    )
}
```

## Future Enhancements

Potential improvements:
1. **Weekly/Monthly aggregates**: Show average screentime per week/month
2. **Charts and graphs**: Visual representation of screentime trends
3. **Comparison views**: Compare current week vs previous weeks
4. **Export functionality**: Export data as CSV or PDF
5. **Custom date ranges**: Allow users to select specific date ranges
6. **Detailed hourly view**: Show screentime breakdown by hour
7. **Goals and targets**: Set screentime goals and track progress
8. **Notifications**: Alert users when approaching daily limits

## Testing

To test the feature:
1. Ensure Firebase is properly configured
2. Use the app with usage access permission granted
3. Wait for automatic sync or manually trigger via MainActivity refresh
4. Navigate to Screentime History
5. Verify data is displayed correctly
6. Test swipe-to-refresh functionality
7. Check different screentime values for color coding

## Notes

- Data is synced automatically when MainActivity is opened/resumed
- Requires user to be authenticated with Firebase
- Requires usage access permission to collect screentime data
- Historical data is retained indefinitely in Firestore
- Users can view up to 30 days of history by default (configurable)
