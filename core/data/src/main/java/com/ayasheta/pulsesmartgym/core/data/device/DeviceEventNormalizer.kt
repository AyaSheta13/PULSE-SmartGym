package com.ayasheta.pulsesmartgym.core.data.device

import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepEvent
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement

sealed interface NormalizedRepResult {
    data class Accepted(val measurement: RepMeasurement) : NormalizedRepResult
    data class Rejected(val reason: String) : NormalizedRepResult
}

object DeviceEventNormalizer {

    const val MAX_ACCEPTED_EVENTS_PER_SET: Int = 100
    const val MAX_RETAINED_EVENT_IDS: Int = 120

    fun normalizeAndValidate(
        event: DeviceRepEvent,
        expectedProviderType: DeviceProviderType,
        expectedConnectionGeneration: Long,
        expectedTrackingWindowId: String,
        isConnected: Boolean,
        isSubmitting: Boolean,
        capabilities: DeviceCapabilities,
        seenEventIds: Set<String>,
        nextRepIndex: Int,
        isOwnerAuthenticated: Boolean = true,
        isSessionActive: Boolean = true,
        isSetValidAndUnrecorded: Boolean = true,
        isAcceptingEvents: Boolean = true,
        expectedEnvironment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.DEMO,
        maxAcceptedEvents: Int = MAX_ACCEPTED_EVENTS_PER_SET,
    ): NormalizedRepResult {
        if (!isOwnerAuthenticated) {
            return NormalizedRepResult.Rejected("Owner is not authenticated")
        }
        if (!isSessionActive || !isSetValidAndUnrecorded) {
            return NormalizedRepResult.Rejected("Session or set is not active/unrecorded")
        }
        if (!isAcceptingEvents || isSubmitting) {
            return NormalizedRepResult.Rejected("Counter is not accepting device events")
        }
        if (!isConnected) {
            return NormalizedRepResult.Rejected("Provider is not connected")
        }
        if (expectedProviderType == DeviceProviderType.SIMULATED && expectedEnvironment != ExecutionEnvironmentKind.DEMO) {
            return NormalizedRepResult.Rejected("Simulated provider cannot emit events in GENUINE environment")
        }
        if (!capabilities.automaticRepEvents) {
            return NormalizedRepResult.Rejected("Provider does not support automatic rep events")
        }
        if (nextRepIndex > maxAcceptedEvents) {
            return NormalizedRepResult.Rejected("Exceeded maximum accepted events ($maxAcceptedEvents) for set")
        }
        if (event.providerType != expectedProviderType) {
            return NormalizedRepResult.Rejected("Provider mismatch")
        }
        if (event.connectionGeneration != expectedConnectionGeneration) {
            return NormalizedRepResult.Rejected("Stale connection generation")
        }
        if (event.trackingWindowId != expectedTrackingWindowId) {
            return NormalizedRepResult.Rejected("Wrong tracking window")
        }
        if (event.eventId.isBlank() || event.eventId in seenEventIds) {
            return NormalizedRepResult.Rejected("Duplicate or blank eventId")
        }
        if (expectedProviderType == DeviceProviderType.SIMULATED && event.origin != MeasurementOrigin.SIMULATED_TELEMETRY) {
            return NormalizedRepResult.Rejected("Simulated provider requires SIMULATED_TELEMETRY origin")
        }
        if (expectedProviderType == DeviceProviderType.REAL_BLE && event.origin != MeasurementOrigin.SENSOR_TELEMETRY) {
            return NormalizedRepResult.Rejected("Real BLE provider requires SENSOR_TELEMETRY origin")
        }
        val totalDurationMs = event.totalDurationMs
        if (totalDurationMs != null && totalDurationMs < 0L) {
            return NormalizedRepResult.Rejected("Negative totalDurationMs")
        }
        val concentricDurationMs = event.concentricDurationMs
        if (concentricDurationMs != null && concentricDurationMs < 0L) {
            return NormalizedRepResult.Rejected("Negative concentricDurationMs")
        }
        val eccentricDurationMs = event.eccentricDurationMs
        if (eccentricDurationMs != null && eccentricDurationMs < 0L) {
            return NormalizedRepResult.Rejected("Negative eccentricDurationMs")
        }
        val angularRomDegrees = event.angularRomDegrees
        if (angularRomDegrees != null && (!angularRomDegrees.isFinite() || angularRomDegrees < 0.0)) {
            return NormalizedRepResult.Rejected("Invalid angularRomDegrees")
        }
        val linearRomCm = event.linearRomCm
        if (linearRomCm != null && (!linearRomCm.isFinite() || linearRomCm < 0.0)) {
            return NormalizedRepResult.Rejected("Invalid linearRomCm")
        }

        val measurement = RepMeasurement(
            repIndex = nextRepIndex,
            classification = event.classification,
            totalDurationMs = if (capabilities.repDuration) totalDurationMs else null,
            concentricDurationMs = if (capabilities.concentricDuration) concentricDurationMs else null,
            eccentricDurationMs = if (capabilities.eccentricDuration) eccentricDurationMs else null,
            angularRomDegrees = if (capabilities.angularRom) angularRomDegrees else null,
            linearRomCm = if (capabilities.linearRom) linearRomCm else null,
            origin = event.origin,
        )
        return NormalizedRepResult.Accepted(measurement)
    }
}
