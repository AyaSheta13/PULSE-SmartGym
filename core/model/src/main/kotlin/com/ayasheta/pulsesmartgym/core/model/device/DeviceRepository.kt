package com.ayasheta.pulsesmartgym.core.model.device

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val activeProviderType: StateFlow<DeviceProviderType>
    val linkState: StateFlow<DeviceLinkState>
    val discoveredDevices: StateFlow<List<DeviceDescriptor>>
    val events: Flow<DeviceEvent>
    val isDemoSessionActive: StateFlow<Boolean>
    val diagnostics: StateFlow<HardwareDiagnosticsSnapshot>

    fun selectProvider(providerType: DeviceProviderType)
    suspend fun startScan(): DeviceOperationResult
    suspend fun stopScan(): DeviceOperationResult
    suspend fun connect(deviceId: String): DeviceOperationResult
    suspend fun cancelConnection(): DeviceOperationResult
    suspend fun disconnect(): DeviceOperationResult
    suspend fun reconnect(): DeviceOperationResult
    fun setDemoSessionActive(active: Boolean)
    fun setActiveTrackingWindow(trackingWindowId: String)
    fun onOwnerChanged()
}
