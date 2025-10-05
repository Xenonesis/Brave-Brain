package com.example.testing

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ThemeSettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_settings)
        
        setupToolbar()
        setupThemeOptions()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Theme Settings"
        toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupThemeOptions() {
        val radioGroup = findViewById<RadioGroup>(R.id.themeRadioGroup)
        val currentTheme = ThemeManager.getThemePreference(this)
        
        // Set current selection
        when (currentTheme) {
            ThemeManager.THEME_LIGHT -> radioGroup.check(R.id.radioLight)
            ThemeManager.THEME_DARK -> radioGroup.check(R.id.radioDark)
            ThemeManager.THEME_SYSTEM -> radioGroup.check(R.id.radioSystem)
        }
        
        // Handle theme changes
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.radioLight -> ThemeManager.THEME_LIGHT
                R.id.radioDark -> ThemeManager.THEME_DARK
                R.id.radioSystem -> ThemeManager.THEME_SYSTEM
                else -> ThemeManager.THEME_SYSTEM
            }
            ThemeManager.saveThemePreference(this, newTheme)
        }
    }
}
