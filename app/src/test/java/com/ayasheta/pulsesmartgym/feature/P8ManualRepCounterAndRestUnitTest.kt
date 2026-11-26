package com.ayasheta.pulsesmartgym.feature

import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
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
import com.ayasheta.pulsesmartgym.core.model.session.SessionProgression
import com.ayasheta.pulsesmartgym.core.model.session.SessionRestState
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterEvent
import com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterUiState
import com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterViewModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionEvent
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionUiState
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionViewModel
import com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetUiState
import com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class P8ManualRepCounterAndRestUnitTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun readyStartSetViewModel_resolvesSnapshotTarget_andHandlesMissingOrCompleted() = runBlocking {
        val sessionRepo = FakeP8SessionRepository()
        val authRepo = FakeP8AuthRepository("user_1")
        val prefsRepo = FakeP8PreferencesRepository()
        sessionRepo.putSession(sampleSession())

        val vm = ReadyStartSetViewModel(
            sessionId = "sess_1",
            exerciseId = "ex_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
        )

        val ready = withTimeout(5_000) {
            vm.uiState.first { it is ReadyStartSetUiState.Ready } as ReadyStartSetUiState.Ready
        }
        assertEquals("Barbell Bench Press", ready.exerciseName)
        assertEquals(1, ready.setIndex)
        assertEquals(2, ready.totalSetsForExercise)
        assertEquals(10, ready.targetReps)
        assertEquals(80.0, ready.targetLoad!!.toKilograms(), 0.01)
        assertEquals(90, ready.targetRestSeconds)

        val missingVm = ReadyStartSetViewModel(
            sessionId = "sess_1",
            exerciseId = "ex_missing",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
        )
        val missing = withTimeout(5_000) {
            missingVm.uiState.first { it is ReadyStartSetUiState.Missing }
        }
        assertTrue(missing is ReadyStartSetUiState.Missing)
    }

    @Test
    fun manualRepCounter_startsAtZero_clampsDecrement_adjustsWeight_andProtectsDuplicateSubmit() = runBlocking {
        var nowMs = 10_000L
        val sessionRepo = FakeP8SessionRepository(clockMs = { nowMs })
        val authRepo = FakeP8AuthRepository("user_1")
        val prefsRepo = FakeP8PreferencesRepository()
        sessionRepo.putSession(sampleSession())

        val vm = ManualRepCounterViewModel(
            sessionId = "sess_1",
            exerciseId = "ex_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            elapsedRealtimeMs = { nowMs },
        )

        val initial = withTimeout(5_000) {
            vm.uiState.first { it is ManualRepCounterUiState.Active } as ManualRepCounterUiState.Active
        }
        assertEquals(0, initial.currentReps)
        assertEquals(80.0, initial.actualLoad!!.toKilograms(), 0.01)

        // Decrement at 0 never goes negative
        vm.decrementRep()
        assertEquals(0, (vm.uiState.value as ManualRepCounterUiState.Active).currentReps)

        // Increment reps & weight
        repeat(9) { vm.incrementRep() }
        vm.decrementRep()
        vm.incrementWeight()

        val adjusted = withTimeout(5_000) {
            vm.uiState.first {
                it is ManualRepCounterUiState.Active &&
                    it.currentReps == 8 &&
                    it.actualLoad != null &&
                    it.actualLoad.toKilograms() > 82.0
            } as ManualRepCounterUiState.Active
        }
        assertEquals(8, adjusted.currentReps)
        assertEquals(82.5, adjusted.actualLoad!!.toKilograms(), 0.01)

        // LBS unit step
        prefsRepo.updatePreferredWeightUnit(WeightUnit.LBS)
        vm.incrementWeight()
        val lbsAdjusted = withTimeout(5_000) {
            vm.uiState.first {
                it is ManualRepCounterUiState.Active && it.preferredWeightUnit == WeightUnit.LBS
            } as ManualRepCounterUiState.Active
        }
        assertEquals(WeightUnit.LBS, lbsAdjusted.preferredWeightUnit)

        // Collect SetFinished event & submit twice rapidly
        val events = mutableListOf<ManualRepCounterEvent>()
        val collectJob = launch(start = CoroutineStart.UNDISPATCHED) { vm.events.collect { events += it } }
        nowMs = 25_000L
        vm.finishSet()
        vm.finishSet()
        withTimeout(5_000) {
            while (events.isEmpty()) kotlinx.coroutines.delay(10)
        }
        collectJob.cancel()

        assertEquals(1, sessionRepo.recordCalls)
        assertEquals(1, events.size)
        val finished = events.first() as ManualRepCounterEvent.SetFinished
        assertTrue(finished.hasRestStarted)
        assertEquals("ex_1", finished.nextTarget?.exerciseId)
        assertEquals(2, finished.nextTarget?.setIndex)

        val savedSession = sessionRepo.getSession("user_1", "sess_1")!!
        val performed = savedSession.exercises.first().performedSets.single()
        assertEquals(8, performed.fullReps)
        assertEquals(0, performed.partialReps)
        assertEquals(TrackingMode.MANUAL, performed.trackingMode)
        assertEquals(MeasurementOrigin.MANUAL_INPUT, performed.origin)
        assertTrue(performed.repRecords.isEmpty())
        assertEquals(15_000L, performed.durationMs)

        val rest = sessionRepo.observeRestState("user_1", "sess_1").first()
        assertNotNull(rest)
        assertEquals(90, rest!!.durationSeconds)
        assertEquals(25_000L + 90_000L, rest.deadlineEpochMs)
    }

    @Test
    fun sessionProgression_findsNextUncompletedSet_respectingNonSequentialOrder() {
        val base = sampleSession()
        // Complete ex_2 set 1 out of order
        val ex2Completed = base.copy(
            exercises = base.exercises.map { ex ->
                if (ex.id == "ex_2") {
                    ex.copy(
                        performedSets = listOf(
                            PerformedSet(
                                id = "p_1",
                                sessionId = "sess_1",
                                sessionExerciseId = "ex_2",
                                setIndex = 1,
                                fullReps = 12,
                                partialReps = 0,
                                actualLoad = WeightLoad.fromKilograms(40.0),
                                completedAtEpochMs = 1_000L,
                                trackingMode = TrackingMode.MANUAL,
                                origin = MeasurementOrigin.MANUAL_INPUT,
                            ),
                        ),
                    )
                } else {
                    ex
                }
            },
        )
        // After ex_2 set 1, nothing left after it -> wraps around to ex_1 set 1
        val next = SessionProgression.findNextUncompletedSet(
            session = ex2Completed,
            afterExerciseId = "ex_2",
            afterSetIndex = 1,
        )
        assertEquals("ex_1", next?.exerciseId)
        assertEquals(1, next?.setIndex)
    }

    @Test
    fun restTimer_supportsExtend30s_skip_andNaturalExpiration() = runBlocking {
        var nowMs = 100_000L
        val sessionRepo = FakeP8SessionRepository(clockMs = { nowMs })
        val authRepo = FakeP8AuthRepository("user_1")
        val prefsRepo = FakeP8PreferencesRepository()
        sessionRepo.putSession(sampleSession())
        sessionRepo.startRestTimer(
            ownerId = "user_1",
            sessionId = "sess_1",
            exerciseId = "ex_1",
            exerciseName = "Barbell Bench Press",
            completedSetIndex = 1,
            durationSeconds = 60,
            nextExerciseId = "ex_1",
            nextSetIndex = 2,
        )

        val vm = WorkoutSessionViewModel(
            sessionId = "sess_1",
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            clockMs = { nowMs },
        )
        val events = mutableListOf<WorkoutSessionEvent>()
        val collectJob = launch(start = CoroutineStart.UNDISPATCHED) { vm.events.collect { events += it } }
        val initial = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer != null } as WorkoutSessionUiState.Active
        }
        assertEquals(60, initial.restTimer!!.remainingSeconds)
        assertEquals("01:00", initial.restTimer!!.remainingFormatted)

        // Extend +30s
        vm.extendRestTimer(30)
        val extended = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer?.remainingSeconds == 90 } as WorkoutSessionUiState.Active
        }
        assertEquals(90, extended.restTimer!!.remainingSeconds)

        // Advance time past deadline -> refresh + onRestExpired clears rest and keeps user on dashboard (no forced navigation)
        nowMs = 195_000L
        vm.refreshElapsedTime()
        vm.onRestExpired()
        val afterExpire = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer == null } as WorkoutSessionUiState.Active
        }
        assertNull(afterExpire.restTimer)
        assertTrue(events.none { it is WorkoutSessionEvent.NavigateToNextSet })

        // Start another rest and test Skip Rest (also keeps user on dashboard without forced navigation)
        sessionRepo.startRestTimer(
            ownerId = "user_1",
            sessionId = "sess_1",
            exerciseId = "ex_1",
            exerciseName = "Barbell Bench Press",
            completedSetIndex = 1,
            durationSeconds = 90,
            nextExerciseId = "ex_2",
            nextSetIndex = 1,
        )
        withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer != null }
        }
        vm.skipRestTimer()
        val afterSkip = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer == null } as WorkoutSessionUiState.Active
        }
        assertNull(afterSkip.restTimer)
        assertTrue(events.none { it is WorkoutSessionEvent.NavigateToNextSet })
        collectJob.cancel()
    }

    @Test
    fun manualRepCounter_whenRestTimerPersistenceFails_keepsSetRecorded_emitsHasRestStartedFalse_andSurfacesError() = runBlocking {
        var nowMs = 5_000L
        val sessionRepo = FakeP8SessionRepository(clockMs = { nowMs }).apply {
            failStartRest = true
        }
        val authRepo = FakeP8AuthRepository("user_1")
        val prefsRepo = FakeP8PreferencesRepository()
        sessionRepo.putSession(sampleSession())

        val vm = ManualRepCounterViewModel(
            sessionId = "sess_1",
            exerciseId = "ex_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            elapsedRealtimeMs = { nowMs },
        )
        withTimeout(5_000) {
            vm.uiState.first { it is ManualRepCounterUiState.Active }
        }
        repeat(6) { vm.incrementRep() }

        val events = mutableListOf<ManualRepCounterEvent>()
        val collectJob = launch(start = CoroutineStart.UNDISPATCHED) { vm.events.collect { events += it } }
        nowMs = 17_500L
        vm.finishSet()
        withTimeout(5_000) {
            while (events.isEmpty()) kotlinx.coroutines.delay(10)
        }
        collectJob.cancel()

        // Performed set remains recorded and is never re-recorded
        assertEquals(1, sessionRepo.recordCalls)
        val savedSession = sessionRepo.getSession("user_1", "sess_1")!!
        val performed = savedSession.exercises.first().performedSets.single()
        assertEquals(6, performed.fullReps)
        assertEquals(12_500L, performed.durationMs)

        // Never claim rest started; emit error + keep next uncompleted set accessible
        val finished = events.single() as ManualRepCounterEvent.SetFinished
        assertFalse(finished.hasRestStarted)
        assertNotNull(finished.restErrorMessage)
        assertTrue(finished.restErrorMessage!!.contains("rest timer could not be started", ignoreCase = true))
        assertEquals("ex_1", finished.nextTarget?.exerciseId)
        assertEquals(2, finished.nextTarget?.setIndex)
        assertNull(sessionRepo.observeRestState("user_1", "sess_1").first())

        // Dashboard displays recoverable error while next uncompleted set remains accessible
        val dashboardVm = WorkoutSessionViewModel(
            sessionId = "sess_1",
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            clockMs = { nowMs },
        )
        dashboardVm.setActionError(finished.restErrorMessage!!)
        val dashboardState = withTimeout(5_000) {
            dashboardVm.uiState.first {
                it is WorkoutSessionUiState.Active && it.actionError != null
            } as WorkoutSessionUiState.Active
        }
        assertEquals(finished.restErrorMessage, dashboardState.actionError)
        assertNull(dashboardState.restTimer)
        assertNull(dashboardState.exercises.first().sets.first { it.setIndex == 2 }.performed)
    }

    @Test
    fun restTimer_handlesExtendFailure_skipFailure_expirationFailure_retry_andAccountSwitch(): Unit = runBlocking {
        var nowMs = 100_000L
        val sessionRepo = FakeP8SessionRepository(clockMs = { nowMs })
        val authRepo = FakeP8AuthRepository("user_1")
        val prefsRepo = FakeP8PreferencesRepository()
        sessionRepo.putSession(sampleSession())
        sessionRepo.startRestTimer("user_1", "sess_1", "ex_1", "Barbell Bench Press", 1, 60, "ex_1", 2)

        val vm = WorkoutSessionViewModel("sess_1", authRepo, sessionRepo, prefsRepo, clockMs = { nowMs })
        withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer != null }
        }

        // 1. Failed extend preserves deadline and surfaces error; retry succeeds
        sessionRepo.failExtendRest = true
        vm.extendRestTimer(30)
        val afterExtendFail = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.actionError != null } as WorkoutSessionUiState.Active
        }
        assertEquals(60, afterExtendFail.restTimer!!.remainingSeconds)
        assertTrue(afterExtendFail.actionError!!.contains("extend", ignoreCase = true))

        sessionRepo.failExtendRest = false
        vm.extendRestTimer(30)
        val afterExtendRetry = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer?.remainingSeconds == 90 && it.actionError == null } as WorkoutSessionUiState.Active
        }
        assertEquals(90, afterExtendRetry.restTimer!!.remainingSeconds)

        // 2. Failed skip preserves rest state; rapid skip + retry clears cleanly
        sessionRepo.failClearRest = true
        vm.skipRestTimer()
        vm.skipRestTimer()
        val afterSkipFail = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.actionError != null } as WorkoutSessionUiState.Active
        }
        assertNotNull(afterSkipFail.restTimer)
        assertTrue(afterSkipFail.actionError!!.contains("skip", ignoreCase = true))

        sessionRepo.failClearRest = false
        vm.skipRestTimer()
        withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer == null }
        }

        // 3. Failed natural expiration does not loop infinitely
        sessionRepo.clearCallsForLastFail = 0
        sessionRepo.startRestTimer("user_1", "sess_1", "ex_1", "Barbell Bench Press", 1, 30, "ex_1", 2)
        withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.restTimer != null }
        }
        sessionRepo.failClearRest = true
        nowMs = 140_000L
        vm.refreshElapsedTime()
        vm.onRestExpired()
        vm.onRestExpired()
        val afterExpireFail = withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.Active && it.actionError != null } as WorkoutSessionUiState.Active
        }
        assertEquals(1, sessionRepo.clearCallsForLastFail)
        assertTrue(afterExpireFail.actionError!!.contains("clear", ignoreCase = true))

        // 4. Account switch transitions cleanly to SignedOut
        authRepo.signOut()
        withTimeout(5_000) {
            vm.uiState.first { it is WorkoutSessionUiState.SignedOut }
        }
    }

    @Test
    fun manualRepCounter_restoresSavedStateHandle_andDurableCompletedState() = runBlocking {
        var nowMs = 20_000L
        val sessionRepo = FakeP8SessionRepository(clockMs = { nowMs })
        val authRepo = FakeP8AuthRepository("user_1")
        val prefsRepo = FakeP8PreferencesRepository()
        sessionRepo.putSession(sampleSession())
        val savedState = androidx.lifecycle.SavedStateHandle()

        val vm1 = ManualRepCounterViewModel(
            sessionId = "sess_1",
            exerciseId = "ex_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            savedStateHandle = savedState,
            elapsedRealtimeMs = { nowMs },
        )
        withTimeout(5_000) { vm1.uiState.first { it is ManualRepCounterUiState.Active } }
        repeat(5) { vm1.incrementRep() }
        vm1.incrementWeight()

        // Simulate process/VM recreation with the same SavedStateHandle
        nowMs = 30_000L
        val vm2 = ManualRepCounterViewModel(
            sessionId = "sess_1",
            exerciseId = "ex_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            savedStateHandle = savedState,
            elapsedRealtimeMs = { nowMs },
        )
        val restored = withTimeout(5_000) {
            vm2.uiState.first { it is ManualRepCounterUiState.Active } as ManualRepCounterUiState.Active
        }
        assertEquals(5, restored.currentReps)
        assertEquals(82.5, restored.actualLoad!!.toKilograms(), 0.01)

        // Finish set -> durable Completed state survives recreation even if SharedFlow event was missed
        vm2.finishSet()
        val completed = withTimeout(5_000) {
            vm2.uiState.first { it is ManualRepCounterUiState.Completed } as ManualRepCounterUiState.Completed
        }
        assertTrue(completed.hasRestStarted)
        val savedPerf = sessionRepo.getSession("user_1", "sess_1")!!.exercises.first().performedSets.single()
        assertEquals(10_000L, savedPerf.durationMs)

        val vm3 = ManualRepCounterViewModel(
            sessionId = "sess_1",
            exerciseId = "ex_1",
            setIndex = 1,
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            savedStateHandle = savedState,
            elapsedRealtimeMs = { nowMs },
        )
        val restoredCompleted = withTimeout(5_000) {
            vm3.uiState.first { it is ManualRepCounterUiState.Completed } as ManualRepCounterUiState.Completed
        }
        assertEquals("sess_1", restoredCompleted.sessionId)
    }

    private fun sampleSession(): WorkoutSession = WorkoutSession(
        id = "sess_1",
        ownerProfileId = "user_1",
        sourcePlanId = "plan_1",
        planNameSnapshot = "Upper Body Power",
        startedAtEpochMs = 1_000L,
        endedAtEpochMs = null,
        status = SessionStatus.ACTIVE,
        exercises = listOf(
            SessionExercise(
                id = "ex_1",
                sessionId = "sess_1",
                sourceExerciseId = "cat_bench",
                exerciseNameSnapshot = "Barbell Bench Press",
                muscleGroupSnapshot = MuscleGroup.CHEST,
                equipmentSnapshot = EquipmentCategory.BARBELL,
                trackingCategorySnapshot = TrackingCategory.LINEAR,
                orderIndex = 0,
                plannedSets = listOf(
                    SessionPlannedSet("ps_1", "ex_1", 1, 10, WeightLoad.fromKilograms(80.0), 90),
                    SessionPlannedSet("ps_2", "ex_1", 2, 10, WeightLoad.fromKilograms(80.0), 90),
                ),
                performedSets = emptyList(),
            ),
            SessionExercise(
                id = "ex_2",
                sessionId = "sess_1",
                sourceExerciseId = "cat_row",
                exerciseNameSnapshot = "Bent-Over Row",
                muscleGroupSnapshot = MuscleGroup.BACK,
                equipmentSnapshot = EquipmentCategory.BARBELL,
                trackingCategorySnapshot = TrackingCategory.LINEAR,
                orderIndex = 1,
                plannedSets = listOf(
                    SessionPlannedSet("ps_3", "ex_2", 1, 12, WeightLoad.fromKilograms(60.0), 60),
                ),
                performedSets = emptyList(),
            ),
        ),
    )
}

