package com.andrenormanlang.quizapp

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

// Updated Question data class for Multiple Choice
data class Question(
        val questionText: String,
        val options: List<String>,
        val correctAnswerIndex:
                Int, // Index of the correct answer in the options list (0, 1, 2, or 3)
        val explanation: String = "" // Why the correct answer is right
)

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val TOTAL_QUESTIONS = 15
        private const val PARALLEL_BATCHES = 3
        private const val POINTS_PER_QUESTION = 10
        private const val NEXT_DELAY_CORRECT_MS = 1200L
        private const val NEXT_DELAY_WRONG_MS = 2400L
    }

    private data class QuizTopic(val label: String, val prompt: String)

    private val quizTopics =
            listOf(
                    QuizTopic(
                            "Fullstack Development (General)",
                            "Fullstack Web Development covering frontend, backend, databases, and deployment"
                    ),
                    QuizTopic(
                            "Frontend (HTML, CSS & JavaScript)",
                            "Frontend Development with HTML, CSS, and JavaScript (DOM, browser APIs, layout, styling)"
                    ),
                    QuizTopic(
                            "Frontend Frameworks (React, Next.js & Vue)",
                            "Frontend Frameworks: React, Next.js, and Vue.js"
                    ),
                    QuizTopic(
                            "Backend (Node.js, Express & APIs)",
                            "Backend Development with Node.js, Express, and server-side APIs"
                    ),
                    QuizTopic(
                            "Databases (SQL, NoSQL, MongoDB & PostgreSQL)",
                            "Databases: SQL, NoSQL, MongoDB, and PostgreSQL"
                    ),
                    QuizTopic(
                            "REST APIs & GraphQL",
                            "REST API design and GraphQL for web applications"
                    ),
                    QuizTopic(
                            "Authentication & Security",
                            "Web Authentication, Authorization, and Security (sessions, JWT, OAuth, OWASP)"
                    ),
                    QuizTopic(
                            "DevOps & Deployment (Docker, CI/CD & Cloud)",
                            "DevOps: Docker, CI/CD pipelines, and cloud deployment"
                    ),
                    QuizTopic(
                            "System Design & Architecture",
                            "System Design and Software Architecture for web applications"
                    ),
                    QuizTopic(
                            "TypeScript & Modern JavaScript",
                            "TypeScript and Modern JavaScript (ES6+)"
                    ),
                    QuizTopic(
                            "Testing & QA (Unit, Integration & E2E)",
                            "Software Testing: unit, integration, and end-to-end testing for web applications"
                    ),
                    QuizTopic(
                            "C# & .NET (ASP.NET Core, EF Core & Blazor)",
                            "C# and .NET: ASP.NET Core, Entity Framework Core, Blazor, and LINQ"
                    ),
                    QuizTopic(
                            ".NET Backend (Web API, MVC & Middleware)",
                            ".NET Backend: ASP.NET Core Web API, MVC, middleware, dependency injection, and SignalR"
                    ),
                    QuizTopic(
                            "Mixed Fullstack Topics",
                            "Fullstack Web Development — a deliberate mix across frontend, backend, databases, APIs, security, DevOps, and C#/.NET"
                    )
            )

    private lateinit var topicTextView: TextView
    private lateinit var questionTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var questionCounterTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var feedbackTextView: TextView
    private lateinit var quizProgress: LinearProgressIndicator
    private lateinit var newTopicButton: MaterialButton
    private lateinit var loadingGroup: View
    private lateinit var loadingSubtitle: TextView
    private lateinit var reviewGroup: View
    private lateinit var reviewSummary: TextView
    private lateinit var reviewNewHighScore: TextView
    private lateinit var reviewList: android.widget.LinearLayout
    private lateinit var reviewPlayAgainButton: MaterialButton
    private lateinit var reviewNewTopicButton: MaterialButton
    private lateinit var allOptionButtons: List<MaterialButton>

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var geminiApiService: GeminiApiService

    private var questions = mutableListOf<Question>()
    private var userAnswers = mutableListOf<Int>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var questionsAnswered = 0

    // Loading state: the quiz starts as soon as the first batch of questions arrives,
    // remaining batches keep loading in the background while the user plays.
    private var isLoading = false
    private var waitingForMoreQuestions = false
    private var pendingBatches = 0
    private var quizGeneration = 0
    private var currentTopic: QuizTopic? = null

    private var defaultOptionBackground: ColorStateList? = null
    private var defaultOptionTextColor: ColorStateList? = null
    private var defaultOptionStroke: ColorStateList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
        geminiApiService = GeminiApiService()

        initializeViews()
        setupClickListeners()
        updateScoreViews()
        updateProgressViews()

        // Show topic selection dialog on startup
        showTopicSelectionDialog()
    }

    private fun initializeViews() {
        topicTextView = findViewById(R.id.topic_text)
        questionTextView = findViewById(R.id.question_text)
        scoreTextView = findViewById(R.id.score_text)
        questionCounterTextView = findViewById(R.id.question_counter)
        highScoreTextView = findViewById(R.id.high_score_text)
        feedbackTextView = findViewById(R.id.feedback_text)
        quizProgress = findViewById(R.id.quiz_progress)
        newTopicButton = findViewById(R.id.new_topic_button)
        loadingGroup = findViewById(R.id.loading_group)
        loadingSubtitle = findViewById(R.id.loading_subtitle)
        reviewGroup = findViewById(R.id.review_group)
        reviewSummary = findViewById(R.id.review_summary)
        reviewNewHighScore = findViewById(R.id.review_new_high_score)
        reviewList = findViewById(R.id.review_list)
        reviewPlayAgainButton = findViewById(R.id.review_play_again_button)
        reviewNewTopicButton = findViewById(R.id.review_new_topic_button)

        allOptionButtons =
                listOf(
                        findViewById(R.id.option_button_a),
                        findViewById(R.id.option_button_b),
                        findViewById(R.id.option_button_c),
                        findViewById(R.id.option_button_d)
                )

        // Remember the default button styling so answer highlights can be reset
        val reference = allOptionButtons.first()
        defaultOptionBackground = reference.backgroundTintList
        defaultOptionTextColor = reference.textColors
        defaultOptionStroke = reference.strokeColor

        setButtonsEnabled(false)
    }

    private fun setupClickListeners() {
        allOptionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { checkAnswer(index) }
        }

        newTopicButton.setOnClickListener {
            if (!isLoading) {
                showTopicSelectionDialog()
            }
        }

        reviewPlayAgainButton.setOnClickListener { resetQuiz() }
        reviewNewTopicButton.setOnClickListener { showTopicSelectionDialog() }
    }

    private fun showTopicSelectionDialog() {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_topic_title)
                .setItems(quizTopics.map { it.label }.toTypedArray()) { _, which ->
                    startQuiz(quizTopics[which])
                }
                // Allow dismissing only when there is already a quiz to return to
                .setCancelable(questions.isNotEmpty())
                .show()
    }

    private fun startQuiz(topic: QuizTopic) {
        quizGeneration++
        val generation = quizGeneration

        currentTopic = topic
        questions.clear()
        userAnswers.clear()
        currentQuestionIndex = 0
        score = 0
        questionsAnswered = 0
        isLoading = true
        waitingForMoreQuestions = false
        pendingBatches = PARALLEL_BATCHES

        reviewGroup.visibility = View.GONE
        topicTextView.text = topic.label
        updateScoreViews()
        updateProgressViews()
        showLoadingOverlay(getString(R.string.loading_subtitle, topic.label))

        // Fire several small requests in parallel instead of one big one: the quiz
        // starts as soon as the first batch lands, cutting the wait to a fraction.
        val perBatch = TOTAL_QUESTIONS / PARALLEL_BATCHES
        for (batchIndex in 1..PARALLEL_BATCHES) {
            lifecycleScope.launch {
                try {
                    val batch =
                            geminiApiService.generateQuestions(
                                    topic.prompt,
                                    perBatch,
                                    batchIndex,
                                    PARALLEL_BATCHES
                            )
                    if (generation == quizGeneration) onBatchLoaded(batch)
                } catch (e: Exception) {
                    Log.e(TAG, "Batch $batchIndex failed", e)
                    if (generation == quizGeneration) onBatchFailed(e)
                }
            }
        }
    }

    private fun onBatchLoaded(batch: List<Question>) {
        pendingBatches--

        // Parallel batches can occasionally produce near-identical questions; drop them
        val newQuestions =
                batch.filter { candidate ->
                    questions.none {
                        it.questionText.trim().equals(candidate.questionText.trim(), ignoreCase = true)
                    }
                }
        questions.addAll(newQuestions.shuffled())

        when {
            isLoading && questions.isNotEmpty() -> {
                // First batch arrived — start playing while the rest keeps loading
                isLoading = false
                hideLoadingOverlay()
                showQuestion()
            }
            waitingForMoreQuestions && questionsAnswered < questions.size -> {
                // User was waiting at the end of the loaded questions — continue
                waitingForMoreQuestions = false
                hideLoadingOverlay()
                currentQuestionIndex = questionsAnswered
                showQuestion()
            }
            else -> updateProgressViews()
        }

        if (pendingBatches == 0 && waitingForMoreQuestions) {
            waitingForMoreQuestions = false
            hideLoadingOverlay()
            endQuiz()
        }
    }

    private fun onBatchFailed(error: Exception) {
        pendingBatches--

        when {
            pendingBatches == 0 && questions.isEmpty() -> {
                isLoading = false
                hideLoadingOverlay()
                showError(getString(R.string.error_generation, error.message))
            }
            pendingBatches == 0 && waitingForMoreQuestions -> {
                waitingForMoreQuestions = false
                hideLoadingOverlay()
                endQuiz()
            }
            else -> updateProgressViews()
        }
    }

    private fun checkAnswer(userAnswerIndex: Int) {
        if (questions.isEmpty() || isLoading || waitingForMoreQuestions) return

        val currentQuestion = questions[currentQuestionIndex]
        val correctIndex = currentQuestion.correctAnswerIndex
        val isCorrect = userAnswerIndex == correctIndex
        questionsAnswered++
        userAnswers.add(userAnswerIndex)

        // Disable buttons to prevent multiple clicks before moving on
        setButtonsEnabled(false)

        if (isCorrect) {
            score += POINTS_PER_QUESTION
            showFeedback(getString(R.string.feedback_correct, POINTS_PER_QUESTION), R.color.quiz_on_correct)
            highlightButton(allOptionButtons[userAnswerIndex], R.color.quiz_correct_container, R.color.quiz_on_correct)
        } else {
            showFeedback(
                    getString(R.string.feedback_wrong, currentQuestion.options[correctIndex]),
                    R.color.quiz_on_wrong
            )
            highlightButton(allOptionButtons[userAnswerIndex], R.color.quiz_wrong_container, R.color.quiz_on_wrong)
            highlightButton(allOptionButtons[correctIndex], R.color.quiz_correct_container, R.color.quiz_on_correct)
        }

        updateScoreViews()
        updateProgressViews()

        val generation = quizGeneration
        val delay = if (isCorrect) NEXT_DELAY_CORRECT_MS else NEXT_DELAY_WRONG_MS
        questionTextView.postDelayed(
                {
                    if (generation != quizGeneration) return@postDelayed
                    when {
                        questionsAnswered < questions.size -> {
                            currentQuestionIndex++
                            showQuestion()
                        }
                        pendingBatches > 0 -> {
                            // Answered everything loaded so far; wait for the next batch
                            waitingForMoreQuestions = true
                            showLoadingOverlay(getString(R.string.loading_more))
                        }
                        else -> endQuiz()
                    }
                },
                delay
        )
    }

    private fun showQuestion() {
        if (questions.isEmpty()) return

        val currentQuestion = questions[currentQuestionIndex]
        questionTextView.text = currentQuestion.questionText
        feedbackTextView.visibility = View.INVISIBLE

        allOptionButtons.forEachIndexed { index, button ->
            button.text = "${'A' + index}.  ${currentQuestion.options[index]}"
            resetButtonStyle(button)
        }

        setButtonsEnabled(true)
        updateProgressViews()
    }

    private fun showFeedback(message: String, colorRes: Int) {
        feedbackTextView.text = message
        feedbackTextView.setTextColor(ContextCompat.getColor(this, colorRes))
        feedbackTextView.visibility = View.VISIBLE
    }

    private fun highlightButton(button: MaterialButton, containerColorRes: Int, onColorRes: Int) {
        val container = ContextCompat.getColor(this, containerColorRes)
        button.backgroundTintList = ColorStateList.valueOf(container)
        button.strokeColor = ColorStateList.valueOf(container)
        button.setTextColor(ContextCompat.getColor(this, onColorRes))
    }

    private fun resetButtonStyle(button: MaterialButton) {
        button.backgroundTintList = defaultOptionBackground
        button.strokeColor = defaultOptionStroke
        button.setTextColor(defaultOptionTextColor)
    }

    private fun updateScoreViews() {
        scoreTextView.text = getString(R.string.score_format, score)
        highScoreTextView.text =
                getString(R.string.high_score_format, sharedPreferences.getInt("high_score", 0))
    }

    private fun updateProgressViews() {
        if (questions.isEmpty()) {
            questionCounterTextView.text = getString(R.string.question_counter_empty)
            quizProgress.max = TOTAL_QUESTIONS
            quizProgress.setProgressCompat(0, false)
            return
        }

        val displayIndex = (questionsAnswered + 1).coerceAtMost(questions.size)
        // While batches are still loading, show progress against the expected total
        val total = if (pendingBatches > 0) TOTAL_QUESTIONS else questions.size
        questionCounterTextView.text =
                getString(R.string.question_counter_format, displayIndex, total)
        quizProgress.max = total
        quizProgress.setProgressCompat(questionsAnswered, true)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        allOptionButtons.forEach { it.isEnabled = enabled }
    }

    private fun showLoadingOverlay(subtitle: String) {
        loadingSubtitle.text = subtitle
        loadingGroup.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        loadingGroup.visibility = View.GONE
    }

    private fun endQuiz() {
        val highScore = sharedPreferences.getInt("high_score", 0)
        val isNewHighScore = score > highScore
        if (isNewHighScore) {
            sharedPreferences.edit().putInt("high_score", score).apply()
        }
        updateScoreViews()

        val answeredCount = minOf(questionsAnswered, questions.size, userAnswers.size)
        val percentage =
                if (answeredCount > 0) {
                    score * 100 / (answeredCount * POINTS_PER_QUESTION)
                } else {
                    0
                }

        reviewNewHighScore.visibility = if (isNewHighScore) View.VISIBLE else View.GONE
        reviewSummary.text =
                getString(R.string.review_summary_format, score, maxOf(score, highScore), percentage)
        populateReviewList(answeredCount)

        reviewGroup.visibility = View.VISIBLE
        reviewGroup.scrollTo(0, 0)
    }

    /** Fills the review screen with every answered question, the user's answer,
     * the correct answer, and the AI's justification. */
    private fun populateReviewList(count: Int) {
        reviewList.removeAllViews()

        for (i in 0 until count) {
            val question = questions[i]
            val userIndex = userAnswers[i]
            val isCorrect = userIndex == question.correctAnswerIndex

            val item = layoutInflater.inflate(R.layout.item_review_question, reviewList, false)
            item.findViewById<TextView>(R.id.review_question_text).text =
                    "${i + 1}. ${question.questionText}"

            val userAnswerView = item.findViewById<TextView>(R.id.review_user_answer)
            val correctAnswerView = item.findViewById<TextView>(R.id.review_correct_answer)
            val explanationView = item.findViewById<TextView>(R.id.review_explanation)

            val userOption = question.options.getOrNull(userIndex) ?: ""
            if (isCorrect) {
                userAnswerView.text = getString(R.string.review_your_answer_correct, userOption)
                userAnswerView.setTextColor(ContextCompat.getColor(this, R.color.quiz_on_correct))
            } else {
                userAnswerView.text = getString(R.string.review_your_answer_wrong, userOption)
                userAnswerView.setTextColor(ContextCompat.getColor(this, R.color.quiz_on_wrong))
                correctAnswerView.text =
                        getString(
                                R.string.review_correct_answer,
                                question.options[question.correctAnswerIndex]
                        )
                correctAnswerView.visibility = View.VISIBLE
            }

            if (question.explanation.isNotBlank()) {
                explanationView.text = question.explanation
            } else {
                explanationView.visibility = View.GONE
            }

            reviewList.addView(item)
        }
    }

    private fun showError(errorMessage: String) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error_title)
                .setMessage(errorMessage)
                .setPositiveButton(R.string.action_retry) { _, _ ->
                    currentTopic?.let { startQuiz(it) } ?: showTopicSelectionDialog()
                }
                .setNegativeButton(R.string.action_new_topic) { _, _ -> showTopicSelectionDialog() }
                .setCancelable(false)
                .show()
    }

    private fun resetQuiz() {
        if (questions.isNotEmpty()) {
            currentQuestionIndex = 0
            score = 0
            questionsAnswered = 0
            userAnswers.clear()
            questions.shuffle() // Shuffle questions for a new game
            reviewGroup.visibility = View.GONE
            updateScoreViews()
            showQuestion()
        } else {
            showTopicSelectionDialog()
        }
    }
}
