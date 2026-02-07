package com.andrenormanlang.quizapp

import android.util.Log
import org.json.JSONObject

/**
 * Utility class for managing API configuration and security
 */
object ApiConfig {
    private const val TAG = "ApiConfig"
    
    /**
     * Validates if the API key is properly configured
     */
    fun isApiKeyConfigured(): Boolean {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isValid = apiKey.isNotBlank() && 
                     apiKey != "YOUR_API_KEY_HERE" && 
                     apiKey.startsWith("AIza")
        
        if (!isValid) {
            Log.w(TAG, "Gemini API key is not properly configured")
        }
        
        return isValid
    }
    
    /**
     * Gets the configured API key with validation
     */
    fun getApiKey(): String {
        if (!isApiKeyConfigured()) {
            throw IllegalStateException(
                "Gemini API key not configured. Please add GEMINI_API_KEY to local.properties"
            )
        }
        return BuildConfig.GEMINI_API_KEY
    }
    
    /**
     * Gets the model name to use for API requests
     */
    fun getModelName(): String = "gemini-3-flash-preview"
    
    /**
     * Gets the base URL for API requests
     */
    fun getBaseUrl(): String = "https://generativelanguage.googleapis.com/v1beta/models/${getModelName()}:generateContent"
      /**
     * Validates the API response for common error patterns
     */
    fun isValidResponse(responseBody: String?): Boolean {
        if (responseBody.isNullOrBlank()) return false
        
        try {
            // Try to parse as JSON first - if it's valid JSON, it's likely a good response
            val jsonResponse = JSONObject(responseBody)
            
            // Check if it has the expected Gemini API structure
            if (jsonResponse.has("candidates")) {
                return true
            }
            
            // Check for specific API error structure
            if (jsonResponse.has("error")) {
                return false
            }
            
            return true
        } catch (e: Exception) {
            // If it's not valid JSON, check for specific error patterns in the text
            val lowerResponse = responseBody.lowercase()
            val errorPatterns = listOf(
                "\"error\":",
                "invalid_api_key",
                "quota_exceeded", 
                "permission_denied",
                "api key not valid"
            )
            
            return !errorPatterns.any { lowerResponse.contains(it) }
        }
    }
    
    /**
     * Sanitizes user input to prevent injection attacks
     */
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("[<>\"'&]"), "") // Remove potentially harmful characters
            .take(1000) // Limit length to prevent oversized requests
    }
}
