package com.andrenormanlang.quizapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Updated Question data class for Multiple Choice
data class Question(
        val questionText: String,
        val options: List<String>,
        val correctAnswerIndex:
                Int // Index of the correct answer in the options list (0, 1, 2, or 3)
)

class MainActivity : AppCompatActivity() {
    private lateinit var questionTextView: TextView
    private lateinit var optionButtonA: Button
    private lateinit var optionButtonB: Button
    private lateinit var optionButtonC: Button
    private lateinit var optionButtonD: Button
    private lateinit var scoreTextView: TextView
    private lateinit var questionCounterTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var newTopicButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var geminiApiService: GeminiApiService

    private var questions = mutableListOf<Question>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var questionsAnswered = 0
    private lateinit var allOptionButtons: List<Button>
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        geminiApiService = GeminiApiService()

        initializeViews()
        setupClickListeners()
        updateUI()
        displayHighScore()

        // Show topic selection dialog on startup
        showTopicSelectionDialog()
    }

    private fun initializeViews() {
        questionTextView = findViewById(R.id.question_text)
        optionButtonA = findViewById(R.id.option_button_a)
        optionButtonB = findViewById(R.id.option_button_b)
        optionButtonC = findViewById(R.id.option_button_c)
        optionButtonD = findViewById(R.id.option_button_d)
        scoreTextView = findViewById(R.id.score_text)
        questionCounterTextView = findViewById(R.id.question_counter)
        highScoreTextView = findViewById(R.id.high_score_text)
        newTopicButton = findViewById(R.id.new_topic_button)

        // Group all option buttons
        allOptionButtons = listOf(optionButtonA, optionButtonB, optionButtonC, optionButtonD)
    }

    private fun setupClickListeners() {
        // Set up click listeners for all option buttons
        allOptionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (!isLoading) {
                    checkAnswer(index) // Pass the index of the clicked button
                }
            }
        }

        // Set up click listener for change topic button
        newTopicButton.setOnClickListener {
            if (!isLoading) {
                showTopicSelectionDialog()
            }
        }
    }

    private fun showTopicSelectionDialog() {
        val topics =
                arrayOf(
                        "Fullstack Development (General)",
                        "Frontend (HTML, CSS & JavaScript)",
                        "Frontend Frameworks (React, Next.js & Vue)",
                        "Backend Development (Node.js, Express & APIs)",
                        "Databases (SQL, NoSQL, MongoDB & PostgreSQL)",
                        "REST APIs & GraphQL",
                        "Authentication & Security",
                        "DevOps & Deployment (Docker, CI/CD & Cloud)",
                        "System Design & Architecture",
                        "TypeScript & Modern JavaScript",
                        "Testing & QA (Unit, Integration & E2E)",
                        "C# & .NET (ASP.NET Core, EF Core & Blazor)",
                        ".NET Backend (Web API, MVC & Middleware)",
                        "Generate AI Questions (Mixed Fullstack Topics)"
                )

        AlertDialog.Builder(this)
                .setTitle("Choose Quiz Topic")
                .setItems(topics) { _, which ->
                    val selectedTopic =
                            when (which) {
                                0 -> "Fullstack Development"
                                1 -> "Frontend Development including HTML, CSS, and JavaScript"
                                2 -> "Frontend Frameworks including React, Next.js, and Vue.js"
                                3 ->
                                        "Backend Development including Node.js, Express, and server-side APIs"
                                4 -> "Databases including SQL, NoSQL, MongoDB, and PostgreSQL"
                                5 -> "REST APIs and GraphQL for fullstack applications"
                                6 -> "Authentication, Authorization, and Web Security"
                                7 ->
                                        "DevOps, Deployment, Docker, CI/CD pipelines, and Cloud services"
                                8 -> "System Design and Software Architecture for web applications"
                                9 -> "TypeScript and Modern JavaScript (ES6+)"
                                10 ->
                                        "Software Testing including Unit, Integration, and End-to-End testing"
                                11 ->
                                        "C# and .NET including ASP.NET Core, Entity Framework Core, Blazor, and LINQ"
                                12 ->
                                        ".NET Backend Development including Web API, MVC, middleware, dependency injection, and SignalR"
                                13 ->
                                        "Fullstack Web Development (mixed topics across the entire stack including C# .NET)"
                                else -> "Fullstack Development"
                            }
                    loadQuestionsForTopic(selectedTopic)
                }
                .setCancelable(false)
                .show()
    }

    private fun loadQuestionsForTopic(topic: String) {
        isLoading = true
        showLoadingState()

        lifecycleScope.launch {
            try {
                questions = geminiApiService.generateQuestions(topic, 15).toMutableList()

                if (questions.isNotEmpty()) {
                    questions.shuffle()
                    currentQuestionIndex = 0
                    score = 0
                    questionsAnswered = 0

                    runOnUiThread {
                        hideLoadingState()
                        updateQuestion()
                        updateUI()
                        setButtonsEnabled(true)
                        isLoading = false
                    }
                } else {
                    runOnUiThread {
                        hideLoadingState()
                        showError(
                                "AI generated an empty question list. Please try a different topic."
                        )
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hideLoadingState()
                    showError(
                            "Failed to generate AI questions: ${e.message}\n\nPlease check your internet connection and try again."
                    )
                    isLoading = false
                }
            }
        }
    }

    private fun showLoadingState() {
        questionTextView.text =
                "🤖 Generating AI questions...\n\nPlease wait while we create personalized questions for you using Google Gemini AI."
        allOptionButtons.forEach { button ->
            button.text = "Loading..."
            button.isEnabled = false
        }
        questionCounterTextView.text = "Preparing Quiz..."
    }

    private fun hideLoadingState() {
        setButtonsEnabled(true)
    }

    private fun checkAnswer(userAnswerIndex: Int) {
        if (questions.isEmpty() || isLoading) return

        val currentQuestion = questions[currentQuestionIndex]
        val correctAnswerIndex = currentQuestion.correctAnswerIndex
        questionsAnswered++

        // Disable buttons to prevent multiple clicks before moving on
        setButtonsEnabled(false)

        if (userAnswerIndex == correctAnswerIndex) {
            score += 10
            Toast.makeText(this, "Correct! +10 points", Toast.LENGTH_SHORT).show()
            // Highlight correct answer in green
            allOptionButtons[userAnswerIndex].setBackgroundColor(
                    resources.getColor(android.R.color.holo_green_light, null)
            )
        } else {
            Toast.makeText(
                            this,
                            "Incorrect! The correct answer was: '${currentQuestion.options[correctAnswerIndex]}'",
                            Toast.LENGTH_LONG
                    )
                    .show()
            // Highlight wrong answer in red and correct answer in green
            allOptionButtons[userAnswerIndex].setBackgroundColor(
                    resources.getColor(android.R.color.holo_red_light, null)
            )
            allOptionButtons[correctAnswerIndex].setBackgroundColor(
                    resources.getColor(android.R.color.holo_green_light, null)
            )
        }

        // Add a delay before moving to the next question
        questionTextView.postDelayed(
                {
                    if (questionsAnswered < questions.size) {
                        currentQuestionIndex++ // Move to the next question
                        updateQuestion()
                        updateUI()
                        setButtonsEnabled(true) // Re-enable buttons for the next question
                    } else {
                        endQuiz()
                    }
                },
                2000
        ) // 2 second delay to see the answer feedback
    }

    private fun updateQuestion() {
        if (questions.isEmpty()) return

        val currentQuestion = questions[currentQuestionIndex]
        questionTextView.text = currentQuestion.questionText
        allOptionButtons.forEachIndexed { index, button ->
            button.text = currentQuestion.options[index]
            // Reset button background color
            button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_bright, null))
        }
    }

    private fun updateUI() {
        scoreTextView.text = "Score: $score"
        if (questions.isNotEmpty()) {
            questionCounterTextView.text = "Question: ${questionsAnswered + 1}/${questions.size}"
            if (questionsAnswered == questions.size) {
                questionCounterTextView.text = "Question: ${questions.size}/${questions.size}"
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        allOptionButtons.forEach { it.isEnabled = enabled }
    }

    private fun displayHighScore() {
        val highScore = sharedPreferences.getInt("high_score", 0)
        highScoreTextView.text = "High Score: $highScore"
    }

    private fun endQuiz() {
        val highScore = sharedPreferences.getInt("high_score", 0)
        var message = "Quiz Complete!\n\nFinal Score: $score points\n"

        if (score > highScore) {
            sharedPreferences.edit().putInt("high_score", score).apply()
            message += "🎉 NEW HIGH SCORE! 🎉"
            highScoreTextView.text = "High Score: $score"
        } else {
            message += "High Score: $highScore"
        }

        val percentage =
                if (questions.isNotEmpty()) {
                    (score.toFloat() / (questions.size * 10) * 100).toInt()
                } else {
                    0
                }
        message += "\nAccuracy: $percentage%"

        AlertDialog.Builder(this)
                .setTitle("Quiz Results")
                .setMessage(message)
                .setPositiveButton("New Topic") { _, _ -> showTopicSelectionDialog() }
                .setNegativeButton("Same Topic") { _, _ -> resetQuiz() }
                .setNeutralButton("Exit") { _, _ -> finish() }
                .setCancelable(false)
                .show()
    }

    private fun showError(errorMessage: String) {
        AlertDialog.Builder(this)
                .setTitle("AI Quiz Error")
                .setMessage(errorMessage)
                .setPositiveButton("Try Different Topic") { _, _ -> showTopicSelectionDialog() }
                .setNegativeButton("Exit") { _, _ -> finish() }
                .setCancelable(false)
                .show()
    }

    private fun resetQuiz() {
        if (questions.isNotEmpty()) {
            currentQuestionIndex = 0
            score = 0
            questionsAnswered = 0
            questions.shuffle() // Shuffle questions for a new game
            updateQuestion()
            updateUI()
            setButtonsEnabled(true) // Ensure buttons are enabled for new quiz
        } else {
            showTopicSelectionDialog()
        }
    }
}
