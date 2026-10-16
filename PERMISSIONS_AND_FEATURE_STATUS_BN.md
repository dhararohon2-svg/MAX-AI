# MAX AI Gaming Module — Permission & Feature Status (বাংলা)

## Overlay চালু করার ধাপ
1. MAX AI খুলে Home-এ `In-game Floating Panel` চাপুন।
2. Android-এর `Display over other apps` / `Allow display over other apps` সেটিংস খুললে MAX AI নির্বাচন করে Allow দিন।
3. অ্যাপে ফিরে আবার `In-game Floating Panel` চাপুন।
4. স্ক্রিনের উপরের চিকন লাল বার দেখা গেলে সেটি চাপলে প্যানেল খুলবে; `×` চাপলে overlay বন্ধ হবে।
5. Android 13+ এ notification permission চাইলে অনুমতি দিন। না দিলেও Android-এর foreground-service notification আচরণ ডিভাইসভেদে ভিন্ন হতে পারে।

## Overlay-এ থাকা controls
- Battery / Balance / Turbo guidance profile
- Sensitivity 1–10 slider ও Low / Balanced / High preset
- FPS / refresh-rate monitor (ডিসপ্লে Hz দেখায়; অন্য গেমের FPS নিশ্চিতভাবে নয়)
- Network TCP latency check
- Thermal / battery status
- Custom crosshair overlay
- Edge/gesture helper preferences
- Per-game profile by package name
- Screen-recording shortcut (Android capture consent প্রয়োজন)
- CPU read-only status যেখানে OEM sysfs expose করে
- Panel drag এবং panel height resize
- Quick tools: speed/battery settings, game mode guidance, network, touch settings guidance, FPS guidance, storage, display, crosshair, CPU stats, latency, refresh, thermal, recording, notification settings, brightness settings

## সীমা
Normal third-party Android app সাধারণত CPU/GPU clock/governor জোর করে বদলাতে, thermal protection bypass করতে, অন্য গেমের rendering settings inject করতে, universal in-game FPS পড়তে বা game sensitivity সরাসরি বদলাতে পারে না। এই UI-তে profile/preset সংরক্ষণ মানেই গেমের নিজস্ব sensitivity বদলে যাওয়া নয়। Crosshair/overlay কিছু গেমে নিষিদ্ধ বা ব্লক হতে পারে।

## Build status
এই source ZIP তৈরি করা হয়েছে; এখানে Android SDK/Gradle build চালিয়ে APK compile বা physical device runtime test করা হয়নি। GitHub Actions সফল না হওয়া পর্যন্ত APK build সফল হয়েছে বলা যাবে না।
