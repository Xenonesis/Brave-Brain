# App Blocking Mechanism

## Overview
The app uses a multi-layered approach to interrupt and discourage usage of blocked apps when time limits are reached. **Important: Due to Android security restrictions, the app cannot force-kill foreground apps, but it can effectively interrupt and redirect the user.**

## How It Works

### 1. Continuous Monitoring
- **ImprovedBlockerService** runs as a foreground service
- Checks every 1 second (1000ms) for foreground app
- Tracks usage time for blocked apps in real-time

### 2. Time Limit Detection
When a blocked app reaches its time limit:
```kotlin
if (currentUsageMinutes >= timeLimitMinutes) {
    blockAppImmediately(packageName)
}
```

### 3. Multi-Method Blocking
The `executeBlocking()` method uses 4 different techniques **IN THIS ORDER**:

#### Method 1: Immediate Home Redirect 🏠
- Sends user back to home screen immediately
- Interrupts the blocked app's foreground status
- **Most effective first step** - removes app from foreground

#### Method 2: Full-Screen Blocking Activity 📱
- Shows a full-screen blocking message activity
- Prevents return to blocked app
- Stays on top even when device is locked
- Cannot be dismissed with back button

#### Method 3: Persistent Overlay Blocking 🔒
```kotlin
// Aggressive window flags
FLAG_SHOW_WHEN_LOCKED
FLAG_DISMISS_KEYGUARD  
FLAG_TURN_SCREEN_ON
FLAG_KEEP_SCREEN_ON
FLAG_NOT_FOCUSABLE
```
- Shows a full-screen overlay over the blocked app
- Prevents user interaction with the app
- Stays visible even on lock screen
- Most effective visual blocking

#### Method 4: Background Process Termination ⚠️
```kotlin
private fun forceKillApp(packageName: String) {
    // Kill background processes (limited on Android 8+)
    activityManager.killBackgroundProcesses(packageName)
    
    // Remove from recent tasks
    for (task in runningTasks) {
        if (taskInfo.baseIntent?.component?.packageName == packageName) {
            task.finishAndRemoveTask()  // Removes from recents
        }
    }
}
```

**What this does:**
- Kills background processes (only works if app is in background)
- Removes the app from Android's recent tasks list
- Forces the app to completely restart if opened again

**⚠️ LIMITATION:** On Android 8.0+, `killBackgroundProcesses()` can ONLY kill processes that are already in the background. It **CANNOT** kill the foreground app. This is why we redirect to home first.

### 4. Aggressive Re-Access Monitoring
After blocking an app, the service monitors for 60 seconds at 500ms intervals:
```kotlin
private fun startContinuousMonitoring(blockedPackage: String) {
    // Checks every 500ms for 60 seconds
    // If user tries to reopen, blocks again immediately
}
```

## Blocking Flow

```
User's blocked app hits time limit
    ↓
Service detects time limit exceeded
    ↓
EXECUTE BLOCKING (in order):
    ↓
1. Redirect to home screen (removes from foreground)
    ↓
2. Launch blocking activity (prevents return)
    ↓
3. Show persistent overlay (visual barrier)
    ↓
4. Kill background processes (cleanup)
    ↓
5. Remove from recent tasks (harder to reopen)
    ↓
Start aggressive monitoring (500ms checks for 60s)
    ↓
If user tries to reopen → BLOCK AGAIN immediately
```

## Key Features

### ✅ Immediate Interruption
- Home redirect happens in <100ms
- Multiple blocking methods execute simultaneously
- No delay between detection and blocking

### ✅ Persistent Blocking
- Aggressive 500ms monitoring for 60 seconds after block
- Automatic re-blocking if user tries to reopen
- Notifications sent for repeated access attempts
- Overlay stays visible even on lock screen

### ✅ Process Management (Limited)
- **Can** kill background processes
- **Can** remove from recent tasks
- **Cannot** kill foreground apps (Android restriction)
- **Strategy:** Interrupt first, then kill

### ✅ Multiple Fallbacks
- If one method fails, others still work
- Home + Activity + Overlay + Kill = comprehensive interruption
- Ensures app is blocked even on different Android versions

## Android Limitations

**⚠️ CRITICAL ANDROID RESTRICTIONS:**

Android security prevents apps from:
- ❌ Force-stopping other apps (requires system permissions)
- ❌ Killing foreground apps (only background processes can be killed)
- ❌ Completely preventing app launches (user can always override)
- ❌ Accessing root-level process management

**What we CAN do:**
- ✅ Redirect to home screen (interrupts foreground status)
- ✅ Show blocking overlays (visual barrier)
- ✅ Launch blocking activities (prevents return)
- ✅ Kill background processes (after redirect)
- ✅ Remove from recent tasks (harder to reopen)
- ✅ Monitor and re-block immediately (500ms intervals)

**Why it's effective anyway:**
1. Home redirect + blocking activity creates immediate interruption
2. Persistent overlay prevents accidental returns
3. Background process killing prevents sneaky re-activation
4. Aggressive re-monitoring catches re-access attempts instantly
5. Combined approach makes it very difficult to bypass

## Testing the Blocking

1. Set a 1-minute time limit on an app
2. Use the app for 1 minute
3. Observe:
   - Immediately redirected to home screen
   - Blocking activity appears
   - Overlay shows if app is reopened
   - App processes killed (in background)
   - App removed from recents
4. Try to reopen the app:
   - Should be blocked again within 500ms
   - Notification about repeated access
   - Overlay appears immediately

## Logs

Check Android Logcat for blocking events:
```
ImprovedBlockerService: === EXECUTING MULTI-LAYER BLOCKING for [app] ===
ImprovedBlockerService: ✓ Redirected to home screen
ImprovedBlockerService: ✓ Launched blocking activity
ImprovedBlockerService: ✓ Activated overlay blocking
ImprovedBlockerService: ✓ Attempted to kill app process
ImprovedBlockerService: === BLOCKING EXECUTION COMPLETE (success=true) ===
ImprovedBlockerService: Started AGGRESSIVE continuous monitoring (500ms interval)
ImprovedBlockerService: ⚠️ RE-ACCESS DETECTED - BLOCKING AGAIN!
```

## Summary

The app **interrupts and discourages** blocked app usage using:
1. **Immediate home redirect** - Removes app from foreground (most important!)
2. **Full-screen blocking activity** - Prevents return to app
3. **Persistent overlay** - Visual barrier that's hard to dismiss
4. **Background process killing** - Terminates remaining processes
5. **Task removal** - Removes from recent apps list
6. **Aggressive monitoring** - Re-blocks within 500ms if user tries again

This provides the **strongest possible interruption and blocking** within Android's security constraints. While we cannot literally "pause" or "kill" a foreground app due to Android restrictions, the multi-layer approach effectively prevents usage by:
- Making it very difficult to use the app
- Immediately interrupting any attempts
- Creating persistent barriers to access
- Monitoring and re-blocking constantly

**The goal is behavior modification through persistent interruption, not literal app termination** (which Android doesn't allow for third-party apps).
