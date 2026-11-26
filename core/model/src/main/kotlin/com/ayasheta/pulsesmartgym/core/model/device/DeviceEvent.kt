package com.ayasheta.pulsesmartgym.core.model.device

data class DeviceRepEvent(
    val eventId: String,
    val providerType: DeviceProviderType,
    val connectionGeneration: Long,
    val trackingWindowId: String,
    val sequenceNumber: Long? = null,
    val classification: RepClassification,
    val totalDurationMs: Long? = null,
    val concentricDurationMs: Long? = null,
    val eccentricDurationMs: Long? = null,
    val angularRomDegrees: Double? = null,
    val linearRomCm: Double? = null,
    val origin: MeasurementOrigin,
)

sealed interface DeviceEvent {
    data class RepRecorded(val rep: DeviceRepEvent) : DeviceEvent
    data class BatteryUpdated(
        val connectionGeneration: Long,
        val percent: Int,
    ) : DeviceEvent
}
