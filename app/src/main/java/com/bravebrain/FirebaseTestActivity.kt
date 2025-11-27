package com.bravebrain

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseTestActivity : AppCompatActivity() {
    private val firebaseHelper = FirebaseAuthStorageHelper(this)
    private val firebaseManager = FirebaseManager.getInstance()
    private val firestoreService = FirestoreService(this)
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var statusTextView: TextView
    
    companion object {
        private const val RC_GOOGLE_SIGN_IN = 123
        private const val TAG = "FirebaseTestActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create custom layout
        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(container)
        setContentView(scrollView)
        
        // Create status text view
        statusTextView = TextView(this).apply {
            textSize = 14f
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFFEEEEEE.toInt())
        }
        container.addView(statusTextView)
        
        // Create test buttons
        addButton(container, "🔍 Run Database Connectivity Tests") { runDatabaseTests() }
        addButton(container, "✓ Check Auth Status") { testCheckAuthStatus() }
        addButton(container, "🔑 Sign In Anonymously") { testSignInAnonymously() }
        addButton(container, "🌐 Sign In with Google") { testSignInWithGoogle() }
        addButton(container, "💾 Test Firestore Write") { testFirestoreWrite() }
        addButton(container, "📖 Test Firestore Read") { testFirestoreRead() }
        addButton(container, "👤 Test User Profile") { testUserProfile() }
        addButton(container, "📊 Test Analytics") { testAnalytics() }
        addButton(container, "🎮 Test Gamification") { testGamification() }
        addButton(container, "🚪 Sign Out") { testSignOut() }
        addButton(container, "⬅️ Back to Main") { 
            startActivity(Intent(this@FirebaseTestActivity, MainActivity::class.java))
            finish()
        }
        
        // Initial status check
        updateStatus()
    }
    
    private fun addButton(container: LinearLayout, text: String, onClick: () -> Unit) {
        Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }.also { container.addView(it) }
    }
    
    private fun updateStatus() {
        try {
            val app = FirebaseApp.getInstance()
            val options = app.options
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            val statusBuilder = StringBuilder()
            statusBuilder.appendLine("=== FIREBASE DATABASE STATUS ===\n")
            statusBuilder.appendLine("📦 Firebase Initialized: ✓")
            statusBuilder.appendLine("🆔 Project ID: ${options.projectId}")
            statusBuilder.appendLine("🔗 Database URL: ${options.databaseUrl ?: "N/A"}\n")
            
            if (currentUser != null) {
                statusBuilder.appendLine("✓ AUTHENTICATED")
                statusBuilder.appendLine("👤 User ID: ${currentUser.uid}")
                statusBuilder.appendLine("📧 Email: ${currentUser.email ?: "Anonymous"}")
                statusBuilder.appendLine("👤 Name: ${currentUser.displayName ?: "N/A"}")
            } else {
                statusBuilder.appendLine("✗ NOT AUTHENTICATED")
                statusBuilder.appendLine("⚠️ Sign in to test database operations")
            }
            
            statusTextView.text = statusBuilder.toString()
        } catch (e: Exception) {
            statusTextView.text = "❌ Error checking status:\n${e.message}"
            Log.e(TAG, "Error updating status", e)
        }
    }
    
    private fun runDatabaseTests() {
        scope.launch {
            try {
                statusTextView.text = "🔄 Running comprehensive database tests...\n\nThis may take a few moments..."
                
                val results = StringBuilder()
                results.appendLine("=" .repeat(50))
                results.appendLine("DATABASE CONNECTIVITY TEST RESULTS")
                results.appendLine("=" .repeat(50))
                results.appendLine()
                
                // Test 1: Firebase Initialization
                results.appendLine("Test 1: Firebase Initialization")
                try {
                    val app = FirebaseApp.getInstance()
                    val options = app.options
                    results.appendLine("✓ PASS")
                    results.appendLine("  Project ID: ${options.projectId}")
                    results.appendLine("  App ID: ${options.applicationId}")
                } catch (e: Exception) {
                    results.appendLine("✗ FAIL: ${e.message}")
                }
                results.appendLine()
                
                // Test 2: Firestore Connection
                results.appendLine("Test 2: Firestore Connection")
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val settings = firestore.firestoreSettings
                    results.appendLine("✓ PASS")
                    results.appendLine("  Persistence: ${settings.isPersistenceEnabled}")
                    results.appendLine("  Host: ${settings.host}")
                } catch (e: Exception) {
                    results.appendLine("✗ FAIL: ${e.message}")
                }
                results.appendLine()
                
                // Test 3: Authentication Status
                results.appendLine("Test 3: Authentication Status")
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    results.appendLine("✓ PASS - User authenticated")
                    results.appendLine("  UID: ${currentUser.uid}")
                    results.appendLine("  Email: ${currentUser.email ?: "Anonymous"}")
                } else {
                    results.appendLine("⚠️ WARNING - Not authenticated")
                    results.appendLine("  Sign in to test database operations")
                }
                results.appendLine()
                
                // Only run write/read tests if authenticated
                if (currentUser != null) {
                    // Test 4: Firestore Write
                    results.appendLine("Test 4: Firestore Write Operation")
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val testData = hashMapOf(
                            "testField" to "test_${System.currentTimeMillis()}",
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        firestore.collection("_test_collection")
                            .document(currentUser.uid)
                            .set(testData)
                            .await()
                        results.appendLine("✓ PASS - Write successful")
                    } catch (e: Exception) {
                        results.appendLine("✗ FAIL: ${e.message}")
                        Log.e(TAG, "Write test failed", e)
                    }
                    results.appendLine()
                    
                    // Test 5: Firestore Read
                    results.appendLine("Test 5: Firestore Read Operation")
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val doc = firestore.collection("_test_collection")
                            .document(currentUser.uid)
                            .get()
                            .await()
                        if (doc.exists()) {
                            results.appendLine("✓ PASS - Read successful")
                            results.appendLine("  Data found: ${doc.data?.size ?: 0} fields")
                        } else {
                            results.appendLine("⚠️ No data found")
                        }
                    } catch (e: Exception) {
                        results.appendLine("✗ FAIL: ${e.message}")
                        Log.e(TAG, "Read test failed", e)
                    }
                    results.appendLine()
                    
                    // Test 6: User Profile Operations
                    results.appendLine("Test 6: User Profile Operations")
                    try {
                        val saveResult = firestoreService.createOrUpdateUserProfile(
                            email = currentUser.email ?: "test@example.com",
                            displayName = currentUser.displayName ?: "Test User"
                        )
                        if (saveResult.isSuccess) {
                            val readResult = firestoreService.getUserProfile()
                            if (readResult.isSuccess) {
                                results.appendLine("✓ PASS - Profile operations successful")
                                results.appendLine("  Profile saved and retrieved")
                            } else {
                                results.appendLine("✗ FAIL: Read failed")
                            }
                        } else {
                            results.appendLine("✗ FAIL: ${saveResult.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        results.appendLine("✗ FAIL: ${e.message}")
                        Log.e(TAG, "Profile test failed", e)
                    }
                    results.appendLine()
                    
                    // Test 7: Analytics Operations
                    results.appendLine("Test 7: Analytics Operations")
                    try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val today = dateFormat.format(Date())
                        val saveResult = firestoreService.saveAnalytics(
                            date = today,
                            totalScreenTimeMs = 3600000L,
                            productivityScore = 75,
                            blockedAttempts = 5,
                            challengesCompleted = 3,
                            challengesFailed = 1,
                            usagePatterns = mapOf("test" to "value")
                        )
                        if (saveResult.isSuccess) {
                            results.appendLine("✓ PASS - Analytics saved successfully")
                        } else {
                            results.appendLine("✗ FAIL: ${saveResult.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        results.appendLine("✗ FAIL: ${e.message}")
                        Log.e(TAG, "Analytics test failed", e)
                    }
                    results.appendLine()
                    
                    // Test 8: Gamification Operations
                    results.appendLine("Test 8: Gamification Operations")
                    try {
                        val saveResult = firestoreService.saveGamificationData(
                            points = 100,
                            level = 5,
                            badges = listOf("tester", "early_adopter"),
                            challenges = mapOf("test" to true)
                        )
                        if (saveResult.isSuccess) {
                            val readResult = firestoreService.getGamificationData()
                            if (readResult.isSuccess) {
                                results.appendLine("✓ PASS - Gamification operations successful")
                                val data = readResult.getOrNull()
                                results.appendLine("  Level: ${data?.level}, Points: ${data?.points}")
                            } else {
                                results.appendLine("✗ FAIL: Read failed")
                            }
                        } else {
                            results.appendLine("✗ FAIL: ${saveResult.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        results.appendLine("✗ FAIL: ${e.message}")
                        Log.e(TAG, "Gamification test failed", e)
                    }
                    results.appendLine()
                }
                
                results.appendLine("=" .repeat(50))
                results.appendLine("TEST SUITE COMPLETED")
                results.appendLine("=" .repeat(50))
                
                statusTextView.text = results.toString()
                Log.i(TAG, results.toString())
                
                Toast.makeText(this@FirebaseTestActivity, "Tests completed! Check results above.", Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                val errorMsg = "❌ Test suite failed:\n${e.message}\n\n${e.stackTraceToString()}"
                statusTextView.text = errorMsg
                Log.e(TAG, "Test suite error", e)
                Toast.makeText(this@FirebaseTestActivity, "Tests failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun testSignInAnonymously() {
        scope.launch {
            try {
                statusTextView.text = "🔄 Signing in anonymously..."
                val result = firebaseHelper.signInAnonymously()
                
                if (result.isSuccess) {
                    Toast.makeText(this@FirebaseTestActivity, "✓ Signed in successfully!", Toast.LENGTH_SHORT).show()
                    updateStatus()
                } else {
                    val error = "✗ Sign in failed: ${result.exceptionOrNull()?.message}"
                    Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                    statusTextView.text = error
                    Log.e(TAG, "Sign in failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error signing in", e)
                val error = "❌ Error: ${e.message}"
                statusTextView.text = error
                Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun testSignInWithGoogle() {
        val signInIntent = firebaseHelper.getGoogleSignInIntent()
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }
    
    private fun testFirestoreWrite() {
        scope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@FirebaseTestActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                statusTextView.text = "🔄 Testing Firestore write..."
                val firestore = FirebaseFirestore.getInstance()
                val testData = hashMapOf(
                    "testField" to "test_${System.currentTimeMillis()}",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "message" to "Manual write test"
                )
                firestore.collection("_test_collection")
                    .document(currentUser.uid)
                    .set(testData)
                    .await()
                
                statusTextView.text = "✓ Firestore write successful!\n\nData written:\n${testData}"
                Toast.makeText(this@FirebaseTestActivity, "✓ Write successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                val error = "✗ Write failed:\n${e.message}"
                statusTextView.text = error
                Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Write test failed", e)
            }
        }
    }
    
    private fun testFirestoreRead() {
        scope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@FirebaseTestActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                statusTextView.text = "🔄 Testing Firestore read..."
                val firestore = FirebaseFirestore.getInstance()
                val doc = firestore.collection("_test_collection")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (doc.exists()) {
                    statusTextView.text = "✓ Firestore read successful!\n\nData retrieved:\n${doc.data}"
                    Toast.makeText(this@FirebaseTestActivity, "✓ Read successful", Toast.LENGTH_SHORT).show()
                } else {
                    statusTextView.text = "⚠️ No data found. Try writing first."
                    Toast.makeText(this@FirebaseTestActivity, "No data found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val error = "✗ Read failed:\n${e.message}"
                statusTextView.text = error
                Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Read test failed", e)
            }
        }
    }
    
    private fun testUserProfile() {
        scope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this@FirebaseTestActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                statusTextView.text = "🔄 Testing user profile operations..."
                
                // Save profile
                val saveResult = firestoreService.createOrUpdateUserProfile(
                    email = currentUser.email ?: "test@example.com",
                    displayName = currentUser.displayName ?: "Test User"
                )
                
                if (saveResult.isFailure) {
                    throw saveResult.exceptionOrNull() ?: Exception("Unknown error")
                }
                
                // Read profile
                val readResult = firestoreService.getUserProfile()
                if (readResult.isSuccess) {
                    val profile = readResult.getOrNull()
                    statusTextView.text = "✓ User profile test successful!\n\nProfile:\nEmail: ${profile?.email}\nName: ${profile?.displayName}\nLast Sync: ${profile?.lastSyncAt}"
                    Toast.makeText(this@FirebaseTestActivity, "✓ Profile operations successful", Toast.LENGTH_SHORT).show()
                } else {
                    throw readResult.exceptionOrNull() ?: Exception("Read failed")
                }
            } catch (e: Exception) {
                val error = "✗ Profile test failed:\n${e.message}"
                statusTextView.text = error
                Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Profile test failed", e)
            }
        }
    }
    
    private fun testAnalytics() {
        scope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    Toast.makeText(this@FirebaseTestActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                statusTextView.text = "🔄 Testing analytics operations..."
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                
                val saveResult = firestoreService.saveAnalytics(
                    date = today,
                    totalScreenTimeMs = 3600000L,
                    productivityScore = 75,
                    blockedAttempts = 5,
                    challengesCompleted = 3,
                    challengesFailed = 1,
                    usagePatterns = mapOf("test" to "value", "manual_test" to true)
                )
                
                if (saveResult.isSuccess) {
                    statusTextView.text = "✓ Analytics test successful!\n\nData saved:\nDate: $today\nScreen Time: 1 hour\nProductivity: 75%"
                    Toast.makeText(this@FirebaseTestActivity, "✓ Analytics saved", Toast.LENGTH_SHORT).show()
                } else {
                    throw saveResult.exceptionOrNull() ?: Exception("Save failed")
                }
            } catch (e: Exception) {
                val error = "✗ Analytics test failed:\n${e.message}"
                statusTextView.text = error
                Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Analytics test failed", e)
            }
        }
    }
    
    private fun testGamification() {
        scope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    Toast.makeText(this@FirebaseTestActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                statusTextView.text = "🔄 Testing gamification operations..."
                
                val saveResult = firestoreService.saveGamificationData(
                    points = 100,
                    level = 5,
                    badges = listOf("tester", "early_adopter", "database_explorer"),
                    challenges = mapOf("test_challenge" to true, "manual_test" to "completed")
                )
                
                if (saveResult.isFailure) {
                    throw saveResult.exceptionOrNull() ?: Exception("Save failed")
                }
                
                val readResult = firestoreService.getGamificationData()
                if (readResult.isSuccess) {
                    val data = readResult.getOrNull()
                    statusTextView.text = "✓ Gamification test successful!\n\nData:\nLevel: ${data?.level}\nPoints: ${data?.points}\nBadges: ${data?.badges?.joinToString(", ")}"
                    Toast.makeText(this@FirebaseTestActivity, "✓ Gamification operations successful", Toast.LENGTH_SHORT).show()
                } else {
                    throw readResult.exceptionOrNull() ?: Exception("Read failed")
                }
            } catch (e: Exception) {
                val error = "✗ Gamification test failed:\n${e.message}"
                statusTextView.text = error
                Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Gamification test failed", e)
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            scope.launch {
                try {
                    statusTextView.text = "🔄 Processing Google Sign-In..."
                    val result = firebaseHelper.handleGoogleSignInResult(data)
                    
                    if (result.isSuccess) {
                        Toast.makeText(this@FirebaseTestActivity, "✓ Google Sign-In successful!", Toast.LENGTH_SHORT).show()
                        updateStatus()
                    } else {
                        val error = "✗ Google Sign-In failed: ${result.exceptionOrNull()?.message}"
                        Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                        statusTextView.text = error
                        Log.e(TAG, "Google Sign-In failed", result.exceptionOrNull())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing Google Sign-In", e)
                    val error = "❌ Error: ${e.message}"
                    statusTextView.text = error
                    Toast.makeText(this@FirebaseTestActivity, error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun testSignOut() {
        firebaseHelper.signOut()
        Toast.makeText(this@FirebaseTestActivity, "✓ Signed out", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun testCheckAuthStatus() {
        updateStatus()
        Toast.makeText(this@FirebaseTestActivity, "✓ Status updated", Toast.LENGTH_SHORT).show()
    }
}
