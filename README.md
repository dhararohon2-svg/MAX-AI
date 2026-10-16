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


## In-game floating gaming panel

The app includes a user-started overlay service with a slim draggable red trigger bar. Tap the bar to expand/collapse the panel; use the × control or Android's app settings to stop it. The first launch requires the user to grant **Display over other apps** permission in Android Settings, then return to MAX AI and tap the panel card again. A persistent notification is shown while the overlay service is active. The panel layout measures the available display and scrolls in portrait or landscape.

The panel exposes Battery / Balance / Turbo guidance profiles, Wi-Fi settings, battery saver settings, display settings, read-only CPU0 frequency where exposed, and explanatory quick tools. Android does not allow a normal third-party app to universally force CPU/GPU clocks, kill arbitrary apps, change another game's touch sensitivity, inject a crosshair safely into every game, or read exact FPS / game-server ping on all devices. Those items therefore open relevant system settings or explain the platform limitation instead of claiming fake optimization. Some OEM gaming modes require the phone maker's own Game Space/Game Turbo app.


## Latest voice and gaming-panel update
- Spoken replies pass through `SpeechTextCleaner`: decorative emoji/markdown/URL symbols are not read aloud; ordinary words and sentence punctuation are retained. TTS speech rate and pitch are set to conversational defaults. Voice naturalness and Bengali pronunciation still depend on the Android TTS engine and installed voice data.
- The floating panel now includes a stored 1–10 sensitivity preset with Low/Balanced/High shortcuts. This is a personal reference preset only; Android does not provide a universal API for changing sensitivity inside third-party games.
- Added quick links/guidance for network/Wi-Fi, notification settings and display brightness. These open Android settings or provide guidance rather than claiming hidden system changes.

## MAX AI Gaming Module (added)

The slim red trigger opens a single configurable floating Gaming Module. The panel supports drag-to-move via its title bar, a separate draggable trigger, and an adjustable panel height. It includes sensitivity level and low/balanced/high presets, advisory per-game profiles keyed by package name, display refresh-rate readout, a TCP connection-latency check, thermal/battery status, a movable crosshair overlay, and a screen-recording shortcut/fallback to Android Quick Settings.

### Android capability and permission notes
- Floating trigger, panel, and crosshair require the user to grant **Display over other apps** permission in Android Settings. Disable the overlay by tapping × or stopping the foreground notification.
- Screen recording must use Android's visible system capture/consent flow. This shortcut attempts a vendor settings screen, then directs the user to Quick Settings; it does not secretly record other apps.
- Sensitivity values and per-game profiles are stored as MAX AI preferences. Android does not provide ordinary apps a universal API to rewrite another game's internal sensitivity settings.
- The refresh-rate reading reports the display mode, not actual game FPS. The network check reports TCP connect time to a fixed endpoint, not game-server ping.
- Thermal status and battery percentage are read-only. Android thermal protection remains authoritative; no thermal bypass is attempted.
- Crosshair overlays may be blocked or disallowed by particular games/anti-cheat rules. Use only where permitted.
- CPU/GPU clocks, governors, render pipeline, resolution, forced FPS, and vendor spoofing cannot be safely or universally changed by an ordinary non-root Android app. Those are intentionally not presented as working tweaks.

Build with GitHub Actions: upload the contents of `MAX_AI_Android_Project` to the repository root, then run **Build MAX AI Android APK**. Download artifact **MAX-AI-debug-APK** only after the workflow succeeds. This source ZIP was checked for archive integrity; an APK compile/runtime test has not been performed in this environment.

## Unified Gaming Module configuration
`app/src/main/java/com/maxai/gamingsuite/GamingModuleConfig.kt` contains the single source-of-truth catalog for the screenshot-inspired gaming settings: CPU/governor preferences, GPU/rendering preferences, GFX pipeline, RAM/VM, touch/sensitivity, thermal/battery, network latency, FPS/refresh monitoring, crosshair, edge gestures, screen recording, per-game-compatible profile choices, and device compatibility options. Values persist in a dedicated SharedPreferences namespace and the module has a master enable switch.

Important Android limitation: these are user profile/preferences unless the OS/game exposes an official API. A normal app cannot universally force CPU/GPU clocks/governors, spoof device/GPU identity, inject Vulkan/OpenGL settings into another game, bypass game throttling/thermal safety, set another app's sensitivity, or unlock unsupported FPS. Those options are explicitly marked unsupported/advisory instead of showing fake success. Overlays require user-granted Display over other apps permission; screen capture requires Android's system capture consent. Actual compile/device validation must be performed by GitHub Actions and on the target phone.

### Overlay permission shortcut
The Home screen includes an **Overlay Permission Settings** card. Tapping **Grant Overlay Permission** opens Android's official `ACTION_MANAGE_OVERLAY_PERMISSION` page for MAX AI (where supported). After the user returns, the card refreshes the permission status. Android requires the user to grant this permission manually; an app cannot silently approve it. Once granted, use **In-game Floating Panel** to start the floating trigger bar.
