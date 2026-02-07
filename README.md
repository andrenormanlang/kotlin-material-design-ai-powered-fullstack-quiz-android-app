# Android Fullstack Quiz App with AI Integration

An Android quiz application that generates dynamic questions about fullstack development using Google Gemini AI.

## Features

- 🤖 **AI-Powered Questions**: Dynamic question generation using Google Gemini AI
- 📚 **Multiple Topics**: Choose from 14 different fullstack development categories
- 🎯 **Interactive Quiz**: Multiple-choice questions with immediate feedback
- 🏆 **Score Tracking**: Keep track of your high scores
- 🎨 **Modern UI**: Clean and intuitive user interface

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
- Mixed Fullstack Topics (AI Generated)

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

## Security Features

- ✅ **API Key Security**: API keys are stored in `local.properties` (not tracked by Git)
- ✅ **BuildConfig Integration**: API keys are injected securely during build time
- ✅ **No Hardcoded Secrets**: No sensitive information in source code

## Architecture

- **Language**: Kotlin
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 35
- **Network**: OkHttp for API communication
- **AI Integration**: Google Gemini API
- **Architecture**: MVVM-like pattern with coroutines

## Dependencies

- OkHttp 4.12.0 - HTTP client for API calls
- Kotlin Coroutines - Async programming
- Android Lifecycle - Modern Android lifecycle management
- JSON parsing for API responses

## Contributing

Feel free to contribute to this project by:

- Adding new quiz topics
- Improving the UI/UX
- Adding new features
- Fixing bugs

## License

This project is open source and available under the MIT License.
