# P12 Release Checklist & Artifact Manifest

## Application Configuration
- **Application Name**: `PULSE` (`app/src/main/res/values/strings.xml`)
- **Application ID**: `com.ayasheta.pulsesmartgym`
- **Version**: `versionCode = 1`, `versionName = "1.0"`
- **SDK Levels**: `minSdk = 24`, `targetSdk = 36`, `compileSdk = 37` (with `desugar_jdk_libs:2.1.5`)
- **R8 / Shrinking**: `isMinifyEnabled = true`, `isShrinkResources = true` (release)
- **Backup**: `android:allowBackup="false"` + DB/DataStore exclusions in `backup_rules.xml` & `data_extraction_rules.xml`
- **Developer Diagnostics**: Gated behind `ApplicationInfo.FLAG_DEBUGGABLE` (`false` in release builds)

## Generated Build Artifacts & SHA-256 Checksums
| Artifact | Path | Size | SHA-256 |
| --- | --- | --- | --- |
| **Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` | `32,726,904` bytes | `ab0cdbc05b41c8fee22e3f4aec9729984f4fc0e0669decef8ac2e78c2a4c8efb` |
| **Release APK (Unsigned)** | `app/build/outputs/apk/release/app-release-unsigned.apk` | `15,808,270` bytes | `3b260d2b4913694735431677bb2acf9d03686db8064e7c22c78e0c32ed474bd4` |
| **Release AAB** | `app/build/outputs/bundle/release/app-release.aab` | `19,227,002` bytes | `83c33ed0650557061ba610714e71620a5a83b0ef87ef8546a9916dd19f64dea7` |

## Release Signing Blocker
- Production signing keystore credentials are not stored in the repository; `app-release-unsigned.apk` and `app-release.aab` require signing with an authorized release keystore before Play Store or production distribution.
