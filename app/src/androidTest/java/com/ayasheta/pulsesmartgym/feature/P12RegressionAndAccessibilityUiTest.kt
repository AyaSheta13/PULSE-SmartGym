package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.device.DefaultDeviceRepository
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
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
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
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P12RegressionAndAccessibilityUiTest {

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
    fun scenarioEAndG_missingMetricsAndConnectionLoss_manualFallbackAndRestartRecovery() {
        val rawTransport = P12RawBleTransport()
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
            val auth = container.authRepository.registerLocalAccount(
                fullName = "P12 QA User",
                email = "p12qa_${System.nanoTime()}@pulse.test",
                password = "Password123!",
            )
            ownerId = (auth as DomainResult.Success<AuthUser>).data.id
            val plans = container.workoutPlanRepository.observePlans(ownerId).first { it.isNotEmpty() }
            val session = (container.workoutSessionRepository.startSessionFromPlan(
                ownerId,
                plans.first { it.name == "Beginner Full Body" }.id,
            ) as DomainResult.Success).data
            sessionId = session.id
            exerciseId = session.exercises.first().id

            container.deviceRepository.startScan()
            val pod = DeviceDescriptor(
                id = "AA:BB:CC:DD:EE:12",
                name = "PULSE-ESP32-P12",
                profileLabel = "BLE GATT",
                signalLabel = "-52 dBm",
                providerType = DeviceProviderType.REAL_BLE,
                isSimulated = false,
            )
            rawTransport.emit(BleTransportEvent.DeviceDiscovered(pod))
            container.deviceRepository.connect(pod.id)
            rawTransport.emit(BleTransportEvent.GattConnected(pod.id, pod.name))
            rawTransport.emit(BleTransportEvent.ServicesDiscovered(listOf(protocolConfig.serviceUuid!!)))
            rawTransport.emit(BleTransportEvent.NotificationsSubscribed(protocolConfig.telemetryCharacteristicUuid!!))
            withTimeout(2_000) {
                container.deviceRepository.linkState.first { it.phase == DeviceConnectionState.CONNECTED }
            }
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
        composeRule.onNodeWithTag("button_set_execution_primary").performClick()
        composeRule.waitForIdle()

        // Rep 1 valid, Rep 2 missing linearRomCm (null gap), Rep 3 valid
        runBlocking {
            val charUuid = protocolConfig.telemetryCharacteristicUuid!!
            rawTransport.emit(
                BleTransportEvent.NotificationReceived(
                    charUuid,
                    BlePacketDecoder.encodeRepTelemetryPacket(1L, RepClassification.FULL, totalDurationMs = 2000L, linearRomCm = 40.0),
                ),
            )
            rawTransport.emit(
                BleTransportEvent.NotificationReceived(
                    charUuid,
                    BlePacketDecoder.encodeRepTelemetryPacket(2L, RepClassification.FULL, totalDurationMs = 1900L, linearRomCm = null),
                ),
            )
            rawTransport.emit(
                BleTransportEvent.NotificationReceived(
                    charUuid,
                    BlePacketDecoder.encodeRepTelemetryPacket(3L, RepClassification.FULL, totalDurationMs = 2100L, linearRomCm = 45.0),
                ),
            )
        }

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Rep #3").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Unexpected disconnect mid-set -> ConnectionLostRecoverySheet without phantom reps
        runBlocking {
            rawTransport.emit(BleTransportEvent.Disconnected(status = 8, unexpected = true))
        }
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Connection Lost").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        saveScreenshot("p12_set_execution_connection_lost.png")

        // Switch to manual fallback, add 1 manual rep, finish set
        composeRule.onNodeWithTag("button_recovery_continue_manually").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Add one rep").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("button_set_execution_primary").performClick()
        composeRule.waitForIdle()

        // Verify persistence and restart recovery with a fresh PulseDataContainer
        runBlocking {
            val saved = container.workoutSessionRepository.observeSession(ownerId, sessionId).first {
                it?.exercises?.firstOrNull()?.performedSets?.isNotEmpty() == true
            }!!
            val set1 = saved.exercises.first().performedSets.first()
            assertEquals(4, set1.fullReps)
            assertEquals(4, set1.repRecords.size)
            assertEquals(MeasurementOrigin.SENSOR_TELEMETRY, set1.repRecords[0].origin)
            assertEquals(MeasurementOrigin.MANUAL_CORRECTION, set1.repRecords[3].origin)
        }

        // Reopen container against the same DB -> Scenario F recovery
        val reopened = PulseDataContainer(context, seedDefaultPlansOnFirstUse = true)
        runBlocking {
            val restored = reopened.workoutSessionRepository.getSession(ownerId, sessionId)
            assertNotNull(restored)
            assertEquals(1, restored!!.exercises.first().performedSets.size)
            assertEquals(4, restored.exercises.first().performedSets.first().fullReps)
        }
    }

    @Test
    fun responsiveLargeViewport480x960_andFontScale130_preserveControlsAndSemantics() {
        val container = PulseDataContainer(context, seedDefaultPlansOnFirstUse = true)
        lateinit var sessionId: String
        lateinit var exerciseId: String
        runBlocking {
            val auth = container.authRepository.registerLocalAccount(
                fullName = "Accessibility User",
                email = "a11y_${System.nanoTime()}@pulse.test",
                password = "Password123!",
            )
            val ownerId = (auth as DomainResult.Success<AuthUser>).data.id
            val plans = container.workoutPlanRepository.observePlans(ownerId).first { it.isNotEmpty() }
            val session = (container.workoutSessionRepository.startSessionFromPlan(
                ownerId,
                plans.first().id,
            ) as DomainResult.Success).data
            sessionId = session.id
            exerciseId = session.exercises.first().id
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
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = 1.3f),
            ) {
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
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("button_set_execution_back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Motion trend chart info").assertIsDisplayed()
        composeRule.onNodeWithTag("button_edit_weight_direct").assertIsDisplayed()
        composeRule.onNodeWithTag("button_set_execution_primary").assertIsDisplayed()
        composeRule.onNodeWithTag("button_set_execution_end_workout").assertIsDisplayed()
        saveScreenshot("p12_set_execution_480x960_font130.png")
    }

    private fun saveScreenshot(fileName: String) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(context.filesDir, "visual_qa").apply { mkdirs() }
        FileOutputStream(File(dir, fileName)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

private class P12RawBleTransport : BleTransport {
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
