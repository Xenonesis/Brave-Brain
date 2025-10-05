package com.bravebrain

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    
    init {
        // Initialize Google Sign In options
        // Using the default web client ID from google-services.json
        // Note: For this to work properly, you need to add your SHA-1 fingerprint to Firebase Console
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Automatically reads from google-services.json
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            // Log the exception for debugging
            android.util.Log.e("FirebaseAuthManager", "Sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun createUserWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("User creation failed"))
        } catch (e: Exception) {
            // Log the exception for debugging
            android.util.Log.e("FirebaseAuthManager", "Create user error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("Google sign in failed"))
        } catch (e: Exception) {
            // Log the exception for debugging
            android.util.Log.e("FirebaseAuthManager", "Google sign in error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
    
    fun isSignedIn(): Boolean = auth.currentUser != null
    
    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient
    
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent
}
