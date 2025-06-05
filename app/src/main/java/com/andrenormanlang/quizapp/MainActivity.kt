package com.andrenormanlang.quizapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// Updated Question data class for Multiple Choice
data class Question(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int // Index of the correct answer in the options list (0, 1, 2, or 3)
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

    private lateinit var sharedPreferences: SharedPreferences

    // --- YOUR NEW FRONTEND DEVELOPER QUESTIONS ---
    private var questions = mutableListOf( // Changed to mutableListOf for shuffling
        Question(
            "Which HTML tag is used to define the root of an HTML document?",
            listOf("<body>", "<head>", "<html>", "<!DOCTYPE html>"), 2
        ),
        Question(
            "What does CSS stand for?",
            listOf("Creative Style Sheets", "Cascading Style Sheets", "Computer Style Sheets", "Colorful Style Sheets"), 1
        ),
        Question(
            "Which JavaScript keyword is used to declare a variable that cannot be reassigned?",
            listOf("var", "let", "const", "static"), 2
        ),
        Question(
            "What is the box model in CSS?",
            listOf("A way to arrange elements in a grid", "A system for responsive design", "A conceptual model for how elements are rendered and displayed", "A method for creating 3D effects"), 2
        ),
        Question(
            "What is the purpose of the `useEffect` hook in React?",
            listOf("To manage component state", "To perform side effects in functional components", "To handle user input", "To create a custom hook"), 1
        ),
        Question(
            "Which CSS property controls the spacing between lines of text?",
            listOf("line-spacing", "text-height", "line-height", "vertical-align"), 2
        ),
        Question(
            "What is 'hoisting' in JavaScript?",
            listOf("A way to lift elements in the DOM", "The process of moving variable and function declarations to the top of their scope", "A technique for optimizing image loading", "A type of asynchronous operation"), 1
        ),
        Question(
            "Which of these is NOT a CSS preprocessor?",
            listOf("Sass", "Less", "Stylus", "TypeScript"), 3
        ),
        Question(
            "What is the main advantage of using a CSS framework like Bootstrap or Tailwind CSS?",
            listOf("They make your website faster", "They provide pre-built components and utility classes for faster development", "They automatically optimize images", "They add server-side rendering capabilities"), 1
        ),
        Question(
            "In JavaScript, what does `NaN` stand for?",
            listOf("Not a Number", "New and Null", "No Available Name", "Numerical Annotation Node"), 0
        ),
        Question(
            "Which of the following is an example of a semantic HTML tag?",
            listOf("<div>", "<span>", "<section>", "<b>"), 2
        ),
        Question(
            "What is the purpose of Webpack?",
            listOf("A CSS framework", "A JavaScript library for DOM manipulation", "A module bundler for JavaScript applications", "A database management system"), 2
        ),
        Question(
            "What does CORS stand for?",
            listOf("Cross-Origin Resource Sharing", "Client-Oriented Rendering System", "Content Object Retrieval Service", "Cascading Object Relational Styles"), 0
        ),
        Question(
            "Which method is used to add an event listener in JavaScript?",
            listOf("attachEvent()", "addEventListener()", "onEvent()", "listenEvent()"), 1
        ),
        Question(
            "What is the key difference between `==` and `===` in JavaScript?",
            listOf("`==` compares value and type, `===` only compares value", "`==` performs type coercion, `===` does not", "They are identical in modern JavaScript", "`==` is for numbers, `===` is for strings"), 1
        )
    )

    private var currentQuestionIndex = 0
    private var score = 0
    private var questionsAnswered = 0
    private lateinit var allOptionButtons: List<Button> // To easily manage all option buttons

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)

        questionTextView = findViewById(R.id.question_text)
        optionButtonA = findViewById(R.id.option_button_a)
        optionButtonB = findViewById(R.id.option_button_b)
        optionButtonC = findViewById(R.id.option_button_c)
        optionButtonD = findViewById(R.id.option_button_d)
        scoreTextView = findViewById(R.id.score_text)
        questionCounterTextView = findViewById(R.id.question_counter)
        highScoreTextView = findViewById(R.id.high_score_text)

        // Group all option buttons
        allOptionButtons = listOf(optionButtonA, optionButtonB, optionButtonC, optionButtonD)

        // Set up click listeners for all option buttons
        allOptionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                checkAnswer(index) // Pass the index of the clicked button
            }
        }

        // Shuffle questions when the quiz starts
        questions.shuffle()

        updateQuestion()
        updateUI()
        displayHighScore()
    }

    private fun checkAnswer(userAnswerIndex: Int) {
        val currentQuestion = questions[currentQuestionIndex]
        val correctAnswerIndex = currentQuestion.correctAnswerIndex
        questionsAnswered++

        // Disable buttons to prevent multiple clicks before moving on
        setButtonsEnabled(false)

        if (userAnswerIndex == correctAnswerIndex) {
            score += 10
            Toast.makeText(this, "Correct! +10 points", Toast.LENGTH_SHORT).show()
            // Optional: Visually indicate correct answer (e.g., change button color)
            allOptionButtons[userAnswerIndex].setBackgroundResource(android.R.drawable.btn_default) // Reset color (default)
            // You might want a custom drawable here to show green for correct
            // For now, let's keep it simple and just show toast
        } else {
            Toast.makeText(this, "Incorrect! The correct answer was: '${currentQuestion.options[correctAnswerIndex]}'", Toast.LENGTH_LONG).show()
            // Optional: Visually indicate incorrect and correct answers
            // allOptionButtons[userAnswerIndex].setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
            // allOptionButtons[correctAnswerIndex].setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
        }

        // Add a slight delay before moving to the next question
        questionTextView.postDelayed({
            if (questionsAnswered < questions.size) {
                currentQuestionIndex++ // Move to the next question
                updateQuestion()
                updateUI()
                setButtonsEnabled(true) // Re-enable buttons for the next question
            } else {
                endQuiz()
            }
        }, 1000) // 1 second delay
    }

    private fun updateQuestion() {
        val currentQuestion = questions[currentQuestionIndex]
        questionTextView.text = currentQuestion.questionText
        allOptionButtons.forEachIndexed { index, button ->
            button.text = currentQuestion.options[index]
            button.setBackgroundResource(android.R.drawable.btn_default) // Reset button background
        }
    }

    private fun updateUI() {
        scoreTextView.text = "Score: $score"
        questionCounterTextView.text = "Question: ${questionsAnswered + 1}/${questions.size}"
        if (questionsAnswered == questions.size) { // Fix for final question counter
            questionCounterTextView.text = "Question: ${questions.size}/${questions.size}"
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

        val percentage = (score.toFloat() / (questions.size * 10) * 100).toInt()
        message += "\nAccuracy: $percentage%"

        AlertDialog.Builder(this)
            .setTitle("Quiz Results")
            .setMessage(message)
            .setPositiveButton("Play Again") { _, _ ->
                resetQuiz()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun resetQuiz() {
        currentQuestionIndex = 0
        score = 0
        questionsAnswered = 0
        questions.shuffle() // Shuffle questions for a new game
        updateQuestion()
        updateUI()
        setButtonsEnabled(true) // Ensure buttons are enabled for new quiz
    }
}