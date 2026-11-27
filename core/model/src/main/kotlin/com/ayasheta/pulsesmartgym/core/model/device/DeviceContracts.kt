package com.ayasheta.pulsesmartgym.core.model.device

enum class DeviceConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    CONNECTION_LOST,
    FAILED,
}

enum class ExecutionEnvironmentKind {
    GENUINE,
    DEMO,
}

enum class TrackingMode {
    MANUAL,
    DEVICE_ASSISTED,
}

enum class MeasurementOrigin {
    MANUAL_INPUT,
    SENSOR_TELEMETRY,
    SIMULATED_TELEMETRY,
    MANUAL_CORRECTION,
}

enum class RepClassification {
    FULL,
    PARTIAL,
}

data class RepMeasurement(
    val repIndex: Int,
    val classification: RepClassification,
    val totalDurationMs: Long? = null,
    val concentricDurationMs: Long? = null,
    val eccentricDurationMs: Long? = null,
    val angularRomDegrees: Double? = null,
    val linearRomCm: Double? = null,
    val peakAccelerationMetersPerSecondSquared: Double? = null,
    val origin: MeasurementOrigin = MeasurementOrigin.MANUAL_INPUT,
)
