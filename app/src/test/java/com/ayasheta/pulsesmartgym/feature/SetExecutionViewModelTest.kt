package com.ayasheta.pulsesmartgym.feature.session.execution

import androidx.lifecycle.SavedStateHandle
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
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
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
class SetExecutionViewModelTest {

    private lateinit var mainThreadSurrogate: kotlinx.coroutines.ExecutorCoroutineDispatcher

    @Before
    fun setUp() {
        mainThreadSurrogate = newSingleThreadContext("SetExecutionVmTest")
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun readyState_ignoresRepAdjustmentsAndDeviceEvents_preservesWeightAndStartsTimerOnce() = runBlocking {
        var clockMs = 10_000L
        val authRepo = ExecTestAuthRepo()
        val sessionRepo = ExecTestSessionRepo().apply { putSession(sampleSession()) }
        val prefsRepo = ExecTestPrefsRepo()
        val deviceRepo = ExecTestDeviceRepo(connectedLink())

        val vm = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            deviceRepository = deviceRepo,
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { clockMs },
            executionEnvironment = ExecutionEnvironmentKind.DEMO,
        )

        val ready = vm.uiState.first { it is SetExecutionUiState.Active } as SetExecutionUiState.Active
        assertEquals(SetExecutionPhase.Ready, ready.phase)
        assertEquals(0, ready.fullReps)
        assertNull(ready.startedElapsedRealtimeMs)
        assertEquals(80_000L, ready.actualLoad?.grams)

        // Adjust weight in Ready and attempt rep increment + premature device event
        vm.incrementWeight()
        vm.incrementRep()
        deviceRepo.emitRep("ev_premature", 1, RepClassification.FULL)

        val stillReady = vm.uiState.first { it is SetExecutionUiState.Active && it.actualLoad?.grams == 82_500L } as SetExecutionUiState.Active
        assertEquals(0, stillReady.fullReps)

        // Start Set -> transitions to InProgress, starts timer once, preserves 82.5kg
        vm.startSet()
        clockMs = 15_000L
        vm.startSet() // duplicate tap must be ignored

        val inProgress = vm.uiState.first { it is SetExecutionUiState.Active && it.phase == SetExecutionPhase.InProgress } as SetExecutionUiState.Active
        assertEquals(10_000L, inProgress.startedElapsedRealtimeMs)
        assertEquals(82_500L, inProgress.actualLoad?.grams)

        // Now device event is accepted
        deviceRepo.emitRep("ev_valid_1", 1, RepClassification.FULL)
        val withRep = vm.uiState.first { it is SetExecutionUiState.Active && it.fullReps == 1 } as SetExecutionUiState.Active
        assertEquals(1, withRep.fullReps)
    }

    @Test
    fun connectionLoss_andManualFallback_preservesProgress_andFinishPersistsOnce() = runBlocking {
        var clockMs = 5_000L
        val authRepo = ExecTestAuthRepo()
        val sessionRepo = ExecTestSessionRepo().apply { putSession(sampleSession()) }
        val prefsRepo = ExecTestPrefsRepo()
        val deviceRepo = ExecTestDeviceRepo(connectedLink())
        val draftStore = SetDraftStore()

        val vm = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { clockMs },
            executionEnvironment = ExecutionEnvironmentKind.DEMO,
        )

        vm.uiState.first { it is SetExecutionUiState.Active }
        vm.startSet()
        vm.uiState.first { it is SetExecutionUiState.Active && it.phase == SetExecutionPhase.InProgress }

        deviceRepo.emitRep("ev_1", 1, RepClassification.FULL)
        vm.uiState.first { it is SetExecutionUiState.Active && it.fullReps == 1 }

        // Drop connection -> ConnectionLost
        deviceRepo.setLinkState(disconnectedLink())
        val lost = vm.uiState.first { it is SetExecutionUiState.Active && it.phase == SetExecutionPhase.ConnectionLost } as SetExecutionUiState.Active
        assertEquals(1, lost.fullReps)

