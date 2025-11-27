package com.bravebrain

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.widget.ImageButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamificationActivity : AppCompatActivity() {

    private val firestoreService by lazy { FirestoreService(this) }
    private val authManager by lazy { FirebaseAuthManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        setupUI()
        loadGamificationData()
        
        // Sync data with Firestore in background
        syncWithFirestore()
    }

    private fun setupUI() {
        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            finish()
        }
    }

    private fun loadGamificationData() {
        val prefs = getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        
        // Initialize with demo data if first time
        if (!prefs.contains("initialized")) {
            prefs.edit().apply {
                putBoolean("initialized", true)
                putInt("user_level", 1)
                putInt("user_xp", 0)
                putInt("daily_streak", 0)
                putInt("challenge_streak", 0)
                putInt("productivity_streak", 0)
                putInt("total_badges", 0)
                apply()
            }
        }
        
        updateUI(prefs)
    }
    
    private fun updateUI(prefs: android.content.SharedPreferences) {
        // Load and display level
        val currentLevel = prefs.getInt("user_level", 1)
        val currentXP = prefs.getInt("user_xp", 0)
        val xpForNextLevel = calculateXPForNextLevel(currentLevel)
        
        findViewById<TextView>(R.id.levelText)?.text = "Level $currentLevel"
        findViewById<TextView>(R.id.xpText)?.text = "$currentXP / $xpForNextLevel XP"
        
        // Update XP progress bar if available
        findViewById<ProgressBar>(R.id.levelProgressBar)?.apply {
            max = xpForNextLevel
            progress = currentXP
        }
        
        // Load streaks
        val dailyStreak = prefs.getInt("daily_streak", 0)
        val challengeStreak = prefs.getInt("challenge_streak", 0)
        val productivityStreak = prefs.getInt("productivity_streak", 0)
        
        findViewById<TextView>(R.id.dailyStreakValue)?.text = "$dailyStreak days"
        findViewById<TextView>(R.id.challengeStreakValue)?.text = "$challengeStreak in a row"
        findViewById<TextView>(R.id.productivityStreakValue)?.text = "$productivityStreak days"
        
        // Load badges
        val totalBadges = prefs.getInt("total_badges", 0)
        val earnedBadges = prefs.getStringSet("earned_badges", emptySet()) ?: emptySet()
        findViewById<TextView>(R.id.badgesEarnedText)?.text = "${earnedBadges.size} badges earned"
        
        // Display motivational message
        displayMotivationalMessage(dailyStreak, currentLevel)
    }

    private fun calculateXPForNextLevel(level: Int): Int {
        return level * 100 // Simple formula: 100 XP per level
    }

    private fun displayMotivationalMessage(streak: Int, level: Int) {
        val message = when {
            streak >= 7 -> "🔥 Amazing! You're on fire with a $streak day streak!"
            streak >= 3 -> "💪 Great job! Keep that $streak day streak going!"
            level >= 10 -> "⭐ You're becoming a digital wellness expert!"
            level >= 5 -> "🌟 You're making great progress! Keep it up!"
            else -> "🌟 Every journey starts with a single step. You've got this!"
        }
        
        findViewById<TextView>(R.id.motivationalMessage)?.text = message
    }
    
    /**
     * Sync gamification data with Firestore
     */
    private fun syncWithFirestore() {
        if (!authManager.isSignedIn()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First try to load from Firestore (cloud data might be more recent)
                val result = firestoreService.getGamificationData()
                result.getOrNull()?.let { cloudData ->
                    val prefs = getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
                    val localLevel = prefs.getInt("user_level", 1)
                    val localXP = prefs.getInt("user_xp", 0)
                    
                    // If cloud data is better, update local
                    if (cloudData.level > localLevel || 
                        (cloudData.level == localLevel && cloudData.points > localXP)) {
                        prefs.edit().apply {
                            putInt("user_level", cloudData.level)
                            putInt("user_xp", cloudData.points)
                            putStringSet("earned_badges", cloudData.badges.toSet())
                            putInt("total_badges", cloudData.badges.size)
                            
                            // Restore streak data from challenges map
                            val challenges = cloudData.challenges
                            putInt("daily_streak", (challenges["dailyStreak"] as? Number)?.toInt() ?: 0)
                            putInt("challenge_streak", (challenges["challengeStreak"] as? Number)?.toInt() ?: 0)
                            putInt("productivity_streak", (challenges["productivityStreak"] as? Number)?.toInt() ?: 0)
                            
                            apply()
                        }
                        
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            updateUI(prefs)
                        }
                    }
                }
                
                // Now sync local data to cloud
                DataSyncManager(this@GamificationActivity).syncAllData()
                
            } catch (e: Exception) {
                android.util.Log.e("GamificationActivity", "Error syncing with Firestore: ${e.message}")
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload data when returning to this activity
        loadGamificationData()
    }
}
