package com.bravebrain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupButton: MaterialButton
    private lateinit var googleSignInButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    
    private lateinit var authManager: FirebaseAuthManager
    
    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        showLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                
                val authResult = withContext(Dispatchers.IO) {
                    authManager.signInWithGoogle(account.idToken!!)
                }
                
                showLoading(false)
                
                authResult.fold(
                    onSuccess = { user ->
                        FeedbackManager.showLoginSuccess(this@LoginActivity)
                        saveLoginState()
                        navigateToNextScreen()
                    },
                    onFailure = { error ->
                        FeedbackManager.showLoginError(this@LoginActivity, "Google sign in failed: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                FeedbackManager.showLoginError(this@LoginActivity, "Google sign in failed: ${e.message}")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        super.onCreate(savedInstanceState)
        
        authManager = FirebaseAuthManager(this)

        if (isUserLoggedIn()) {
            navigateToNextScreen()
            return
        }
        
        setContentView(R.layout.activity_login)
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signupButton = findViewById(R.id.signupButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        progressIndicator = findViewById(R.id.progressIndicator)
    }
    
    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            
            if (validateInput(email, password)) {
                loginWithEmail(email, password)
            }
        }
        
        signupButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            
            if (validateInput(email, password)) {
                signupWithEmail(email, password)
            }
        }
        
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email format"
            return false
        }
        
        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return false
        }
        
        return true
    }
    
    private fun loginWithEmail(email: String, password: String) {
        showLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                authManager.signInWithEmail(email, password)
            }
            
            showLoading(false)
            
            result.fold(
                onSuccess = { user ->
                    FeedbackManager.showLoginSuccess(this@LoginActivity)
                    saveLoginState()
                    navigateToNextScreen()
                },
                onFailure = { error ->
                    FeedbackManager.showLoginError(this@LoginActivity, "Login failed: ${error.message}")
                }
            )
        }
    }
    
    private fun signupWithEmail(email: String, password: String) {
        showLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                authManager.createUserWithEmail(email, password)
            }
            
            showLoading(false)
            
            result.fold(
                onSuccess = { user ->
                    FeedbackManager.showSignupSuccess(this@LoginActivity)
                    saveLoginState()
                    navigateToNextScreen()
                },
                onFailure = { error ->
                    FeedbackManager.showLoginError(this@LoginActivity, "Signup failed: ${error.message}")
                }
            )
        }
    }
    
    private fun signInWithGoogle() {
        val signInIntent = authManager.getSignInIntent()
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun showLoading(show: Boolean) {
        progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        loginButton.isEnabled = !show
        signupButton.isEnabled = !show
        googleSignInButton.isEnabled = !show
        emailInput.isEnabled = !show
        passwordInput.isEnabled = !show
    }
    
    private fun saveLoginState() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        
        val syncManager = DataSyncManager(this)
        
        // Initialize Firestore collections first, then sync data
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, initialize all Firestore collections to ensure they exist
                val initialized = syncManager.initializeCollections()
                if (initialized) {
                    android.util.Log.d("LoginActivity", "Firestore collections initialized successfully")
                } else {
                    android.util.Log.w("LoginActivity", "Firestore collections initialization returned false")
                }
                
                // Sync all local data to Firestore after login
                syncManager.syncAllData()
                
                // Also try to restore data from cloud if this is a returning user
                syncManager.restoreFromCloud()
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Error during post-login sync: ${e.message}")
            }
        }
    }
    
    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        return isLoggedIn && authManager.isSignedIn()
    }
    
    private fun navigateToNextScreen() {
        val onboardingPrefs = getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        val onboardingComplete = onboardingPrefs.getBoolean("onboarding_complete", false)
        
        val intent = if (onboardingComplete) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, OnboardingActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
