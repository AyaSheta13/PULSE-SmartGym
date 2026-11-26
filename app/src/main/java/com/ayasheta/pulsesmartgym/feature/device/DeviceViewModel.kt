package com.ayasheta.pulsesmartgym.feature.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.data.device.DeviceSimulationScenario
import com.ayasheta.pulsesmartgym.core.data.device.FakeDeviceDataSource
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.feature.device.permissions.BluetoothReadinessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DiscoveryScreenStage {
    SEARCHING,
    AVAILABLE_DEVICES,
    NO_DEVICES,
    CONNECTING,
    CONNECTED_SUCCESS,
    CONNECTION_FAILED,
    PROTOCOL_NOT_CONFIGURED,
    PERMISSION_OR_BLUETOOTH_BLOCKED,
}

data class DeviceUiState(
    val linkState: DeviceLinkState = DeviceLinkState(),
    val discoveredDevices: List<DeviceDescriptor> = emptyList(),
    val activeProviderType: DeviceProviderType = DeviceProviderType.SIMULATED,
    val isDemoSessionActive: Boolean = false,
    val bluetoothReadiness: BluetoothReadinessState = BluetoothReadinessState.READY,
    val discoveryStage: DiscoveryScreenStage = DiscoveryScreenStage.SEARCHING,
    val showTroubleshootingInfo: Boolean = false,
)

class DeviceViewModel(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val fakeDeviceDataSource: FakeDeviceDataSource? = null,
) : ViewModel() {

    private val bluetoothReadinessFlow = MutableStateFlow(BluetoothReadinessState.READY)
    private val showTroubleshootingFlow = MutableStateFlow(false)
    private var lastOwnerId: String? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { auth ->
                val currentOwner = (auth as? AuthState.Authenticated)?.user?.id
                if (lastOwnerId != null && lastOwnerId != currentOwner) {
                    deviceRepository.onOwnerChanged()
                    showTroubleshootingFlow.value = false
                }
                lastOwnerId = currentOwner
            }
        }
    }

    val uiState: StateFlow<DeviceUiState> = combine(
        deviceRepository.linkState,
        deviceRepository.discoveredDevices,
        deviceRepository.activeProviderType,
        deviceRepository.isDemoSessionActive,
        combine(bluetoothReadinessFlow, showTroubleshootingFlow) { r, t -> r to t },
    ) { link, devices, provider, isDemo, (readiness, troubleshooting) ->
        val stage = when {
            provider == DeviceProviderType.REAL_BLE &&
                link.lastError == DeviceOperationError.PROTOCOL_NOT_CONFIGURED ->
                DiscoveryScreenStage.PROTOCOL_NOT_CONFIGURED
            provider == DeviceProviderType.REAL_BLE &&
                readiness != BluetoothReadinessState.READY ->
                DiscoveryScreenStage.PERMISSION_OR_BLUETOOTH_BLOCKED
            link.phase == DeviceConnectionState.SCANNING -> DiscoveryScreenStage.SEARCHING
            link.phase == DeviceConnectionState.CONNECTING -> DiscoveryScreenStage.CONNECTING
            link.phase == DeviceConnectionState.FAILED -> DiscoveryScreenStage.CONNECTION_FAILED
            link.phase == DeviceConnectionState.CONNECTED -> DiscoveryScreenStage.CONNECTED_SUCCESS
            link.hasSearched && devices.isEmpty() -> DiscoveryScreenStage.NO_DEVICES
            devices.isNotEmpty() -> DiscoveryScreenStage.AVAILABLE_DEVICES
            else -> DiscoveryScreenStage.SEARCHING
        }
        DeviceUiState(
            linkState = link,
            discoveredDevices = devices,
            activeProviderType = provider,
            isDemoSessionActive = isDemo,
            bluetoothReadiness = readiness,
            discoveryStage = stage,
            showTroubleshootingInfo = troubleshooting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeviceUiState(),
    )

    fun selectProvider(providerType: DeviceProviderType) {
        deviceRepository.selectProvider(providerType)
        if (providerType == DeviceProviderType.SIMULATED) {
            fakeDeviceDataSource?.setScenario(DeviceSimulationScenario.DEFAULT_AVAILABLE)
        }
    }

    fun updateBluetoothReadiness(readiness: BluetoothReadinessState) {
        bluetoothReadinessFlow.value = readiness
    }

    fun startScan(simulateNoDevices: Boolean = false) {
        showTroubleshootingFlow.value = false
        if (deviceRepository.activeProviderType.value == DeviceProviderType.SIMULATED) {
            fakeDeviceDataSource?.setScenario(
                if (simulateNoDevices) {
                    DeviceSimulationScenario.NO_DEVICES
                } else {
                    DeviceSimulationScenario.DEFAULT_AVAILABLE
                },
            )
        }
        viewModelScope.launch {
            deviceRepository.startScan()
        }
    }

    fun simulateNoDevicesFound() {
        viewModelScope.launch {
            deviceRepository.stopScan()
            fakeDeviceDataSource?.setScenario(DeviceSimulationScenario.NO_DEVICES)
            deviceRepository.startScan()
        }
    }

    fun cancelSearch() {
        viewModelScope.launch {
            deviceRepository.stopScan()
        }
    }

    fun connect(deviceId: String, simulateTimeout: Boolean = false) {
        if (deviceRepository.activeProviderType.value == DeviceProviderType.SIMULATED) {
            fakeDeviceDataSource?.setScenario(
                if (simulateTimeout) {
                    DeviceSimulationScenario.CONNECTION_TIMEOUT
                } else {
                    DeviceSimulationScenario.DEFAULT_AVAILABLE
                },
            )
            deviceRepository.setDemoSessionActive(true)
        }
        viewModelScope.launch {
            deviceRepository.connect(deviceId)
        }
    }

    fun simulateHandshakeTimeout() {
        val targetId = uiState.value.linkState.selectedDevice?.id ?: return
        viewModelScope.launch {
            deviceRepository.cancelConnection()
            fakeDeviceDataSource?.setScenario(DeviceSimulationScenario.CONNECTION_TIMEOUT)
            deviceRepository.connect(targetId)
        }
    }

    fun cancelConnection() {
        viewModelScope.launch {
            deviceRepository.cancelConnection()
        }
    }

    fun retryConnection() {
        val targetId = uiState.value.linkState.selectedDevice?.id ?: return
        fakeDeviceDataSource?.setScenario(DeviceSimulationScenario.DEFAULT_AVAILABLE)
        viewModelScope.launch {
            deviceRepository.connect(targetId)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            deviceRepository.disconnect()
            deviceRepository.setDemoSessionActive(false)
        }
    }

    fun toggleTroubleshooting() {
        showTroubleshootingFlow.value = !showTroubleshootingFlow.value
    }

    fun enterDemoMode() {
        deviceRepository.selectProvider(DeviceProviderType.SIMULATED)
        deviceRepository.setDemoSessionActive(true)
    }

    fun exitDemoMode() {
        deviceRepository.setDemoSessionActive(false)
        viewModelScope.launch {
            deviceRepository.disconnect()
        }
    }
}
