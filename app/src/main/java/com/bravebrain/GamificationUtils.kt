package com.bravebrain

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            
            FeedbackManager.showLevelUp(context, newLevel)
            
            // Trigger level up notification
            triggerLevelUpNotification(context, newLevel)
        } else {
            prefs.edit().apply {
                putInt("user_xp", newXP)
                apply()
            }
            
            if (reason.isNotEmpty()) {
                FeedbackManager.showXPEarned(context, amount)
            }
            
            // Trigger XP notification
            triggerXPNotification(context, amount, reason)
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
            awardXP(context, 10, "7-day streak milestone!")
        }
        
        // Trigger streak notification if milestone reached
        if ((currentStreak + 1) % 7 == 0 || (currentStreak + 1) == 1) {
            triggerStreakNotification(context, streakType, currentStreak + 1)
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
            
            FeedbackManager.showBadgeUnlocked(context, badgeName)
            awardXP(context, 50, "Badge earned!")
            
            // Trigger badge notification
            triggerBadgeNotification(context, badgeName)
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
    
    /**
     * Trigger XP notification through the notification service
     */
    private fun triggerXPNotification(context: Context, amount: Int, reason: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent("TRIGGER_GAMIFICATION_NOTIFICATION").apply {
                setPackage(context.packageName)
                putExtra("achievement_type", "xp")
                putExtra("xp_amount", amount)
                putExtra("reason", reason)
            }
            context.sendBroadcast(intent)
        }
    }
    
    /**
     * Trigger level up notification through the notification service
     */
    private fun triggerLevelUpNotification(context: Context, newLevel: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent("TRIGGER_GAMIFICATION_NOTIFICATION").apply {
                setPackage(context.packageName)
                putExtra("achievement_type", "level_up")
                putExtra("new_level", newLevel)
            }
            context.sendBroadcast(intent)
        }
    }
    
    /**
     * Trigger streak notification through the notification service
     */
    private fun triggerStreakNotification(context: Context, streakType: String, streakCount: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent("TRIGGER_GAMIFICATION_NOTIFICATION").apply {
                setPackage(context.packageName)
                putExtra("achievement_type", "streak")
                putExtra("streak_type", streakType)
                putExtra("streak_count", streakCount)
            }
            context.sendBroadcast(intent)
        }
    }
    
    /**
     * Trigger badge notification through the notification service
     */
    private fun triggerBadgeNotification(context: Context, badgeName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent("TRIGGER_GAMIFICATION_NOTIFICATION").apply {
                setPackage(context.packageName)
                putExtra("achievement_type", "badge")
                putExtra("badge_name", badgeName)
            }
            context.sendBroadcast(intent)
        }
    }
}
