package com.ayasheta.pulsesmartgym.feature

import android.Manifest
import androidx.lifecycle.SavedStateHandle
import com.ayasheta.pulsesmartgym.core.data.device.DefaultDeviceRepository
import com.ayasheta.pulsesmartgym.core.data.device.DeviceEventNormalizer
import com.ayasheta.pulsesmartgym.core.data.device.DeviceSimulationScenario
import com.ayasheta.pulsesmartgym.core.data.device.FakeDeviceDataSource
import com.ayasheta.pulsesmartgym.core.data.device.NormalizedRepResult
import com.ayasheta.pulsesmartgym.core.data.device.RealBleDeviceDataSource
import com.ayasheta.pulsesmartgym.core.data.device.SimulatedDeviceCatalog
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepEvent
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionRestState
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.device.permissions.BluetoothPermissionPolicy
import com.ayasheta.pulsesmartgym.feature.device.permissions.BluetoothReadinessState
import com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterUiState
import com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterViewModel
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftStore
import com.ayasheta.pulsesmartgym.feature.session.devicecounter.DeviceAssistedRepCounterUiState
import com.ayasheta.pulsesmartgym.feature.session.devicecounter.DeviceAssistedRepCounterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class P10DeviceAndCounterUnitTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deviceStateMachine_handlesScanConnectCancelTimeoutReconnectAndRealBleBoundary() = runBlocking {
        val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val fake = FakeDeviceDataSource(scope = ioScope, scanDelayMs = 40L, connectStepDelayMs = 20L)
        val realBle = RealBleDeviceDataSource()
        val repo = DefaultDeviceRepository(fakeDataSource = fake, realBleDataSource = realBle, scope = ioScope)

        // 1. Scan & deduplicate
        assertTrue(repo.startScan() is DeviceOperationResult.Success)
        awaitUntil { repo.discoveredDevices.value.size == 2 }
        assertEquals(2, repo.discoveredDevices.value.size)
        assertTrue(repo.startScan() is DeviceOperationResult.Success)
        awaitUntil { repo.discoveredDevices.value.size == 2 }
        assertEquals(2, repo.discoveredDevices.value.size)

        // 2. No devices scenario
        fake.setScenario(DeviceSimulationScenario.NO_DEVICES)
        assertTrue(repo.startScan() is DeviceOperationResult.Success)
        awaitUntil { repo.discoveredDevices.value.isEmpty() }
        assertTrue(repo.discoveredDevices.value.isEmpty())

        // 3. Scan cancellation
        val scanJob = async { fake.startScan() }
        delay(5L)
        assertTrue(fake.stopScan() is DeviceOperationResult.Success)
        val scanRes = scanJob.await()
        assertTrue(scanRes is DeviceOperationResult.Failure && scanRes.error == DeviceOperationError.CANCELED)

        // 4. Connection timeout & failure
        fake.setScenario(DeviceSimulationScenario.CONNECTION_TIMEOUT)
        val timeoutRes = repo.connect(SimulatedDeviceCatalog.pulse001.id)
        assertTrue(timeoutRes is DeviceOperationResult.Failure && timeoutRes.error == DeviceOperationError.CONNECTION_TIMEOUT)
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.FAILED }
        assertEquals(DeviceConnectionState.FAILED, repo.linkState.value.phase)

        // 5. Connection cancellation ignores late callbacks
        fake.setScenario(DeviceSimulationScenario.DEFAULT_AVAILABLE)
        val connJob = async { repo.connect(SimulatedDeviceCatalog.pulse001.id) }
        delay(10L)
        assertTrue(repo.cancelConnection() is DeviceOperationResult.Success)
        val canceledConn = connJob.await()
        assertTrue(canceledConn is DeviceOperationResult.Failure && canceledConn.error == DeviceOperationError.CANCELED)
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.DISCONNECTED }
        assertEquals(DeviceConnectionState.DISCONNECTED, repo.linkState.value.phase)

        // 6. Successful connect, connection loss, reconnect, idempotent disconnect
        assertTrue(repo.connect(SimulatedDeviceCatalog.pulse001.id) is DeviceOperationResult.Success)
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.CONNECTED }
        assertEquals(DeviceConnectionState.CONNECTED, repo.linkState.value.phase)
        val gen1 = repo.linkState.value.connectionGeneration

        fake.simulateConnectionLoss()
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.CONNECTION_LOST }
        assertEquals(DeviceConnectionState.CONNECTION_LOST, repo.linkState.value.phase)
        assertTrue(repo.reconnect() is DeviceOperationResult.Success)
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.CONNECTED }
        assertEquals(DeviceConnectionState.CONNECTED, repo.linkState.value.phase)
        assertTrue(repo.linkState.value.connectionGeneration > gen1)

        assertTrue(repo.disconnect() is DeviceOperationResult.Success)
        assertTrue(repo.disconnect() is DeviceOperationResult.Success)
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.DISCONNECTED }
        assertEquals(DeviceConnectionState.DISCONNECTED, repo.linkState.value.phase)

        // 7. RealBleDeviceDataSource never fakes connectivity
        repo.selectProvider(DeviceProviderType.REAL_BLE)
        val bleScan = repo.startScan()
        assertTrue(bleScan is DeviceOperationResult.Failure && bleScan.error == DeviceOperationError.PROTOCOL_NOT_CONFIGURED)
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.FAILED }
        assertEquals(DeviceConnectionState.FAILED, repo.linkState.value.phase)
        assertTrue(repo.discoveredDevices.value.isEmpty())

        // 8. Owner change resets provider cleanly
        repo.selectProvider(DeviceProviderType.SIMULATED)
        repo.setDemoSessionActive(true)
        repo.connect(SimulatedDeviceCatalog.pulse001.id)
        repo.onOwnerChanged()
        assertFalse(repo.isDemoSessionActive.value)
        awaitUntil { repo.linkState.value.phase == DeviceConnectionState.DISCONNECTED }
        assertEquals(DeviceConnectionState.DISCONNECTED, repo.linkState.value.phase)
    }

    @Test
    fun eventNormalizer_validatesOriginDuplicatesGenerationWindowAndCapabilities() {
        val caps = DeviceCapabilities(
            automaticRepEvents = true,
            fullPartialClassification = true,
            angularRom = false,
            linearRom = false,
            repDuration = true,
        )
        val base = DeviceRepEvent(
            eventId = "e1",
            providerType = DeviceProviderType.SIMULATED,
            connectionGeneration = 5L,
            trackingWindowId = "s1_ex1_1",
            classification = RepClassification.FULL,
            totalDurationMs = 1500L,
            angularRomDegrees = 90.0,
            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
        )

        // Valid full rep strips unsupported angularRomDegrees
        val ok = DeviceEventNormalizer.normalizeAndValidate(
            event = base,
            expectedProviderType = DeviceProviderType.SIMULATED,
            expectedConnectionGeneration = 5L,
            expectedTrackingWindowId = "s1_ex1_1",
            isConnected = true,
            isSubmitting = false,
            capabilities = caps,
            seenEventIds = emptySet(),
            nextRepIndex = 1,
        )
        assertTrue(ok is NormalizedRepResult.Accepted)
        val meas = (ok as NormalizedRepResult.Accepted).measurement
        assertEquals(1500L, meas.totalDurationMs)
        assertNull(meas.angularRomDegrees)
        assertEquals(MeasurementOrigin.SIMULATED_TELEMETRY, meas.origin)

        // Duplicate, stale generation, wrong window, NaN, negative duration, SENSOR_TELEMETRY for simulator, submitting
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(base, DeviceProviderType.SIMULATED, 5L, "s1_ex1_1", true, false, caps, setOf("e1"), 2) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(base.copy(connectionGeneration = 4L), DeviceProviderType.SIMULATED, 5L, "s1_ex1_1", true, false, caps, emptySet(), 2) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(base.copy(trackingWindowId = "other"), DeviceProviderType.SIMULATED, 5L, "s1_ex1_1", true, false, caps, emptySet(), 2) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(base.copy(totalDurationMs = -10L), DeviceProviderType.SIMULATED, 5L, "s1_ex1_1", true, false, caps, emptySet(), 2) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(base.copy(angularRomDegrees = Double.NaN), DeviceProviderType.SIMULATED, 5L, "s1_ex1_1", true, false, caps, emptySet(), 2) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(base.copy(origin = MeasurementOrigin.SENSOR_TELEMETRY), DeviceProviderType.SIMULATED, 5L, "s1_ex1_1", true, false, caps, emptySet(), 2) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(base, DeviceProviderType.SIMULATED, 5L, "s1_ex1_1", true, true, caps, emptySet(), 2) is NormalizedRepResult.Rejected)
    }

    @Test
    fun deviceAssistedCounter_preserves8RepsAcrossDisconnect_andManualFallbackPersists10Reps() = runBlocking {
        val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val fake = FakeDeviceDataSource(scope = ioScope, scanDelayMs = 0L, connectStepDelayMs = 0L)
        val deviceRepo = DefaultDeviceRepository(fakeDataSource = fake, scope = ioScope)
        deviceRepo.connect(SimulatedDeviceCatalog.pulse001.id)

        val auth = FakeP10AuthRepo("owner_a")
        val sessionRepo = FakeP10SessionRepo(sampleActiveSession("s1", "owner_a"))
        val prefs = FakeP10PrefsRepo()
        val draftStore = SetDraftStore()
        var nowElapsed = 10_000L

        val deviceVm = DeviceAssistedRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            deviceRepository = deviceRepo,
            fakeDeviceDataSource = fake,
            setDraftStore = draftStore,
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { nowElapsed },
        )
        yield()

        // Emit 7 simulated full reps + 1 partial rep
        repeat(7) { idx ->
            fake.emitSimulatedRep("s1_ex1_1", RepClassification.FULL, eventId = "ev_$idx")
        }
        // Duplicate event ignored
        fake.emitSimulatedRep("s1_ex1_1", RepClassification.FULL, eventId = "ev_0")
        fake.emitSimulatedRep("s1_ex1_1", RepClassification.PARTIAL, eventId = "ev_p1")
        // +1 manual full rep -> 8 full, 1 partial
        deviceVm.incrementManualFullRep()
        yield()

        val beforeDrop = deviceVm.uiState.value as DeviceAssistedRepCounterUiState.Active
        assertEquals(8, beforeDrop.fullReps)
        assertEquals(1, beforeDrop.partialReps)

        // Simulate disconnect -> recovery sheet opens, reps preserved!
        fake.simulateConnectionLoss()
        awaitUntil { (deviceVm.uiState.value as? DeviceAssistedRepCounterUiState.Active)?.showRecoverySheet == true }
        val dropped = deviceVm.uiState.value as DeviceAssistedRepCounterUiState.Active
        assertTrue(dropped.showRecoverySheet)
        assertEquals(8, dropped.fullReps)
        assertEquals(1, dropped.partialReps)

        // Continue Manually -> ManualRepCounterViewModel consumes draft starting at 8 full + 1 partial
        deviceVm.continueManually()
        nowElapsed += 5_000L
        val manualVm = ManualRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { nowElapsed },
            setDraftStore = draftStore,
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        yield()
        val manualActive = manualVm.uiState.value as ManualRepCounterUiState.Active
        assertEquals(8, manualActive.currentReps)

        // Add 2 manual reps -> 10 full reps -> Finish Set
        manualVm.incrementRep()
        manualVm.incrementRep()
        yield()
        manualVm.finishSet()
        yield()

        val recorded = sessionRepo.lastRecordedRequest!!
        assertEquals(10, recorded.fullReps)
        assertEquals(1, recorded.partialReps)
        assertEquals(TrackingMode.DEVICE_ASSISTED, recorded.trackingMode)
        assertEquals(MeasurementOrigin.SIMULATED_TELEMETRY, recorded.origin)
        assertEquals(11, recorded.repRecords.size)
        assertEquals(MeasurementOrigin.SIMULATED_TELEMETRY, recorded.repRecords.first().origin)
        assertEquals(MeasurementOrigin.MANUAL_INPUT, recorded.repRecords.last().origin)
    }

    @Test
    fun bluetoothPermissionPolicy_handlesApiLevelsAndReadinessStates() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            BluetoothPermissionPolicy.runtimePermissionsForApi(29),
        )
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
            BluetoothPermissionPolicy.runtimePermissionsForApi(31),
        )
        assertEquals(
            BluetoothReadinessState.HARDWARE_UNAVAILABLE,
            BluetoothPermissionPolicy.resolveReadiness(false, true, true, false, false),
        )
        assertEquals(
            BluetoothReadinessState.PERMISSION_NOT_REQUESTED,
            BluetoothPermissionPolicy.resolveReadiness(true, true, false, false, false),
        )
        assertEquals(
            BluetoothReadinessState.PERMISSION_DENIED,
            BluetoothPermissionPolicy.resolveReadiness(true, true, false, true, true),
        )
        assertEquals(
            BluetoothReadinessState.PERMISSION_PERMANENTLY_DENIED,
            BluetoothPermissionPolicy.resolveReadiness(true, true, false, true, false),
        )
        assertEquals(
            BluetoothReadinessState.BLUETOOTH_DISABLED,
            BluetoothPermissionPolicy.resolveReadiness(true, false, true, true, false),
        )
        assertEquals(
            BluetoothReadinessState.READY,
            BluetoothPermissionPolicy.resolveReadiness(true, true, true, true, false),
        )
    }

    @Test
    fun ledgerRestoration_reconstructsDeviceAndManualFallbackFromSavedState_rejectsReplays_preservesNullLoadAndReboot() = runBlocking {
        val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val fake = FakeDeviceDataSource(scope = ioScope, scanDelayMs = 0L, connectStepDelayMs = 0L)
        val deviceRepo = DefaultDeviceRepository(fakeDataSource = fake, scope = ioScope)
        deviceRepo.connect(SimulatedDeviceCatalog.pulse001.id)

        val auth = FakeP10AuthRepo("owner_a")
        val sessionRepo = FakeP10SessionRepo(sampleActiveSession("s1", "owner_a"))
        val prefs = FakeP10PrefsRepo()
        val originalSaved = SavedStateHandle()
        var nowElapsed = 50_000L

        val vm1 = DeviceAssistedRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            deviceRepository = deviceRepo,
            fakeDeviceDataSource = fake,
            setDraftStore = null,
            savedStateHandle = originalSaved,
            elapsedRealtimeMs = { nowElapsed },
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { vm1.uiState.value is DeviceAssistedRepCounterUiState.Active }
        vm1.setExplicitNullLoadForTesting()
        repeat(6) { idx ->
            fake.emitSimulatedRep("s1_ex1_1", RepClassification.FULL, eventId = "r_$idx")
        }
        fake.emitSimulatedRep("s1_ex1_1", RepClassification.PARTIAL, eventId = "r_p1")
        vm1.incrementManualFullRep()
        awaitUntil { (vm1.uiState.value as? DeviceAssistedRepCounterUiState.Active)?.fullReps == 7 }

        // Genuinely reconstruct SavedStateHandle (simulating process death / VM recreation, after device reboot so elapsedRealtime < saved!)
        val reconstructedMap = originalSaved.keys().associateWith { k -> originalSaved.get<Any?>(k) }
        val restoredSaved = SavedStateHandle(reconstructedMap)
        val rebootedElapsed = 2_000L

        val vm2 = DeviceAssistedRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            deviceRepository = deviceRepo,
            fakeDeviceDataSource = fake,
            setDraftStore = null,
            savedStateHandle = restoredSaved,
            elapsedRealtimeMs = { rebootedElapsed },
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { (vm2.uiState.value as? DeviceAssistedRepCounterUiState.Active)?.fullReps == 7 }
        val restoredActive = vm2.uiState.value as DeviceAssistedRepCounterUiState.Active
        assertEquals(7, restoredActive.fullReps)
        assertEquals(1, restoredActive.partialReps)
        assertNull(restoredActive.actualLoad)
        assertEquals(8, restoredActive.acceptedLedger.size)

        // Replayed eventId MUST be rejected after restoration
        fake.emitSimulatedRep("s1_ex1_1", RepClassification.FULL, eventId = "r_0")
        delay(30L)
        assertEquals(7, (vm2.uiState.value as DeviceAssistedRepCounterUiState.Active).fullReps)

        // Add 2 more reps (1 auto + 1 manual) and continueManually -> recreate ManualRepCounterViewModel from SavedStateHandle
        fake.emitSimulatedRep("s1_ex1_1", RepClassification.FULL, eventId = "r_new")
        awaitUntil { (vm2.uiState.value as? DeviceAssistedRepCounterUiState.Active)?.fullReps == 8 }
        val draftStore = SetDraftStore()
        val vm3 = DeviceAssistedRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            deviceRepository = deviceRepo,
            fakeDeviceDataSource = fake,
            setDraftStore = draftStore,
            savedStateHandle = restoredSaved,
            elapsedRealtimeMs = { rebootedElapsed + 1_000L },
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { (vm3.uiState.value as? DeviceAssistedRepCounterUiState.Active)?.fullReps == 8 }
        vm3.continueManually()

        val manualSaved = SavedStateHandle()
        val manualVm1 = ManualRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            savedStateHandle = manualSaved,
            elapsedRealtimeMs = { rebootedElapsed + 2_000L },
            setDraftStore = draftStore,
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { (manualVm1.uiState.value as? ManualRepCounterUiState.Active)?.currentReps == 8 }

        // Recreate ManualRepCounterViewModel from its SavedStateHandle (SetDraftStore is now empty!)
        val manualRestoredSaved = SavedStateHandle(manualSaved.keys().associateWith { k -> manualSaved.get<Any?>(k) })
        val manualVm2 = ManualRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            savedStateHandle = manualRestoredSaved,
            elapsedRealtimeMs = { rebootedElapsed + 5_000L },
            setDraftStore = draftStore,
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { (manualVm2.uiState.value as? ManualRepCounterUiState.Active)?.currentReps == 8 }
        manualVm2.incrementRep()
        manualVm2.incrementRep()
        manualVm2.finishSet()
        awaitUntil { sessionRepo.lastRecordedRequest != null }

        val recorded = sessionRepo.lastRecordedRequest!!
        assertEquals(10, recorded.fullReps)
        assertEquals(1, recorded.partialReps)
        assertNull(recorded.actualLoad)
        assertTrue(recorded.durationMs!! >= 0L)
        assertEquals(11, recorded.repRecords.size)
        assertEquals(TrackingMode.DEVICE_ASSISTED, recorded.trackingMode)
        assertEquals(MeasurementOrigin.SIMULATED_TELEMETRY, recorded.origin)
    }

    @Test
    fun eventBoundaries_setFinishHelper_andGenuineReadyStartSet_enforcedStrictly() = runBlocking {
        val caps = DeviceCapabilities(automaticRepEvents = true, fullPartialClassification = true, repDuration = true)
        val simRep = DeviceRepEvent(
            eventId = "e1",
            providerType = DeviceProviderType.SIMULATED,
            connectionGeneration = 1L,
            trackingWindowId = "w1",
            classification = RepClassification.FULL,
            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
        )
        // Simulated event rejected in GENUINE environment
        assertTrue(
            DeviceEventNormalizer.normalizeAndValidate(
                simRep, DeviceProviderType.SIMULATED, 1L, "w1", true, false, caps, emptySet(), 1,
                expectedEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.GENUINE,
            ) is NormalizedRepResult.Rejected,
        )
        // Unauthenticated / inactive session / not accepting / >100 events rejected
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(simRep, DeviceProviderType.SIMULATED, 1L, "w1", true, false, caps, emptySet(), 1, isOwnerAuthenticated = false) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(simRep, DeviceProviderType.SIMULATED, 1L, "w1", true, false, caps, emptySet(), 1, isSessionActive = false) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(simRep, DeviceProviderType.SIMULATED, 1L, "w1", true, false, caps, emptySet(), 1, isAcceptingEvents = false) is NormalizedRepResult.Rejected)
        assertTrue(DeviceEventNormalizer.normalizeAndValidate(simRep, DeviceProviderType.SIMULATED, 1L, "w1", true, false, caps, emptySet(), 101) is NormalizedRepResult.Rejected)

        // SetFinishHelper handles storage exceptions, AlreadyRecorded, rest failure, and owner change
        val auth = FakeP10AuthRepo("owner_a")
        val sessionRepo = FakeP10SessionRepo(sampleActiveSession("s1", "owner_a"))
        val req = RecordSetRequest("s1", "ex1", 1, 10, 0, WeightLoad.fromKilograms(80.0))

        sessionRepo.throwOnRecord = IllegalStateException("Disk full")
        val errOutcome = com.ayasheta.pulsesmartgym.feature.session.counter.SetFinishHelper.submitSetAndStartRest(
            "owner_a", req, "Bench", 60, null, sessionRepo, { "owner_a" },
        )
        assertTrue(errOutcome is com.ayasheta.pulsesmartgym.feature.session.counter.SetSubmissionOutcome.Failure)

        sessionRepo.throwOnRecord = null
        sessionRepo.alreadyRecorded = true
        val dupOutcome = com.ayasheta.pulsesmartgym.feature.session.counter.SetFinishHelper.submitSetAndStartRest(
            "owner_a", req, "Bench", 60, null, sessionRepo, { "owner_a" },
        )
        assertTrue(dupOutcome is com.ayasheta.pulsesmartgym.feature.session.counter.SetSubmissionOutcome.AlreadyCompleted)

        // Simulated device connected NEVER authorizes device-assisted mode on GENUINE ReadyStartSet
        val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val fake = FakeDeviceDataSource(scope = ioScope, scanDelayMs = 0L, connectStepDelayMs = 0L)
        val deviceRepo = DefaultDeviceRepository(fakeDataSource = fake, scope = ioScope)
        deviceRepo.connect(SimulatedDeviceCatalog.pulse001.id)
        val readyVm = com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = FakeP10PrefsRepo(),
            deviceRepository = deviceRepo,
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.GENUINE,
        )
        awaitUntil { readyVm.uiState.value is com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetUiState.Ready }
        val readyState = readyVm.uiState.value as com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetUiState.Ready
        assertFalse(readyState.canStartDeviceAssisted)
    }

    @Test
    fun ledgerBoundary_enforces100AcrossDeviceManualFallbackAndCodec_andPreservesOrdinaryManual() = runBlocking {
        val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val fake = FakeDeviceDataSource(scope = ioScope, scanDelayMs = 0L, connectStepDelayMs = 0L)
        val deviceRepo = DefaultDeviceRepository(fakeDataSource = fake, scope = ioScope)
        deviceRepo.connect(SimulatedDeviceCatalog.pulse001.id)

        val auth = FakeP10AuthRepo("owner_a")
        val sessionRepo = FakeP10SessionRepo(sampleActiveSession("s1", "owner_a"))
        val prefs = FakeP10PrefsRepo()
        val draftStore = SetDraftStore()
        val deviceSaved = SavedStateHandle()

        val deviceVm = DeviceAssistedRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            deviceRepository = deviceRepo,
            fakeDeviceDataSource = fake,
            setDraftStore = draftStore,
            savedStateHandle = deviceSaved,
            elapsedRealtimeMs = { 10_000L },
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { deviceVm.uiState.value is DeviceAssistedRepCounterUiState.Active }

        repeat(100) { idx ->
            fake.emitSimulatedRep("s1_ex1_1", RepClassification.FULL, eventId = "cap_$idx")
        }
        awaitUntil { (deviceVm.uiState.value as? DeviceAssistedRepCounterUiState.Active)?.fullReps == 100 }

        // 101st auto rep is rejected with clear message
        fake.emitSimulatedRep("s1_ex1_1", RepClassification.FULL, eventId = "cap_101")
        awaitUntil { (deviceVm.uiState.value as? DeviceAssistedRepCounterUiState.Active)?.errorMessage != null }
        val afterAutoCap = deviceVm.uiState.value as DeviceAssistedRepCounterUiState.Active
        assertEquals(100, afterAutoCap.fullReps)
        assertTrue(afterAutoCap.errorMessage!!.contains("100"))

        // Manual +1 on device counter also respects 100 limit
        deviceVm.incrementManualFullRep()
        assertEquals(100, (deviceVm.uiState.value as DeviceAssistedRepCounterUiState.Active).fullReps)

        // Codec never silently truncates > 100 reps
        val oversizedDraft = com.ayasheta.pulsesmartgym.feature.session.counter.SetExecutionDraft(
            ownerId = "owner_a",
            environment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            fullReps = 101,
            partialReps = 0,
            repRecords = (1..101).map { com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement(it, RepClassification.FULL) },
            actualLoad = null,
            setStartedAtElapsedMs = 10_000L,
            hadDeviceTracking = true,
        )
        val threwOnOversized = runCatching {
            com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftSavedStateCodec.encode(oversizedDraft)
        }.isFailure
        assertTrue(threwOnOversized)

        // Continue manually -> recreate ManualRepCounterViewModel -> verify 100 limit & finish with 100 records
        deviceVm.continueManually()
        val manualSaved = SavedStateHandle()
        val manualVm1 = ManualRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            savedStateHandle = manualSaved,
            elapsedRealtimeMs = { 12_000L },
            setDraftStore = draftStore,
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { (manualVm1.uiState.value as? ManualRepCounterUiState.Active)?.currentReps == 100 }
        val manualRestored = ManualRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            savedStateHandle = SavedStateHandle(manualSaved.keys().associateWith { k -> manualSaved.get<Any?>(k) }),
            elapsedRealtimeMs = { 15_000L },
            setDraftStore = draftStore,
            executionEnvironment = com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
        )
        awaitUntil { (manualRestored.uiState.value as? ManualRepCounterUiState.Active)?.currentReps == 100 }
        manualRestored.incrementRep()
        awaitUntil { (manualRestored.uiState.value as? ManualRepCounterUiState.Active)?.errorMessage != null }
        assertEquals(100, (manualRestored.uiState.value as ManualRepCounterUiState.Active).currentReps)
        manualRestored.finishSet()
        awaitUntil { sessionRepo.lastRecordedRequest != null }
        assertEquals(100, sessionRepo.lastRecordedRequest!!.repRecords.size)

        // Ordinary manual counter (hadDeviceTracking = false) still allows > 100 reps
        val ordinaryManual = ManualRepCounterViewModel(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            authRepository = auth,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefs,
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { 20_000L },
            setDraftStore = null,
        )
        awaitUntil { ordinaryManual.uiState.value is ManualRepCounterUiState.Active }
        repeat(101) { ordinaryManual.incrementRep() }
        assertEquals(101, (ordinaryManual.uiState.value as ManualRepCounterUiState.Active).currentReps)
    }

    @Test
    fun dashboardEditSet_selectsExplicitRepCorrectionPolicy() = runBlocking {
        val existingPerformed = PerformedSet(
            id = "ps1",
            sessionId = "s1",
            sessionExerciseId = "ex1",
            setIndex = 1,
            fullReps = 8,
            partialReps = 1,
            actualLoad = WeightLoad.fromKilograms(80.0),
            completedAtEpochMs = 5_000L,
            trackingMode = TrackingMode.DEVICE_ASSISTED,
            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
            durationMs = 15_000L,
            repRecords = listOf(com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement(1, RepClassification.FULL, origin = MeasurementOrigin.SIMULATED_TELEMETRY)),
        )
        val sessionWithPerformed = sampleActiveSession("s1", "owner_a").let { s ->
            s.copy(exercises = listOf(s.exercises[0].copy(performedSets = listOf(existingPerformed))))
        }
        val sessionRepo = FakeP10SessionRepo(sessionWithPerformed)
        val dashVm = com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionViewModel(
            sessionId = "s1",
            authRepository = FakeP10AuthRepo("owner_a"),
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = FakeP10PrefsRepo(),
        )
        awaitUntil { dashVm.uiState.value is com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionUiState.Active }

        // Weight-only edit -> PRESERVE_IF_COUNTS_MATCH & preserves DEVICE_ASSISTED
        dashVm.openSetEntry("ex1", 1)
        val target = (dashVm.uiState.value as com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionUiState.Active).manualEntryTarget!!
        assertTrue(target.hasDetailedMeasurements)
        dashVm.submitManualSet("8", "1", "85")
        awaitUntil { sessionRepo.lastCorrectedRequest != null }
        assertEquals(
            com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy.PRESERVE_IF_COUNTS_MATCH,
            sessionRepo.lastCorrectedRequest!!.repCorrectionPolicy,
        )
        assertEquals(TrackingMode.DEVICE_ASSISTED, sessionRepo.lastCorrectedRequest!!.trackingMode)

        // Count change -> INVALIDATE_LEDGER & MANUAL_CORRECTION
        sessionRepo.lastCorrectedRequest = null
        dashVm.openSetEntry("ex1", 1)
        dashVm.submitManualSet("10", "0", "85")
        awaitUntil { sessionRepo.lastCorrectedRequest != null }
        assertEquals(
            com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy.INVALIDATE_LEDGER,
            sessionRepo.lastCorrectedRequest!!.repCorrectionPolicy,
        )
        assertEquals(MeasurementOrigin.MANUAL_CORRECTION, sessionRepo.lastCorrectedRequest!!.origin)
    }

    private suspend fun awaitUntil(timeoutMs: Long = 500L, predicate: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!predicate() && System.currentTimeMillis() - start < timeoutMs) {
            delay(10L)
        }
    }

    private fun sampleActiveSession(id: String, ownerId: String): WorkoutSession = WorkoutSession(
        id = id,
        ownerProfileId = ownerId,
        sourcePlanId = "p1",
        planNameSnapshot = "Upper Body Power",
        startedAtEpochMs = 1_000L,
        endedAtEpochMs = null,
        status = SessionStatus.ACTIVE,
        exercises = listOf(
            SessionExercise(
                id = "ex1",
                sessionId = id,
                sourceExerciseId = "bench",
                exerciseNameSnapshot = "Barbell Bench Press",
                muscleGroupSnapshot = MuscleGroup.CHEST,
                equipmentSnapshot = EquipmentCategory.BARBELL,
                trackingCategorySnapshot = TrackingCategory.LINEAR,
                orderIndex = 0,
                plannedSets = listOf(SessionPlannedSet("sp1", "ex1", 1, 10, WeightLoad.fromKilograms(80.0), 60)),
                performedSets = emptyList(),
            ),
        ),
    )
}

