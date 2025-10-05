package com.bravebrain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class UserProfile(
    @DocumentId val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastSyncAt: Timestamp = Timestamp.now(),
    val preferences: Map<String, Any> = emptyMap()
)

data class AppUsageData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val usageTimeMs: Long = 0,
    val dailyLimitMs: Long = 0,
    val category: String = "",
    val date: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class NotificationData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val sentAt: Timestamp = Timestamp.now(),
    val wasClicked: Boolean = false,
    val wasDismissed: Boolean = false,
    val effectiveness: Double = 0.0,
    val context: Map<String, Any> = emptyMap()
)

data class GamificationData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val points: Int = 0,
    val level: Int = 1,
    val badges: List<String> = emptyList(),
    val challenges: Map<String, Any> = emptyMap(),
    val achievements: List<Map<String, Any>> = emptyList(),
    val lastUpdated: Timestamp = Timestamp.now()
)

data class AnalyticsData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val totalScreenTimeMs: Long = 0,
    val productivityScore: Int = 0,
    val blockedAttempts: Int = 0,
    val challengesCompleted: Int = 0,
    val challengesFailed: Int = 0,
    val usagePatterns: Map<String, Any> = emptyMap(),
    val timestamp: Timestamp = Timestamp.now()
)

data class UserFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val feedbackType: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val context: Map<String, Any> = emptyMap(),
    val timestamp: Timestamp = Timestamp.now()
)
