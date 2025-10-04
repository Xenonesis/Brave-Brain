package com.example.testing

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton

class GamificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        setupUI()
        loadGamificationData()
    }

    private fun setupUI() {
        findViewById<MaterialButton>(R.id.backButton)?.setOnClickListener {
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
        
        // Load and display level
        val currentLevel = prefs.getInt("user_level", 1)
        val currentXP = prefs.getInt("user_xp", 0)
        val xpForNextLevel = calculateXPForNextLevel(currentLevel)
        
        findViewById<TextView>(R.id.levelText)?.text = "Level $currentLevel"
        findViewById<TextView>(R.id.xpText)?.text = "$currentXP / $xpForNextLevel XP"
        
        // Load streaks
        val dailyStreak = prefs.getInt("daily_streak", 0)
        val challengeStreak = prefs.getInt("challenge_streak", 0)
        val productivityStreak = prefs.getInt("productivity_streak", 0)
        
        findViewById<TextView>(R.id.dailyStreakValue)?.text = "$dailyStreak days"
        findViewById<TextView>(R.id.challengeStreakValue)?.text = "$challengeStreak in a row"
        findViewById<TextView>(R.id.productivityStreakValue)?.text = "$productivityStreak days"
        
        // Load badges
        val totalBadges = prefs.getInt("total_badges", 0)
        findViewById<TextView>(R.id.badgesEarnedText)?.text = "$totalBadges badges earned"
        
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
            else -> "🌟 Every journey starts with a single step. You've got this!"
        }
        
        findViewById<TextView>(R.id.motivationalMessage)?.text = message
    }
}
