package com.bravebrain

import android.content.Context
import android.util.Log
import java.util.*

/**
 * NotificationContentGenerator creates adaptive notification content
 * that adapts to context and user behavior
 */
class NotificationContentGenerator(
    private val context: Context,
    private val contextAnalyzer: ContextAnalyzer,
    private val userEngagementAnalyzer: UserEngagementAnalyzer
) {
    companion object {
        private const val TAG = "NotificationContentGen"
    }

    /**
     * Generates personalized notification content based on context and user behavior
     */
    fun generateNotificationContent(
        notificationType: String,
        userContext: ContextAnalyzer.UserContext? = null,
        additionalData: Map<String, Any> = emptyMap()
    ): Pair<String, String> { // Returns title and content
        val context = userContext ?: contextAnalyzer.analyzeContext()
        val engagementMetrics = userEngagementAnalyzer.analyzeEngagement()

        return when (notificationType.lowercase()) {
            "proactive_warning" -> generateProactiveWarningContent(context, additionalData)
            "positive_reinforcement" -> generatePositiveReinforcementContent(context, engagementMetrics, additionalData)
            "insights_analytics" -> generateInsightsContent(context, additionalData)
            "gamification_update" -> generateGamificationContent(context, additionalData)
            "contextual_suggestion" -> generateContextualSuggestionContent(context, additionalData)
            else -> generateGenericContent(notificationType, context, additionalData)
        }
    }

    /**
     * Generates proactive warning content based on context
     */
    private fun generateProactiveWarningContent(
        userContext: ContextAnalyzer.UserContext,
        additionalData: Map<String, Any>
    ): Pair<String, String> {
        val appName = additionalData["app_name"] as? String ?: "the app"
        val timeLimit = additionalData["time_limit"] as? String ?: "your limit"
        val timeRemaining = additionalData["time_remaining"] as? String ?: "a few minutes"

        val titles = when (userContext.timeOfDay) {
            ContextAnalyzer.TimeOfDay.MORNING -> listOf(
                "Morning Awareness",
                "Start Your Day Mindfully",
                "Mindful Moment"
            )
            ContextAnalyzer.TimeOfDay.AFTERNOON -> listOf(
                "Afternoon Check-in",
                "Stay on Track",
                "Time Awareness"
            )
            ContextAnalyzer.TimeOfDay.EVENING -> listOf(
                "Evening Reminder",
                "Wind Down Time",
                "Day Reflection"
            )
            ContextAnalyzer.TimeOfDay.NIGHT -> listOf(
                "Late Night Awareness",
                "Time to Unwind",
                "Rest Time Reminder"
            )
        }

        val messages = when {
            userContext.isWorkTime -> listOf(
                "You're approaching your time limit for $appName. Consider taking a break to maintain productivity.",
                "Mindful tip: Taking breaks during work hours can improve focus. You've been on $appName for a while.",
                "Approaching your limit for $appName. A brief pause might help you refocus on work tasks."
            )
            userContext.isFocusTime -> listOf(
                "You're approaching your time limit for $appName. Consider returning to your focused activity.",
                "Attention: You've been on $appName for a while. Remember your focus goals.",
                "Gentle reminder: You have limited time left for $appName. Time to refocus?"
            )
            userContext.usageIntensity == ContextAnalyzer.UsageIntensity.PEAK -> listOf(
                "You've been actively using $appName. Maybe take a short break to refresh?",
                "Intensive usage detected for $appName. A moment of pause could be beneficial.",
                "You're spending significant time on $appName. Consider a quick break."
            )
            else -> listOf(
                "You're approaching $timeLimit for $appName. $timeRemaining left.",
                "Mindful reminder: You've been on $appName for a while now.",
                "Time awareness: Your $appName usage is approaching $timeLimit."
            )
        }

        val title = titles.random()
        val message = messages.random()

        return Pair(title, message)
    }

    /**
     * Generates positive reinforcement content based on user achievements
     */
    private fun generatePositiveReinforcementContent(
        userContext: ContextAnalyzer.UserContext,
        engagementMetrics: UserEngagementAnalyzer.EngagementMetrics,
        additionalData: Map<String, Any>
    ): Pair<String, String> {
        val achievements = additionalData["achievements"] as? List<String> ?: emptyList()
        val streakDays = additionalData["streak_days"] as? Int ?: 0

        val titles = when {
            streakDays > 0 -> listOf(
                "Streak Success! 🎉",
                "Consistency Champion!",
                "Great Streak Going!"
            )
            engagementMetrics.responseRate > 0.7f -> listOf(
                "Engagement Star!",
                "Great Interaction!",
                "You're Staying Engaged!"
            )
            else -> listOf(
                "Great Job!",
                "Well Done!",
                "Keep It Up!",
                "You're Making Progress!"
            )
        }

        val messages = when {
            streakDays >= 7 -> listOf(
                "Amazing! You've maintained good screen time habits for $streakDays days straight. Keep up the excellent work!",
                "Impressive $streakDays-day streak of mindful usage! You're building great habits.",
                "Fantastic achievement: $streakDays days of conscious screen time management!"
            )
            streakDays >= 3 -> listOf(
                "Great work! You're on a $streakDays-day streak of mindful usage.",
                "Keep going! $streakDays days of maintaining your screen time goals.",
                "Consistency pays off! $streakDays days of good screen time habits."
            )
            achievements.isNotEmpty() -> {
                val achievement = achievements.random()
                listOf(
                    "Congratulations! You've unlocked: $achievement",
                    "Achievement earned: $achievement. Keep up the great work!",
                    "Nice! You've achieved: $achievement. Your efforts are paying off."
                )
            }
            userContext.engagementLevel == ContextAnalyzer.EngagementLevel.VERY_HIGH -> listOf(
                "Balanced approach: You're engaging well with the app while maintaining control.",
                "Perfect balance: Active engagement with mindful usage. That's the way!",
                "Great job balancing engagement and control today."
            )
            else -> listOf(
                "You're maintaining good screen time habits today. Keep it up!",
                "Great progress on your mindful usage goals.",
                "Your efforts toward mindful screen time are showing results.",
                "Well done on managing your screen time mindfully today."
            )
        }

        val title = titles.random()
        val message = messages.random()

        return Pair(title, message)
    }

    /**
     * Generates insights and analytics content based on usage patterns
     */
    private fun generateInsightsContent(
        userContext: ContextAnalyzer.UserContext,
        additionalData: Map<String, Any>
    ): Pair<String, String> {
        val insightType = additionalData["insight_type"] as? String ?: "weekly"
        val topApp = additionalData["top_app"] as? String ?: "an app"
        val usageChange = additionalData["usage_change"] as? String ?: "unchanged"

        val titles = when (insightType.lowercase()) {
            "daily" -> listOf(
                "Daily Insights Ready",
                "Today's Summary",
                "Daily Reflection"
            )
            "weekly" -> listOf(
                "Weekly Insights Ready",
                "Week in Review",
                "Weekly Progress Report"
            )
            "monthly" -> listOf(
                "Monthly Insights Ready",
                "Month in Review",
                "Monthly Progress Report"
            )
            else -> listOf(
                "Usage Insights Ready",
                "Your Analytics Report",
                "Personalized Insights"
            )
        }

        val messages = when {
            usageChange.contains("decreased", ignoreCase = true) -> listOf(
                "Great news! Your usage of $topApp has decreased this $insightType. Keep up the mindful approach!",
                "Positive trend detected: You're using $topApp less this $insightType. Your mindful efforts are working!",
                "Notable improvement: $topApp usage down this $insightType. You're building healthier habits."
            )
            usageChange.contains("increased", ignoreCase = true) -> listOf(
                "Your usage of $topApp has increased this $insightType. Consider setting mindful goals.",
                "Awareness moment: $topApp usage up this $insightType. Maybe reflect on your usage patterns.",
                "Interesting pattern: $topApp usage has grown this $insightType. Worth examining your habits."
            )
            else -> listOf(
                "Your weekly analytics are ready. You spent the most time on $topApp this week.",
                "Insights update: Review your $insightType usage patterns and progress toward goals.",
                "Personalized report: See how your screen time habits have evolved this $insightType.",
                "Data-driven insights: Understand your $topApp usage patterns and trends."
            )
        }

        val title = titles.random()
        val message = messages.random()

        return Pair(title, message)
    }

    /**
     * Generates gamification update content based on achievements
     */
    private fun generateGamificationContent(
        userContext: ContextAnalyzer.UserContext,
        additionalData: Map<String, Any>
    ): Pair<String, String> {
        val achievementType = additionalData["achievement_type"] as? String ?: "milestone"
        val achievementName = additionalData["achievement_name"] as? String ?: "a new achievement"
        val badgeName = additionalData["badge_name"] as? String ?: "a badge"

        val titles = when (achievementType.lowercase()) {
            "streak" -> listOf(
                "Streak Achievement!",
                "Consistency Award",
                "Streak Success!"
            )
            "milestone" -> listOf(
                "Milestone Reached!",
                "Goal Achievement",
                "Progress Unlocked"
            )
            "badge" -> listOf(
                "New Badge Earned!",
                "Achievement Badge",
                "Badge Unlocked!"
            )
            else -> listOf(
                "Achievement Unlocked!",
                "New Accomplishment",
                "Goal Reached!"
            )
        }

        val messages = when {
            achievementType.lowercase() == "streak" -> listOf(
                "Congratulations! You've reached a new milestone with your $achievementName streak.",
                "Impressive consistency! Your $achievementName streak continues to grow.",
                "Streak achievement unlocked: Your dedication to mindful usage is paying off."
            )
            achievementType.lowercase() == "milestone" -> listOf(
                "Fantastic achievement unlocked: $achievementName! Your efforts are recognized.",
                "Milestone reached: $achievementName. Your commitment to mindful usage is impressive.",
                "Goal achieved: $achievementName. Keep up the great work on your mindful journey."
            )
            achievementType.lowercase() == "badge" -> listOf(
                "New badge earned: $badgeName! Your mindful usage habits are being recognized.",
                "Badge unlocked: $badgeName. You're making great progress on your goals.",
                "Achievement badge earned: $badgeName. Your dedication to mindful usage shines."
            )
            else -> listOf(
                "Achievement unlocked: $achievementName! Your mindful usage efforts are paying off.",
                "New accomplishment: $achievementName. You're building great digital wellness habits.",
                "Well deserved: $achievementName. Your commitment to mindful usage is inspiring."
            )
        }

        val title = titles.random()
        val message = messages.random()

        return Pair(title, message)
    }

    /**
     * Generates contextual suggestion content based on user context
     */
    private fun generateContextualSuggestionContent(
        userContext: ContextAnalyzer.UserContext,
        additionalData: Map<String, Any>
    ): Pair<String, String> {
        val currentApp = additionalData["current_app"] as? String ?: "the app"
        val alternativeApp = additionalData["alternative_app"] as? String ?: "a different app"
        val timeOfDay = userContext.timeOfDay

        val titles = when (timeOfDay) {
            ContextAnalyzer.TimeOfDay.MORNING -> listOf(
                "Morning Suggestion",
                "Start Your Day Right",
                "Productive Morning"
            )
            ContextAnalyzer.TimeOfDay.AFTERNOON -> listOf(
                "Afternoon Boost",
                "Productive Suggestion",
                "Afternoon Alternative"
            )
            ContextAnalyzer.TimeOfDay.EVENING -> listOf(
                "Evening Wind-Down",
                "Relaxing Alternative",
                "Evening Choice"
            )
            ContextAnalyzer.TimeOfDay.NIGHT -> listOf(
                "Night Time Option",
                "Wind Down Suggestion",
                "Rest Time Choice"
            )
        }

        val messages = when {
            userContext.isWorkTime -> listOf(
                "During work hours, consider switching to $alternativeApp for productivity.",
                "Work time suggestion: $alternativeApp might be more appropriate right now.",
                "Productivity tip: $alternativeApp could help you stay focused during work hours."
            )
            userContext.isFocusTime -> listOf(
                "To maintain your focus, $alternativeApp might be a better option right now.",
                "Focus time suggestion: Try $alternativeApp for a more concentrated activity.",
                "Maintain your focus: $alternativeApp could provide a more mindful alternative."
            )
            userContext.engagementLevel == ContextAnalyzer.EngagementLevel.HIGH || 
            userContext.engagementLevel == ContextAnalyzer.EngagementLevel.VERY_HIGH -> listOf(
                "You've been actively engaged with $currentApp. Maybe try $alternativeApp for a change?",
                "High engagement detected. How about exploring $alternativeApp for a different experience?",
                "You're spending significant time on $currentApp. $alternativeApp might offer a refreshing change."
            )
            timeOfDay == ContextAnalyzer.TimeOfDay.NIGHT -> listOf(
                "Late night usage detected. Consider switching to $alternativeApp for a more relaxing activity.",
                "Evening wind-down suggestion: $alternativeApp might be more appropriate for this time.",
                "Night time choice: $alternativeApp could be a better option as you prepare for rest."
            )
            else -> listOf(
                "Context-aware suggestion: $alternativeApp might be more suitable for your current situation.",
                "Personalized recommendation: Try $alternativeApp for a different experience.",
                "Based on your current context, $alternativeApp could be a better choice right now.",
                "Mindful suggestion: $alternativeApp might align better with your current needs."
            )
        }

        val title = titles.random()
        val message = messages.random()

        return Pair(title, message)
    }

    /**
     * Generates generic content for unknown notification types
     */
    private fun generateGenericContent(
        notificationType: String,
        userContext: ContextAnalyzer.UserContext,
        additionalData: Map<String, Any>
    ): Pair<String, String> {
        val titles = listOf(
            "Personalized Update",
            "Mindful Reminder",
            "Digital Wellness Tip",
            "Usage Insight",
            "Wellness Check-in"
        )

        val messages = listOf(
            "Based on your usage patterns, we have a personalized recommendation for you.",
            "Mindful moment: Here's a tailored suggestion based on your habits.",
            "Your digital wellness journey continues with this personalized insight.",
            "Time for a wellness check-in based on your recent activity.",
            "Personalized content based on your usage patterns and preferences."
        )

        val title = titles.random()
        val message = messages.random()

        return Pair(title, message)
    }

    /**
     * Adapts notification content based on user's preferred communication style
     */
    fun adaptContentToUserStyle(
        title: String,
        content: String,
        userContext: ContextAnalyzer.UserContext,
        engagementMetrics: UserEngagementAnalyzer.EngagementMetrics
    ): Pair<String, String> {
        // Determine user's preferred communication style based on engagement patterns
        val responseRate = engagementMetrics.responseRate
        val preferredTypes = engagementMetrics.preferredContentTypes

        // Adjust tone based on engagement level
        val adjustedTitle = if (userContext.engagementLevel == ContextAnalyzer.EngagementLevel.LOW) {
            // For low engagement, use more encouraging tone
            title.replace("Reminder", "Friendly Reminder")
                .replace("Warning", "Gentle Awareness")
                .replace("Check-in", "Mindful Moment")
        } else {
            title
        }

        var adjustedContent = content

        // Adjust content based on user preferences
        if ("positive_reinforcement" in preferredTypes) {
            // Add more positive elements if user responds well to positive content
            adjustedContent = if (!adjustedContent.contains("great", ignoreCase = true) &&
                                 !adjustedContent.contains("well done", ignoreCase = true) &&
                                 !adjustedContent.contains("good job", ignoreCase = true)) {
                "Great work! $adjustedContent"
            } else {
                adjustedContent
            }
        } else if ("proactive_warning" in preferredTypes) {
            // Add more direct language if user responds well to warnings
            adjustedContent = if (!adjustedContent.contains("remember", ignoreCase = true) &&
                                 !adjustedContent.contains("consider", ignoreCase = true)) {
                "$adjustedContent Remember to consider your usage patterns."
            } else {
                adjustedContent
            }
        }

        // Adjust based on response rate
        if (responseRate < 0.3f) {
            // For low response rate, make content more compelling
            adjustedContent = if (!adjustedContent.contains("now", ignoreCase = true)) {
                "$adjustedContent Consider this now."
            } else {
                adjustedContent
            }
        } else if (responseRate > 0.7f) {
            // For high response rate, keep current approach
            adjustedContent = adjustedContent
        }

        return Pair(adjustedTitle, adjustedContent)
    }

    /**
     * Personalizes notification content based on user's time of day preferences
     */
    fun personalizeForTimeOfDay(
        title: String,
        content: String,
        timeOfDay: ContextAnalyzer.TimeOfDay
    ): Pair<String, String> {
        val personalizedTitle = when (timeOfDay) {
            ContextAnalyzer.TimeOfDay.MORNING -> {
                title.replace(Regex("^(?!Morning|Start).*"), "Morning: $title")
            }
            ContextAnalyzer.TimeOfDay.AFTERNOON -> {
                title.replace(Regex("^(?!Afternoon|Midday).*"), "Afternoon: $title")
            }
            ContextAnalyzer.TimeOfDay.EVENING -> {
                title.replace(Regex("^(?!Evening|Tonight).*"), "Evening: $title")
            }
            ContextAnalyzer.TimeOfDay.NIGHT -> {
                title.replace(Regex("^(?!Night|Tonight).*"), "Night: $title")
            }
        }

        var personalizedContent = content

        // Adjust content based on time of day
        when (timeOfDay) {
            ContextAnalyzer.TimeOfDay.MORNING -> {
                personalizedContent = if (!personalizedContent.contains("morning", ignoreCase = true)) {
                    "Starting your morning mindfully: $personalizedContent"
                } else {
                    personalizedContent
                }
            }
            ContextAnalyzer.TimeOfDay.NIGHT -> {
                personalizedContent = if (!personalizedContent.contains("tonight", ignoreCase = true) &&
                                         !personalizedContent.contains("night", ignoreCase = true) &&
                                         !personalizedContent.contains("rest", ignoreCase = true)) {
                    "$personalizedContent As you wind down tonight..."
                } else {
                    personalizedContent
                }
            }
            else -> {
                // No specific changes for afternoon/evening
            }
        }

        return Pair(personalizedTitle, personalizedContent)
    }

    /**
     * Creates contextual notification content based on user's current activity
     */
    fun createContextualContent(
        baseType: String,
        currentActivity: String,
        userContext: ContextAnalyzer.UserContext
    ): Pair<String, String> {
        val (baseTitle, baseContent) = generateNotificationContent(baseType, userContext)

        // Enhance content based on current activity
        val enhancedContent = when {
            currentActivity.contains("social", ignoreCase = true) -> {
                "$baseContent Since you're on a social app, consider taking a mindful break."
            }
            currentActivity.contains("game", ignoreCase = true) -> {
                "$baseContent Gaming can be engaging. Remember to take breaks for well-being."
            }
            currentActivity.contains("video", ignoreCase = true) -> {
                "$baseContent Video content can be immersive. Consider a mindful pause."
            }
            else -> baseContent
        }

        return Pair(baseTitle, enhancedContent)
    }
}