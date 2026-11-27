# PULSE Smart Gym — Known Limitations

1. **Physical ESP32 BLE Hardware Validation Pending (Gate C)**:
   - BLE service/characteristic UUIDs, binary packet framing (`0x01`, `0x02`, `0x03`), and sensor calibration offsets are **Proposed v1** until signed off with ESP32 firmware (`BleProtocolConfig.isProtocolConfirmed = false`, `isSensorCalibrated = false` by default).
   - Live over-the-air GATT discovery, notifications, and physical rep/ROM accuracy have not yet been tested against physical ESP32 hardware.
2. **Release Signing Credentials Not Configured**:
   - `app-release-unsigned.apk` and `app-release.aab` require signing with an authorized production keystore outside the repository.
3. **Local-Only Authentication**:
   - Accounts and sessions are stored locally in Room/DataStore (salted SHA-256); there is no cloud sync or remote password recovery backend.
4. **Live Performance Waveform Scope**:
   - `LivePerformanceWaveformCard` plots **per-repetition summary telemetry** (`Linear ROM cm`, `Angular ROM °`, `Peak Accel m/s²`, or `Rep Duration s`) rather than a continuous 50 Hz raw IMU waveform.
5. **Accessibility Walkthrough**:
   - Verified via Compose semantics and `1.3x` font-scale instrumentation tests; an interactive TalkBack audio walkthrough on a physical device was not performed.
