package com.andrenormanlang.quizapp

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses Gemini generateContent responses into [Question] lists.
 *
 * Pure JVM logic with no Android dependencies so it can be unit-tested on the host.
 * Tolerates markdown code fences and truncated JSON, and skips malformed entries
 * instead of failing the whole batch.
 */
object QuestionParser {

    /** Extracts the model text from a full generateContent response body and parses it. */
    fun parseResponse(responseBody: String?): List<Question> {
        val jsonResponse = JSONObject(responseBody ?: "")
        val content =
                jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
        return parseQuestionsJson(content)
    }

    /** Parses the question JSON array produced by the model. */
    fun parseQuestionsJson(content: String): List<Question> {
        val questionsArray = JSONArray(cleanJsonResponse(content))

        val questions = mutableListOf<Question>()
        for (i in 0 until questionsArray.length()) {
            try {
                val questionObj = questionsArray.getJSONObject(i)
                val optionsArray = questionObj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }

                val correctIndex = questionObj.getInt("correctAnswer")
                if (options.size != 4 || correctIndex !in 0..3) {
                    continue // skip malformed entries instead of failing the batch
                }

                questions.add(
                        Question(
                                questionText = questionObj.getString("question"),
                                options = options,
                                correctAnswerIndex = correctIndex,
                                explanation = questionObj.optString("explanation", "")
                        )
                )
            } catch (e: Exception) {
                continue // entry missing required fields — skip it
            }
        }
        return questions
    }

    /**
     * Strips markdown fences and extracts the JSON array. If the array was truncated
     * mid-object (e.g. token limit hit), recovers every complete question object.
     */
    internal fun cleanJsonResponse(content: String): String {
        var cleaned = content.trim()

        // Remove markdown code blocks
        cleaned = cleaned.replace("```json", "").replace("```", "")

        val startIndex = cleaned.indexOf('[')
        if (startIndex == -1) {
            throw IllegalArgumentException("Could not extract valid JSON array from AI response")
        }

        // Happy path: everything up to the last closing bracket is a valid array.
        // A truncated response can still contain "]" (from inner options arrays),
        // so validity — not bracket presence — decides whether recovery is needed.
        val endIndex = cleaned.lastIndexOf(']')
        if (endIndex > startIndex) {
            val candidate = cleaned.substring(startIndex, endIndex + 1)
            if (isValidJsonArray(candidate)) {
                return candidate
            }
        }

        // Truncated: keep every complete question object and close the array
        val lastCompleteObject = findLastCompleteJsonObject(cleaned, startIndex)
        if (lastCompleteObject != -1) {
            return cleaned.substring(startIndex, lastCompleteObject + 1) + "]"
        }
        throw IllegalArgumentException("Could not recover from truncated JSON response")
    }

    private fun isValidJsonArray(text: String): Boolean =
            try {
                JSONArray(text)
                true
            } catch (e: Exception) {
                false
            }

    private fun findLastCompleteJsonObject(content: String, startIndex: Int): Int {
        var braceCount = 0
        var lastCompleteIndex = -1
        var inString = false
        var escapeNext = false

        for (i in startIndex until content.length) {
            val char = content[i]

            when {
                escapeNext -> escapeNext = false
                char == '\\' && inString -> escapeNext = true
                char == '"' && !escapeNext -> inString = !inString
                !inString && char == '{' -> braceCount++
                !inString && char == '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        lastCompleteIndex = i
                    }
                }
            }
        }

        return lastCompleteIndex
    }
}
