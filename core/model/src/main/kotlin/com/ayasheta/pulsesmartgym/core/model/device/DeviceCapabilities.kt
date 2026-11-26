package com.ayasheta.pulsesmartgym.core.model.device

import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory

enum class DeviceProviderType {
    SIMULATED,
    REAL_BLE,
}

data class DeviceCapabilities(
    val automaticRepEvents: Boolean = false,
    val fullPartialClassification: Boolean = false,
    val angularRom: Boolean = false,
    val linearRom: Boolean = false,
    val repDuration: Boolean = false,
    val concentricDuration: Boolean = false,
    val eccentricDuration: Boolean = false,
    val batteryTelemetry: Boolean = false,
    val velocity: Boolean = false,
    val supportedTrackingCategories: Set<TrackingCategory> = emptySet(),
) {
    fun supportsTrackingCategory(category: TrackingCategory): Boolean =
        automaticRepEvents && category in supportedTrackingCategories
}
