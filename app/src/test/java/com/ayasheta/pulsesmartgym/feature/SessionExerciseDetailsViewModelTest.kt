package com.ayasheta.pulsesmartgym.feature

import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionRestState
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsUiState
import com.ayasheta.pulsesmartgym.feature.workout.exercise.SessionExerciseDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
class SessionExerciseDetailsViewModelTest {

    private val mainThreadSurrogate = newSingleThreadContext("SessionExerciseDetailsTest")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun resolvesDistinctExercisesByOccurrenceId_withoutModifyingSession() = runBlocking {
        val authRepo = TestAuthRepo(AuthState.Authenticated(AuthUser("owner_1", "Aya", "aya@pulse.test", false)))
        val sessionRepo = TestSessionRepo()
        val exerciseRepo = TestExerciseRepo()

        val session = WorkoutSession(
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
                    plannedSets = listOf(SessionPlannedSet("ps_1", "se_1", 1, 10, null)),
                    performedSets = emptyList(),
                ),
                SessionExercise(
                    id = "se_2",
                    sessionId = "sess_1",
                    sourceExerciseId = "ex_db_incline_press",
                    exerciseNameSnapshot = "DB Incline Press",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.DUMBBELL,
                    trackingCategorySnapshot = TrackingCategory.COMPOUND,
                    orderIndex = 1,
                    plannedSets = listOf(SessionPlannedSet("ps_2", "se_2", 1, 10, null)),
                    performedSets = emptyList(),
                ),
            ),
        )
        sessionRepo.putSession(session)

        val vm1 = SessionExerciseDetailsViewModel("sess_1", "se_1", authRepo, sessionRepo, exerciseRepo)
        val vm2 = SessionExerciseDetailsViewModel("sess_1", "se_2", authRepo, sessionRepo, exerciseRepo)

        val state1 = vm1.uiState.first { it !is ExerciseDetailsUiState.Loading } as ExerciseDetailsUiState.Loaded
        val state2 = vm2.uiState.first { it !is ExerciseDetailsUiState.Loading } as ExerciseDetailsUiState.Loaded

        assertEquals("ex_bench_press", state1.exerciseId)
        assertEquals("Barbell Bench Press", state1.name)
        assertEquals("ex_db_incline_press", state2.exerciseId)
        assertEquals("DB Incline Press", state2.name)
        assertTrue(state1.guide.instructions != state2.guide.instructions)
        assertEquals(session, sessionRepo.getSession("owner_1", "sess_1"))
    }

    @Test
    fun enforcesOwnerIsolation_andSimulatorIsolation() = runBlocking {
        val owner1Auth = TestAuthRepo(AuthState.Authenticated(AuthUser("owner_1", "Aya", "aya@pulse.test", false)))
        val owner2Auth = TestAuthRepo(AuthState.Authenticated(AuthUser("owner_2", "Other", "other@pulse.test", false)))
        val genuineSessionRepo = TestSessionRepo()
        val simulatorSessionRepo = TestSessionRepo()
        val exerciseRepo = TestExerciseRepo()

        genuineSessionRepo.putSession(
            WorkoutSession(
                id = "sess_iso",
                ownerProfileId = "owner_1",
                sourcePlanId = "plan_1",
                planNameSnapshot = "Push",
                status = SessionStatus.ACTIVE,
                startedAtEpochMs = 1_000L,
                exercises = listOf(
                    SessionExercise(
                        id = "se_1",
                        sessionId = "sess_iso",
                        sourceExerciseId = "ex_bench_press",
                        exerciseNameSnapshot = "Barbell Bench Press",
                        muscleGroupSnapshot = MuscleGroup.CHEST,
                        equipmentSnapshot = EquipmentCategory.BARBELL,
                        trackingCategorySnapshot = TrackingCategory.LINEAR,
                        orderIndex = 0,
                        plannedSets = emptyList(),
                        performedSets = emptyList(),
                    ),
                ),
            ),
        )

        val wrongOwnerVm = SessionExerciseDetailsViewModel("sess_iso", "se_1", owner2Auth, genuineSessionRepo, exerciseRepo)
        val wrongOwnerState = wrongOwnerVm.uiState.first { it !is ExerciseDetailsUiState.Loading }
        assertTrue(wrongOwnerState is ExerciseDetailsUiState.NotFound)

        val simulatorVm = SessionExerciseDetailsViewModel("sess_iso", "se_1", owner1Auth, simulatorSessionRepo, exerciseRepo)
        val simulatorState = simulatorVm.uiState.first { it !is ExerciseDetailsUiState.Loading }
        assertTrue(simulatorState is ExerciseDetailsUiState.NotFound)
    }

    @Test
    fun missingCatalogDefinition_fallsBackSafelyWithoutLosingSessionAccess() = runBlocking {
        val authRepo = TestAuthRepo(AuthState.Authenticated(AuthUser("owner_1", "Aya", "guest@pulse.test", true)))
        val sessionRepo = TestSessionRepo()
        val exerciseRepo = TestExerciseRepo()

        sessionRepo.putSession(
            WorkoutSession(
                id = "sess_custom",
                ownerProfileId = "owner_1",
                sourcePlanId = "plan_custom",
                planNameSnapshot = "Custom Plan",
                status = SessionStatus.ACTIVE,
                startedAtEpochMs = 1_000L,
                exercises = listOf(
                    SessionExercise(
                        id = "se_custom",
                        sessionId = "sess_custom",
                        sourceExerciseId = "deleted_custom_ex",
                        exerciseNameSnapshot = "Landmine Rotational Press",
                        muscleGroupSnapshot = MuscleGroup.SHOULDERS,
                        equipmentSnapshot = EquipmentCategory.BARBELL,
                        trackingCategorySnapshot = TrackingCategory.COMPOUND,
                        orderIndex = 0,
                        plannedSets = emptyList(),
                        performedSets = emptyList(),
                    ),
                ),
            ),
        )

        val vm = SessionExerciseDetailsViewModel("sess_custom", "se_custom", authRepo, sessionRepo, exerciseRepo)
        val state = vm.uiState.first { it !is ExerciseDetailsUiState.Loading } as ExerciseDetailsUiState.Loaded
        assertEquals("Landmine Rotational Press", state.name)
        assertTrue(state.guide.instructions.isNotEmpty())
    }
}

