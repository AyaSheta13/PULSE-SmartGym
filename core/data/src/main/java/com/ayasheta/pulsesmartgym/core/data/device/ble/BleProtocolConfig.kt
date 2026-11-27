package com.ayasheta.pulsesmartgym.core.data.device.ble

/**
 * Configurable BLE protocol parameters for physical hardware integration.
 *
 * Firmware UUIDs, packet framing, and sensor calibration are NOT finalized until hardware/firmware
 * sign-off; both [isProtocolConfirmed] and [isSensorCalibrated] default to false so unconfigured
 * production builds fail honestly and never enable uncalibrated automatic rep detection.
 */
data class BleProtocolConfig(
    val isProtocolConfirmed: Boolean = false,
    val isSensorCalibrated: Boolean = false,
    val protocolVersion: Int = 1,
    val serviceUuid: String? = null,
    val telemetryCharacteristicUuid: String? = null,
    val commandCharacteristicUuid: String? = null,
    val batteryCharacteristicUuid: String? = null,
    val deviceNamePrefixes: List<String> = listOf("PULSE"),
    val requestedMtu: Int = 64,
    val scanTimeoutMs: Long = 12_000L,
    val connectTimeoutMs: Long = 10_000L,
) {
    fun hasValidUuids(): Boolean =
        !serviceUuid.isNullOrBlank() && !telemetryCharacteristicUuid.isNullOrBlank()

    companion object {
        /**
         * Proposed v1 UUIDs for pre-hardware integration testing & raw-transport replay.
         * Must be replaced or confirmed once ESP32 firmware UUIDs are finalized.
         */
        fun proposedTestConfig(isSensorCalibrated: Boolean = false): BleProtocolConfig = BleProtocolConfig(
            isProtocolConfirmed = true,
            isSensorCalibrated = isSensorCalibrated,
            protocolVersion = 1,
            serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
            telemetryCharacteristicUuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            commandCharacteristicUuid = "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
            batteryCharacteristicUuid = "00002a19-0000-1000-8000-00805f9b34fb",
        )
    }
}
