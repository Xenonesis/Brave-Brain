# Firebase Authentication Troubleshooting Guide

## Issue Description
Error: "The supplied auth credential is incorrect, malformed or has expired." for action `RecaptchaAction(action=signInWithPassword)`

This error typically occurs during email/password authentication when using Firebase Authentication with reCAPTCHA verification enabled.

## Root Causes and Solutions

### 1. SHA-1/SHA-256 Certificate Fingerprint
**Problem**: Android apps require SHA-1/SHA-256 fingerprints to be registered in the Firebase Console for authentication to work properly.

**Solution**:
1. Generate your SHA-1 and SHA-256 fingerprints:
   ```bash
   # For debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # For release keystore
   keytool -list -v -keystore /path/to/your/release/keystore.jks -alias your_alias_name
   ```

2. Go to Firebase Console → Project Settings → General
3. Add the SHA-1 and SHA-256 fingerprints to your Android app configuration

### 2. Google Sign-In Client ID
**Problem**: The Google Sign-In client ID in `FirebaseAuthManager.kt` might be incorrect.

**Solution**:
1. Go to Firebase Console → Project Settings → General
2. Find your "Web client ID" (not Android client ID) under OAuth consent
3. Update the client ID in `FirebaseAuthManager.kt`:
   ```kotlin
   .requestIdToken("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")
   ```

### 3. reCAPTCHA Configuration
**Problem**: Firebase Authentication may require reCAPTCHA verification in certain scenarios.

**Solution**:
1. Go to Firebase Console → Authentication → Sign-in method
2. Check if reCAPTCHA is enabled for your app
3. Ensure your app is properly configured to handle reCAPTCHA verification

### 4. App Configuration
**Problem**: The app package name in `google-services.json` must exactly match the package name in your `AndroidManifest.xml` and `build.gradle`.

**Solution**:
1. Verify that the package name in `google-services.json` matches:
   - `applicationId` in `app/build.gradle.kts`
   - `package` attribute in `AndroidManifest.xml`

### 5. Network/Emulator Issues
**Problem**: Running on an emulator or network restrictions may trigger additional security checks.

**Solution**:
1. Try running on a physical device
2. Ensure proper internet connectivity
3. Check if any VPN or proxy is interfering with Firebase requests

## Additional Debugging Steps

1. Enable Firebase debug logging in `BraveBrainApplication.kt`:
   ```kotlin
   override fun onCreate() {
       super.onCreate()
       
       // Enable Firebase debugging
       if (BuildConfig.DEBUG) {
           FirebaseApp.initializeApp(this)
           android.util.Log.d("FirebaseApp", "Firebase initialized")
       } else {
           FirebaseApp.initializeApp(this)
       }
   }
   ```

2. Check Android logs during authentication:
   ```bash
   adb logcat | grep -i firebase
   adb logcat | grep -i auth
   ```

3. Ensure all Firebase dependencies are properly configured in `app/build.gradle.kts`:
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
   implementation("com.google.firebase:firebase-auth-ktx")
   implementation("com.google.android.gms:play-services-auth:20.7.0")
   ```

## Testing the Fix

After implementing the solutions:

1. Clean and rebuild the project:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. Test authentication with valid credentials
3. Check logs for any remaining errors
4. Verify that authentication works on both debug and release builds

## Notes

- The error specifically mentions `RecaptchaAction`, indicating that the issue is likely related to reCAPTCHA verification
- Make sure your Firebase project has the Authentication service enabled
- Verify that your app meets all requirements for the authentication methods you're using
