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
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionEvent
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionUiState
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionViewModel
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultUiState
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class P7WorkoutSessionUnitTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mapper_calculatesProgressNonSequentialRepeatedExercisesAndPartialReps() {
        val ex1 = sessionExercise("se_1", "ex_bench", "Bench Press", 0, plannedCount = 2, performed = emptyList())
        val ex2 = sessionExercise("se_2", "ex_bench", "Bench Press", 1, plannedCount = 2, performed = emptyList())
        val session0 = WorkoutSession("s1", "owner_a", "p1", "Upper", 1_000L, null, SessionStatus.ACTIVE, listOf(ex1, ex2))

        val ui0 = SessionUiMapper.mapToActiveUiState(session0, WeightUnit.KG, nowEpochMs = 66_000L)
        assertEquals(0, ui0.totalRecordedSets)
        assertEquals(4, ui0.totalPlannedSets)
        assertEquals(0f, ui0.progressFraction, 0.001f)
        assertEquals("01:05", ui0.formattedElapsedTime)
        assertFalse(ui0.allSetsCompleted)

        // Non-sequential: Ex2 Set 2 and Ex1 Set 1 recorded (partial reps kept separate)
        val p1 = PerformedSet("ps_1", "s1", "se_1", 1, fullReps = 10, partialReps = 2, actualLoad = WeightLoad.fromKilograms(80.0), 5_000L, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)
        val p2 = PerformedSet("ps_2", "s1", "se_2", 2, fullReps = 8, partialReps = 0, actualLoad = null, 6_000L, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)
        val sessionNonSeq = session0.copy(
            exercises = listOf(
                ex1.copy(performedSets = listOf(p1)),
                ex2.copy(performedSets = listOf(p2)),
            ),
        )
        val uiNonSeq = SessionUiMapper.mapToActiveUiState(sessionNonSeq, WeightUnit.KG, nowEpochMs = 10_000L)
        assertEquals(2, uiNonSeq.totalRecordedSets)
        assertEquals(0.5f, uiNonSeq.progressFraction, 0.001f)
        assertEquals(1, uiNonSeq.exercises[0].completedSetsCount)
        assertEquals(1, uiNonSeq.exercises[1].completedSetsCount)
        assertNotNull(uiNonSeq.exercises[0].sets[0].performed)
        assertNull(uiNonSeq.exercises[0].sets[1].performed)
        assertNull(uiNonSeq.exercises[1].sets[0].performed)
        assertNotNull(uiNonSeq.exercises[1].sets[1].performed)
        assertEquals(2, uiNonSeq.exercises[0].sets[0].performed?.partialReps)
        assertFalse(uiNonSeq.exercises[0].isCompleted)

        // Complete Ex1 -> Ex1 completed while Ex2 remains incomplete
        val p1b = PerformedSet("ps_1b", "s1", "se_1", 2, fullReps = 9, partialReps = 0, actualLoad = WeightLoad.fromKilograms(80.0), 7_000L, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)
        val uiEx1Done = SessionUiMapper.mapToActiveUiState(
            sessionNonSeq.copy(exercises = listOf(ex1.copy(performedSets = listOf(p1, p1b)), ex2.copy(performedSets = listOf(p2)))),
            WeightUnit.KG,
            nowEpochMs = 10_000L,
        )
        assertTrue(uiEx1Done.exercises[0].isCompleted)
        assertFalse(uiEx1Done.exercises[1].isCompleted)
        assertFalse(uiEx1Done.allSetsCompleted)

        // Negative clock difference clamped to 00:00
        assertEquals(0L, SessionUiMapper.computeElapsedSeconds(startedAtEpochMs = 50_000L, nowEpochMs = 10_000L))
        assertEquals("00:00", SessionUiMapper.formatElapsedMmSs(-5L))
    }

    @Test
    fun viewModel_handlesValidationRecordingCorrectionCompletionAndAccountSwitch() = runBlocking {
        var nowMs = 10_000L
        val authRepo = FakeSessionAuthRepo(AuthState.Authenticated(user("owner_a")))
        val sessionRepo = FakeWorkoutSessionRepo()
        val prefsRepo = FakePrefsRepo()

        val initialSession = WorkoutSession(
            id = "sess_1",
            ownerProfileId = "owner_a",
            sourcePlanId = "plan_1",
            planNameSnapshot = "Push Day",
            startedAtEpochMs = 10_000L,
            status = SessionStatus.ACTIVE,
            exercises = listOf(
                sessionExercise("se_1", "ex_bench", "Bench Press", 0, plannedCount = 1, performed = emptyList()),
            ),
        )
        sessionRepo.putSession(initialSession)

        val vm = WorkoutSessionViewModel(
            sessionId = "sess_1",
            authRepository = authRepo,
            workoutSessionRepository = sessionRepo,
            userPreferencesRepository = prefsRepo,
            clockMs = { nowMs },
        )

        val active = vm.uiState.awaitValue { it is WorkoutSessionUiState.Active } as WorkoutSessionUiState.Active
        assertEquals("Not Connected", active.connectionLabel)

        // Clock advance + refreshElapsedTime
        nowMs = 75_000L
        vm.refreshElapsedTime()
        assertEquals("01:05", (vm.uiState.value as WorkoutSessionUiState.Active).formattedElapsedTime)

        // Validation rejects 0 reps and negative weight
        vm.openSetEntry("se_1", 1)
        vm.submitManualSet("0", "0", "80")
        assertNotNull((vm.uiState.value as WorkoutSessionUiState.Active).manualEntryError)
        vm.submitManualSet("10", "0", "-5")
        assertNotNull((vm.uiState.value as WorkoutSessionUiState.Active).manualEntryError)

        // Valid set record
        vm.submitManualSet("10", "1", "80")
        val afterRecord = vm.uiState.awaitValue {
            it is WorkoutSessionUiState.Active && it.totalRecordedSets == 1
        } as WorkoutSessionUiState.Active
        assertTrue(afterRecord.allSetsCompleted)
        assertNull(afterRecord.manualEntryTarget)

        // Correction updates existing set
        vm.openSetEntry("se_1", 1)
        assertTrue((vm.uiState.value as WorkoutSessionUiState.Active).manualEntryTarget?.isCorrection == true)
        vm.submitManualSet("12", "0", "82.5")
        val afterCorrect = vm.uiState.awaitValue {
            it is WorkoutSessionUiState.Active &&
                it.exercises.first().sets.first().performed?.fullReps == 12
        } as WorkoutSessionUiState.Active
        assertEquals(82_500L, afterCorrect.exercises.first().sets.first().performed?.actualLoad?.grams)

        // Account switch hides owner_a's session
        authRepo.authState.value = AuthState.Authenticated(user("owner_b"))
        val missing = vm.uiState.awaitValue { it is WorkoutSessionUiState.Missing }
        assertTrue(missing is WorkoutSessionUiState.Missing)

        // Switch back and finish workout
        authRepo.authState.value = AuthState.Authenticated(user("owner_a"))
        vm.uiState.awaitValue { it is WorkoutSessionUiState.Active }
        val events = mutableListOf<WorkoutSessionEvent>()
        val job = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { vm.events.collect { events += it } }
        vm.finishWorkout()
        kotlinx.coroutines.yield()
        assertTrue(events.any { it is WorkoutSessionEvent.Completed && it.sessionId == "sess_1" })
        job.cancel()
    }

    @Test
    fun resultMapper_computesAccurateSummaryForCompletePartialAndAbandonedSessions() {
        val ex1 = sessionExercise(
            "se_1", "ex_1", "Bench Press", 0, 2,
            performed = listOf(
                PerformedSet("ps_1", "s1", "se_1", 1, 10, 2, WeightLoad.fromKilograms(80.0), 5_000L, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT),
                PerformedSet("ps_2", "s1", "se_1", 2, 8, 0, null, 6_000L, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT),
            ),
        )
        val ex2 = sessionExercise("se_2", "ex_2", "Row", 1, 2, performed = emptyList())
        val partial = WorkoutSession("s1", "owner_a", "p1", "Upper", 10_000L, 265_000L, SessionStatus.PARTIALLY_COMPLETED, listOf(ex1, ex2))

        val partialUi = WorkoutResultViewModel.mapSessionToResult(partial, WeightUnit.KG)
        assertEquals("Workout Ended Early", partialUi.statusHeadline)
        assertEquals(18, partialUi.totalFullReps)
        assertEquals(2, partialUi.totalPartialReps)
        assertEquals("4m 15s", partialUi.durationText)
        assertEquals("2 / 4 sets", partialUi.setsChipText)
        assertEquals("800 kg volume", partialUi.volumeChipText)
        assertEquals("1 of 2 Exercises Recorded", partialUi.breakdownHeaderRight)
        assertFalse(partialUi.exercises[0].isSkipped)
        assertTrue(partialUi.exercises[1].isSkipped)

        val abandoned = partial.copy(
            status = SessionStatus.ABANDONED,
            exercises = listOf(ex1.copy(performedSets = emptyList()), ex2),
        )
        val abandonedUi = WorkoutResultViewModel.mapSessionToResult(abandoned, WeightUnit.KG)
        assertEquals("Workout Abandoned", abandonedUi.statusHeadline)
        assertTrue(abandonedUi.statusSubtitle.contains("Not saved to workout history"))
        assertEquals(0, abandonedUi.totalFullReps)
        assertEquals("0 kg volume", abandonedUi.volumeChipText)
    }
}

