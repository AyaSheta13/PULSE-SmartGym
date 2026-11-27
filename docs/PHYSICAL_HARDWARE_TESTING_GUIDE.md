# PULSE Smart Gym — Physical Hardware Testing Guide

Use this 14-stage checklist when connecting the physical ESP32 + IMU/distance sensors. Test stages in order so any failure is isolated to its exact layer.

---

## Stage 1: ESP32 BLE Advertisement Verification
- **Expected Result**: ESP32 advertises BLE with `PULSE-*` device name and primary service UUID matching `BleProtocolConfig.serviceUuid`.
- **How to Verify**: Check nRF Connect (or Android BLE scanner) on a test phone before opening PULSE.
- **Logs / Tools**: Serial monitor on ESP32 (`esp_ble_gap_start_advertising`), nRF Connect RSSI/Adv Data.
- **Failure Attribution**: **Hardware / Firmware** if device does not appear in nRF Connect.

## Stage 2: Android Device Discovery
- **Expected Result**: `DeviceDiscoveryScreen` lists the ESP32 pod under `REAL_BLE` provider without permission errors.
- **How to Verify**: Switch provider to `Real BLE` in `MyDeviceScreen`, grant `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`, tap Scan.
- **Logs / Tools**: `HardwareDiagnosticsScreen` (`Connection State = SCANNING`), Logcat `AndroidBleTransport`.
- **Failure Attribution**: **BLE Transport / Permissions** if nRF Connect sees the device but PULSE does not.

## Stage 3: GATT Connection & MTU Negotiation
- **Expected Result**: Selecting the device transitions `CONNECTING -> CONNECTED` within `connectTimeoutMs` (`10,000 ms`), and `AndroidBleTransport` negotiates ATT MTU (`requestMtu(64)` or falls back cleanly to 23-byte ATT MTU / 20-byte payload).
- **How to Verify**: Observe `MyDeviceScreen` / `HardwareDiagnosticsScreen` (`Device Name`, `Device Address`, `Service Discovery` MTU log).
- **Logs / Tools**: `HardwareDiagnosticsScreen` (`Last Error`), Logcat `BluetoothGattCallback.onConnectionStateChange`.
- **Failure Attribution**: **BLE Transport** (timeout/GATT status 133) or **Firmware** (device rejects connection).

## Stage 4: Service & Characteristic Discovery
- **Expected Result**: `ServicesDiscovered` finds `serviceUuid` and `telemetryCharacteristicUuid`; CCCD notification descriptor write succeeds.
- **How to Verify**: `HardwareDiagnosticsScreen` shows `Service Discovery = Discovered N service(s)` and `Notifications = Subscribed (<uuid>)`.
- **Logs / Tools**: `HardwareDiagnosticsScreen`, `AndroidBleTransport.onServicesDiscovered` & `onDescriptorWrite`.
- **Failure Attribution**: **Firmware / Contract** if UUIDs mismatch or CCCD descriptor `0x2902` is missing.

## Stage 5: Raw Notification Reception
- **Expected Result**: `Incoming Packets` counter increments in `HardwareDiagnosticsScreen` as the sensor emits notifications.
- **How to Verify**: Trigger a rep or 50 Hz stream on ESP32; watch `Incoming Packets`, `Data Rate (pkt/s)`, and `Last Packet Hex`.
- **Logs / Tools**: `HardwareDiagnosticsScreen` (`diag_incoming_packets`, `diag_last_packet_hex`).
- **Failure Attribution**: **Firmware** if `Subscribed` is shown but `Incoming Packets` stays `0`.

## Stage 6: Packet Decoding
- **Expected Result**: `Last Decoded` shows parsed fields (`Rep FULL/PARTIAL`, `LinROM`, `AngROM`, `PeakA`, `Dur`) and `Invalid Packets == 0`.
- **How to Verify**: Inspect `HardwareDiagnosticsScreen` -> `Last Raw & Decoded Packet`.
- **Logs / Tools**: `BlePacketDecoder`, `HardwareDiagnosticsScreen` (`diag_invalid_packets`, `diag_last_error`).
- **Failure Attribution**: **Decoding / Firmware** if `Invalid Packets` increments (endianness, length, version, or CRC-8 mismatch).

