package com.example.testing

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.math.max

/**
 * Smart Blocking Engine with adaptive strategies and progressive difficulty
 */
class SmartBlockingEngine(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_blocking", Context.MODE_PRIVATE)
    private val analyticsPrefs: SharedPreferences = context.getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
    
    companion object {
        private const val VIOLATION_COUNT_KEY = "violation_count"
        private const val LAST_VIOLATION_KEY = "last_violation"
        private const val COOLING_OFF_KEY = "cooling_off_until"
        private const val CHALLENGE_DIFFICULTY_KEY = "challenge_difficulty"
        private const val ADAPTIVE_LIMITS_KEY = "adaptive_limits"
        private const val CONTEXT_RULES_KEY = "context_rules"
        
        // Blocking strategies
        const val STRATEGY_STANDARD = "standard"
        const val STRATEGY_PROGRESSIVE = "progressive"
        const val STRATEGY_ADAPTIVE = "adaptive"
        const val STRATEGY_STRICT = "strict"
    }
    
    /**
     * Determines if an app should be blocked based on smart rules
     */
    fun shouldBlockApp(packageName: String, currentUsage: Int, timeLimit: Int): BlockingDecision {
        val strategy = getBlockingStrategy(packageName)
        val contextRules = getContextRules()
        
        // Check context-based rules first
        val contextDecision = evaluateContextRules(packageName, contextRules)
        if (contextDecision.shouldBlock) {
            return contextDecision
        }
        
        // Apply strategy-specific logic
        return when (strategy) {
            STRATEGY_PROGRESSIVE -> evaluateProgressiveBlocking(packageName, currentUsage, timeLimit)
            STRATEGY_ADAPTIVE -> evaluateAdaptiveBlocking(packageName, currentUsage, timeLimit)
            STRATEGY_STRICT -> evaluateStrictBlocking(packageName, currentUsage, timeLimit)
            else -> evaluateStandardBlocking(packageName, currentUsage, timeLimit)
        }
    }
    
    /**
     * Standard blocking - block when time limit is exceeded
     */
    private fun evaluateStandardBlocking(packageName: String, currentUsage: Int, timeLimit: Int): BlockingDecision {
        val shouldBlock = currentUsage >= timeLimit
        return BlockingDecision(
            shouldBlock = shouldBlock,
            reason = if (shouldBlock) "Time limit exceeded" else "",
            challengeType = ChallengeType.MATH,
            coolingOffPeriod = 0L,
            allowedOvertime = 0
        )
    }
    
    /**
     * Progressive blocking - gradually increase restrictions with violations
     */
    private fun evaluateProgressiveBlocking(packageName: String, currentUsage: Int, timeLimit: Int): BlockingDecision {
        val violationCount = getViolationCount(packageName)
        val adjustedLimit = calculateProgressiveLimit(timeLimit, violationCount)
        
        if (currentUsage >= adjustedLimit) {
            incrementViolationCount(packageName)
            
            val challengeType = when (violationCount) {
                0, 1 -> ChallengeType.MATH
                2, 3 -> ChallengeType.REFLECTION
                4, 5 -> ChallengeType.PHYSICAL
                else -> ChallengeType.WAITING
            }
            
            val coolingOff = calculateCoolingOffPeriod(violationCount)
            
            return BlockingDecision(
                shouldBlock = true,
                reason = "Progressive limit reached (violation #${violationCount + 1})",
                challengeType = challengeType,
                coolingOffPeriod = coolingOff,
                allowedOvertime = 0
            )
        }
        
        return BlockingDecision(false, "", ChallengeType.NONE, 0L, 0)
    }
    
    /**
     * Adaptive blocking - learns from user patterns and adjusts accordingly
     */
    private fun evaluateAdaptiveBlocking(packageName: String, currentUsage: Int, timeLimit: Int): BlockingDecision {
        val userPattern = analyzeUserPattern(packageName)
        val adaptiveLimit = calculateAdaptiveLimit(timeLimit, userPattern)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // Check if user is in their typical high-usage period
        if (userPattern.peakHours.contains(currentHour) && currentUsage >= (adaptiveLimit * 0.8).toInt()) {
            return BlockingDecision(
                shouldBlock = true,
                reason = "Peak usage time detected - early intervention",
                challengeType = ChallengeType.MINDFULNESS,
                coolingOffPeriod = 5 * 60 * 1000L, // 5 minutes
                allowedOvertime = (timeLimit * 0.1).toInt() // 10% overtime allowed
            )
        }
        
        if (currentUsage >= adaptiveLimit) {
            return BlockingDecision(
                shouldBlock = true,
                reason = "Adaptive limit reached based on your patterns",
                challengeType = selectAdaptiveChallengeType(userPattern),
                coolingOffPeriod = 0L,
                allowedOvertime = (timeLimit * 0.15).toInt() // 15% overtime for adaptive
            )
        }
        
        return BlockingDecision(false, "", ChallengeType.NONE, 0L, 0)
    }
    
    /**
     * Strict blocking - no mercy, immediate blocking with minimal overtime
     */
    private fun evaluateStrictBlocking(packageName: String, currentUsage: Int, timeLimit: Int): BlockingDecision {
        val warningThreshold = (timeLimit * 0.9).toInt()
        
        if (currentUsage >= timeLimit) {
            return BlockingDecision(
                shouldBlock = true,
                reason = "Strict mode: Time limit exceeded",
                challengeType = ChallengeType.COMPLEX_MATH,
                coolingOffPeriod = 10 * 60 * 1000L, // 10 minutes mandatory cooling off
                allowedOvertime = 0
            )
        }
        
        if (currentUsage >= warningThreshold) {
            // Send warning but don't block yet
            sendUsageWarning(packageName, currentUsage, timeLimit)
        }
        
        return BlockingDecision(false, "", ChallengeType.NONE, 0L, 0)
    }
    
    /**
     * Evaluate context-based rules (time of day, location, etc.)
     */
    private fun evaluateContextRules(packageName: String, rules: ContextRules): BlockingDecision {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        // Check bedtime rules
        if (rules.bedtimeBlocking && (currentHour >= rules.bedtimeStart || currentHour < rules.bedtimeEnd)) {
            return BlockingDecision(
                shouldBlock = true,
                reason = "Bedtime mode active",
                challengeType = ChallengeType.REFLECTION,
                coolingOffPeriod = 0L,
                allowedOvertime = 0
            )
        }
        
        // Check work hours rules
        if (rules.workHoursBlocking && isWorkDay(currentDay) && 
            currentHour >= rules.workHoursStart && currentHour < rules.workHoursEnd) {
            return BlockingDecision(
                shouldBlock = true,
                reason = "Work hours - focus mode active",
                challengeType = ChallengeType.PRODUCTIVITY_QUESTION,
                coolingOffPeriod = 15 * 60 * 1000L, // 15 minutes
                allowedOvertime = 0
            )
        }
        
        // Check family time rules
        if (rules.familyTimeBlocking && rules.familyTimeHours.contains(currentHour)) {
            return BlockingDecision(
                shouldBlock = true,
                reason = "Family time - be present",
                challengeType = ChallengeType.MINDFULNESS,
                coolingOffPeriod = 30 * 60 * 1000L, // 30 minutes
                allowedOvertime = 0
            )
        }
        
        return BlockingDecision(false, "", ChallengeType.NONE, 0L, 0)
    }
    
    /**
     * Calculate progressive limit based on violation history
     */
    private fun calculateProgressiveLimit(originalLimit: Int, violationCount: Int): Int {
        val reductionPercentage = min(violationCount * 10, 50) // Max 50% reduction
        return max((originalLimit * (100 - reductionPercentage) / 100), originalLimit / 4) // Min 25% of original
    }
    
    /**
     * Calculate cooling off period based on violations
     */
    private fun calculateCoolingOffPeriod(violationCount: Int): Long {
        return when (violationCount) {
            0, 1 -> 0L
            2 -> 2 * 60 * 1000L // 2 minutes
            3 -> 5 * 60 * 1000L // 5 minutes
            4 -> 10 * 60 * 1000L // 10 minutes
            5 -> 15 * 60 * 1000L // 15 minutes
            else -> 30 * 60 * 1000L // 30 minutes max
        }
    }
    
    /**
     * Analyze user patterns for adaptive blocking
     */
    private fun analyzeUserPattern(packageName: String): UserPattern {
        // This would analyze historical usage data
        // For now, return a mock pattern
        return UserPattern(
            averageSessionLength = 15 * 60, // 15 minutes
            peakHours = listOf(12, 13, 19, 20, 21), // Lunch and evening
            typicalDailyUsage = 60, // 60 minutes
            bingeTendency = 0.7f, // 70% tendency to binge
            selfControlScore = 0.6f // 60% self-control
        )
    }
    
    /**
     * Calculate adaptive limit based on user patterns
     */
    private fun calculateAdaptiveLimit(originalLimit: Int, pattern: UserPattern): Int {
        var adaptiveLimit = originalLimit
        
        // Adjust based on self-control score
        if (pattern.selfControlScore < 0.5f) {
            adaptiveLimit = (adaptiveLimit * 0.8f).toInt() // Reduce by 20% for low self-control
        }
        
        // Adjust based on binge tendency
        if (pattern.bingeTendency > 0.7f) {
            adaptiveLimit = (adaptiveLimit * 0.9f).toInt() // Reduce by 10% for high binge tendency
        }
        
        return max(adaptiveLimit, originalLimit / 3) // Minimum 33% of original
    }
    
    /**
     * Select appropriate challenge type based on user pattern
     */
    private fun selectAdaptiveChallengeType(pattern: UserPattern): ChallengeType {
        return when {
            pattern.selfControlScore < 0.3f -> ChallengeType.WAITING
            pattern.bingeTendency > 0.8f -> ChallengeType.REFLECTION
            pattern.averageSessionLength > 30 * 60 -> ChallengeType.PHYSICAL
            else -> ChallengeType.MATH
        }
    }
    
    /**
     * Check if current day is a work day
     */
    private fun isWorkDay(dayOfWeek: Int): Boolean {
        return dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
    }
    
    /**
     * Send usage warning notification
     */
    private fun sendUsageWarning(packageName: String, currentUsage: Int, timeLimit: Int) {
        val remaining = timeLimit - currentUsage
        // Implementation would send a notification
        android.util.Log.d("SmartBlocking", "Warning: $remaining minutes remaining for $packageName")
    }
    
    // Getter and setter methods for configuration
    fun getBlockingStrategy(packageName: String): String {
        return prefs.getString("strategy_$packageName", STRATEGY_STANDARD) ?: STRATEGY_STANDARD
    }
    
    fun setBlockingStrategy(packageName: String, strategy: String) {
        prefs.edit().putString("strategy_$packageName", strategy).apply()
    }
    
    fun getViolationCount(packageName: String): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return prefs.getInt("${VIOLATION_COUNT_KEY}_${packageName}_$today", 0)
    }
    
    fun incrementViolationCount(packageName: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentCount = getViolationCount(packageName)
        prefs.edit().putInt("${VIOLATION_COUNT_KEY}_${packageName}_$today", currentCount + 1).apply()
        prefs.edit().putLong("${LAST_VIOLATION_KEY}_$packageName", System.currentTimeMillis()).apply()
    }
    
    fun getContextRules(): ContextRules {
        return ContextRules(
            bedtimeBlocking = prefs.getBoolean("bedtime_blocking", false),
            bedtimeStart = prefs.getInt("bedtime_start", 22),
            bedtimeEnd = prefs.getInt("bedtime_end", 7),
            workHoursBlocking = prefs.getBoolean("work_hours_blocking", false),
            workHoursStart = prefs.getInt("work_hours_start", 9),
            workHoursEnd = prefs.getInt("work_hours_end", 17),
            familyTimeBlocking = prefs.getBoolean("family_time_blocking", false),
            familyTimeHours = prefs.getStringSet("family_time_hours", setOf("18", "19", "20"))?.map { it.toInt() } ?: emptyList()
        )
    }
    
    fun updateContextRules(rules: ContextRules) {
        prefs.edit()
            .putBoolean("bedtime_blocking", rules.bedtimeBlocking)
            .putInt("bedtime_start", rules.bedtimeStart)
            .putInt("bedtime_end", rules.bedtimeEnd)
            .putBoolean("work_hours_blocking", rules.workHoursBlocking)
            .putInt("work_hours_start", rules.workHoursStart)
            .putInt("work_hours_end", rules.workHoursEnd)
            .putBoolean("family_time_blocking", rules.familyTimeBlocking)
            .putStringSet("family_time_hours", rules.familyTimeHours.map { it.toString() }.toSet())
            .apply()
    }
    
    // Data classes
    data class BlockingDecision(
        val shouldBlock: Boolean,
        val reason: String,
        val challengeType: ChallengeType,
        val coolingOffPeriod: Long, // in milliseconds
        val allowedOvertime: Int // in minutes
    )
    
    data class UserPattern(
        val averageSessionLength: Int, // in seconds
        val peakHours: List<Int>,
        val typicalDailyUsage: Int, // in minutes
        val bingeTendency: Float, // 0.0 to 1.0
        val selfControlScore: Float // 0.0 to 1.0
    )
    
    data class ContextRules(
        val bedtimeBlocking: Boolean,
        val bedtimeStart: Int, // hour (0-23)
        val bedtimeEnd: Int, // hour (0-23)
        val workHoursBlocking: Boolean,
        val workHoursStart: Int,
        val workHoursEnd: Int,
        val familyTimeBlocking: Boolean,
        val familyTimeHours: List<Int>
    )
    
    enum class ChallengeType {
        NONE,
        MATH,
        COMPLEX_MATH,
        REFLECTION,
        MINDFULNESS,
        PHYSICAL,
        PRODUCTIVITY_QUESTION,
        WAITING
    }
}