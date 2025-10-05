package com.bravebrain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Helper class that demonstrates common Firebase authentication and storage operations
 */
class FirebaseAuthStorageHelper(private val context: Context) {
    private val authManager = FirebaseAuthManager(context)
    private val storageManager = FirebaseStorageManager(context)
    private val firebaseManager = FirebaseManager.getInstance()
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return authManager.signInWithEmail(email, password)
    }
    
    /**
     * Create a new user with email and password
     */
    suspend fun createUserWithEmail(email: String, password: String): Result<FirebaseUser> {
        return authManager.createUserWithEmail(email, password)
    }
    
    /**
     * Sign in anonymously
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return authManager.signInAnonymously()
    }
    
    /**
     * Sign in with Google using ID token
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return authManager.signInWithGoogle(idToken)
    }
    
    /**
     * Get Google Sign-In intent for the activity
     */
    fun getGoogleSignInIntent(): Intent = authManager.getSignInIntent()
    
    /**
     * Process the result from Google Sign-In activity
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            
            if (idToken != null) {
                authManager.signInWithGoogle(idToken)
            } else {
                Result.failure(Exception("No ID token found in Google Sign-In result"))
            }
        } catch (e: ApiException) {
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        authManager.signOut()
    }
    
    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean {
        return authManager.isSignedIn()
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return authManager.getCurrentUserId()
    }
    
    /**
     * Upload a profile picture to Firebase Storage
     */
    suspend fun uploadProfilePicture(imageUri: Uri): Result<String> {
        return storageManager.uploadFile(
            fileUri = imageUri,
            folderName = "profile_pictures",
            fileName = "${getCurrentUserId()}_profile.jpg"
        )
    }
    
    /**
     * Upload a file to user's storage
     */
    suspend fun uploadFile(file: File, folder: String, fileName: String): Result<String> {
        return storageManager.uploadLocalFile(
            file = file,
            folderName = folder,
            fileName = fileName
        )
    }
    
    /**
     * Download a file from Firebase Storage
     */
    suspend fun downloadFile(remotePath: String, destinationFile: File): Result<Unit> {
        return storageManager.downloadFile(remotePath, destinationFile)
    }
    
    /**
     * Delete a file from Firebase Storage
     */
    suspend fun deleteFile(remotePath: String): Result<Unit> {
        return storageManager.deleteFile(remotePath)
    }
    
    /**
     * Get download URL for a file
     */
    suspend fun getFileDownloadUrl(remotePath: String): Result<String> {
        return storageManager.getFileDownloadUrl(remotePath)
    }
    
    /**
     * Example function showing how to use Firebase for app-specific data
     */
    suspend fun saveUserData(data: Map<String, Any>): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            
            // Example: Save user data to Firestore using the existing FirestoreService
            val firestoreService = FirestoreService(context)
            
            // You can customize this based on your app's specific needs
            // For example, save user preferences, settings, or other data
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthStorageHelper", "Error saving user data: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Example function showing how to sync app data with Firebase
     */
    suspend fun syncAppData(): Result<Unit> {
        return try {
            if (!isSignedIn()) {
                // Sign in anonymously if not signed in
                signInAnonymously()
            }
            
            // Example: Sync app usage data, settings, or preferences
            // This would typically involve uploading local data to Firestore
            val firestoreService = FirestoreService(context)
            
            // Implementation would depend on what data you want to sync
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthStorageHelper", "Error syncing app data: ${e.message}", e)
            Result.failure(e)
        }
    }
}
