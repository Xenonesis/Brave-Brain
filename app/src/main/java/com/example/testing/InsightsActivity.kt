package com.example.testing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Analytics and Insights Dashboard
 */
class InsightsActivity : AppCompatActivity() {
    
    private lateinit var productivityScoreText: TextView
    private lateinit var weeklyTrendText: TextView
    private lateinit var insightsContainer: LinearLayout
    private lateinit var statsContainer: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)
        
        setupUI()
        loadAnalyticsData()
    }
    
    private fun setupUI() {
        // Initialize views
        productivityScoreText = findViewById(R.id.productivityScoreText)
        weeklyTrendText = findViewById(R.id.weeklyTrendText)
        insightsContainer = findViewById(R.id.insightsContainer)
        statsContainer = findViewById(R.id.statsContainer)
        
        // Setup toolbar
        supportActionBar?.title = "📊 Usage Insights"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Setup refresh button
        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            refreshAnalytics()
        }
        
        // Setup export button
        findViewById<Button>(R.id.exportButton).setOnClickListener {
            exportAnalytics()
        }
    }
    
    private fun loadAnalyticsData() {
        val prefs = getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        
        // Load productivity score
        val productivityScore = prefs.getInt("productivity_score", 50)
        updateProductivityScore(productivityScore)
        
        // Load weekly trend
        updateWeeklyTrend()
        
        // Load insights
        loadInsights()
        
        // Load detailed stats
        loadDetailedStats()
    }
    
    private fun updateProductivityScore(score: Int) {
        productivityScoreText.text = "$score/100"
        
        // Color code the score
        val color = when {
            score >= 80 -> ContextCompat.getColor(this, R.color.success_green)
            score >= 60 -> ContextCompat.getColor(this, R.color.warning_orange)
            else -> ContextCompat.getColor(this, R.color.error_red)
        }
        productivityScoreText.setTextColor(color)
        
        // Update progress bar if exists
        findViewById<ProgressBar>(R.id.productivityProgressBar)?.progress = score
    }
    
    private fun updateWeeklyTrend() {
        val improvement = calculateWeeklyImprovement()
        val trendText = when {
            improvement > 5 -> "📈 Improving (+${improvement.toInt()}%)"
            improvement < -5 -> "📉 Declining (${improvement.toInt()}%)"
            else -> "📊 Stable"
        }
        weeklyTrendText.text = trendText
        
        val color = when {
            improvement > 0 -> ContextCompat.getColor(this, R.color.success_green)
            improvement < 0 -> ContextCompat.getColor(this, R.color.error_red)
            else -> ContextCompat.getColor(this, R.color.text_secondary)
        }
        weeklyTrendText.setTextColor(color)
    }
    
    private fun loadInsights() {
        insightsContainer.removeAllViews()
        
        val prefs = getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val insightsString = prefs.getString("insights", "")
        
        if (!insightsString.isNullOrEmpty()) {
            val insights = insightsString.split("|")
            insights.forEach { insight ->
                addInsightCard(insight)
            }
        } else {
            addInsightCard("📊 Start using the app to see personalized insights!")
        }
        
        // Add some default insights
        addDefaultInsights()
    }
    
    private fun addDefaultInsights() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        when {
            currentHour < 12 -> addInsightCard("🌅 Good morning! Set your daily digital wellness goals.")
            currentHour < 18 -> addInsightCard("☀️ Afternoon check-in: How's your screen time today?")
            else -> addInsightCard("🌙 Evening reflection: Review your digital habits today.")
        }
        
        // Peak usage insights
        val peakHour = getPeakUsageHour()
        if (peakHour != -1) {
            addInsightCard("⏰ Your peak usage time is around $peakHour:00. Consider setting focus mode during this time.")
        }
        
        // Weekly goal insights
        val weeklyGoal = getWeeklyScreenTimeGoal()
        val currentWeeklyUsage = getCurrentWeeklyUsage()
        if (weeklyGoal > 0) {
            val percentage = (currentWeeklyUsage.toFloat() / weeklyGoal * 100).toInt()
            when {
                percentage > 100 -> addInsightCard("⚠️ You've exceeded your weekly screen time goal by ${percentage - 100}%")
                percentage > 80 -> addInsightCard("🔔 You're at $percentage% of your weekly screen time goal")
                else -> addInsightCard("✅ Great job! You're at $percentage% of your weekly screen time goal")
            }
        }
    }
    
    private fun addInsightCard(insight: String) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            cardElevation = 4f
            radius = 12f
            setCardBackgroundColor(ContextCompat.getColor(this@InsightsActivity, R.color.card_background))
        }
        
        val textView = TextView(this).apply {
            text = insight
            textSize = 14f
            setPadding(24, 16, 24, 16)
            setTextColor(ContextCompat.getColor(this@InsightsActivity, R.color.text_primary))
        }
        
        card.addView(textView)
        insightsContainer.addView(card)
    }
    
    private fun loadDetailedStats() {
        statsContainer.removeAllViews()
        
        // Today's stats
        addStatCard("Today's Screen Time", formatTime(getTodayScreenTime()), "📱")
        addStatCard("Apps Used Today", getAppsUsedToday().toString(), "📊")
        addStatCard("Blocked Attempts", getBlockedAttemptsToday().toString(), "🚫")
        addStatCard("Challenges Completed", getChallengesCompletedToday().toString(), "🧮")
        
        // Weekly stats
        addStatCard("Weekly Average", formatTime(getWeeklyAverageScreenTime()), "📈")
        addStatCard("Most Used App", getMostUsedAppThisWeek(), "🎯")
        addStatCard("Best Day This Week", getBestDayThisWeek(), "⭐")
        addStatCard("Productivity Streak", getProductivityStreak().toString() + " days", "🔥")
    }
    
    private fun addStatCard(title: String, value: String, icon: String) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(8, 0, 8, 16)
            }
            cardElevation = 2f
            radius = 8f
            setCardBackgroundColor(ContextCompat.getColor(this@InsightsActivity, R.color.white))
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.CENTER
        }
        
        val iconText = TextView(this).apply {
            text = icon
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }
        
        val titleText = TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@InsightsActivity, R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
        }
        
        val valueText = TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@InsightsActivity, R.color.text_primary))
            gravity = android.view.Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        container.addView(iconText)
        container.addView(titleText)
        container.addView(valueText)
        card.addView(container)
        
        // Add to a horizontal layout (create rows of 2)
        val lastRow = if (statsContainer.childCount > 0) {
            val lastChild = statsContainer.getChildAt(statsContainer.childCount - 1)
            if (lastChild is LinearLayout && lastChild.childCount < 2) lastChild else null
        } else null
        
        if (lastRow != null) {
            lastRow.addView(card)
        } else {
            val newRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            newRow.addView(card)
            statsContainer.addView(newRow)
        }
    }
    
    private fun refreshAnalytics() {
        // Start analytics service to refresh data
        startService(Intent(this, AnalyticsService::class.java))
        
        // Reload data after a short delay
        findViewById<Button>(R.id.refreshButton).postDelayed({
            loadAnalyticsData()
            Toast.makeText(this, "Analytics refreshed!", Toast.LENGTH_SHORT).show()
        }, 1000)
    }
    
    private fun exportAnalytics() {
        val analyticsData = generateAnalyticsReport()
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, analyticsData)
            putExtra(Intent.EXTRA_SUBJECT, "Brave Brain - Usage Analytics Report")
            type = "text/plain"
        }
        
        startActivity(Intent.createChooser(shareIntent, "Export Analytics"))
    }
    
    private fun generateAnalyticsReport(): String {
        val report = StringBuilder()
        report.appendLine("📊 Brave Brain - Usage Analytics Report")
        report.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}")
        report.appendLine()
        
        report.appendLine("🎯 Productivity Score: ${getProductivityScore()}/100")
        report.appendLine("📱 Today's Screen Time: ${formatTime(getTodayScreenTime())}")
        report.appendLine("📊 Apps Used Today: ${getAppsUsedToday()}")
        report.appendLine("🚫 Blocked Attempts: ${getBlockedAttemptsToday()}")
        report.appendLine("🧮 Challenges Completed: ${getChallengesCompletedToday()}")
        report.appendLine()
        
        report.appendLine("📈 Weekly Statistics:")
        report.appendLine("Average Screen Time: ${formatTime(getWeeklyAverageScreenTime())}")
        report.appendLine("Most Used App: ${getMostUsedAppThisWeek()}")
        report.appendLine("Best Day: ${getBestDayThisWeek()}")
        report.appendLine("Productivity Streak: ${getProductivityStreak()} days")
        
        return report.toString()
    }
    
    // Helper methods for data retrieval
    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
    
    private fun calculateWeeklyImprovement(): Float {
        // Placeholder implementation
        return kotlin.random.Random.nextFloat() * 20 - 10 // Random between -10 and +10
    }
    
    private fun getPeakUsageHour(): Int = 14 // Placeholder: 2 PM
    private fun getWeeklyScreenTimeGoal(): Long = 7 * 60 * 60 * 1000 // 7 hours per week
    private fun getCurrentWeeklyUsage(): Long = 5 * 60 * 60 * 1000 // 5 hours this week
    private fun getTodayScreenTime(): Long = UsageUtils.getUsage(this).values.sum().toLong() * 60 * 1000
    private fun getAppsUsedToday(): Int = UsageUtils.getUsage(this).size
    private fun getBlockedAttemptsToday(): Int = 3 // Placeholder
    private fun getChallengesCompletedToday(): Int = 2 // Placeholder
    private fun getWeeklyAverageScreenTime(): Long = 4 * 60 * 60 * 1000 // 4 hours average
    private fun getMostUsedAppThisWeek(): String = "Social Media" // Placeholder
    private fun getBestDayThisWeek(): String = "Tuesday" // Placeholder
    private fun getProductivityStreak(): Int = 5 // Placeholder
    private fun getProductivityScore(): Int {
        val prefs = getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        return prefs.getInt("productivity_score", 50)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}