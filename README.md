# MAX AI — Enhanced Native Android Project

Kotlin + Jetpack Compose app for Android 8.0+ (minSdk 26). This is source code plus a GitHub Actions workflow; it is not a prebuilt APK.

## Functional modules included
- Futuristic dark UI with animated companion orb, gradients, cards and Material icons.
- General-purpose AI chat through an OpenAI-compatible `/chat/completions` endpoint; OpenRouter is the default provider (`https://openrouter.ai/api/v1`, model `openrouter/auto`).
- API key storage uses AndroidX EncryptedSharedPreferences backed by Android Keystore. If encrypted storage cannot initialize, saving is disabled rather than writing a new plaintext key. A legacy fallback key from an older build is migrated when possible and the old file is cleared only after successful migration.
- Voice input via Android speech recognition and spoken output via TextToSpeech. Language availability depends on device services.
- Persistent Module Hub toggles; main feature screens are gated by their associated modules.
- Game Library: search installed launchable apps, add/remove them, and launch them. It can only list apps Android exposes through package visibility and that have a launcher activity; it does not install arbitrary APKs.
- Device Monitor: available RAM, battery charge, Android thermal status (API 29+), CPU frequency read-only inspection where sysfs permissions allow, best-effort GPU frequency read, active display refresh rate, and a TCP connect-latency probe for a chosen host/port.
- Gaming profiles, refresh-rate guidance, DND settings shortcut, battery advice, and optional keep-screen-awake while Monitor is open.
- Unit tests and GitHub Actions debug APK build workflow.

## Build on GitHub using an Android phone
1. Extract this ZIP on your phone and upload the ZIP contents (not the ZIP itself) to your repository root.
2. Confirm `.github/workflows/android.yml` exists. Open Actions and run **Build MAX AI Android APK**.
3. Download `MAX-AI-debug-APK` from a successful run and extract `app-debug.apk`.

## OpenRouter setup
In API & Settings, enter your OpenRouter API key from https://openrouter.ai/keys. Defaults are base URL `https://openrouter.ai/api/v1` and model `openrouter/auto`. You can choose a specific model available to your account. Provider usage fees/free-tier limits apply; the app does not provide credits. The key is sent only to the configured HTTPS endpoint when a chat request is made. Never commit keys to GitHub.

## Honest Android capability limits
- CPU/GPU frequency reads are best-effort and can show `Not exposed` because many OEMs restrict sysfs access. The app does not write CPU/GPU clock controls.
- The Android thermal service reports thermal status; ordinary apps cannot disable system thermal protection.
- Active display refresh rate is not the game’s actual FPS. A normal app cannot reliably read arbitrary third-party games’ FPS, private memory, or in-game server ping.
- TCP latency is a connection-time measurement to a user-specified host/port, not ICMP ping and not guaranteed to match a game server's latency.
- Profiles are in-app presets/guidance; they do not override OEM system performance modes. The anime companion is an animated placeholder, not a full 3D avatar.
