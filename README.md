# Android Fullstack Quiz App with AI Integration

An Android quiz application that generates dynamic questions about fullstack development using Google Gemini AI.

## Features

- 🤖 **AI-Powered Questions**: Dynamic question generation using Google Gemini AI with structured JSON output
- ⚡ **Fast Loading**: Questions are fetched in three parallel batches — the quiz starts as soon as the first batch arrives while the rest load in the background
- 🎯 **Topic-Focused**: Strict prompting keeps every question on the topic you picked
- 📚 **Multiple Topics**: Choose from 14 different fullstack development categories
- 💬 **Instant Feedback**: Inline correct/incorrect feedback with color-coded answer highlighting
- 📝 **Answer Review**: After each quiz, review every question with your answer, the correct answer, and an AI-written explanation of why it's right
- 🏆 **Score Tracking**: Progress bar, live score, and persistent high score
- 🎨 **Material 3 UI**: Modern Material Design 3 interface with full dark mode support

## Topics Available

- Fullstack Development (General)
- Frontend (HTML, CSS & JavaScript)
- Frontend Frameworks (React, Next.js & Vue)
- Backend Development (Node.js, Express & APIs)
- Databases (SQL, NoSQL, MongoDB & PostgreSQL)
- REST APIs & GraphQL
- Authentication & Security
- DevOps & Deployment (Docker, CI/CD & Cloud)
- System Design & Architecture
- TypeScript & Modern JavaScript
- Testing & QA (Unit, Integration & E2E)
- C# & .NET (ASP.NET Core, EF Core & Blazor)
- .NET Backend (Web API, MVC & Middleware)
- Mixed Fullstack Topics

## How It Works

1. Pick a topic — the app fires **3 parallel requests** to the Gemini API, each asking for 5 questions with a strict topic-only prompt and a JSON response schema.
2. The quiz starts the moment the first batch lands; remaining batches append silently while you play. Near-duplicate questions across batches are filtered out.
3. Each question ships with a short explanation. When the quiz ends you get a full review screen: ✓/✗ per question, the correct answer where you missed, and the justification.
4. Responses are parsed defensively (`QuestionParser`): markdown fences are stripped, truncated responses recover all complete questions, and malformed entries are skipped instead of failing the batch.

## Setup Instructions

### Prerequisites

- Android Studio (latest version)
- Android SDK API level 24 or higher
- Google Gemini API key

### Getting a Google Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Sign in with your Google account
3. Click "Get API Key" in the top navigation
4. Create a new API key or use an existing one
5. Copy the API key for use in the next step

### Configuration

1. Clone or download this repository
2. Open the project in Android Studio
3. **Configure your API key**:
   - Open the `local.properties` file in the root directory
   - Add your Gemini API key like this:

     ```bash
     GEMINI_API_KEY=your_actual_api_key_here
     ```

   - Replace `your_actual_api_key_here` with your actual Google Gemini API key

⚠️ **Important**: The `local.properties` file is automatically ignored by Git and will not be committed to version control, keeping your API key secure.

### Building and Running

1. Sync the project with Gradle files
2. Build and run the app on an Android device or emulator
3. The app will prompt you to select a topic when it starts
4. Enjoy your AI-powered quiz experience!

### Running the Tests

```bash
./gradlew testDebugUnitTest    # local unit tests (JSON parsing)
./gradlew assembleDebug        # build the debug APK
```

`QuestionParserTest` covers the Gemini response parsing: well-formed responses, markdown-fenced output, truncated JSON recovery, and malformed question entries.

## Security Features

- ✅ **API Key Security**: API keys are stored in `local.properties` (not tracked by Git)
- ✅ **BuildConfig Integration**: API keys are injected securely during build time
- ✅ **No Hardcoded Secrets**: No sensitive information in source code
- ✅ **Input Sanitization**: Topic strings are sanitized before being sent to the API

## Architecture

- **Language**: Kotlin
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 35
- **UI**: Single-activity, View-based layout with Material Components (Material 3 theme, light + dark)
- **Async**: Kotlin coroutines with `lifecycleScope`; parallel batch loading with generation tokens to discard stale responses
- **Network**: OkHttp for Gemini API communication
- **AI Integration**: Gemini `generateContent` with `responseSchema` structured output and low thinking level for latency

### Key Classes

| Class | Responsibility |
| --- | --- |
| `MainActivity` | Quiz flow, progressive batch loading, scoring, review screen |
| `GeminiApiService` | Prompt construction, API requests, retry/fallback handling |
| `QuestionParser` | Pure-JVM response parsing (unit-testable, no Android deps) |
| `ApiConfig` | API key validation, endpoint config, input sanitization |

## Dependencies

- Material Components 1.10 - Material 3 widgets and theming
- OkHttp 4.12.0 - HTTP client for API calls
- Kotlin Coroutines - Async programming
- org.json - JSON parsing (also used directly in unit tests)
- JUnit 4 - Local unit tests

## Contributing

Feel free to contribute to this project by:

- Adding new quiz topics
- Improving the UI/UX
- Adding new features
- Fixing bugs

## License

This project is open source and available under the MIT License.
