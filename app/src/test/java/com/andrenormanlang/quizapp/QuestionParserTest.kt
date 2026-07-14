package com.andrenormanlang.quizapp

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionParserTest {

    /** Wraps model text in the Gemini generateContent response envelope. */
    private fun geminiResponse(text: String): String =
            JSONObject()
                    .put(
                            "candidates",
                            JSONArray().put(
                                    JSONObject().put(
                                            "content",
                                            JSONObject().put(
                                                    "parts",
                                                    JSONArray().put(JSONObject().put("text", text))
                                            )
                                    )
                            )
                    )
                    .toString()

    private val validQuestionsJson =
            """
            [
              {
                "question": "What does HTML stand for?",
                "options": ["HyperText Markup Language", "HighText Machine Language", "Hyperlink Text Mode Language", "Home Tool Markup Language"],
                "correctAnswer": 0,
                "explanation": "HTML is the standard markup language for web pages."
              },
              {
                "question": "Which CSS property controls text size?",
                "options": ["font-weight", "text-style", "font-size", "text-size"],
                "correctAnswer": 2,
                "explanation": "font-size sets the size of the font; text-size does not exist."
              }
            ]
            """.trimIndent()

    @Test
    fun `parses well-formed response into questions`() {
        val questions = QuestionParser.parseResponse(geminiResponse(validQuestionsJson))

        assertEquals(2, questions.size)

        val first = questions[0]
        assertEquals("What does HTML stand for?", first.questionText)
        assertEquals(4, first.options.size)
        assertEquals(0, first.correctAnswerIndex)
        assertEquals("HTML is the standard markup language for web pages.", first.explanation)

        assertEquals(2, questions[1].correctAnswerIndex)
    }

    @Test
    fun `parses response wrapped in markdown code fences`() {
        val fenced = "```json\n$validQuestionsJson\n```"

        val questions = QuestionParser.parseResponse(geminiResponse(fenced))

        assertEquals(2, questions.size)
        assertEquals("What does HTML stand for?", questions[0].questionText)
    }

    @Test
    fun `recovers complete questions from truncated json`() {
        // Second object cut off mid-way, closing bracket missing (token limit hit)
        val truncated =
                """
                [
                  {
                    "question": "What does HTML stand for?",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": 0,
                    "explanation": "Because."
                  },
                  {
                    "question": "Which CSS property contr
                """.trimIndent()

        val questions = QuestionParser.parseQuestionsJson(truncated)

        assertEquals(1, questions.size)
        assertEquals("What does HTML stand for?", questions[0].questionText)
    }

    @Test
    fun `skips malformed entries instead of failing the batch`() {
        val mixed =
                """
                [
                  {
                    "question": "Has five options",
                    "options": ["A", "B", "C", "D", "E"],
                    "correctAnswer": 0
                  },
                  {
                    "question": "Answer index out of range",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": 7
                  },
                  {
                    "question": "Missing options entirely",
                    "correctAnswer": 1
                  },
                  {
                    "question": "The only valid one",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": 3,
                    "explanation": "Valid entry."
                  }
                ]
                """.trimIndent()

        val questions = QuestionParser.parseQuestionsJson(mixed)

        assertEquals(1, questions.size)
        assertEquals("The only valid one", questions[0].questionText)
        assertEquals(3, questions[0].correctAnswerIndex)
    }

    @Test
    fun `missing explanation defaults to empty string`() {
        val withoutExplanation =
                """
                [
                  {
                    "question": "No explanation here",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": 1
                  }
                ]
                """.trimIndent()

        val questions = QuestionParser.parseQuestionsJson(withoutExplanation)

        assertEquals(1, questions.size)
        assertEquals("", questions[0].explanation)
    }

    @Test
    fun `throws on content with no json array`() {
        assertThrows(IllegalArgumentException::class.java) {
            QuestionParser.parseQuestionsJson("Sorry, I cannot generate questions right now.")
        }
    }

    @Test
    fun `throws on garbage response body`() {
        assertThrows(Exception::class.java) { QuestionParser.parseResponse("not json at all") }
    }

    @Test
    fun `empty array parses to empty list`() {
        val questions = QuestionParser.parseQuestionsJson("[]")
        assertTrue(questions.isEmpty())
    }
}
