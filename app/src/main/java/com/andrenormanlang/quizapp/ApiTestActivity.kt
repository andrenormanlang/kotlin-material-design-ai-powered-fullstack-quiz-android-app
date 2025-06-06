package com.andrenormanlang.quizapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Simple test activity to debug API connectivity
 * You can temporarily set this as the launcher activity to test the API
 */
class ApiTestActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Test API configuration
        testApiConfiguration()
    }
    
    private fun testApiConfiguration() {
        Log.d("ApiTestActivity", "Testing API configuration...")
        
        try {
            // Test 1: Check if API key is configured
            val isConfigured = ApiConfig.isApiKeyConfigured()
            Log.d("ApiTestActivity", "API key configured: $isConfigured")
            
            if (!isConfigured) {
                Toast.makeText(this, "❌ API key not configured", Toast.LENGTH_LONG).show()
                return
            }
            
            // Test 2: Get API key (this will throw if not configured)
            val apiKey = ApiConfig.getApiKey()
            Log.d("ApiTestActivity", "API key length: ${apiKey.length}")
            Log.d("ApiTestActivity", "API key starts with: ${apiKey.take(10)}...")
            
            // Test 3: Test API call
            val geminiService = GeminiApiService()
            lifecycleScope.launch {
                try {
                    Log.d("ApiTestActivity", "Making test API call...")
                    val questions = geminiService.generateQuestions("JavaScript", 3)
                    
                    if (questions.size > 3) {
                        // If we get more than 3 questions, it's likely the defaults (15 questions)
                        Log.w("ApiTestActivity", "❌ Got ${questions.size} questions - likely default questions")
                        Toast.makeText(this@ApiTestActivity, "❌ API call failed - using defaults", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("ApiTestActivity", "✅ Got ${questions.size} AI-generated questions")
                        Toast.makeText(this@ApiTestActivity, "✅ API working! Got ${questions.size} questions", Toast.LENGTH_LONG).show()
                    }
                    
                    // Log first question for verification
                    if (questions.isNotEmpty()) {
                        Log.d("ApiTestActivity", "First question: ${questions.first().questionText}")
                    }
                    
                } catch (e: Exception) {
                    Log.e("ApiTestActivity", "❌ API test failed", e)
                    Toast.makeText(this@ApiTestActivity, "❌ API test failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
        } catch (e: Exception) {
            Log.e("ApiTestActivity", "❌ Configuration test failed", e)
            Toast.makeText(this, "❌ Configuration failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
