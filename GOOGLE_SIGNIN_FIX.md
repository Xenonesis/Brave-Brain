# Google Sign-In Fix Summary

## What Was Wrong

1. **Missing OAuth Configuration**: Your `google-services.json` had an empty `oauth_client` array
2. **Hardcoded Web Client ID**: The app was using a hardcoded (incomplete) client ID instead of reading from google-services.json

## What I Fixed

### 1. Updated FirebaseAuthManager.kt
Changed from hardcoded client ID to automatic reading from google-services.json:
```kotlin
// Before (hardcoded - incomplete ID)
.requestIdToken("96136112843-7v4m2qf74h279j0g3r7k5q8q4g4.apps.googleusercontent.com")

// After (reads from google-services.json automatically)
.requestIdToken(context.getString(R.string.default_web_client_id))
```

### 2. Created Setup Guide
See `GET_SHA1_FINGERPRINTS.md` for complete instructions.

## What You Need To Do

### Quick Steps:
1. **Get SHA-1 fingerprints** (run `gradlew signingReport` for debug)
2. **Add to Firebase Console** (Project Settings → Your app → Add fingerprint)
3. **Download new google-services.json** and replace the current one
4. **Rebuild the app** (`gradlew clean assembleDebug`)

### Detailed Instructions:
Follow the step-by-step guide in `GET_SHA1_FINGERPRINTS.md`

## Why This Happens

Google Sign-In requires:
- SHA-1 fingerprint registered in Firebase (for security)
- OAuth 2.0 client configuration (generated after adding SHA-1)
- Proper Web Client ID in the app (now reads automatically)

Without the SHA-1 fingerprint, Firebase doesn't generate the OAuth client configuration, causing sign-in to fail.

## Testing After Fix

1. Install the app on a device/emulator
2. Click "Sign in with Google"
3. You should see the Google account picker
4. Select an account
5. Sign-in should complete successfully

If it still fails, check logcat for specific error messages.
