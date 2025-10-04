package com.example.testing

import android.content.Context
import android.widget.Toast

object GamificationUtils {
    
    /**
     * Award XP to the user and handle level ups
     */
    fun awardXP(context: Context, amount: Int, reason: String = "") {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        val currentLevel = prefs.getInt("user_level", 1)
        val currentXP = prefs.getInt("user_xp", 0)
        
        val newXP = currentXP + amount
        val xpForNextLevel = currentLevel * 100
        
        if (newXP >= xpForNextLevel) {
            // Level up!
            val newLevel = currentLevel + 1
            val remainingXP = newXP - xpForNextLevel
            
            prefs.edit().apply {
                putInt("user_level", newLevel)
                putInt("user_xp", remainingXP)
                apply()
            }
            
            Toast.makeText(context, "🎉 Level Up! You're now Level $newLevel!", Toast.LENGTH_LONG).show()
        } else {
            prefs.edit().apply {
                putInt("user_xp", newXP)
                apply()
            }
            
            if (reason.isNotEmpty()) {
                Toast.makeText(context, "+$amount XP: $reason", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Increment a streak counter
     */
    fun incrementStreak(context: Context, streakType: String) {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        val currentStreak = prefs.getInt(streakType, 0)
        
        prefs.edit().apply {
            putInt(streakType, currentStreak + 1)
            apply()
        }
        
        // Award XP for maintaining streaks
        if ((currentStreak + 1) % 7 == 0) {
            awardXP(context, 100, "7-day streak milestone!")
        }
    }
    
    /**
     * Reset a streak counter
     */
    fun resetStreak(context: Context, streakType: String) {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(streakType, 0)
            apply()
        }
    }
    
    /**
     * Award a badge to the user
     */
    fun awardBadge(context: Context, badgeName: String) {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        val badges = prefs.getStringSet("earned_badges", mutableSetOf()) ?: mutableSetOf()
        
        if (!badges.contains(badgeName)) {
            badges.add(badgeName)
            val totalBadges = badges.size
            
            prefs.edit().apply {
                putStringSet("earned_badges", badges)
                putInt("total_badges", totalBadges)
                apply()
            }
            
            Toast.makeText(context, "🏆 Badge Unlocked: $badgeName!", Toast.LENGTH_LONG).show()
            awardXP(context, 50, "Badge earned!")
        }
    }
    
    /**
     * Check and award badges based on achievements
     */
    fun checkAndAwardBadges(context: Context) {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        val dailyStreak = prefs.getInt("daily_streak", 0)
        val challengeStreak = prefs.getInt("challenge_streak", 0)
        val level = prefs.getInt("user_level", 1)
        
        // Check for various badge conditions
        if (dailyStreak >= 7) {
            awardBadge(context, "Focus Master")
        }
        
        if (challengeStreak >= 50) {
            awardBadge(context, "Challenge Champion")
        }
        
        if (level >= 10) {
            awardBadge(context, "Productivity Pro")
        }
    }
}
