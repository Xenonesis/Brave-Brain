# Fix Google Sign-In - Get SHA-1 Fingerprints

## The Problem
Your `google-services.json` file is missing OAuth client configuration because the SHA-1 fingerprints haven't been added to Firebase Console.

## Step 1: Get Your SHA-1 Fingerprints

### For Debug Build (Development)
Run this command in your project root:
```cmd
gradlew signingReport
```

Or on Windows PowerShell:
```powershell
.\gradlew signingReport
```

Look for the output under "Variant: debug" and copy the **SHA-1** value.

### For Release Build (Production)
Since you have a keystore file, run:
```cmd
keytool -list -v -keystore app\bravebrain-release-key.keystore -alias bravebrain_key
```

When prompted, enter your keystore password: `bravebrain2025`

Copy the **SHA-1** certificate fingerprint from the output.

## Step 2: Add SHA-1 to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **bravebrain-59cdc**
3. Click the gear icon ⚙️ → **Project Settings**
4. Scroll down to **Your apps** section
5. Find your Android app (com.bravebrain)
6. Click **Add fingerprint**
7. Paste your **debug SHA-1** and click Save
8. Click **Add fingerprint** again
9. Paste your **release SHA-1** and click Save

## Step 3: Download Updated google-services.json

1. Still in Firebase Console → Project Settings
2. Scroll to your Android app
3. Click **Download google-services.json**
4. Replace the file at `app/google-services.json` with the new one

## Step 4: Verify the Fix

After replacing the file, check that `google-services.json` now has OAuth clients:
- Open `app/google-services.json`
- Look for the `oauth_client` array
- It should now contain entries with `client_id` and `client_type`

## Step 5: Rebuild and Test

```cmd
gradlew clean
gradlew assembleDebug
```

Then install and test Google Sign-In on your device.

## Common Issues

### "SHA-1 already exists"
If you see this error, the fingerprint is already added. Make sure you're downloading the latest `google-services.json`.

### "Web client ID not found"
Make sure you have enabled Google Sign-In in Firebase Console:
1. Go to **Authentication** → **Sign-in method**
2. Enable **Google** provider
3. Save changes

### Still not working?
The Web Client ID in `FirebaseAuthManager.kt` might need updating. After downloading the new `google-services.json`, look for the `client_id` with `client_type: 3` (web client) and update line 18 in `FirebaseAuthManager.kt`.