        // Continue Manually -> InProgress with preserved rep & timer, allows manual +1
        vm.continueManually()
        vm.uiState.first { it is SetExecutionUiState.Active && it.phase == SetExecutionPhase.InProgress && it.isFallbackManual }
        vm.incrementRep()
        val manualState = vm.uiState.first { it is SetExecutionUiState.Active && it.fullReps == 2 } as SetExecutionUiState.Active
        assertEquals(5_000L, manualState.startedElapsedRealtimeMs)

        // Simulate failure on first Finish Set -> RecoverableError preserves reps, retry succeeds once
        sessionRepo.failNextRecord = true
        clockMs = 20_000L
        vm.finishSet()
        val failed = vm.uiState.first { it is SetExecutionUiState.Active && it.phase == SetExecutionPhase.RecoverableError } as SetExecutionUiState.Active
        assertEquals(2, failed.fullReps)

        sessionRepo.failNextRecord = false
        vm.finishSet()
        val completed = vm.uiState.first { it is SetExecutionUiState.Completed } as SetExecutionUiState.Completed
        assertEquals("sess_1", completed.sessionId)
        assertEquals(1, sessionRepo.recordCalls)
        assertEquals(MeasurementOrigin.MANUAL_CORRECTION, sessionRepo.lastRecordedRequest?.origin)
        assertEquals(2, sessionRepo.lastRecordedRequest?.fullReps)
        assertEquals(15_000L, sessionRepo.lastRecordedRequest?.durationMs)
    }

    @Test
    fun activityRecreation_restoresInProgressPhaseAndTimerWithoutDuplication() = runBlocking {
        var clockMs = 3_000L
        val savedStateHandle = SavedStateHandle()
        val draftStore = SetDraftStore()
        val authRepo = ExecTestAuthRepo()
        val sessionRepo = ExecTestSessionRepo().apply { putSession(sampleSession()) }
        val prefsRepo = ExecTestPrefsRepo()
        val deviceRepo = ExecTestDeviceRepo(disconnectedLink())

        val vm1 = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            savedStateHandle = savedStateHandle,
            elapsedRealtimeMs = { clockMs },
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
        )
        vm1.uiState.first { it is SetExecutionUiState.Active }
        vm1.startSet()
        vm1.uiState.first { it is SetExecutionUiState.Active && it.phase == SetExecutionPhase.InProgress }
        vm1.incrementRep()
        vm1.uiState.first { it is SetExecutionUiState.Active && it.fullReps == 1 }

        // Recreate ViewModel with same SavedStateHandle at later clock
        clockMs = 12_000L
        val vm2 = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            savedStateHandle = savedStateHandle,
            elapsedRealtimeMs = { clockMs },
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
        )
        val restored = vm2.uiState.first { it is SetExecutionUiState.Active } as SetExecutionUiState.Active
        assertEquals(SetExecutionPhase.InProgress, restored.phase)
        assertEquals(1, restored.fullReps)
        assertEquals(3_000L, restored.startedElapsedRealtimeMs)
    }

    private fun connectedLink(): DeviceLinkState = DeviceLinkState(
        providerType = DeviceProviderType.SIMULATED,
        phase = DeviceConnectionState.CONNECTED,
        selectedDevice = DeviceDescriptor(
            id = "dev_1",
            name = "PULSE Pod",
            profileLabel = "Smart Gym",
            rssiDbm = -45,
            signalLabel = "Strong",
            providerType = DeviceProviderType.SIMULATED,
            isSimulated = true,
        ),
        capabilities = DeviceCapabilities(automaticRepEvents = true, fullPartialClassification = true),
        connectionGeneration = 1L,
    )

    private fun disconnectedLink(): DeviceLinkState = DeviceLinkState(
        providerType = DeviceProviderType.SIMULATED,
        phase = DeviceConnectionState.DISCONNECTED,
        connectionGeneration = 1L,
    )

    @Test
    fun ordinaryManualSet_supportsOver100RepsUpTo999_withoutSyntheticLedger() = runBlocking {
        val sessionRepo = ExecTestSessionRepo().apply { putSession(sampleSession()) }
        val deviceRepo = ExecTestDeviceRepo(disconnectedLink())
        val draftStore = SetDraftStore()
        var nowMs = 1_000L
        val vm = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = ExecTestAuthRepo(),
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = ExecTestPrefsRepo(),
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { nowMs },
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
            forceManualMode = true,
        )
        while (vm.uiState.value !is SetExecutionUiState.Active) kotlinx.coroutines.yield()
        vm.startSet()
        while ((vm.uiState.value as SetExecutionUiState.Active).phase != SetExecutionPhase.InProgress) kotlinx.coroutines.yield()

        repeat(125) { vm.incrementRep() }
        while ((vm.uiState.value as SetExecutionUiState.Active).fullReps < 125) kotlinx.coroutines.yield()
        val active = vm.uiState.value as SetExecutionUiState.Active
        assertEquals(125, active.fullReps)
        assertEquals(0, active.repRecords.size)
        assertNull(active.errorMessage)

        // Recreation preserves 125 ordinary manual reps
        val recreated = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = ExecTestAuthRepo(),
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = ExecTestPrefsRepo(),
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { nowMs + 500L },
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
            forceManualMode = true,
        )
        while (recreated.uiState.value !is SetExecutionUiState.Active) kotlinx.coroutines.yield()
        assertEquals(125, (recreated.uiState.value as SetExecutionUiState.Active).fullReps)
    }

    @Test
    fun directWeightInput_supportsDecimalsZeroValidationPlusMinusRestoration_andPersistence() = runBlocking {
        withTimeout(3_000) {
        val sessionRepo = ExecTestSessionRepo().apply { putSession(sampleSession()) }
        val deviceRepo = ExecTestDeviceRepo(disconnectedLink())
        val draftStore = SetDraftStore()
        val savedState = SavedStateHandle()
        val vm = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = ExecTestAuthRepo(),
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = ExecTestPrefsRepo(),
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            savedStateHandle = savedState,
            elapsedRealtimeMs = { 1_000L },
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
            forceManualMode = true,
        )
        while (vm.uiState.value !is SetExecutionUiState.Active) kotlinx.coroutines.yield()

        // Invalid inputs rejected without mutating existing load
        assertEquals("Enter a weight value.", vm.setDirectWeight("   "))
        assertEquals("Enter a valid numeric weight.", vm.setDirectWeight("abc"))
        assertEquals("Weight cannot be negative.", vm.setDirectWeight("-2.5"))
        assertEquals("Enter a weight up to 500 kg.", vm.setDirectWeight("501"))
        assertEquals(80_000L, (vm.uiState.value as SetExecutionUiState.Active).actualLoad?.grams)

        // Zero weight allowed
        assertNull(vm.setDirectWeight("0"))
        vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == 0L }

        // Supported decimals (1.25, 7.5, 12.5, 42.75 kg)
        assertNull(vm.setDirectWeight("1.25"))
        vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == 1_250L }
        assertNull(vm.setDirectWeight("7.5"))
        vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == 7_500L }
        assertNull(vm.setDirectWeight("12.5"))
        vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == 12_500L }
        assertNull(vm.setDirectWeight("42.75"))
        vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == 42_750L }

        // +/- continues from newly entered decimal value
        vm.incrementWeight()
        vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == 45_250L }
        vm.decrementWeight()
        vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == 42_750L }

        // Recreated VM restores 42.75 kg
        val recreated = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = ExecTestAuthRepo(),
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = ExecTestPrefsRepo(),
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            savedStateHandle = savedState,
            elapsedRealtimeMs = { 1_000L },
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
            forceManualMode = true,
        )
        val restoredActive = recreated.uiState.first { it is SetExecutionUiState.Active } as SetExecutionUiState.Active
        assertEquals(42_750L, restoredActive.actualLoad?.grams)

        // Finish set persists exact 42.75 kg load
        recreated.startSet()
        recreated.uiState.first { (it as? SetExecutionUiState.Active)?.phase == SetExecutionPhase.InProgress }
        repeat(4) { recreated.incrementRep() }
        recreated.uiState.first { (it as? SetExecutionUiState.Active)?.fullReps == 4 }
        recreated.finishSet()
        while (sessionRepo.recordCalls == 0) kotlinx.coroutines.delay(5)
        assertEquals(42_750L, sessionRepo.lastRecordedRequest?.actualLoad?.grams)
        }
    }

    @Test
    fun directWeightInput_respectsLbsPreferenceConversion() = runBlocking {
        withTimeout(3_000) {
        val sessionRepo = ExecTestSessionRepo().apply { putSession(sampleSession()) }
        val deviceRepo = ExecTestDeviceRepo(disconnectedLink())
        val prefsRepo = object : UserPreferencesRepository by ExecTestPrefsRepo() {
            override fun observePreferences(): Flow<UserPreferences> =
                MutableStateFlow(UserPreferences(preferredWeightUnit = WeightUnit.LBS))
        }
        val vm = SetExecutionViewModel(
            sessionId = "sess_1",
            exerciseId = "se_1",
            setIndex = 1,
            authRepository = ExecTestAuthRepo(),
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            deviceRepository = deviceRepo,
            setDraftStore = SetDraftStore(),
            savedStateHandle = SavedStateHandle(),
            elapsedRealtimeMs = { 1_000L },
            executionEnvironment = ExecutionEnvironmentKind.GENUINE,
            forceManualMode = true,
        )
        vm.uiState.first { it is SetExecutionUiState.Active }
        assertNull(vm.setDirectWeight("45.5"))
        val expected = WeightLoad.fromPounds(45.5).grams
        val updated = vm.uiState.first { (it as? SetExecutionUiState.Active)?.actualLoad?.grams == expected } as SetExecutionUiState.Active
        assertEquals(expected, updated.actualLoad?.grams)
        }
    }

    private fun sampleSession(): WorkoutSession = WorkoutSession(
        id = "sess_1",
        ownerProfileId = "owner_1",
        sourcePlanId = "plan_1",
        planNameSnapshot = "Upper Body Power",
        status = SessionStatus.ACTIVE,
        startedAtEpochMs = 1_000L,
        exercises = listOf(
            SessionExercise(
                id = "se_1",
                sessionId = "sess_1",
                sourceExerciseId = "ex_bench_press",
                exerciseNameSnapshot = "Barbell Bench Press",
                muscleGroupSnapshot = MuscleGroup.CHEST,
                equipmentSnapshot = EquipmentCategory.BARBELL,
                trackingCategorySnapshot = TrackingCategory.LINEAR,
                orderIndex = 0,
                plannedSets = listOf(
                    SessionPlannedSet("ps_1", "se_1", 1, 10, WeightLoad(80_000L), 90),
                    SessionPlannedSet("ps_2", "se_1", 2, 10, WeightLoad(80_000L), 90),
                ),
                performedSets = emptyList(),
            ),
        ),
    )
}