private fun user(id: String) = AuthUser(id = id, displayName = "Athlete $id", email = "$id@pulse.local", isGuest = false, isDemoUser = false)

private fun sessionExercise(
    id: String,
    sourceId: String,
    name: String,
    orderIndex: Int,
    plannedCount: Int,
    performed: List<PerformedSet>,
): SessionExercise = SessionExercise(
    id = id,
    sessionId = "s1",
    sourceExerciseId = sourceId,
    exerciseNameSnapshot = name,
    muscleGroupSnapshot = MuscleGroup.CHEST,
    equipmentSnapshot = EquipmentCategory.BARBELL,
    trackingCategorySnapshot = TrackingCategory.LINEAR,
    orderIndex = orderIndex,
    plannedSets = (1..plannedCount).map { idx ->
        SessionPlannedSet("sp_${id}_$idx", id, idx, 10, WeightLoad.fromKilograms(80.0), 90)
    },
    performedSets = performed,
)

private suspend fun <T> Flow<T>.awaitValue(predicate: (T) -> Boolean): T = first { predicate(it) }

private class FakeSessionAuthRepo(initial: AuthState) : AuthRepository {
    val authState = MutableStateFlow(initial)
    override fun observeAuthState(): Flow<AuthState> = authState
    override suspend fun restoreSession(): AuthState = authState.value
    override suspend fun continueAsGuest() = throw UnsupportedOperationException()
    override suspend fun registerLocalAccount(fullName: String, email: String, password: String) = throw UnsupportedOperationException()
    override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean) = throw UnsupportedOperationException()
    override suspend fun resetPassword(newPassword: String) = throw UnsupportedOperationException()
    override suspend fun signInDemo(displayName: String, email: String) = throw UnsupportedOperationException()
    override suspend fun signOut() { authState.value = AuthState.Unauthenticated }
}