private class FakeP8AuthRepository(userId: String) : AuthRepository {
    private val state = MutableStateFlow<AuthState>(
        AuthState.Authenticated(
            AuthUser(
                id = userId,
                displayName = "Tester",
                email = "$userId@pulse.local",
                isGuest = true,
                isDemoUser = false,
            ),
        ),
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

private class FakeP8PreferencesRepository : UserPreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences())
    override fun observePreferences(): Flow<UserPreferences> = prefs
    override suspend fun updatePreferredWeightUnit(unit: WeightUnit) { prefs.value = prefs.value.copy(preferredWeightUnit = unit) }
    override suspend fun setOnboardingCompleted(completed: Boolean) { prefs.value = prefs.value.copy(onboardingCompleted = completed) }
    override suspend fun updateDefaultRestSeconds(seconds: Int) { prefs.value = prefs.value.copy(defaultRestSeconds = seconds) }
}

private class FakeP8SessionRepository(
    private val clockMs: () -> Long = { 10_000L },
) : WorkoutSessionRepository {
    private val sessions = MutableStateFlow<Map<String, WorkoutSession>>(emptyMap())
    private val restStates = MutableStateFlow<Map<String, SessionRestState>>(emptyMap())
    var recordCalls = 0
    var failStartRest = false
    var failExtendRest = false
    var failClearRest = false
    var clearCallsForLastFail = 0

    fun putSession(session: WorkoutSession) {
        sessions.value = sessions.value + (session.id to session)
    }

    override fun observeActiveSession(ownerId: String): Flow<WorkoutSession?> =
        sessions.map { map -> map.values.firstOrNull { it.ownerProfileId == ownerId && it.status == SessionStatus.ACTIVE } }

    override fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        sessions.map { map -> map[sessionId]?.takeIf { it.ownerProfileId == ownerId } }

    override suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession? =
        sessions.value[sessionId]?.takeIf { it.ownerProfileId == ownerId }

    override suspend fun startSessionFromPlan(ownerId: String, planId: String) = throw UnsupportedOperationException()

    override suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest): RecordSetResult {
        recordCalls++
        val current = getSession(ownerId, request.sessionId) ?: return RecordSetResult.NotFound("Missing")
        val targetEx = current.exercises.firstOrNull { it.id == request.sessionExerciseId } ?: return RecordSetResult.NotFound("Missing ex")
        if (targetEx.performedSets.any { it.setIndex == request.setIndex }) {
            return RecordSetResult.AlreadyRecorded(request.sessionId, request.sessionExerciseId, request.setIndex)
        }
        val performed = PerformedSet(
            id = "perf_${request.sessionExerciseId}_${request.setIndex}",
            sessionId = request.sessionId,
            sessionExerciseId = request.sessionExerciseId,
            setIndex = request.setIndex,
            fullReps = request.fullReps,
            partialReps = request.partialReps,
            actualLoad = request.actualLoad,
            completedAtEpochMs = clockMs(),
            trackingMode = request.trackingMode,
            origin = request.origin,
            durationMs = request.durationMs,
            repRecords = request.repRecords,
        )
        putSession(
            current.copy(
                exercises = current.exercises.map { ex ->
                    if (ex.id == request.sessionExerciseId) ex.copy(performedSets = ex.performedSets + performed) else ex
                },
            ),
        )
        return RecordSetResult.Recorded(performed.id)
    }

    override suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest) = throw UnsupportedOperationException()

    override suspend fun completeSession(ownerId: String, sessionId: String): DomainResult<WorkoutSession> =
        throw UnsupportedOperationException()

    override suspend fun endSessionEarly(ownerId: String, sessionId: String): DomainResult<WorkoutSession> {
        val current = getSession(ownerId, sessionId) ?: return DomainResult.Failure(DataError.NotFound("Missing"))
        val ended = current.copy(status = SessionStatus.PARTIALLY_COMPLETED, endedAtEpochMs = clockMs())
        putSession(ended)
        restStates.value = restStates.value - sessionId
        return DomainResult.Success(ended)
    }

    override fun observeRestState(ownerId: String, sessionId: String): Flow<SessionRestState?> =
        restStates.map { map ->
            val session = sessions.value[sessionId]
            if (session != null && session.ownerProfileId == ownerId && session.status == SessionStatus.ACTIVE) {
                map[sessionId]
            } else {
                null
            }
        }

    override suspend fun startRestTimer(
        ownerId: String,
        sessionId: String,
        exerciseId: String,
        exerciseName: String,
        completedSetIndex: Int,
        durationSeconds: Int,
        nextExerciseId: String?,
        nextSetIndex: Int?,
    ): DomainResult<SessionRestState> {
        if (failStartRest) {
            return DomainResult.Failure(DataError.StorageError("Simulated rest persistence failure"))
        }
        val rest = SessionRestState(
            sessionId = sessionId,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            completedSetIndex = completedSetIndex,
            nextExerciseId = nextExerciseId,
            nextSetIndex = nextSetIndex,
            durationSeconds = durationSeconds,
            deadlineEpochMs = clockMs() + durationSeconds.toLong() * 1000L,
        )
        restStates.value = restStates.value + (sessionId to rest)
        return DomainResult.Success(rest)
    }

    override suspend fun extendRestTimer(ownerId: String, sessionId: String, additionalSeconds: Int): DomainResult<SessionRestState> {
        if (failExtendRest) {
            return DomainResult.Failure(DataError.StorageError("Simulated extend failure"))
        }
        val existing = restStates.value[sessionId] ?: return DomainResult.Failure(DataError.NotFound("No rest"))
        val updated = existing.copy(
            durationSeconds = existing.durationSeconds + additionalSeconds,
            deadlineEpochMs = existing.deadlineEpochMs + additionalSeconds.toLong() * 1000L,
        )
        restStates.value = restStates.value + (sessionId to updated)
        return DomainResult.Success(updated)
    }

    override suspend fun clearRestTimer(ownerId: String, sessionId: String): DomainResult<Unit> {
        if (failClearRest) {
            clearCallsForLastFail++
            return DomainResult.Failure(DataError.StorageError("Simulated clear failure"))
        }
        restStates.value = restStates.value - sessionId
        return DomainResult.Success(Unit)
    }
}