private class ExecTestAuthRepo : AuthRepository {
    private val state = MutableStateFlow<AuthState>(
        AuthState.Authenticated(AuthUser("owner_1", "Aya", "aya@pulse.test", false)),
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

private class ExecTestPrefsRepo : UserPreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences(preferredWeightUnit = WeightUnit.KG))
    override fun observePreferences(): Flow<UserPreferences> = prefs
    override suspend fun updatePreferredWeightUnit(unit: WeightUnit) {}
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun updateDefaultRestSeconds(seconds: Int) {}
}

private class ExecTestDeviceRepo(initial: DeviceLinkState) : DeviceRepository {
    override val activeProviderType = MutableStateFlow(DeviceProviderType.SIMULATED)
    override val linkState: StateFlow<DeviceLinkState> = MutableStateFlow(initial)
    override val discoveredDevices = MutableStateFlow<List<DeviceDescriptor>>(emptyList())
    private val _events = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 16)
    override val events: Flow<DeviceEvent> = _events
    override val isDemoSessionActive = MutableStateFlow(true)
    override val diagnostics = MutableStateFlow(
        com.ayasheta.pulsesmartgym.core.model.device.HardwareDiagnosticsSnapshot(),
    )

    override fun setActiveTrackingWindow(trackingWindowId: String) = Unit

    fun setLinkState(state: DeviceLinkState) { (linkState as MutableStateFlow).value = state }
    suspend fun emitRep(eventId: String, repNumber: Int, classification: RepClassification) {
        _events.emit(
            DeviceEvent.RepRecorded(
                DeviceRepEvent(
                    eventId = eventId,
                    providerType = DeviceProviderType.SIMULATED,
                    connectionGeneration = 1L,
                    trackingWindowId = "sess_1_se_1_1",
                    sequenceNumber = repNumber.toLong(),
                    classification = classification,
                    origin = MeasurementOrigin.SIMULATED_TELEMETRY,
                ),
            ),
        )
    }

    override fun selectProvider(providerType: DeviceProviderType) {}
    override suspend fun startScan(): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun stopScan(): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun connect(deviceId: String): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun cancelConnection(): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun disconnect(): DeviceOperationResult = DeviceOperationResult.Success
    override suspend fun reconnect(): DeviceOperationResult = DeviceOperationResult.Success
    override fun setDemoSessionActive(active: Boolean) {}
    override fun onOwnerChanged() {}
}

