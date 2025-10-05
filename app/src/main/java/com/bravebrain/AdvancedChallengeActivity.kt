package com.bravebrain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlin.random.Random

/**
 * Advanced Challenge Activity with multiple challenge types
 */
class AdvancedChallengeActivity : AppCompatActivity() {
    
    private lateinit var challengeType: SmartBlockingEngine.ChallengeType
    private lateinit var packageName: String
    private lateinit var challengeContainer: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var timerText: TextView
    
    private var countDownTimer: CountDownTimer? = null
    private var challengeStartTime: Long = 0
    private var currentMathAnswer: Int = 0
    
    companion object {
        const val EXTRA_CHALLENGE_TYPE = "challenge_type"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_COOLING_OFF_PERIOD = "cooling_off_period"
        const val EXTRA_REASON = "reason"
    }
    
    
    private fun setupUI() {
        challengeContainer = findViewById(R.id.challengeContainer)
        titleText = findViewById(R.id.challengeTitle)
        descriptionText = findViewById(R.id.challengeDescription)
        timerText = findViewById(R.id.timerText)
        
        // Setup back button
        findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            finishChallenge(false)
        }
    }
    
    private fun setupChallenge(coolingOffPeriod: Long, reason: String) {
        when (challengeType) {
            SmartBlockingEngine.ChallengeType.MATH -> setupMathChallenge()
            SmartBlockingEngine.ChallengeType.COMPLEX_MATH -> setupComplexMathChallenge()
            SmartBlockingEngine.ChallengeType.REFLECTION -> setupReflectionChallenge()
            SmartBlockingEngine.ChallengeType.MINDFULNESS -> setupMindfulnessChallenge()
            SmartBlockingEngine.ChallengeType.PHYSICAL -> setupPhysicalChallenge()
            SmartBlockingEngine.ChallengeType.PRODUCTIVITY_QUESTION -> setupProductivityChallenge()
            SmartBlockingEngine.ChallengeType.WAITING -> setupWaitingChallenge(coolingOffPeriod)
            else -> setupMathChallenge()
        }
        
        // Show reason for blocking
        descriptionText.text = reason
    }
    
    private fun setupMathChallenge() {
        titleText.text = "🧮 Math Challenge"
        
        val num1 = Random.nextInt(10, 50)
        val num2 = Random.nextInt(10, 50)
        val operation = Random.nextInt(0, 2) // 0 = addition, 1 = subtraction
        
        val questionText = TextView(this).apply {
            text = if (operation == 0) {
                currentMathAnswer = num1 + num2
                "What is $num1 + $num2?"
            } else {
                currentMathAnswer = num1 - num2
                "What is $num1 - $num2?"
            }
            textSize = 24f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.text_primary))
        }
        
        val answerInput = EditText(this).apply {
            hint = "Enter your answer"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            textSize = 18f
            setPadding(20, 16, 20, 16)
            background = ContextCompat.getDrawable(this@AdvancedChallengeActivity, R.drawable.modern_card_background)
        }
        
        val submitButton = MaterialButton(this).apply {
            text = "Submit Answer"
            setOnClickListener {
                val userAnswer = answerInput.text.toString().toIntOrNull()
                if (userAnswer == currentMathAnswer) {
                    showSuccessMessage("Correct! Well done.")
                    finishChallenge(true)
                } else {
                    showErrorMessage("Incorrect. Try again.")
                    answerInput.text.clear()
                }
            }
        }
        
        challengeContainer.addView(questionText)
        challengeContainer.addView(answerInput)
        challengeContainer.addView(submitButton)
    }
    
    private fun setupComplexMathChallenge() {
        titleText.text = "🔢 Complex Math Challenge"
        
        val num1 = Random.nextInt(15, 99)
        val num2 = Random.nextInt(15, 99)
        val num3 = Random.nextInt(5, 15)
        
        currentMathAnswer = (num1 * num2) / num3
        
        val questionText = TextView(this).apply {
            text = "What is ($num1 × $num2) ÷ $num3?\n(Round down to nearest integer)"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.text_primary))
        }
        
        val answerInput = EditText(this).apply {
            hint = "Enter your answer"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textSize = 18f
            setPadding(20, 16, 20, 16)
            background = ContextCompat.getDrawable(this@AdvancedChallengeActivity, R.drawable.modern_card_background)
        }
        
        val submitButton = MaterialButton(this).apply {
            text = "Submit Answer"
            setOnClickListener {
                val userAnswer = answerInput.text.toString().toIntOrNull()
                if (userAnswer == currentMathAnswer) {
                    showSuccessMessage("Excellent! Complex math solved.")
                    finishChallenge(true)
                } else {
                    showErrorMessage("Incorrect. Think carefully and try again.")
                    answerInput.text.clear()
                }
            }
        }
        
        challengeContainer.addView(questionText)
        challengeContainer.addView(answerInput)
        challengeContainer.addView(submitButton)
    }
    
    private fun setupReflectionChallenge() {
        titleText.text = "🤔 Reflection Challenge"
        
        val questions = listOf(
            "Why do you want to use this app right now?",
            "What productive activity could you do instead?",
            "How will you feel after spending more time on this app?",
            "What are your goals for today?",
            "Is this app helping you achieve your goals?"
        )
        
        val selectedQuestion = questions.random()
        
        val questionText = TextView(this).apply {
            text = selectedQuestion
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.text_primary))
        }
        
        val answerInput = EditText(this).apply {
            hint = "Type your thoughtful response (minimum 50 characters)"
            minLines = 3
            maxLines = 5
            textSize = 16f
            setPadding(20, 16, 20, 16)
            background = ContextCompat.getDrawable(this@AdvancedChallengeActivity, R.drawable.modern_card_background)
        }
        
        val submitButton = MaterialButton(this).apply {
            text = "Submit Reflection"
            setOnClickListener {
                val response = answerInput.text.toString().trim()
                if (response.length >= 50) {
                    showSuccessMessage("Thank you for your thoughtful reflection.")
                    saveReflection(selectedQuestion, response)
                    finishChallenge(true)
                } else {
                    showErrorMessage("Please provide a more detailed response (at least 50 characters).")
                }
            }
        }
        
        challengeContainer.addView(questionText)
        challengeContainer.addView(answerInput)
        challengeContainer.addView(submitButton)
    }
    
    private fun setupMindfulnessChallenge() {
        titleText.text = "🧘 Mindfulness Challenge"
        
        val instructionText = TextView(this).apply {
            text = "Take a moment to practice mindfulness.\n\nClose your eyes and take 5 deep breaths.\nFocus on your breathing and be present in this moment."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.text_primary))
        }
        
        val breathingGuide = TextView(this).apply {
            text = "Breathe in... Breathe out..."
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.primary_blue))
        }
        
        val completeButton = MaterialButton(this).apply {
            text = "I've completed my mindful breathing"
            setOnClickListener {
                showSuccessMessage("Great! You've taken a mindful moment.")
                finishChallenge(true)
            }
        }
        
        challengeContainer.addView(instructionText)
        challengeContainer.addView(breathingGuide)
        challengeContainer.addView(completeButton)
        
        // Start breathing animation
        startBreathingAnimation(breathingGuide)
    }
    
    private fun setupPhysicalChallenge() {
        titleText.text = "🏃 Physical Challenge"
        
        val exercises = listOf(
            "Do 10 jumping jacks",
            "Do 5 push-ups",
            "Stretch your arms above your head for 30 seconds",
            "Do 10 squats",
            "Walk around for 2 minutes"
        )
        
        val selectedExercise = exercises.random()
        
        val exerciseText = TextView(this).apply {
            text = "Complete this physical activity:\n\n$selectedExercise"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.text_primary))
        }
        
        val completeButton = MaterialButton(this).apply {
            text = "I've completed the exercise"
            setOnClickListener {
                showSuccessMessage("Excellent! Physical activity completed.")
                finishChallenge(true)
            }
        }
        
        challengeContainer.addView(exerciseText)
        challengeContainer.addView(completeButton)
    }
    
    private fun setupProductivityChallenge() {
        titleText.text = "💼 Productivity Challenge"
        
        val questionText = TextView(this).apply {
            text = "Before accessing this app, complete one productive task:\n\n• Reply to an important email\n• Clean your workspace\n• Review your daily goals\n• Make a to-do list\n• Drink a glass of water"
            textSize = 16f
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.text_primary))
        }
        
        val taskInput = EditText(this).apply {
            hint = "What productive task did you complete?"
            minLines = 2
            textSize = 16f
            setPadding(20, 16, 20, 16)
            background = ContextCompat.getDrawable(this@AdvancedChallengeActivity, R.drawable.modern_card_background)
        }
        
        val submitButton = MaterialButton(this).apply {
            text = "I've completed a productive task"
            setOnClickListener {
                val task = taskInput.text.toString().trim()
                if (task.length >= 10) {
                    showSuccessMessage("Great! Productivity first.")
                    saveProductiveTask(task)
                    finishChallenge(true)
                } else {
                    showErrorMessage("Please describe what you accomplished.")
                }
            }
        }
        
        challengeContainer.addView(questionText)
        challengeContainer.addView(taskInput)
        challengeContainer.addView(submitButton)
    }
    
    private fun setupWaitingChallenge(coolingOffPeriod: Long) {
        titleText.text = "⏰ Cooling Off Period"
        
        val waitText = TextView(this).apply {
            text = "You need to wait before accessing this app again.\n\nUse this time to reflect on your digital habits."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(ContextCompat.getColor(this@AdvancedChallengeActivity, R.color.text_primary))
        }
        
        challengeContainer.addView(waitText)
        
        // Start countdown timer
        if (coolingOffPeriod > 0) {
            startCoolingOffTimer(coolingOffPeriod)
        } else {
            // Default 5 minute cooling off
            startCoolingOffTimer(5 * 60 * 1000L)
        }
    }
    
    private fun startCoolingOffTimer(duration: Long) {
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                timerText.text = String.format("%02d:%02d", minutes, seconds)
            }
            
            override fun onFinish() {
                timerText.text = "Time's up!"
                showSuccessMessage("Cooling off period completed.")
                finishChallenge(true)
            }
        }.start()
    }
    
    private fun startBreathingAnimation(textView: TextView) {
        val breathingTexts = arrayOf("Breathe in...", "Hold...", "Breathe out...", "Hold...")
        var currentIndex = 0
        
        val timer = object : CountDownTimer(20000, 2000) { // 20 seconds total, 2 second intervals
            override fun onTick(millisUntilFinished: Long) {
                textView.text = breathingTexts[currentIndex % breathingTexts.size]
                currentIndex++
            }
            
            override fun onFinish() {
                textView.text = "Well done! 🧘"
            }
        }
        timer.start()
    }
    
    private fun showSuccessMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun saveReflection(question: String, response: String) {
        val prefs = getSharedPreferences("reflections", Context.MODE_PRIVATE)
        val timestamp = System.currentTimeMillis()
        prefs.edit().putString("reflection_$timestamp", "$question|$response").apply()
    }
    
    private fun saveProductiveTask(task: String) {
        val prefs = getSharedPreferences("productive_tasks", Context.MODE_PRIVATE)
        val timestamp = System.currentTimeMillis()
        prefs.edit().putString("task_$timestamp", task).apply()
    }
    
    private fun finishChallenge(success: Boolean) {
        countDownTimer?.cancel()
        
        // Record challenge completion
        val prefs = getSharedPreferences("analytics_data", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val completedToday = prefs.getInt("challenges_completed_$today", 0)
        
        if (success) {
            prefs.edit().putInt("challenges_completed_$today", completedToday + 1).apply()
            
            // Record challenge duration
            val duration = System.currentTimeMillis() - challengeStartTime
            prefs.edit().putLong("last_challenge_duration", duration).apply()
        }
        
        val resultIntent = Intent().apply {
            putExtra("challenge_completed", success)
            putExtra("challenge_type", challengeType.name)
        }
        setResult(if (success) RESULT_OK else RESULT_CANCELED, resultIntent)
        finish()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_challenge)
        
        // Setup modern back press handling
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishChallenge(false)
            }
        })
        
        // Get challenge parameters
        val challengeTypeString = intent.getStringExtra(EXTRA_CHALLENGE_TYPE) ?: "MATH"
        challengeType = SmartBlockingEngine.ChallengeType.valueOf(challengeTypeString)
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val coolingOffPeriod = intent.getLongExtra(EXTRA_COOLING_OFF_PERIOD, 0L)
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Time limit exceeded"
        
        setupUI()
        setupChallenge(coolingOffPeriod, reason)
        
        challengeStartTime = System.currentTimeMillis()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}