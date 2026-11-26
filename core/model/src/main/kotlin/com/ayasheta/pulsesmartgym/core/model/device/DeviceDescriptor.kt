package com.ayasheta.pulsesmartgym.core.model.device

data class DeviceDescriptor(
    val id: String,
    val name: String,
    val profileLabel: String,
    val rssiDbm: Int? = null,
    val signalLabel: String,
    val providerType: DeviceProviderType,
    val isSimulated: Boolean,
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
)
