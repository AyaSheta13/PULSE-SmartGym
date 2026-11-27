# PULSE Smart Gym — Application Installation Guide

## Prerequisites
- Android device or emulator running **Android 7.0 (API 24)** or higher (tested on API 36).
- For Debug APK testing: USB debugging enabled (`adb`).

## Installing the Debug APK (QA / Testing)
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.ayasheta.pulsesmartgym/.MainActivity
```

## Signing & Installing the Release APK / AAB
1. `app/build/outputs/apk/release/app-release-unsigned.apk` and `app/build/outputs/bundle/release/app-release.aab` are R8-minified and resource-shrunk, but **unsigned**.
2. Sign with your authorized keystore using `apksigner` (APK) or `jarsigner` (AAB) before installing on a device or uploading to Play Console.

## First-Time Launch Behavior
- On first launch, complete Onboarding, create a local account or choose Guest Mode, complete Profile Setup, and 3 starter workout plans (`Beginner Full Body`, `Upper Body Strength`, `Lower Body Foundation`) will be seeded automatically.
- Physical BLE hardware is **not** required to create, run, or track manual workouts.
