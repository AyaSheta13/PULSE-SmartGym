package com.ayasheta.pulsesmartgym.core.data.device

import com.ayasheta.pulsesmartgym.core.model.device.DeviceDataSource
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDeviceRepository(
    val fakeDataSource: FakeDeviceDataSource = FakeDeviceDataSource(),
    val realBleDataSource: RealBleDeviceDataSource = RealBleDeviceDataSource(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : DeviceRepository {

    private val _activeProviderType = MutableStateFlow(DeviceProviderType.SIMULATED)
    override val activeProviderType: StateFlow<DeviceProviderType> = _activeProviderType.asStateFlow()

    private val _isDemoSessionActive = MutableStateFlow(false)
    override val isDemoSessionActive: StateFlow<Boolean> = _isDemoSessionActive.asStateFlow()

    private fun currentSource(type: DeviceProviderType = _activeProviderType.value): DeviceDataSource =
        when (type) {
            DeviceProviderType.SIMULATED -> fakeDataSource
            DeviceProviderType.REAL_BLE -> realBleDataSource
        }

    override val linkState: StateFlow<DeviceLinkState> = _activeProviderType
        .flatMapLatest { currentSource(it).connectionState }
        .stateIn(scope, SharingStarted.Eagerly, fakeDataSource.connectionState.value)

    override val discoveredDevices: StateFlow<List<DeviceDescriptor>> = _activeProviderType
        .flatMapLatest { currentSource(it).discoveredDevices }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val events: Flow<DeviceEvent> = _activeProviderType
        .flatMapLatest { currentSource(it).events }

    override fun selectProvider(providerType: DeviceProviderType) {
        if (_activeProviderType.value == providerType) return
        currentSource(_activeProviderType.value).resetToDisconnected()
        _activeProviderType.value = providerType
        if (providerType == DeviceProviderType.REAL_BLE) {
            _isDemoSessionActive.value = false
        }
    }

    override suspend fun startScan(): DeviceOperationResult = currentSource().startScan()

    override suspend fun stopScan(): DeviceOperationResult = currentSource().stopScan()

    override suspend fun connect(deviceId: String): DeviceOperationResult = currentSource().connect(deviceId)

    override suspend fun cancelConnection(): DeviceOperationResult = currentSource().cancelConnection()

    override suspend fun disconnect(): DeviceOperationResult = currentSource().disconnect()

    override suspend fun reconnect(): DeviceOperationResult = currentSource().reconnect()

    override fun setDemoSessionActive(active: Boolean) {
        _isDemoSessionActive.value = active
    }

    override fun onOwnerChanged() {
        _isDemoSessionActive.value = false
        scope.launch {
            fakeDataSource.disconnect()
            realBleDataSource.disconnect()
        }
        fakeDataSource.resetToDisconnected()
        realBleDataSource.resetToDisconnected()
    }
}
