package com.bravebrain

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        
        // Check for new badges
        GamificationUtils.checkAndAwardBadges(this)
        
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
        
        // Update level badge number
        findViewById<TextView>(R.id.levelBadge)?.text = currentLevel.toString()
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
        
        // Format streak values nicely
        findViewById<TextView>(R.id.dailyStreakValue)?.text = dailyStreak.toString()
        findViewById<TextView>(R.id.challengeStreakValue)?.text = challengeStreak.toString()
        findViewById<TextView>(R.id.productivityStreakValue)?.text = productivityStreak.toString()
        
        // Load badges
        val earnedBadges = prefs.getStringSet("earned_badges", emptySet()) ?: emptySet()
        findViewById<TextView>(R.id.badgesEarnedText)?.text = "${earnedBadges.size} earned"
        
        // Display motivational message based on progress
        displayMotivationalMessage(dailyStreak, challengeStreak, currentLevel, earnedBadges.size)
        
        // Calculate and display total XP earned
        val totalXP = (currentLevel - 1) * 100 + currentXP
        android.util.Log.d("GamificationActivity", "Stats - Level: $currentLevel, XP: $currentXP, Total XP: $totalXP")
    }

    private fun calculateXPForNextLevel(level: Int): Int {
        return level * 100 // Simple formula: 100 XP per level
    }

    private fun displayMotivationalMessage(dailyStreak: Int, challengeStreak: Int, level: Int, badgeCount: Int) {
        val message = when {
            dailyStreak >= 30 -> "🏆 Incredible! A $dailyStreak day streak! You're a true champion!"
            dailyStreak >= 14 -> "🔥 Unstoppable! $dailyStreak day streak! Keep crushing it!"
            dailyStreak >= 7 -> "🔥 Amazing! You're on fire with a $dailyStreak day streak!"
            challengeStreak >= 10 -> "🧠 Brain master! $challengeStreak challenges in a row!"
            dailyStreak >= 3 -> "💪 Great job! Keep that $dailyStreak day streak going!"
            badgeCount >= 15 -> "🎖️ Badge collector extraordinaire! $badgeCount badges earned!"
            badgeCount >= 10 -> "🏅 Impressive badge collection! $badgeCount badges and counting!"
            level >= 25 -> "👑 You've mastered digital wellness! Level $level!"
            level >= 15 -> "🌟 Digital wellness expert at Level $level!"
            level >= 10 -> "⭐ You're becoming a digital wellness pro!"
            level >= 5 -> "🌟 You're making great progress! Keep it up!"
            badgeCount >= 5 -> "🎯 Nice work! You've earned $badgeCount badges!"
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