private class ExecTestSessionRepo : WorkoutSessionRepository {
    private val sessions = MutableStateFlow<Map<String, WorkoutSession>>(emptyMap())
    var failNextRecord = false
    var recordCalls = 0
    var lastRecordedRequest: RecordSetRequest? = null

    fun putSession(session: WorkoutSession) { sessions.value = sessions.value + (session.id to session) }
    override fun observeActiveSession(ownerId: String): Flow<WorkoutSession?> =
        sessions.map { m -> m.values.firstOrNull { it.ownerProfileId == ownerId && it.status == SessionStatus.ACTIVE } }
    override fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        sessions.map { m -> m[sessionId]?.takeIf { it.ownerProfileId == ownerId } }
    override suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession? =
        sessions.value[sessionId]?.takeIf { it.ownerProfileId == ownerId }
    override suspend fun startSessionFromPlan(ownerId: String, planId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override suspend fun startSessionFromHistory(ownerId: String, historicalSessionId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest): RecordSetResult {
        if (failNextRecord) return RecordSetResult.InvalidState("Simulated write error")
        recordCalls++
        lastRecordedRequest = request
        val current = getSession(ownerId, request.sessionId) ?: return RecordSetResult.NotFound("Missing")
        val updated = current.copy(
            exercises = current.exercises.map { ex ->
                if (ex.id == request.sessionExerciseId) {
                    ex.copy(
                        performedSets = ex.performedSets + PerformedSet(
                            "ps_rec_${request.setIndex}", request.sessionId, request.sessionExerciseId,
                            request.setIndex, request.fullReps, request.partialReps, request.actualLoad,
                            100L, request.trackingMode, request.origin, request.durationMs, request.repRecords,
                        ),
                    )
                } else {
                    ex
                }
            },
        )
        putSession(updated)
        return RecordSetResult.Recorded("ps_rec_${request.setIndex}")
    }
    override suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest): DomainResult<PerformedSet> = throw UnsupportedOperationException()
    override suspend fun completeSession(ownerId: String, sessionId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override suspend fun endSessionEarly(ownerId: String, sessionId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override fun observeRestState(ownerId: String, sessionId: String): Flow<SessionRestState?> = MutableStateFlow(null)
    override suspend fun startRestTimer(
        ownerId: String, sessionId: String, exerciseId: String, exerciseName: String,
        completedSetIndex: Int, durationSeconds: Int, nextExerciseId: String?, nextSetIndex: Int?,
    ): DomainResult<SessionRestState> = DomainResult.Success(
        SessionRestState(
            sessionId = sessionId,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            completedSetIndex = completedSetIndex,
            nextExerciseId = nextExerciseId,
            nextSetIndex = nextSetIndex,
            durationSeconds = durationSeconds,
            deadlineEpochMs = 100L + durationSeconds * 1000L,
        ),
    )
    override suspend fun extendRestTimer(ownerId: String, sessionId: String, additionalSeconds: Int): DomainResult<SessionRestState> =
        DomainResult.Failure(DataError.NotFound("None"))
    override suspend fun clearRestTimer(ownerId: String, sessionId: String): DomainResult<Unit> = DomainResult.Success(Unit)
}
