package com.bravebrain

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GamificationUtils {
    
    /**
     * Check and update daily streak on app launch
     * Should be called once per day when app opens
     */
    fun checkDailyStreak(context: Context) {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        val lastCheckDate = prefs.getString("last_streak_check_date", "") ?: ""
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val today = dateFormat.format(java.util.Date())
        
        if (lastCheckDate != today) {
            // Check if user met their goals yesterday
            val analyticsPrefs = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
            val yesterday = java.util.Calendar.getInstance().apply { 
                add(java.util.Calendar.DAY_OF_YEAR, -1) 
            }
            val yesterdayStr = dateFormat.format(yesterday.time)
            
            // Get yesterday's productivity score
            val yesterdayStats = analyticsPrefs.getString("daily_stats-$yesterdayStr", null)
            val productivityScore = if (yesterdayStats != null) {
                val parts = yesterdayStats.split(",")
                if (parts.size >= 7) parts[6].toIntOrNull() ?: 0 else 0
            } else {
                // Check if user was under time limits for blocked apps
                val blockedPrefs = context.getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
                val blockedAttempts = analyticsPrefs.getInt("blocked_attempts_$yesterdayStr", 0)
                if (blockedAttempts == 0) 70 else 40 // Simple heuristic
            }
            
            if (productivityScore >= 60) {
                // User met their goal - increment daily streak
                incrementStreak(context, "daily_streak")
                awardXP(context, 15, "Daily goal achieved!")
            } else if (lastCheckDate.isNotEmpty()) {
                // User didn't meet goal - reset daily streak
                resetStreak(context, "daily_streak")
            }
            
            // Update productivity streak based on score
            if (productivityScore >= 70) {
                incrementStreak(context, "productivity_streak")
            } else if (lastCheckDate.isNotEmpty()) {
                resetStreak(context, "productivity_streak")
            }
            
            // Mark today as checked
            prefs.edit().putString("last_streak_check_date", today).apply()
        }
    }
    
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
            
            // Sync to Firestore
            syncGamificationData(context)
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
            
            // Sync to Firestore
            syncGamificationData(context)
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
        
        // Sync to Firestore
        syncGamificationData(context)
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
        
        // Sync to Firestore
        syncGamificationData(context)
    }
    
    /**
     * Award a badge to the user
     */
    fun awardBadge(context: Context, badgeName: String) {
        val prefs = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
        val badges = prefs.getStringSet("earned_badges", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
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
        val productivityStreak = prefs.getInt("productivity_streak", 0)
        val level = prefs.getInt("user_level", 1)
        val xp = prefs.getInt("user_xp", 0)
        
        // Daily streak badges
        if (dailyStreak >= 3) {
            awardBadge(context, "Getting Started")
        }
        if (dailyStreak >= 7) {
            awardBadge(context, "Focus Master")
        }
        if (dailyStreak >= 14) {
            awardBadge(context, "Two Week Warrior")
        }
        if (dailyStreak >= 30) {
            awardBadge(context, "Monthly Champion")
        }
        
        // Challenge streak badges
        if (challengeStreak >= 5) {
            awardBadge(context, "Challenge Starter")
        }
        if (challengeStreak >= 10) {
            awardBadge(context, "Challenge Enthusiast")
        }
        if (challengeStreak >= 25) {
            awardBadge(context, "Challenge Expert")
        }
        if (challengeStreak >= 50) {
            awardBadge(context, "Challenge Champion")
        }
        if (challengeStreak >= 100) {
            awardBadge(context, "Challenge Legend")
        }
        
        // Productivity streak badges
        if (productivityStreak >= 7) {
            awardBadge(context, "Productivity Rookie")
        }
        if (productivityStreak >= 14) {
            awardBadge(context, "Productivity Pro")
        }
        if (productivityStreak >= 30) {
            awardBadge(context, "Productivity Master")
        }
        
        // Level-based badges
        if (level >= 3) {
            awardBadge(context, "Beginner")
        }
        if (level >= 5) {
            awardBadge(context, "Rising Star")
        }
        if (level >= 10) {
            awardBadge(context, "Experienced")
        }
        if (level >= 20) {
            awardBadge(context, "Expert")
        }
        if (level >= 50) {
            awardBadge(context, "Master")
        }
        
        // XP milestone badges
        if (xp + (level * 100) >= 500) {
            awardBadge(context, "500 XP Club")
        }
        if (xp + (level * 100) >= 1000) {
            awardBadge(context, "1000 XP Club")
        }
        if (xp + (level * 100) >= 5000) {
            awardBadge(context, "XP Legend")
        }
    }
    
    /**
     * Sync gamification data to Firestore in background
     */
    private fun syncGamificationData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DataSyncManager(context).syncAllData()
            } catch (e: Exception) {
                android.util.Log.e("GamificationUtils", "Error syncing gamification data: ${e.message}")
            }
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