## Stage 7: Sensor Value Validation
- **Expected Result**: Out-of-range/NaN/negative values are rejected; `0xFFFF` sentinels decode as `null` without becoming `0`.
- **How to Verify**: Inspect `diag_last_decoded` and `SetExecutionScreen` (`Live Performance`).
- **Logs / Tools**: `DeviceEventNormalizer` rejection reasons.
- **Failure Attribution**: **Measurement Processing** if valid physical numbers are rejected, or **Firmware** if corrupted floats/sentinels are sent.

## Stage 8: Calibration & Exercise Capability Gating
- **Expected Result**: Only metrics supported by both the device and the active exercise (`ExerciseTrackingCatalog`) are accepted.
- **How to Verify**: Run an angular exercise (e.g. Dumbbell Curl) vs linear exercise (e.g. Barbell Squat) and verify ROM units (`°` vs `cm`).
- **Logs / Tools**: `HardwareDiagnosticsScreen` (`Calibration`), `SetExecutionViewModel`.
- **Failure Attribution**: **Measurement Processing / Firmware** if wrong units or uncalibrated offsets appear.

## Stage 9: Repetition Detection
- **Expected Result**: Performing 1 physical rep increments `REPS DONE` on `SetExecutionScreen` by `1` (and `FULL`/`PARTIAL` counts match).
- **How to Verify**: Start a set (`Start Set`), perform 3 reps, verify `3 / 10` on `UnifiedRepAndWeightCard`.
- **Logs / Tools**: `HardwareDiagnosticsScreen` (`Detected Reps`), `SetExecutionViewModel.observeDeviceEvents`.
- **Failure Attribution**: **Firmware** if rep is not emitted; **Measurement Processing** if `trackingWindowId` or `connectionGeneration` rejects it.

## Stage 10: Live Performance Waveform Updates
- **Expected Result**: Gray placeholder is replaced by a coral smooth Bézier curve plotting actual measured ROM/accel/duration per rep with badge `Rep #N` (no `• Sim`).
- **How to Verify**: Perform 3+ reps and observe `LivePerformanceWaveformCard` updating automatically.
- **Logs / Tools**: `SetExecutionComponents.kt` (`LivePerformanceWaveformCard`).
- **Failure Attribution**: **UI Binding** if `Detected Reps` increments in Diagnostics but waveform does not update.

## Stage 11: Workout Completion
- **Expected Result**: Finishing the set and completing the workout displays measured ROM, rep duration, peak accel, and full/partial breakdown on `WorkoutResultScreen`.
- **How to Verify**: Tap `Finish Set` -> complete workout -> inspect `Session Performance` card.
- **Logs / Tools**: `WorkoutResultViewModel`, `WorkoutPerformanceAnalytics`.
- **Failure Attribution**: **UI / Analytics** if metrics disappear after finishing set.

## Stage 12: Database Persistence
- **Expected Result**: Force-closing and relaunching the app preserves all measured `RepRecordEntity` rows in Room.
- **How to Verify**: Kill app, reopen, open `History` -> `Session Details`.
- **Logs / Tools**: `RoomWorkoutSessionRepository`, Database Inspector (`rep_records` table).
- **Failure Attribution**: **Persistence** if records vanish after restart.

## Stage 13: History & Analytics Verification
- **Expected Result**: `TrainingProgressScreen` (`Performance Trends`) reflects real `SENSOR_TELEMETRY` sessions (and excludes `SIMULATED_TELEMETRY`).
- **How to Verify**: Open `Progress` tab for the current week and verify the exercise trend point matches `Session Details`.
- **Logs / Tools**: `TrainingProgressViewModel`, `WorkoutPerformanceAnalytics`.
- **Failure Attribution**: **Measurement Processing / Analytics**.

## Stage 14: Connection Loss & Reconnection
- **Expected Result**: Powering off ESP32 mid-set transitions status to `Disconnected` / `CONNECTION_LOST`, triggers `ConnectionLostRecoverySheet` without generating fake reps, and allows `Reconnect` or `Switch to Manual`.
- **How to Verify**: Power off ESP32 during an active set, verify no phantom reps appear, then power back on and tap `Reconnect` (or `Switch to Manual`).
- **Logs / Tools**: `HardwareDiagnosticsScreen`, `AndroidBleTransport.onConnectionStateChange`.
- **Failure Attribution**: **BLE Transport / UI**.
