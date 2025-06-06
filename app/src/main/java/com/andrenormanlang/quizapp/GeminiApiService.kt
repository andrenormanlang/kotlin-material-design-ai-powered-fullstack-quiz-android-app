package com.andrenormanlang.quizapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApiService {
    companion object {
        private const val TAG = "GeminiApiService"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()
    private val apiKey = ApiConfig.getApiKey()
    private val model = ApiConfig.getModelName()
    private val baseUrl = ApiConfig.getBaseUrl()
    
    suspend fun generateQuestions(topic: String, count: Int = 15): List<Question> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting question generation for topic: $topic")
                
                // Validate API configuration
                if (!ApiConfig.isApiKeyConfigured()) {
                    Log.e(TAG, "API key not configured")
                    throw IllegalStateException("Gemini API key not configured. Please add GEMINI_API_KEY to local.properties")
                }
                
                Log.d(TAG, "API key is configured")
                
                // Sanitize input topic
                val sanitizedTopic = ApiConfig.sanitizeInput(topic)
                Log.d(TAG, "Sanitized topic: $sanitizedTopic")
                
                val prompt = createPrompt(sanitizedTopic, count)
                val requestBody = createRequestBody(prompt)
                val request = createRequest(requestBody)
                
                Log.d(TAG, "Making API request to: $baseUrl")
                val response = client.newCall(request).execute()
                
                Log.d(TAG, "Response code: ${response.code}")
                  if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Response body length: ${responseBody?.length ?: 0}")
                    Log.d(TAG, "Response body preview: ${responseBody?.take(200)}")
                    
                    if (ApiConfig.isValidResponse(responseBody)) {
                        Log.d(TAG, "Response is valid, parsing questions")
                        val questions = parseQuestionsFromResponse(responseBody)
                        Log.d(TAG, "Parsed ${questions.size} questions")
                        if (questions.isEmpty()) {
                            throw IllegalStateException("AI generated an empty question list")
                        }
                        return@withContext questions
                    } else {
                        Log.e(TAG, "Response failed validation")
                        Log.e(TAG, "Full response: $responseBody")
                        throw IllegalStateException("AI response failed validation")
                    }
                } else {
                    Log.e(TAG, "API request failed with code: ${response.code}")
                    Log.e(TAG, "Error message: ${response.message}")
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Error body: $errorBody")
                    throw IOException("AI API request failed with code: ${response.code}")
                }            } catch (e: Exception) {
                Log.e(TAG, "Exception during question generation", e)
                throw e // Re-throw the exception instead of returning default questions
            }
        }
    }
    
    private fun createPrompt(topic: String, count: Int): String {
        return """
            Generate exactly $count multiple-choice quiz questions about $topic for frontend web development.
            
            Return ONLY a valid JSON array in this exact format with no additional text:
            [
              {
                "question": "Question text here?",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctAnswer": 0
              }
            ]
            
            Requirements:
            - Each question should be clear and concise
            - All 4 options should be plausible but only one correct
            - correctAnswer should be the index (0-3) of the correct option
            - Focus on practical frontend development knowledge
            - Include topics like HTML, CSS, JavaScript, React, responsive design, accessibility, performance optimization
            - Make questions challenging but fair for intermediate developers
            - Ensure variety in question types and difficulty
        """.trimIndent()
    }
    
    private fun createRequestBody(prompt: String): RequestBody {
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 4000)
                put("topP", 0.8)
                put("topK", 40)
            })
        }
        
        return jsonBody.toString().toRequestBody("application/json".toMediaType())
    }
    
    private fun createRequest(requestBody: RequestBody): Request {
        return Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
    }
      private fun parseQuestionsFromResponse(responseBody: String?): List<Question> {
        return try {
            Log.d(TAG, "Parsing response body")
            val jsonResponse = JSONObject(responseBody ?: "")
            Log.d(TAG, "Created JSON object")
              val candidates = jsonResponse.getJSONArray("candidates")
            Log.d(TAG, "Found ${candidates.length()} candidates")
            
            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            
            Log.d(TAG, "Extracted content: ${content.take(200)}...")
            
            // Clean the response to ensure it's valid JSON
            val cleanedContent = cleanJsonResponse(content)
            Log.d(TAG, "Cleaned content: ${cleanedContent.take(200)}...")
            
            val questionsArray = JSONArray(cleanedContent)
            Log.d(TAG, "Created questions array with ${questionsArray.length()} items")
            
            val questions = mutableListOf<Question>()
            for (i in 0 until questionsArray.length()) {
                val questionObj = questionsArray.getJSONObject(i)
                val optionsArray = questionObj.getJSONArray("options")
                val options = mutableListOf<String>()
                
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                
                questions.add(
                    Question(
                        questionText = questionObj.getString("question"),
                        options = options,
                        correctAnswerIndex = questionObj.getInt("correctAnswer")
                    )
                )
            }
              Log.d(TAG, "Successfully parsed ${questions.size} questions")
            if (questions.isEmpty()) {
                Log.e(TAG, "AI generated an empty question list")
                throw IllegalStateException("AI generated an empty question list")
            }
            questions
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing questions from response", e)
            Log.e(TAG, "Response body was: $responseBody")
            throw IllegalStateException("Failed to parse AI response: ${e.message}", e)
        }
    }
    
    private fun cleanJsonResponse(content: String): String {
        // Remove any markdown formatting and extract just the JSON array
        var cleaned = content.trim()
        
        // Remove markdown code blocks
        cleaned = cleaned.replace("```json", "").replace("```", "")
        
        // Find the start and end of the JSON array
        val startIndex = cleaned.indexOf('[')
        val endIndex = cleaned.lastIndexOf(']')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            // Throw exception if we can't extract clean JSON
            throw IllegalArgumentException("Could not extract valid JSON array from AI response")
        }    }
}
