package com.ayasheta.pulsesmartgym.core.data.device

import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory

enum class DeviceSimulationScenario {
    DEFAULT_AVAILABLE,
    NO_DEVICES,
    CONNECTION_TIMEOUT,
    CONNECTION_FAILURE,
    RECONNECT_FAILURE,
}

object SimulatedDeviceCatalog {
    val demoCapabilities = DeviceCapabilities(
        automaticRepEvents = true,
        fullPartialClassification = true,
        angularRom = true,
        linearRom = true,
        repDuration = true,
        concentricDuration = true,
        eccentricDuration = true,
        batteryTelemetry = true,
        velocity = false,
        peakAcceleration = true,
        supportedTrackingCategories = setOf(
            TrackingCategory.LINEAR,
            TrackingCategory.ANGULAR,
        ),
    )

    val pulse001 = DeviceDescriptor(
        id = "sim_pulse_001",
        name = "PULSE-001",
        profileLabel = "Demo Profile A",
        rssiDbm = -52,
        signalLabel = "Strong Signal",
        providerType = DeviceProviderType.SIMULATED,
        isSimulated = true,
        capabilities = demoCapabilities,
    )

    val pulse002 = DeviceDescriptor(
        id = "sim_pulse_002",
        name = "PULSE-002",
        profileLabel = "Demo Profile B",
        rssiDbm = -71,
        signalLabel = "Moderate Signal",
        providerType = DeviceProviderType.SIMULATED,
        isSimulated = true,
        capabilities = demoCapabilities,
    )

    val defaultDevices: List<DeviceDescriptor> = listOf(pulse001, pulse002)

    fun sampleBatteryFor(deviceId: String): Int = when (deviceId) {
        pulse001.id -> 88
        pulse002.id -> 76
        else -> 80
    }
}