private class FakeP10AuthRepo(initialId: String) : AuthRepository {
    private val state = MutableStateFlow<AuthState>(
        AuthState.Authenticated(AuthUser(initialId, "User", "$initialId@pulse.fit", false, false)),
    )
    override fun observeAuthState(): Flow<AuthState> = state
    override suspend fun restoreSession(): AuthState = state.value
    override suspend fun continueAsGuest() = throw UnsupportedOperationException()
    override suspend fun registerLocalAccount(fullName: String, email: String, password: String) = throw UnsupportedOperationException()
    override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean) = throw UnsupportedOperationException()
    override suspend fun resetPassword(newPassword: String) = throw UnsupportedOperationException()
    override suspend fun signInDemo(displayName: String, email: String) = throw UnsupportedOperationException()
    override suspend fun signOut() { state.value = AuthState.Unauthenticated }
}

private class FakeP10PrefsRepo : UserPreferencesRepository {
    private val flow = MutableStateFlow(UserPreferences())
    override fun observePreferences(): Flow<UserPreferences> = flow
    override suspend fun updatePreferredWeightUnit(unit: com.ayasheta.pulsesmartgym.core.model.common.WeightUnit) {}
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun updateDefaultRestSeconds(seconds: Int) {}
}

private class FakeP10SessionRepo(initial: WorkoutSession) : WorkoutSessionRepository {
    private val sessionFlow = MutableStateFlow<WorkoutSession?>(initial)
    private val restFlow = MutableStateFlow<SessionRestState?>(null)
    var lastRecordedRequest: RecordSetRequest? = null
    var lastCorrectedRequest: RecordSetRequest? = null
    var throwOnRecord: Throwable? = null
    var alreadyRecorded: Boolean = false

