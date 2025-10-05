package com.bravebrain

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseTestActivity : AppCompatActivity() {
    private val firebaseHelper = FirebaseAuthStorageHelper(this)
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var statusTextView: TextView
    
    companion object {
        private const val RC_GOOGLE_SIGN_IN = 123
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main) // Reusing main layout for simplicity
        
        // Replace main content with test UI
        val container = findViewById<android.widget.LinearLayout>(R.id.buttonContainer)
        container?.removeAllViews()
        
        // Create status text view
        statusTextView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
        container?.addView(statusTextView)
        
        // Create test buttons
        val signInAnonButton = Button(this).apply {
            text = "Sign In Anonymously"
            setOnClickListener { testSignInAnonymously() }
        }
        container?.addView(signInAnonButton)
        
        val signInGoogleButton = Button(this).apply {
            text = "Sign In with Google"
            setOnClickListener { testSignInWithGoogle() }
        }
        container?.addView(signInGoogleButton)
        
        val signOutButton = Button(this).apply {
            text = "Sign Out"
            setOnClickListener { testSignOut() }
        }
        container?.addView(signOutButton)
        
        val checkAuthButton = Button(this).apply {
            text = "Check Auth Status"
            setOnClickListener { testCheckAuthStatus() }
        }
        container?.addView(checkAuthButton)
        
        val backToMainButton = Button(this).apply {
            text = "Back to Main"
            setOnClickListener { 
                startActivity(Intent(this@FirebaseTestActivity, MainActivity::class.java))
                finish()
            }
        }
        container?.addView(backToMainButton)
        
        // Initial status check
        updateStatus()
    }
    
    private fun updateStatus() {
        val authStatus = if (firebaseHelper.isSignedIn()) {
            "Signed in as: ${firebaseHelper.getCurrentUserId()}"
        } else {
            "Not signed in"
        }
        statusTextView.text = "Firebase Status:\n$authStatus\n\nClick buttons to test functionality."
    }
    
    private fun testSignInAnonymously() {
        scope.launch {
            try {
                statusTextView.text = "Signing in anonymously..."
                val result = firebaseHelper.signInAnonymously()
                
                if (result.isSuccess) {
                    Toast.makeText(this@FirebaseTestActivity, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    updateStatus()
                } else {
                    Toast.makeText(this@FirebaseTestActivity, "Sign in failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    Log.e("FirebaseTestActivity", "Sign in failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("FirebaseTestActivity", "Error signing in", e)
                Toast.makeText(this@FirebaseTestActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun testSignInWithGoogle() {
        val signInIntent = firebaseHelper.getGoogleSignInIntent()
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            scope.launch {
                try {
                    statusTextView.text = "Processing Google Sign-In..."
                    val result = firebaseHelper.handleGoogleSignInResult(data)
                    
                    if (result.isSuccess) {
                        Toast.makeText(this@FirebaseTestActivity, "Google Sign-In successful!", Toast.LENGTH_SHORT).show()
                        updateStatus()
                    } else {
                        Toast.makeText(this@FirebaseTestActivity, "Google Sign-In failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        Log.e("FirebaseTestActivity", "Google Sign-In failed", result.exceptionOrNull())
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseTestActivity", "Error processing Google Sign-In", e)
                    Toast.makeText(this@FirebaseTestActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun testSignOut() {
        firebaseHelper.signOut()
        Toast.makeText(this@FirebaseTestActivity, "Signed out", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun testCheckAuthStatus() {
        updateStatus()
        Toast.makeText(this@FirebaseTestActivity, "Auth status updated", Toast.LENGTH_SHORT).show()
    }
}
