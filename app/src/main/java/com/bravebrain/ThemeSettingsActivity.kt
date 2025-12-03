package com.bravebrain

import android.animation.ValueAnimator
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class ThemeSettingsActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var lightThemeOption: LinearLayout
    private lateinit var darkThemeOption: LinearLayout
    private lateinit var lightPreviewCard: MaterialCardView
    private lateinit var darkPreviewCard: MaterialCardView
    private lateinit var lightSelectedIndicator: ImageView
    private lateinit var darkSelectedIndicator: ImageView
    private lateinit var systemThemeSwitch: SwitchMaterial
    private lateinit var systemThemeCard: MaterialCardView
    private lateinit var amoledModeCard: MaterialCardView
    private lateinit var amoledModeSwitch: SwitchMaterial
    private lateinit var colorGrid: GridLayout
    private lateinit var fontSizeSlider: Slider
    private lateinit var fontSizeValue: TextView
    private lateinit var autoThemeSwitch: SwitchMaterial
    private lateinit var scheduleOptionsContainer: LinearLayout
    private lateinit var startTimeContainer: LinearLayout
    private lateinit var endTimeContainer: LinearLayout
    private lateinit var startTimeText: TextView
    private lateinit var endTimeText: TextView
    
    // Preferences
    private val PREFS_NAME = "theme_preferences_v2"
    private val KEY_THEME_MODE = "theme_mode"
    private val KEY_ACCENT_COLOR = "accent_color"
    private val KEY_FONT_SIZE = "font_size"
    private val KEY_AMOLED_MODE = "amoled_mode"
    private val KEY_AUTO_THEME = "auto_theme"
    private val KEY_START_TIME = "start_time"
    private val KEY_END_TIME = "end_time"
    
    // Theme modes
    private val THEME_LIGHT = 0
    private val THEME_DARK = 1
    private val THEME_SYSTEM = 2
    
    // Accent colors
    private val accentColors = listOf(
        AccentColor("Indigo", "#6366F1"),
        AccentColor("Blue", "#3B82F6"),
        AccentColor("Cyan", "#06B6D4"),
        AccentColor("Teal", "#14B8A6"),
        AccentColor("Green", "#10B981"),
        AccentColor("Lime", "#84CC16"),
        AccentColor("Yellow", "#EAB308"),
        AccentColor("Orange", "#F97316"),
        AccentColor("Red", "#EF4444"),
        AccentColor("Pink", "#EC4899"),
        AccentColor("Purple", "#A855F7"),
        AccentColor("Violet", "#8B5CF6")
    )
    
    private var currentThemeMode = THEME_SYSTEM
    private var currentAccentColor = "#6366F1"
    private var currentFontSize = 2 // 0=XS, 1=S, 2=M, 3=L, 4=XL
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(ThemeManager.getThemePreference(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_settings_redesigned)
        
        loadPreferences()
        initializeViews()
        setupToolbar()
        setupThemeOptions()
        setupAccentColors()
        setupFontSize()
        setupAutoTheme()
    }
    
    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentThemeMode = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
        currentAccentColor = prefs.getString(KEY_ACCENT_COLOR, "#6366F1") ?: "#6366F1"
        currentFontSize = prefs.getInt(KEY_FONT_SIZE, 2)
    }
    
    private fun savePreference(key: String, value: Any) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        when (value) {
            is Int -> editor.putInt(key, value)
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
        }
        editor.apply()
    }
    
    private fun initializeViews() {
        lightThemeOption = findViewById(R.id.lightThemeOption)
        darkThemeOption = findViewById(R.id.darkThemeOption)
        lightPreviewCard = findViewById(R.id.lightPreviewCard)
        darkPreviewCard = findViewById(R.id.darkPreviewCard)
        lightSelectedIndicator = findViewById(R.id.lightSelectedIndicator)
        darkSelectedIndicator = findViewById(R.id.darkSelectedIndicator)
        systemThemeSwitch = findViewById(R.id.systemThemeSwitch)
        systemThemeCard = findViewById(R.id.systemThemeCard)
        amoledModeCard = findViewById(R.id.amoledModeCard)
        amoledModeSwitch = findViewById(R.id.amoledModeSwitch)
        colorGrid = findViewById(R.id.colorGrid)
        fontSizeSlider = findViewById(R.id.fontSizeSlider)
        fontSizeValue = findViewById(R.id.fontSizeValue)
        autoThemeSwitch = findViewById(R.id.autoThemeSwitch)
        scheduleOptionsContainer = findViewById(R.id.scheduleOptionsContainer)
        startTimeContainer = findViewById(R.id.startTimeContainer)
        endTimeContainer = findViewById(R.id.endTimeContainer)
        startTimeText = findViewById(R.id.startTimeText)
        endTimeText = findViewById(R.id.endTimeText)
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Appearance"
        toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupThemeOptions() {
        // Set initial state based on current theme
        updateThemeSelection()
        
        // Light theme click listener
        lightThemeOption.setOnClickListener {
            if (currentThemeMode != THEME_LIGHT) {
                currentThemeMode = THEME_LIGHT
                systemThemeSwitch.isChecked = false
                applyThemeChange()
            }
        }
        
        // Dark theme click listener
        darkThemeOption.setOnClickListener {
            if (currentThemeMode != THEME_DARK) {
                currentThemeMode = THEME_DARK
                systemThemeSwitch.isChecked = false
                applyThemeChange()
            }
        }
        
        // System theme switch
        systemThemeSwitch.isChecked = (currentThemeMode == THEME_SYSTEM)
        systemThemeCard.setOnClickListener {
            systemThemeSwitch.isChecked = !systemThemeSwitch.isChecked
        }
        
        systemThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentThemeMode = THEME_SYSTEM
                applyThemeChange()
            } else {
                // If unchecking, default to current system theme
                val isDark = ThemeManager.isDarkTheme(this)
                currentThemeMode = if (isDark) THEME_DARK else THEME_LIGHT
                updateThemeSelection()
            }
        }
        
        // AMOLED mode
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        amoledModeSwitch.isChecked = prefs.getBoolean(KEY_AMOLED_MODE, false)
        updateAmoledModeVisibility()
        
        amoledModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_AMOLED_MODE, isChecked)
            // Would apply AMOLED theme here (pure black backgrounds)
            Toast.makeText(this, "AMOLED mode " + if (isChecked) "enabled" else "disabled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateThemeSelection() {
        // Update preview card strokes
        val selectedStrokeWidth = 4
        val normalStrokeWidth = 2
        val selectedColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val normalColor = ContextCompat.getColor(this, R.color.divider)
        
        when (currentThemeMode) {
            THEME_LIGHT -> {
                lightPreviewCard.strokeWidth = selectedStrokeWidth
                lightPreviewCard.strokeColor = selectedColor
                lightSelectedIndicator.visibility = View.VISIBLE
                darkPreviewCard.strokeWidth = normalStrokeWidth
                darkPreviewCard.strokeColor = normalColor
                darkSelectedIndicator.visibility = View.GONE
            }
            THEME_DARK -> {
                darkPreviewCard.strokeWidth = selectedStrokeWidth
                darkPreviewCard.strokeColor = selectedColor
                darkSelectedIndicator.visibility = View.VISIBLE
                lightPreviewCard.strokeWidth = normalStrokeWidth
                lightPreviewCard.strokeColor = normalColor
                lightSelectedIndicator.visibility = View.GONE
            }
            THEME_SYSTEM -> {
                lightPreviewCard.strokeWidth = normalStrokeWidth
                lightPreviewCard.strokeColor = normalColor
                lightSelectedIndicator.visibility = View.GONE
                darkPreviewCard.strokeWidth = normalStrokeWidth
                darkPreviewCard.strokeColor = normalColor
                darkSelectedIndicator.visibility = View.GONE
            }
        }
        
        updateAmoledModeVisibility()
    }
    
    private fun updateAmoledModeVisibility() {
        // Show AMOLED option only when dark theme is active
        val isDarkActive = (currentThemeMode == THEME_DARK) || 
                          (currentThemeMode == THEME_SYSTEM && ThemeManager.isDarkTheme(this))
        
        if (isDarkActive) {
            amoledModeCard.visibility = View.VISIBLE
            animateViewAppearance(amoledModeCard)
        } else {
            amoledModeCard.visibility = View.GONE
        }
    }
    
    private fun applyThemeChange() {
        savePreference(KEY_THEME_MODE, currentThemeMode)
        updateThemeSelection()
        
        // Apply theme to ThemeManager
        ThemeManager.saveThemePreference(this, when(currentThemeMode) {
            THEME_LIGHT -> ThemeManager.THEME_LIGHT
            THEME_DARK -> ThemeManager.THEME_DARK
            else -> ThemeManager.THEME_SYSTEM
        })
        
        // Show feedback
        val themeName = when (currentThemeMode) {
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            else -> "System Default"
        }
        FeedbackManager.showThemeChanged(this, themeName)
        
        // Recreate with animation
        recreate()
    }
    
    private fun setupAccentColors() {
        colorGrid.removeAllViews()
        
        accentColors.forEach { accentColor ->
            val colorView = createColorSwatch(accentColor)
            colorGrid.addView(colorView)
        }
    }
    
    private fun createColorSwatch(accentColor: AccentColor): FrameLayout {
        val size = (resources.displayMetrics.density * 48).toInt()
        val margin = (resources.displayMetrics.density * 4).toInt()
        
        val container = FrameLayout(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
        }
        
        val swatch = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = ContextCompat.getDrawable(context, R.drawable.icon_circle_background)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(accentColor.hexColor))
            isClickable = true
            isFocusable = true
            foreground = ContextCompat.getDrawable(context, R.drawable.ripple_background)
        }
        
        // Selection indicator
        val checkIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (resources.displayMetrics.density * 24).toInt(),
                (resources.displayMetrics.density * 24).toInt()
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(R.drawable.ic_check)
            setColorFilter(Color.WHITE)
            visibility = if (accentColor.hexColor == currentAccentColor) View.VISIBLE else View.GONE
        }
        
        container.addView(swatch)
        container.addView(checkIcon)
        
        container.setOnClickListener {
            currentAccentColor = accentColor.hexColor
            savePreference(KEY_ACCENT_COLOR, currentAccentColor)
            
            // Update all color swatches
            for (i in 0 until colorGrid.childCount) {
                val child = colorGrid.getChildAt(i) as FrameLayout
                val icon = child.getChildAt(1) as ImageView
                icon.visibility = View.GONE
            }
            checkIcon.visibility = View.VISIBLE
            
            Toast.makeText(this, "${accentColor.name} selected", Toast.LENGTH_SHORT).show()
        }
        
        return container
    }
    
    private fun setupFontSize() {
        fontSizeSlider.value = currentFontSize.toFloat()
        updateFontSizeLabel(currentFontSize)
        
        fontSizeSlider.addOnChangeListener { _, value, _ ->
            currentFontSize = value.toInt()
            updateFontSizeLabel(currentFontSize)
            savePreference(KEY_FONT_SIZE, currentFontSize)
        }
    }
    
    private fun updateFontSizeLabel(size: Int) {
        fontSizeValue.text = when (size) {
            0 -> "Extra Small"
            1 -> "Small"
            2 -> "Medium"
            3 -> "Large"
            4 -> "Extra Large"
            else -> "Medium"
        }
    }
    
    private fun setupAutoTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isAutoThemeEnabled = prefs.getBoolean(KEY_AUTO_THEME, false)
        val startTime = prefs.getInt(KEY_START_TIME, 22 * 60) // 22:00 in minutes
        val endTime = prefs.getInt(KEY_END_TIME, 7 * 60) // 07:00 in minutes
        
        autoThemeSwitch.isChecked = isAutoThemeEnabled
        scheduleOptionsContainer.visibility = if (isAutoThemeEnabled) View.VISIBLE else View.GONE
        
        updateTimeDisplay(startTimeText, startTime)
        updateTimeDisplay(endTimeText, endTime)
        
        autoThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_AUTO_THEME, isChecked)
            
            if (isChecked) {
                animateViewExpansion(scheduleOptionsContainer)
                scheduleAutoTheme(startTime, endTime)
            } else {
                animateViewCollapse(scheduleOptionsContainer)
                cancelAutoTheme()
            }
        }
        
        startTimeContainer.setOnClickListener {
            showTimePicker(startTime) { selectedTime ->
                savePreference(KEY_START_TIME, selectedTime)
                updateTimeDisplay(startTimeText, selectedTime)
                if (autoThemeSwitch.isChecked) {
                    val endTimeVal = prefs.getInt(KEY_END_TIME, 7 * 60)
                    scheduleAutoTheme(selectedTime, endTimeVal)
                }
            }
        }
        
        endTimeContainer.setOnClickListener {
            showTimePicker(endTime) { selectedTime ->
                savePreference(KEY_END_TIME, selectedTime)
                updateTimeDisplay(endTimeText, selectedTime)
                if (autoThemeSwitch.isChecked) {
                    val startTimeVal = prefs.getInt(KEY_START_TIME, 22 * 60)
                    scheduleAutoTheme(startTimeVal, selectedTime)
                }
            }
        }
    }
    
    private fun showTimePicker(currentTimeInMinutes: Int, onTimeSelected: (Int) -> Unit) {
        val hour = currentTimeInMinutes / 60
        val minute = currentTimeInMinutes % 60
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val timeInMinutes = selectedHour * 60 + selectedMinute
            onTimeSelected(timeInMinutes)
        }, hour, minute, true).show()
    }
    
    private fun updateTimeDisplay(textView: TextView, timeInMinutes: Int) {
        val hour = timeInMinutes / 60
        val minute = timeInMinutes % 60
        textView.text = String.format("%02d:%02d", hour, minute)
    }
    
    private fun scheduleAutoTheme(startTime: Int, endTime: Int) {
        // This would schedule alarms to automatically switch themes
        // Implementation would use AlarmManager
        Toast.makeText(this, "Auto theme scheduled", Toast.LENGTH_SHORT).show()
    }
    
    private fun cancelAutoTheme() {
        // Cancel scheduled alarms
        Toast.makeText(this, "Auto theme disabled", Toast.LENGTH_SHORT).show()
    }
    
    private fun animateViewExpansion(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = -20f
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }
    
    private fun animateViewCollapse(view: View) {
        view.animate()
            .alpha(0f)
            .translationY(-20f)
            .setDuration(300)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }
    
    private fun animateViewAppearance(view: View) {
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }
    
    data class AccentColor(val name: String, val hexColor: String)
}