private class TestAuthRepo(initial: AuthState) : AuthRepository {
    private val state = MutableStateFlow(initial)
    override fun observeAuthState(): Flow<AuthState> = state
    override suspend fun restoreSession(): AuthState = state.value
    override suspend fun continueAsGuest() = throw UnsupportedOperationException()
    override suspend fun registerLocalAccount(fullName: String, email: String, password: String) = throw UnsupportedOperationException()
    override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean) = throw UnsupportedOperationException()
    override suspend fun resetPassword(newPassword: String) = throw UnsupportedOperationException()
    override suspend fun signInDemo(displayName: String, email: String) = throw UnsupportedOperationException()
    override suspend fun signOut() { state.value = AuthState.Unauthenticated }
}

private class TestExerciseRepo : ExerciseRepository {
    private val items = ExerciseCatalogSeed.defaultExercises.map { entity ->
        ExerciseDefinition(
            id = entity.id,
            name = entity.name,
            muscleGroup = entity.muscleGroup,
            equipment = entity.equipment,
            trackingCategory = entity.trackingCategory,
            description = entity.description,
            imageKey = entity.imageKey,
        )
    }
    override fun observeAll(): Flow<List<ExerciseDefinition>> = MutableStateFlow(items)
    override fun search(
        query: String,
        muscleGroup: MuscleGroup?,
        equipment: EquipmentCategory?,
        trackingCategory: TrackingCategory?,
    ): Flow<List<ExerciseDefinition>> = MutableStateFlow(items)
    override suspend fun getById(id: String): ExerciseDefinition? = items.firstOrNull { it.id == id }
}

private class TestSessionRepo : WorkoutSessionRepository {
    private val sessions = MutableStateFlow<Map<String, WorkoutSession>>(emptyMap())
    fun putSession(session: WorkoutSession) { sessions.value = sessions.value + (session.id to session) }
    override fun observeActiveSession(ownerId: String): Flow<WorkoutSession?> =
        sessions.map { m -> m.values.firstOrNull { it.ownerProfileId == ownerId && it.status == SessionStatus.ACTIVE } }
    override fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        sessions.map { m -> m[sessionId]?.takeIf { it.ownerProfileId == ownerId } }
    override suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession? =
        sessions.value[sessionId]?.takeIf { it.ownerProfileId == ownerId }
    override suspend fun startSessionFromPlan(ownerId: String, planId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override suspend fun startSessionFromHistory(ownerId: String, historicalSessionId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest): RecordSetResult = throw UnsupportedOperationException()
    override suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest): DomainResult<PerformedSet> = throw UnsupportedOperationException()
    override suspend fun completeSession(ownerId: String, sessionId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override suspend fun endSessionEarly(ownerId: String, sessionId: String): DomainResult<WorkoutSession> = throw UnsupportedOperationException()
    override fun observeRestState(ownerId: String, sessionId: String): Flow<SessionRestState?> = MutableStateFlow(null)
    override suspend fun startRestTimer(
        ownerId: String,
        sessionId: String,
        exerciseId: String,
        exerciseName: String,
        completedSetIndex: Int,
        durationSeconds: Int,
        nextExerciseId: String?,
        nextSetIndex: Int?,
    ): DomainResult<SessionRestState> = throw UnsupportedOperationException()
    override suspend fun extendRestTimer(ownerId: String, sessionId: String, additionalSeconds: Int): DomainResult<SessionRestState> =
        throw UnsupportedOperationException()
    override suspend fun clearRestTimer(ownerId: String, sessionId: String): DomainResult<Unit> = throw UnsupportedOperationException()
}
