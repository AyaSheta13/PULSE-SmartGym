package com.ayasheta.pulsesmartgym.core.model.device

enum class HardwareDataOrigin {
    NONE,
    REAL_BLE,
    RAW_REPLAY,
    SIMULATED,
}

data class HardwareDiagnosticsSnapshot(
    val dataOrigin: HardwareDataOrigin = HardwareDataOrigin.NONE,
    val providerType: DeviceProviderType = DeviceProviderType.REAL_BLE,
    val connectionState: DeviceConnectionState = DeviceConnectionState.DISCONNECTED,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val protocolConfirmed: Boolean = false,
    val serviceDiscoveryStatus: String = "Not started",
    val notificationSubscriptionStatus: String = "Not subscribed",
    val incomingPacketCount: Long = 0L,
    val invalidPacketCount: Long = 0L,
    val droppedPacketCount: Long = 0L,
    val duplicatePacketCount: Long = 0L,
    val packetsPerSecond: Double = 0.0,
    val lastPacketHex: String? = null,
    val lastRawSensorSummary: String? = null,
    val lastDecodedValuesSummary: String? = null,
    val measurementUnits: String = "deg, cm, m/s^2, ms",
    val lastReceivedTimestampMs: Long? = null,
    val detectedRepCount: Int = 0,
    val negotiatedMtu: Int? = null,
    val isCalibrated: Boolean = false,
    val calibrationStatus: String = "Per-exercise config active (firmware calibration unconfirmed)",
    val lastError: String? = null,
)