private class FakePrefsRepo : UserPreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences())
    override fun observePreferences(): Flow<UserPreferences> = prefs
    override suspend fun updatePreferredWeightUnit(unit: WeightUnit) { prefs.value = prefs.value.copy(preferredWeightUnit = unit) }
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun updateDefaultRestSeconds(seconds: Int) {}
}

private class FakeWorkoutSessionRepo : WorkoutSessionRepository {
    private val sessions = MutableStateFlow<Map<String, WorkoutSession>>(emptyMap())
    fun putSession(session: WorkoutSession) { sessions.value = sessions.value + (session.id to session) }

    override fun observeActiveSession(ownerId: String): Flow<WorkoutSession?> =
        sessions.map { map -> map.values.firstOrNull { it.ownerProfileId == ownerId && it.status == SessionStatus.ACTIVE } }

    override fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        sessions.map { map -> map[sessionId]?.takeIf { it.ownerProfileId == ownerId } }

    override suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession? =
        sessions.value[sessionId]?.takeIf { it.ownerProfileId == ownerId }

    override suspend fun startSessionFromPlan(ownerId: String, planId: String): DomainResult<WorkoutSession> =
        throw UnsupportedOperationException()

    override suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest): RecordSetResult {
        val current = getSession(ownerId, request.sessionId) ?: return RecordSetResult.NotFound("Not found")
        if (current.status != SessionStatus.ACTIVE) return RecordSetResult.InvalidState("Not active")
        val updated = current.copy(
            exercises = current.exercises.map { ex ->
                if (ex.id == request.sessionExerciseId) {
                    if (ex.performedSets.any { it.setIndex == request.setIndex }) {
                        return RecordSetResult.AlreadyRecorded(request.sessionId, request.sessionExerciseId, request.setIndex)
                    }
                    ex.copy(
                        performedSets = ex.performedSets + PerformedSet(
                            "ps_${request.setIndex}", request.sessionId, request.sessionExerciseId, request.setIndex,
                            request.fullReps, request.partialReps, request.actualLoad, 1L, request.trackingMode, request.origin,
                        ),
                    )
                } else {
                    ex
                }
            },
        )
        putSession(updated)
        return RecordSetResult.Recorded("ps_${request.setIndex}")
    }

    override suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest): DomainResult<PerformedSet> {
        val current = getSession(ownerId, request.sessionId) ?: return DomainResult.Failure(DataError.NotFound("Not found"))
        var corrected: PerformedSet? = null
        val updated = current.copy(
            exercises = current.exercises.map { ex ->
                if (ex.id == request.sessionExerciseId) {
                    ex.copy(
                        performedSets = ex.performedSets.map { ps ->
                            if (ps.setIndex == request.setIndex) {
                                ps.copy(fullReps = request.fullReps, partialReps = request.partialReps, actualLoad = request.actualLoad)
                                    .also { corrected = it }
                            } else {
                                ps
                            }
                        },
                    )
                } else {
                    ex
                }
            },
        )
        putSession(updated)
        return DomainResult.Success(corrected!!)
    }

    override suspend fun completeSession(ownerId: String, sessionId: String): DomainResult<WorkoutSession> {
        val current = getSession(ownerId, sessionId) ?: return DomainResult.Failure(DataError.NotFound("Not found"))
        val completed = current.copy(status = SessionStatus.COMPLETED, endedAtEpochMs = 99_000L)
        putSession(completed)
        return DomainResult.Success(completed)
    }

    override suspend fun endSessionEarly(ownerId: String, sessionId: String): DomainResult<WorkoutSession> {
        val current = getSession(ownerId, sessionId) ?: return DomainResult.Failure(DataError.NotFound("Not found"))
        val count = current.exercises.sumOf { it.performedSets.size }
        val status = if (count > 0) SessionStatus.PARTIALLY_COMPLETED else SessionStatus.ABANDONED
        val ended = current.copy(status = status, endedAtEpochMs = 99_000L)
        putSession(ended)
        return DomainResult.Success(ended)
    }
}
