package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.device.DefaultDeviceRepository
import com.ayasheta.pulsesmartgym.core.data.device.FakeDeviceDataSource
import com.ayasheta.pulsesmartgym.core.data.device.RealBleDeviceDataSource
import com.ayasheta.pulsesmartgym.core.data.device.ble.BlePacketDecoder
import com.ayasheta.pulsesmartgym.core.data.device.ble.BleProtocolConfig
import com.ayasheta.pulsesmartgym.core.data.device.ble.BleTransport
import com.ayasheta.pulsesmartgym.core.data.device.ble.BleTransportEvent
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.feature.device.diagnostics.HardwareDiagnosticsScreen
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftStore
import com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionScreen
import com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P11RawBlePipelineUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.mkdirs()
        context.cacheDir.mkdirs()
        context.deleteDatabase("pulse_smart_gym.db")
        File(context.filesDir, "datastore").deleteRecursively()
    }

    @After
    fun tearDown() {
        context.deleteDatabase("pulse_smart_gym.db")
        File(context.filesDir, "datastore").deleteRecursively()
    }

    @Test
    fun rawBleTransportPayloads_updateSetExecutionWaveform_andPersistToRoom() {
        val rawTransport = TestRawBleTransport()
        val protocolConfig = BleProtocolConfig.proposedTestConfig(isSensorCalibrated = true)
        val container = object : PulseDataContainer(context, seedDefaultPlansOnFirstUse = true) {
            override val realBleDeviceDataSource: RealBleDeviceDataSource by lazy {
                RealBleDeviceDataSource(
                    transport = rawTransport,
                    initialConfig = protocolConfig,
                )
            }
            override val deviceRepository by lazy {
                DefaultDeviceRepository(
                    fakeDataSource = fakeDeviceDataSource,
                    realBleDataSource = realBleDeviceDataSource,
                )
            }
        }
        container.deviceRepository.selectProvider(DeviceProviderType.REAL_BLE)

        lateinit var ownerId: String
        lateinit var sessionId: String
        lateinit var exerciseId: String
        runBlocking {
            val authResult = container.authRepository.registerLocalAccount(
                fullName = "Raw BLE User",
                email = "rawble_${System.nanoTime()}@pulse.test",
                password = "Password123!",
            )
            ownerId = (authResult as DomainResult.Success<com.ayasheta.pulsesmartgym.core.model.profile.AuthUser>).data.id
            val plans = container.workoutPlanRepository.observePlans(ownerId).first { it.isNotEmpty() }
            val beginnerPlan = plans.first { it.name == "Beginner Full Body" }
            val sessionResult = container.workoutSessionRepository.startSessionFromPlan(ownerId, beginnerPlan.id)
            val session = (sessionResult as DomainResult.Success).data
            sessionId = session.id
            exerciseId = session.exercises.first().id

            // Connect through real BLE transport handshake
            container.deviceRepository.startScan()
            val pod = DeviceDescriptor(
                id = "AA:BB:CC:DD:EE:99",
                name = "PULSE-ESP32",
                profileLabel = "BLE GATT",
                signalLabel = "-55 dBm",
                providerType = DeviceProviderType.REAL_BLE,
                isSimulated = false,
            )
            rawTransport.emit(BleTransportEvent.DeviceDiscovered(pod))
            container.deviceRepository.connect(pod.id)
            rawTransport.emit(BleTransportEvent.GattConnected(pod.id, pod.name))
            rawTransport.emit(BleTransportEvent.ServicesDiscovered(listOf(protocolConfig.serviceUuid!!)))
            rawTransport.emit(BleTransportEvent.NotificationsSubscribed(protocolConfig.telemetryCharacteristicUuid!!))
            val connectedLink = kotlinx.coroutines.withTimeout(2_000) {
                container.deviceRepository.linkState.first { it.phase == DeviceConnectionState.CONNECTED }
            }
            assertEquals(DeviceConnectionState.CONNECTED, connectedLink.phase)
        }

        val viewModel = SetExecutionViewModel(
            authRepository = container.authRepository,
            workoutSessionRepository = container.workoutSessionRepository,
            userPreferencesRepository = container.userPreferencesRepository,
            deviceRepository = container.deviceRepository,
            savedStateHandle = SavedStateHandle(),
            sessionId = sessionId,
            exerciseId = exerciseId,
            setIndex = 1,
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
            setDraftStore = SetDraftStore(),
        )

        composeRule.setContent {
            PulseTheme {
                SetExecutionScreen(
                    viewModel = viewModel,
                    onBackClick = {},
                    onSetFinished = { _, _, _, _ -> },
                    onSessionCompleted = {},
                    onSessionEndedEarly = {},
                )
            }
        }

        composeRule.waitForIdle()
        // Connected real BLE shows "Connected" (not "Connected (Sim)")
        composeRule.onNodeWithText("Connected").assertIsDisplayed()
        composeRule.onNodeWithText("No measured reps").assertIsDisplayed()

        // Start set
        composeRule.onNodeWithTag("button_set_execution_primary").performClick()
        composeRule.waitForIdle()

        // Inject raw binary BLE notification payloads at the transport boundary
        runBlocking {
            val charUuid = protocolConfig.telemetryCharacteristicUuid!!
            val p1 = BlePacketDecoder.encodeRepTelemetryPacket(
                sequenceNumber = 1L,
                classification = RepClassification.FULL,
                totalDurationMs = 2100L,
                concentricDurationMs = 900L,
                eccentricDurationMs = 1200L,
                linearRomCm = 41.5,
                peakAccelerationMetersPerSecondSquared = 3.10,
            )
            val p2 = BlePacketDecoder.encodeRepTelemetryPacket(
                sequenceNumber = 2L,
                classification = RepClassification.PARTIAL,
                totalDurationMs = 1450L,
                concentricDurationMs = 650L,
                eccentricDurationMs = 800L,
                linearRomCm = 26.8,
                peakAccelerationMetersPerSecondSquared = 2.40,
            )
            val p3 = BlePacketDecoder.encodeRepTelemetryPacket(
                sequenceNumber = 3L,
                classification = RepClassification.FULL,
                totalDurationMs = 2200L,
                concentricDurationMs = 950L,
                eccentricDurationMs = 1250L,
                linearRomCm = 44.0,
                peakAccelerationMetersPerSecondSquared = 3.55,
            )
            // Also inject malformed packet to verify rejection without crashing or faking reps
            rawTransport.emit(BleTransportEvent.NotificationReceived(charUuid, p1))
            rawTransport.emit(BleTransportEvent.NotificationReceived(charUuid, byteArrayOf(0x01, 0x01, 0x09)))
            rawTransport.emit(BleTransportEvent.NotificationReceived(charUuid, p2))
            rawTransport.emit(BleTransportEvent.NotificationReceived(charUuid, p3))
        }

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Rep #3").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Motion Trend (Linear ROM (cm))").assertIsDisplayed()

        // Finish set & verify Room persistence preserves SENSOR_TELEMETRY measurements
        composeRule.onNodeWithTag("button_set_execution_primary").performClick()
        composeRule.waitForIdle()

        runBlocking {
            val savedSession = container.workoutSessionRepository.observeSession(ownerId, sessionId).first { s ->
                s?.exercises?.firstOrNull()?.performedSets?.isNotEmpty() == true
            }!!
            val performed = savedSession.exercises.first().performedSets.first()
            assertEquals(2, performed.fullReps)
            assertEquals(1, performed.partialReps)
            assertEquals(3, performed.repRecords.size)
            assertTrue(performed.repRecords.all { it.origin == MeasurementOrigin.SENSOR_TELEMETRY })
            assertEquals(41.5, performed.repRecords[0].linearRomCm!!, 0.01)
            assertEquals(44.0, performed.repRecords[2].linearRomCm!!, 0.01)
        }
    }

    @Test
    fun hardwareDiagnosticsScreen_replaysRawPacketAndUpdatesCountersAndScreenshot() {
        val realBleSource = RealBleDeviceDataSource()
        val deviceRepo = DefaultDeviceRepository(
            fakeDataSource = FakeDeviceDataSource(),
            realBleDataSource = realBleSource,
        )
        deviceRepo.selectProvider(DeviceProviderType.REAL_BLE)

        composeRule.setContent {
            PulseTheme {
                HardwareDiagnosticsScreen(
                    deviceRepository = deviceRepo,
                    realBleDeviceDataSource = realBleSource,
                    onBackClick = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("button_diag_replay_valid").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("RAW REPLAY (DEV)").assertIsDisplayed()
        assertEquals(DeviceConnectionState.DISCONNECTED, deviceRepo.linkState.value.phase)
        composeRule.onNodeWithTag("button_diag_replay_malformed").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diag_incoming_packets").assertIsDisplayed()
        composeRule.onNodeWithTag("diag_invalid_packets").assertIsDisplayed()
        composeRule.onNodeWithTag("diag_last_error").assertIsDisplayed()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(context.filesDir, "visual_qa").apply { mkdirs() }
        FileOutputStream(File(dir, "p11_hardware_diagnostics.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

private class TestRawBleTransport : BleTransport {
    private val _events = MutableSharedFlow<BleTransportEvent>(extraBufferCapacity = 64)
    override val events: Flow<BleTransportEvent> = _events.asSharedFlow()

    suspend fun emit(event: BleTransportEvent) {
        _events.emit(event)
    }

    override fun checkAvailabilityAndPermissions(): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun startScan(config: BleProtocolConfig): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun stopScan(): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun connect(deviceId: String, config: BleProtocolConfig): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun disconnect(): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun writeCommand(characteristicUuid: String, payload: ByteArray): Boolean = true
    override fun close() = Unit
}