    override fun observeActiveSession(ownerId: String): Flow<WorkoutSession?> = sessionFlow
    override fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?> = sessionFlow
    override suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession? = sessionFlow.value
    override fun observeRestState(ownerId: String, sessionId: String): Flow<SessionRestState?> = restFlow
    override suspend fun startSessionFromPlan(ownerId: String, planId: String) = throw UnsupportedOperationException()
    override suspend fun startSessionFromHistory(ownerId: String, historicalSessionId: String) = throw UnsupportedOperationException()
    override suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest): RecordSetResult {
        throwOnRecord?.let { throw it }
        if (alreadyRecorded) return RecordSetResult.AlreadyRecorded(request.sessionId, request.sessionExerciseId, request.setIndex)
        lastRecordedRequest = request
        return RecordSetResult.Recorded("ps_1")
    }
    override suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest): DomainResult<PerformedSet> {
        lastCorrectedRequest = request
        return DomainResult.Success(
            PerformedSet(
                id = "ps_1",
                sessionId = request.sessionId,
                sessionExerciseId = request.sessionExerciseId,
                setIndex = request.setIndex,
                fullReps = request.fullReps,
                partialReps = request.partialReps,
                actualLoad = request.actualLoad,
                completedAtEpochMs = 5_000L,
                trackingMode = request.trackingMode,
                origin = request.origin,
                durationMs = request.durationMs,
                repRecords = request.repRecords,
            ),
        )
    }
    override suspend fun startRestTimer(ownerId: String, sessionId: String, exerciseId: String, exerciseName: String, completedSetIndex: Int, durationSeconds: Int, nextExerciseId: String?, nextSetIndex: Int?): DomainResult<SessionRestState> =
        DomainResult.Success(SessionRestState(sessionId, exerciseId, exerciseName, completedSetIndex, nextExerciseId, nextSetIndex, durationSeconds, 9999L))
    override suspend fun extendRestTimer(ownerId: String, sessionId: String, additionalSeconds: Int): DomainResult<SessionRestState> =
        throw UnsupportedOperationException()
    override suspend fun clearRestTimer(ownerId: String, sessionId: String): DomainResult<Unit> = DomainResult.Success(Unit)
    override suspend fun completeSession(ownerId: String, sessionId: String): DomainResult<WorkoutSession> =
        throw UnsupportedOperationException()
    override suspend fun endSessionEarly(ownerId: String, sessionId: String): DomainResult<WorkoutSession> =
        throw UnsupportedOperationException()
}
