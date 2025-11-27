package com.bravebrain

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class NotificationPreferenceActivity : AppCompatActivity() {

    private lateinit var preferenceManager: NotificationPreferenceManager
    private lateinit var feedbackManager: UserFeedbackManager

    companion object {
        private const val TAG = "NotificationPrefActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_preferences)

        preferenceManager = NotificationPreferenceManager(this)
        feedbackManager = UserFeedbackManager(this)

        setupToolbar()
        setupNotificationTypeToggles()
        setupFrequencyControls()
        setupTimingControls()
        setupMessageCategories()
        setupLearningOptions()
        setupFeedbackSection()
        setupActionButtons()

        // Load current preferences
        loadCurrentPreferences()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Notification Preferences"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupNotificationTypeToggles() {
        // Proactive Warnings
        findViewById<SwitchMaterial>(R.id.switchProactiveWarnings).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setProactiveWarningsEnabled(isChecked)
        }

        // Positive Reinforcement
        findViewById<SwitchMaterial>(R.id.switchPositiveReinforcement).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setPositiveReinforcementEnabled(isChecked)
        }

        // Insights & Analytics
        findViewById<SwitchMaterial>(R.id.switchInsightsAnalytics).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setInsightsAnalyticsEnabled(isChecked)
        }

        // Gamification Updates
        findViewById<SwitchMaterial>(R.id.switchGamificationUpdates).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setGamificationUpdatesEnabled(isChecked)
        }

        // Contextual Suggestions
        findViewById<SwitchMaterial>(R.id.switchContextualSuggestions).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setContextualSuggestionsEnabled(isChecked)
        }
    }

    private fun setupFrequencyControls() {
        val frequencySlider = findViewById<Slider>(R.id.sliderNotificationFrequency)
        val frequencyValue = findViewById<TextView>(R.id.textFrequencyValue)

        frequencySlider.addOnChangeListener { _, value, _ ->
            val minutes = value.toInt()
            preferenceManager.setNotificationFrequency(minutes)
            frequencyValue.text = "$minutes minutes"
        }
    }

    private fun setupTimingControls() {
        // Silent Hours
        findViewById<SwitchMaterial>(R.id.switchSilentHours).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setSilentHoursEnabled(isChecked)
            updateSilentHoursVisibility(isChecked)
        }

        // Silent Hours Start
        findViewById<NumberPicker>(R.id.pickerSilentStart).apply {
            minValue = 0
            maxValue = 23
            setOnValueChangedListener { _, _, newVal ->
                preferenceManager.setSilentHoursStart(newVal)
            }
        }

        // Silent Hours End
        findViewById<NumberPicker>(R.id.pickerSilentEnd).apply {
            minValue = 0
            maxValue = 23
            setOnValueChangedListener { _, _, newVal ->
                preferenceManager.setSilentHoursEnd(newVal)
            }
        }

        // Preferred Window Start
        findViewById<NumberPicker>(R.id.pickerPreferredStart).apply {
            minValue = 0
            maxValue = 23
            setOnValueChangedListener { _, _, newVal ->
                preferenceManager.setPreferredNotificationWindowStart(newVal)
            }
        }

        // Preferred Window End
        findViewById<NumberPicker>(R.id.pickerPreferredEnd).apply {
            minValue = 0
            maxValue = 23
            setOnValueChangedListener { _, _, newVal ->
                preferenceManager.setPreferredNotificationWindowEnd(newVal)
            }
        }
    }

    private fun setupMessageCategories() {
        val categoriesContainer = findViewById<LinearLayout>(R.id.containerMessageCategories)
        val categories = setOf("motivational", "informative", "warning", "suggestion", "achievement")

        categories.forEach { category ->
            val switch = SwitchMaterial(this).apply {
                text = category.replaceFirstChar { it.uppercase() }
                setOnCheckedChangeListener { _, isChecked ->
                    val currentCategories = preferenceManager.getMessageCategories().toMutableSet()
                    if (isChecked) {
                        currentCategories.add(category)
                    } else {
                        currentCategories.remove(category)
                    }
                    preferenceManager.setMessageCategories(currentCategories)
                }
            }
            categoriesContainer.addView(switch)
        }
    }

    private fun setupLearningOptions() {
        findViewById<SwitchMaterial>(R.id.switchLearningAdaptation).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setLearningAdaptationEnabled(isChecked)
        }

        findViewById<SwitchMaterial>(R.id.switchFeedbackLearning).setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setFeedbackLearningEnabled(isChecked)
        }
    }

    private fun setupFeedbackSection() {
        val feedbackStats = feedbackManager.analyzeFeedbackTrends()
        val statsContainer = findViewById<LinearLayout>(R.id.containerFeedbackStats)

        // Clear existing stats
        statsContainer.removeAllViews()

        if (feedbackStats.totalFeedback > 0) {
            // Overall rating
            addStatCard(statsContainer, "Overall Rating", "%.1f/5".format(feedbackStats.overallRating))
            
            // Helpful percentage
            addStatCard(statsContainer, "Helpful Responses", "%.1f%%".format(feedbackStats.overallHelpfulPercentage))
            
            // Total feedback
            addStatCard(statsContainer, "Total Feedback", feedbackStats.totalFeedback.toString())

            // Best performing type
            feedbackStats.bestPerformingType?.let { bestType ->
                addStatCard(statsContainer, "Best Type", bestType.replaceFirstChar { it.uppercase() })
            }

            // Suggestions
            if (feedbackStats.suggestions.isNotEmpty()) {
                val suggestionsCard = MaterialCardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 16, 0, 0)
                    }
                    setCardBackgroundColor(ContextCompat.getColor(this@NotificationPreferenceActivity, R.color.cardBackground))
                }

                val suggestionsLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 24, 24, 24)
                }

                val title = TextView(this).apply {
                    text = "Suggestions"
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(this@NotificationPreferenceActivity, R.color.textPrimary))
                }
                suggestionsLayout.addView(title)

                feedbackStats.suggestions.take(3).forEach { suggestion ->
                    val suggestionText = TextView(this).apply {
                        text = "• $suggestion"
                        textSize = 14f
                        setTextColor(ContextCompat.getColor(this@NotificationPreferenceActivity, R.color.textSecondary))
                        setPadding(0, 8, 0, 0)
                    }
                    suggestionsLayout.addView(suggestionText)
                }

                suggestionsCard.addView(suggestionsLayout)
                statsContainer.addView(suggestionsCard)
            }
        } else {
            val noDataText = TextView(this).apply {
                text = "No feedback data yet. Notifications will help collect feedback!"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@NotificationPreferenceActivity, R.color.textSecondary))
                gravity = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 24, 0, 24)
            }
            statsContainer.addView(noDataText)
        }
    }

    private fun addStatCard(container: LinearLayout, title: String, value: String) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
            setCardBackgroundColor(ContextCompat.getColor(this@NotificationPreferenceActivity, R.color.cardBackground))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@NotificationPreferenceActivity, R.color.textSecondary))
        }

        val valueView = TextView(this).apply {
            text = value
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@NotificationPreferenceActivity, R.color.textPrimary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        layout.addView(titleView)
        layout.addView(valueView)
        card.addView(layout)
        container.addView(card)
    }

    private fun setupActionButtons() {
        // Reset to defaults
        findViewById<MaterialButton>(R.id.buttonResetDefaults).setOnClickListener {
            preferenceManager.resetToDefaults()
            loadCurrentPreferences()
            FeedbackManager.showToast(this, "Preferences reset to defaults", FeedbackManager.FeedbackType.SUCCESS)
        }

        // View detailed feedback
        findViewById<MaterialButton>(R.id.buttonViewFeedback).setOnClickListener {
            // This could open a detailed feedback activity in the future
            FeedbackManager.showToast(this, "Detailed feedback view coming soon", FeedbackManager.FeedbackType.INFO)
        }

        // Clear feedback data
        findViewById<MaterialButton>(R.id.buttonClearFeedback).setOnClickListener {
            feedbackManager.clearAllFeedback()
            setupFeedbackSection()
            FeedbackManager.showToast(this, "Feedback data cleared", FeedbackManager.FeedbackType.SUCCESS)
        }
    }

    private fun loadCurrentPreferences() {
        // Notification types
        findViewById<SwitchMaterial>(R.id.switchProactiveWarnings).isChecked = 
            preferenceManager.isProactiveWarningsEnabled()
        findViewById<SwitchMaterial>(R.id.switchPositiveReinforcement).isChecked = 
            preferenceManager.isPositiveReinforcementEnabled()
        findViewById<SwitchMaterial>(R.id.switchInsightsAnalytics).isChecked = 
            preferenceManager.isInsightsAnalyticsEnabled()
        findViewById<SwitchMaterial>(R.id.switchGamificationUpdates).isChecked = 
            preferenceManager.isGamificationUpdatesEnabled()
        findViewById<SwitchMaterial>(R.id.switchContextualSuggestions).isChecked = 
            preferenceManager.isContextualSuggestionsEnabled()

        // Frequency
        val frequency = preferenceManager.getNotificationFrequency()
        findViewById<Slider>(R.id.sliderNotificationFrequency).value = frequency.toFloat()
        findViewById<TextView>(R.id.textFrequencyValue).text = "$frequency minutes"

        // Silent hours
        val silentHoursEnabled = preferenceManager.isSilentHoursEnabled()
        findViewById<SwitchMaterial>(R.id.switchSilentHours).isChecked = silentHoursEnabled
        updateSilentHoursVisibility(silentHoursEnabled)
        findViewById<NumberPicker>(R.id.pickerSilentStart).value = preferenceManager.getSilentHoursStart()
        findViewById<NumberPicker>(R.id.pickerSilentEnd).value = preferenceManager.getSilentHoursEnd()

        // Preferred window
        findViewById<NumberPicker>(R.id.pickerPreferredStart).value = 
            preferenceManager.getPreferredNotificationWindowStart()
        findViewById<NumberPicker>(R.id.pickerPreferredEnd).value = 
            preferenceManager.getPreferredNotificationWindowEnd()

        // Message categories
        val currentCategories = preferenceManager.getMessageCategories()
        findViewById<LinearLayout>(R.id.containerMessageCategories).let { container ->
            for (i in 0 until container.childCount) {
                val switch = container.getChildAt(i) as? SwitchMaterial
                val category = switch?.text?.toString()?.lowercase()
                if (category != null) {
                    switch.isChecked = currentCategories.contains(category)
                }
            }
        }

        // Learning options
        findViewById<SwitchMaterial>(R.id.switchLearningAdaptation).isChecked = 
            preferenceManager.isLearningAdaptationEnabled()
        findViewById<SwitchMaterial>(R.id.switchFeedbackLearning).isChecked = 
            preferenceManager.isFeedbackLearningEnabled()
    }

    private fun updateSilentHoursVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutSilentHoursPickers).visibility = visibility
    }

    override fun onResume() {
        super.onResume()
        // Refresh feedback stats when returning to activity
        setupFeedbackSection()
    }
}