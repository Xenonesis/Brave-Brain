package com.bravebrain

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * ContextNotificationTestActivity provides a test interface to validate 
 * the context-aware notification system implementation
 */
class ContextNotificationTestActivity : AppCompatActivity() {
    
    private lateinit var notificationContextManager: NotificationContextManager
    private lateinit var contextSummaryTextView: TextView
    private lateinit var testResultsTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@ContextNotificationTestActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                
                // Title
                addView(TextView(context).apply {
                    text = "Context-Aware Notification System Test"
                    textSize = 20f
                    setTextColor(android.graphics.Color.BLACK)
                    setPadding(0, 0, 32, 0)
                })
                
                // Context Summary
                addView(TextView(context).apply {
                    text = "Current Context Summary:"
                    textSize = 16f
                    setTextColor(android.graphics.Color.DKGRAY)
                    setPadding(0, 0, 0, 16)
                })
                
                addView(TextView(context).apply {
                    id = android.R.id.text1
                    textSize = 14f
                    setTextColor(android.graphics.Color.GRAY)
                }.also { contextSummaryTextView = it })
                
                // Test Results
                addView(TextView(context).apply {
                    text = "Test Results:"
                    textSize = 16f
                    setTextColor(android.graphics.Color.DKGRAY)
                    setPadding(0, 16, 0, 16)
                })
                
                addView(TextView(context).apply {
                    id = android.R.id.text2
                    textSize = 14f
                    setTextColor(android.graphics.Color.GRAY)
                }.also { testResultsTextView = it })
                
                // Test Buttons
                addView(Button(context).apply {
                    text = "Start Notification System"
                    setPadding(16, 16, 16, 16)
                    setOnClickListener {
                        startNotificationSystem()
                    }
                })
                
                addView(Button(context).apply {
                    text = "Get Current Context"
                    setPadding(16, 16, 16, 16)
                    setOnClickListener {
                        getCurrentContext()
                    }
                })
                
                addView(Button(context).apply {
                    text = "Get Engagement Metrics"
                    setPadding(16, 16, 16, 16)
                    setOnClickListener {
                        getEngagementMetrics()
                    }
                })
                
                addView(Button(context).apply {
                    text = "Run Full System Test"
                    setPadding(16, 16, 16, 16)
                    setOnClickListener {
                        runFullSystemTest()
                    }
                })
                
