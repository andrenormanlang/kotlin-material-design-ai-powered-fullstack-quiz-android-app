package com.andrenormanlang.quizapp

import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiApiService {
    companion object {
        private const val TAG = "GeminiApiService"
    }

    private val client =
            OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .build()
    private val apiKey = ApiConfig.getApiKey()
    private val baseUrl = ApiConfig.getBaseUrl()

    /**
     * Generates [count] questions about [topic].
     *
     * When the quiz is split across several parallel requests, [batchIndex] (1-based) and
     * [batchCount] tell the model which slice this is so each batch covers different
     * sub-areas of the topic and duplicates are less likely.
     */
    suspend fun generateQuestions(
            topic: String,
            count: Int = 15,
            batchIndex: Int = 1,
            batchCount: Int = 1
    ): List<Question> {
        return withContext(Dispatchers.IO) {
            if (!ApiConfig.isApiKeyConfigured()) {
                Log.e(TAG, "API key not configured")
                throw IllegalStateException(
                        "Gemini API key not configured. Please add GEMINI_API_KEY to local.properties"
                )
            }

            val sanitizedTopic = ApiConfig.sanitizeInput(topic)
            val prompt = createPrompt(sanitizedTopic, count, batchIndex, batchCount)
            Log.d(TAG, "Requesting $count questions on '$sanitizedTopic' (batch $batchIndex/$batchCount)")

            // Fast path uses low thinking effort; if the API rejects that config
            // (e.g. the model doesn't support it), retry once without it.
            try {
                executeRequest(prompt, count, includeThinkingConfig = true)
            } catch (e: UnsupportedGenerationConfigException) {
                Log.w(TAG, "thinkingConfig rejected by API, retrying without it")
                executeRequest(prompt, count, includeThinkingConfig = false)
            }
        }
    }

    private class UnsupportedGenerationConfigException(message: String) : IOException(message)

    private fun executeRequest(
            prompt: String,
            count: Int,
            includeThinkingConfig: Boolean
    ): List<Question> {
        val requestBody = createRequestBody(prompt, count, includeThinkingConfig)
        val request =
                Request.Builder()
                        .url("$baseUrl?key=$apiKey")
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json")
                        .build()

        val response = client.newCall(request).execute()
        response.use {
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "API request failed with code ${response.code}: $responseBody")
                if (response.code == 400 &&
                                includeThinkingConfig &&
                                responseBody?.contains("thinking", ignoreCase = true) == true
                ) {
                    throw UnsupportedGenerationConfigException("thinkingConfig not supported")
                }
                throw IOException("AI API request failed with code: ${response.code}")
            }

            if (!ApiConfig.isValidResponse(responseBody)) {
                Log.e(TAG, "Response failed validation: $responseBody")
                throw IllegalStateException("AI response failed validation")
            }

            val questions =
                    try {
                        QuestionParser.parseResponse(responseBody)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing questions from response: $responseBody", e)
                        throw IllegalStateException("Failed to parse AI response: ${e.message}", e)
                    }
            Log.d(TAG, "Parsed ${questions.size} questions")
            if (questions.isEmpty()) {
                throw IllegalStateException("AI generated an empty question list")
            }
            return questions
        }
    }

    private fun createPrompt(topic: String, count: Int, batchIndex: Int, batchCount: Int): String {
        val varietyHint =
                if (batchCount > 1) {
                    """
                    This is set $batchIndex of $batchCount for the same quiz. The other sets are
                    generated separately, so pick a different mix of sub-areas, angles, and
                    difficulty within the topic to avoid overlapping with the other sets.
                    """.trimIndent()
                } else {
                    ""
                }

        return """
            Generate exactly $count multiple-choice quiz questions about the following topic:

            TOPIC: $topic

            STRICT TOPIC RULES:
            - Every single question MUST be directly about "$topic" and nothing else.
            - Do NOT include questions about other programming languages, frameworks, tools,
              or general software topics unless they are explicitly part of the topic above.
            - If the topic lists specific technologies, only ask about those technologies and
              their direct ecosystem.

            QUESTION RULES:
            - Each question has exactly 4 options and exactly one correct answer.
            - "correctAnswer" is the index (0-3) of the correct option.
            - "explanation" is 1-2 concise sentences justifying why the correct option is
              right (and, when useful, why the tempting wrong options are wrong).
            - Questions should be clear and specific; options up to 80 characters.
            - Target intermediate developers: practical, challenging but fair.
            - Include real-world scenarios and best practices where relevant.
            - Vary the position of the correct answer across questions.
            $varietyHint
        """.trimIndent()
    }

    private fun createRequestBody(
            prompt: String,
            count: Int,
            includeThinkingConfig: Boolean
    ): RequestBody {
        val questionSchema =
                JSONObject().apply {
                    put("type", "OBJECT")
                    put(
                            "properties",
                            JSONObject().apply {
                                put("question", JSONObject().put("type", "STRING"))
                                put(
                                        "options",
                                        JSONObject().apply {
                                            put("type", "ARRAY")
                                            put("items", JSONObject().put("type", "STRING"))
                                        }
                                )
                                put("correctAnswer", JSONObject().put("type", "INTEGER"))
                                put("explanation", JSONObject().put("type", "STRING"))
                            }
                    )
                    put(
                            "required",
                            JSONArray().apply {
                                put("question")
                                put("options")
                                put("correctAnswer")
                                put("explanation")
                            }
                    )
                }

        val generationConfig =
                JSONObject().apply {
                    put("temperature", 0.8)
                    // Generous headroom per question (incl. explanation) so responses
                    // are never truncated
                    put("maxOutputTokens", (count * 700 + 2000).coerceAtMost(15000))
                    // Structured output: the API returns a pure JSON array, no markdown
                    put("responseMimeType", "application/json")
                    put(
                            "responseSchema",
                            JSONObject().apply {
                                put("type", "ARRAY")
                                put("items", questionSchema)
                            }
                    )
                    if (includeThinkingConfig) {
                        put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                    }
                }

        val jsonBody =
                JSONObject().apply {
                    put(
                            "contents",
                            JSONArray().put(
                                    JSONObject().put(
                                            "parts",
                                            JSONArray().put(JSONObject().put("text", prompt))
                                    )
                            )
                    )
                    put("generationConfig", generationConfig)
                }

        return jsonBody.toString().toRequestBody("application/json".toMediaType())
    }
}
