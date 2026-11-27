# PULSE Smart Gym — Hardware–Android Communication Contract

**Status**: Android-side transport, decoder, validation, and persistence layers are implemented against a configurable `BleProtocolConfig` (`isProtocolConfirmed = false` by default). All UUIDs, command opcodes, and binary packet layouts below are **Proposed v1** until signed off with ESP32 firmware.

---

## 1. Confirmed vs. Proposed Specifications

| Item | Status | Current Value / Behavior |
| --- | --- | --- |
| Android Runtime Permissions | **Confirmed** | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` (API 31+); `ACCESS_FINE_LOCATION` (API ≤ 30) |
| BLE Transport Mode | **Confirmed** | GATT Client (`BluetoothDevice.TRANSPORT_LE`), CCCD `00002902-0000-1000-8000-00805f9b34fb` |
| Domain Measurement Units | **Confirmed** | Angular ROM (`deg`), Linear ROM (`cm`), Peak Accel (`m/s²`), Durations (`ms`), Battery (`%`) |
| Unavailable Metric Sentinel | **Confirmed** | `0xFFFF` (uint16) decoded as `null` — never substituted with `0` |
| Service & Characteristic UUIDs | **Proposed v1** | Nordic-UART-style custom UUIDs (configurable in `BleProtocolConfig`) |
| Binary Packet Framing & CRC-8 | **Proposed v1** | Little-endian, 1-byte version (`0x01`), 1-byte type, trailing CRC-8 (`poly = 0x07`) |
| Sampling Rate (Continuous IMU) | **Proposed v1** | 50 Hz (`20 ms` interval) for raw sample diagnostics; per-rep summaries on rep completion |
| Calibration & Tracking Commands | **Proposed v1** | Optional write characteristic (`0x10` Start Window, `0x11` Stop Window, `0x12` Zero/Calibrate) |

---

## 2. BLE Service & Characteristic UUIDs (Proposed v1)

| Role | Proposed UUID | Properties | Notes |
| --- | --- | --- | --- |
| Primary Telemetry Service | `6e400001-b5a3-f393-e0a9-e50e24dcca9e` | Primary Service | Advertised by ESP32 (`PULSE-*` name prefix) |
| Telemetry Notify Characteristic | `6e400003-b5a3-f393-e0a9-e50e24dcca9e` | `NOTIFY` | Streams `0x01`, `0x02`, `0x03` packets |
| Command Write Characteristic | `6e400002-b5a3-f393-e0a9-e50e24dcca9e` | `WRITE` | Optional start/stop/calibrate commands |
| Battery Service Characteristic | `00002a19-0000-1000-8000-00805f9b34fb` | `READ` / `NOTIFY` | Standard BLE Battery Level (`0–100%`) |

---

## 3. Binary Packet Structure & Encoding (Proposed v1)

All multi-byte integers are **little-endian**. Every packet ends with a **1-byte CRC-8** (`poly = 0x07`, `init = 0x00`) over bytes `0 .. length - 2`.

### 3.1 Packet Type `0x01` — Repetition Summary Telemetry (22 bytes)

| Offset | Size | Field | Type / Scale | Notes |
| --- | --- | --- | --- | --- |
| 0 | 1 | `protocolVersion` | `uint8` | Must be `0x01` |
| 1 | 1 | `packetType` | `uint8` | `0x01` |
| 2 | 4 | `sequenceNumber` | `uint32` LE | Monotonic per connection |
| 6 | 1 | `flags` | `uint8` bitmask | Bit 0: `0 = FULL`, `1 = PARTIAL` |
| 7 | 2 | `totalDurationMs` | `uint16` LE (`ms`) | `0xFFFF` = unavailable |
| 9 | 2 | `concentricDurationMs` | `uint16` LE (`ms`) | `0xFFFF` = unavailable |
| 11 | 2 | `eccentricDurationMs` | `uint16` LE (`ms`) | `0xFFFF` = unavailable |
| 13 | 2 | `angularRomX10` | `uint16` LE (`deg * 10`) | e.g., `964` = `96.4°`; `0xFFFF` = unavailable |
| 15 | 2 | `linearRomX10` | `uint16` LE (`cm * 10`) | e.g., `442` = `44.2 cm`; `0xFFFF` = unavailable |
| 17 | 2 | `peakAccelX100` | `uint16` LE (`m/s² * 100`) | e.g., `345` = `3.45 m/s²`; `0xFFFF` = unavailable |
| 19 | 2 | `reserved` | `uint16` | `0x0000` |
| 21 | 1 | `crc8` | `uint8` | CRC-8 (`0x07`) over bytes `0..20` |

### 3.2 Packet Type `0x02` — Continuous IMU + Distance Sample (22 bytes, Proposed 50 Hz)

| Offset | Size | Field | Type / Scale | Notes |
| --- | --- | --- | --- | --- |
| 0 | 1 | `protocolVersion` | `uint8` | `0x01` |
| 1 | 1 | `packetType` | `uint8` | `0x02` |
| 2 | 4 | `sequenceNumber` | `uint32` LE | Monotonic |
| 6 | 2 | `timestampOffsetMs` | `uint16` LE (`ms`) | Relative to set start |
| 8 | 2 | `accelX_mg` | `int16` LE (`mg`) | Converted to `m/s²` on Android |
| 10 | 2 | `accelY_mg` | `int16` LE (`mg`) | Converted to `m/s²` on Android |
| 12 | 2 | `accelZ_mg` | `int16` LE (`mg`) | Converted to `m/s²` on Android |
| 14 | 2 | `gyroZ_dpsX10` | `int16` LE (`deg/s * 10`) | Angular velocity |
| 16 | 2 | `distanceMm` | `uint16` LE (`mm`) | Converted to `cm`; `0xFFFF` = unavailable |
| 18 | 3 | `reserved` | `uint8[3]` | `0x00` |
| 21 | 1 | `crc8` | `uint8` | CRC-8 over bytes `0..20` |

### 3.3 Packet Type `0x03` — Battery Status (8 bytes)

| Offset | Size | Field | Type / Scale |
| --- | --- | --- | --- |
| 0 | 1 | `protocolVersion` | `uint8` (`0x01`) |
| 1 | 1 | `packetType` | `uint8` (`0x03`) |
| 2 | 4 | `sequenceNumber` | `uint32` LE |
| 6 | 1 | `batteryPercent` | `uint8` (`0..100`) |
| 7 | 1 | `crc8` | `uint8` |

---

## 4. Firmware vs. Android Responsibility Split

To prevent duplicated or conflicting calculations:
- **ESP32 Firmware Owns**:
  - High-rate IMU + distance sensor polling, hardware filtering, zeroing/calibration offsets, rep segmentation (concentric/eccentric phase detection), and emitting `0x01` Rep Summary packets (plus optional `0x02` raw samples).
- **Android Application Owns**:
  - BLE scanning, permissions, GATT lifecycle, CRC-8 + bounds validation (`BlePacketDecoder`), sequence-number deduplication & gap detection, exercise capability gating (`ExerciseTrackingCatalog.resolveEffectiveCapabilities`), `DeviceEventNormalizer` session/set/generation guards, UI state & waveform rendering, Room persistence, and historical analytics (`WorkoutPerformanceAnalytics`).

---

## 5. Sensor Expectations

- **IMU (Accelerometer + Gyroscope)**: Expected to provide gravity-compensated peak acceleration (`m/s²`) and angular range of motion (`deg`) for angular/compound/isolation exercises.
- **Distance Sensor (ToF / Linear Encoder)**: Expected to provide linear displacement (`cm`) for linear/cable/bar-path exercises. If absent or uncalibrated for an exercise, firmware sends `0xFFFF` and Android gates it out cleanly.

---

## 6. Example Valid & Invalid Packets

- **Valid Rep Telemetry (`0x01`, seq=1, FULL, 2150ms, conc=920ms, ecc=1230ms, ang=96.4°, lin=44.2cm, peakA=3.45m/s²)**:
  `01 01 01 00 00 00 00 66 08 98 03 CE 04 C4 03 BA 01 59 01 00 00 <crc8>` (22 bytes)
- **Valid Battery (`0x03`, seq=2, 87%)**:
  `01 03 02 00 00 00 57 <crc8>` (8 bytes)
- **Invalid — Bad CRC**: Any 22-byte `0x01` packet whose trailing byte does not equal `crc8(bytes[0..20])` → rejected as `DecodedBlePacket.Malformed`, increments `invalidPacketCount`.
- **Invalid — Truncated**: `01 01 00 01 FF` (5 bytes) → rejected as `Packet too short` / `Invalid rep telemetry length`.
- **Invalid — Battery > 100**: `0x03` with `batteryPercent = 120` → rejected as `Battery percent out of range`.