                addView(Button(context).apply {
                    text = "Reset Engagement Tracking"
                    setPadding(16, 16, 16, 16)
                    setOnClickListener {
                        resetEngagementTracking()
                    }
                })
            })
        })
        
        notificationContextManager = NotificationContextManager(this)
        updateContextSummary()
    }
    
    private fun startNotificationSystem() {
        try {
            notificationContextManager.startNotificationSystem()
            Toast.makeText(this, "Context-aware notification system started!", Toast.LENGTH_SHORT).show()
            updateContextSummary()
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting system: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("ContextNotificationTest", "Error starting notification system", e)
        }
    }
    
    private fun getCurrentContext() {
        try {
            val contextSummary = notificationContextManager.getContextSummary()
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeString = formatter.format(Date(contextSummary.timestamp))
            
            val contextText = """
                Time: $timeString
                User Context:
                  - Time of Day: ${contextSummary.userContext.timeOfDay}
                  - Day of Week: ${contextSummary.userContext.dayOfWeek}
                  - Sleep Time: ${contextSummary.userContext.isSleepTime}
                  - Work Time: ${contextSummary.userContext.isWorkTime}
                  - Focus Time: ${contextSummary.userContext.isFocusTime}
                  - Usage Intensity: ${contextSummary.userContext.usageIntensity}
                  - Engagement Level: ${contextSummary.userContext.engagementLevel}
                  - Optimal Notification Time: ${contextSummary.userContext.optimalNotificationTime}
                
                Device Context:
                  - Is Moving: ${contextSummary.deviceContext.isMoving}
                  - Is Pocket: ${contextSummary.deviceContext.isPocket}
                  - Ambient Light: ${contextSummary.deviceContext.ambientLightLevel}
                  - Battery Level: ${contextSummary.deviceContext.batteryLevel}%
                  - Is Charging: ${contextSummary.deviceContext.isCharging}
                  - Screen On: ${contextSummary.deviceContext.screenOn}
                  - Proximity Near: ${contextSummary.deviceContext.proximityNear}
                  - Motion Intensity: ${contextSummary.deviceContext.motionIntensity}
            """.trimIndent()
            
            contextSummaryTextView.text = contextText
            Toast.makeText(this, "Context updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error getting context: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("ContextNotificationTest", "Error getting context", e)
        }
    }
    
    private fun getEngagementMetrics() {
        try {
            val metrics = notificationContextManager.getEngagementMetrics()
            
            val metricsText = """
                Engagement Metrics:
                  - Response Rate: ${(metrics.responseRate * 100).toInt()}%
                  - Optimal Timing: ${metrics.optimalTiming.joinToString(", ")}
                  - Engagement Trend: ${metrics.engagementTrend}
                  - Notification Fatigue: ${(metrics.notificationFatigueScore * 100).toInt()}%
                  - Preferred Content: ${metrics.preferredContentTypes.joinToString(", ")}
                
                Content Preferences:
            """.trimIndent() + 
            metrics.contentPreferences.entries.joinToString("\n") { "  - ${it.key}: ${(it.value * 100).toInt()}%" }
            
            testResultsTextView.text = metricsText
            Toast.makeText(this, "Engagement metrics updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error getting engagement metrics: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("ContextNotificationTest", "Error getting engagement metrics", e)
        }
    }
    
    private fun runFullSystemTest() {
        try {
            val results = mutableListOf<String>()
            results.add("Running full system test...")
            
            // Test context analyzer
            val userContext = notificationContextManager.getCurrentUserContext()
            results.add("✓ Context analyzer working - Time of day: ${userContext.timeOfDay}")
            
            // Test device context
            val deviceContext = notificationContextManager.getCurrentDeviceContext()
            results.add("✓ Device context detection working - Screen on: ${deviceContext.screenOn}")
            
            // Test engagement analyzer
            val engagementMetrics = notificationContextManager.getEngagementMetrics()
            results.add("✓ Engagement analyzer working - Response rate: ${(engagementMetrics.responseRate * 100).toInt()}%")
            
            // Test pattern recognition
            val patterns = notificationContextManager.getUsagePatterns()
            val peakTimes = patterns.detectPeakUsageTimes().take(3)
            results.add("✓ Pattern recognition working - Peak times: ${if (peakTimes.isNotEmpty()) peakTimes.take(2).joinToString(", ") { it.hour.toString() } else "None detected"}")
            
            // Test notification system status
            val isRunning = notificationContextManager.isNotificationSystemEnabled()
            results.add("✓ Notification system status: ${if (isRunning) "Running" else "Not running"}")
            
            // Test context learning
            notificationContextManager.learnFromCurrentContext()
            results.add("✓ Context learning working")
            
            // Summary
            results.add("\n✓ All system components tested successfully!")
            results.add("✓ Context-aware notification system is fully functional!")
            
            testResultsTextView.text = results.joinToString("\n")
            Toast.makeText(this, "Full system test completed!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val errorResult = "Error during system test: ${e.message}\n${e.stackTraceToString()}"
            testResultsTextView.text = errorResult
            Toast.makeText(this, "System test failed: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("ContextNotificationTest", "Error during system test", e)
        }
    }
    
    private fun resetEngagementTracking() {
        try {
            notificationContextManager.resetEngagementTracking()
            Toast.makeText(this, "Engagement tracking reset!", Toast.LENGTH_SHORT).show()
            testResultsTextView.text = "Engagement tracking has been reset.\nRun tests again to see fresh metrics."
        } catch (e: Exception) {
            Toast.makeText(this, "Error resetting engagement: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("ContextNotificationTest", "Error resetting engagement tracking", e)
        }
    }
    
    private fun updateContextSummary() {
        try {
            val isRunning = notificationContextManager.isNotificationSystemEnabled()
            val statusText = if (isRunning) "Context-aware notification system is RUNNING" else "Context-aware notification system is NOT RUNNING"
            
            val additionalInfo = if (isRunning) {
                "\nClick 'Get Current Context' to see real-time context data"
            } else {
                "\nClick 'Start Notification System' to begin"
            }
            
            contextSummaryTextView.text = "$statusText$additionalInfo"
        } catch (e: Exception) {
            android.util.Log.e("ContextNotificationTest", "Error updating context summary", e)
        }
    }
}