# MAX AI — Native Android Starter Project

A buildable Kotlin + Jetpack Compose Android project scaffold for MAX AI.

## Included
- Native Android app, Kotlin, Jetpack Compose, `minSdk 26`
- Home dashboard with MAX AI branding and anime-avatar placeholder
- AI Chat screen with a local demo response engine (no API key required for demo)
- Voice screen using Android TextToSpeech and SpeechRecognizer where available
- Module Hub with individual on/off toggles (saved locally using SharedPreferences)
- Device Tools screen with basic device information
- Gaming screen with safe, capability-aware tips and launch shortcuts
- Unit test for the local demo response engine
- GitHub Actions workflow to build, test, and publish a debug APK artifact

## Important limitations
This is a real buildable starter source project, not a finished AI service or system-level game booster.
- AI chat is local/demo until you connect an AI provider endpoint and configure a key securely.
- Voice recognition and TextToSpeech depend on Android device services/languages.
- Android apps cannot generally force CPU/GPU clocks, bypass thermal limits, clear another app's private memory, or grant themselves privileged permissions. Gaming tools therefore provide safe, user-controlled actions and guidance.
- The anime character is a placeholder illustration made with Compose shapes/text. Replace it with your own licensed image or 3D model when ready.

## Build using GitHub on an Android phone
1. Create/open a GitHub repository.
2. Upload the contents of this ZIP (not the ZIP itself) to the repository root.
3. Open **Actions** and enable workflows if prompted.
4. Run **Build MAX AI Android APK** from Actions, or push a commit to `main`.
5. Open the completed workflow run, then download the `MAX-AI-debug-APK` artifact.
6. Extract the artifact ZIP and install `app-debug.apk`. If Android asks, allow installs from the browser/file manager you used.

## Local build
Install JDK 17 and Android SDK, then run Gradle 8.13:
`gradle assembleDebug testDebugUnitTest`

No secrets are needed for the demo build.
