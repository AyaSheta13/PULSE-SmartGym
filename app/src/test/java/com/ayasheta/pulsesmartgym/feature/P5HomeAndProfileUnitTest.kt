package com.ayasheta.pulsesmartgym.feature

import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.history.HistoryAggregateMetrics
import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.EditProfileDraft
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.profile.UserProfile
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExercise
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSet
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.feature.home.HomeUiState
import com.ayasheta.pulsesmartgym.feature.home.HomeViewModel
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalSheet
import com.ayasheta.pulsesmartgym.feature.profile.ProfileUiState
import com.ayasheta.pulsesmartgym.feature.profile.ProfileViewModel
import com.ayasheta.pulsesmartgym.feature.profile.edit.EditProfileViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class P5HomeAndProfileUnitTest {

    private val mainThreadSurrogate = newSingleThreadContext("P5TestMain")

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
    fun weeklySummary_respectsLocalTimezoneWeekBoundariesAndPartialPolicy() {
        val nyZone = ZoneId.of("America/New_York")
        val refDate = LocalDate.of(2026, 11, 25)

        val prevWeekSundayLate = ZonedDateTime.of(2026, 11, 22, 23, 30, 0, 0, nyZone)
            .toInstant().toEpochMilli()
        val mondayEarly = ZonedDateTime.of(2026, 11, 23, 0, 15, 0, 0, nyZone)
            .toInstant().toEpochMilli()
        val wednesdayPartial = ZonedDateTime.of(2026, 11, 25, 18, 0, 0, 0, nyZone)
            .toInstant().toEpochMilli()
        val wednesdayActive = ZonedDateTime.of(2026, 11, 25, 19, 0, 0, 0, nyZone)
            .toInstant().toEpochMilli()

        val summaries = listOf(
            sampleSummary("prev", prevWeekSundayLate, SessionStatus.COMPLETED, volumeKg = 9999.0),
            sampleSummary("mon", mondayEarly, SessionStatus.COMPLETED, volumeKg = 1200.0),
            sampleSummary("wed_partial", wednesdayPartial, SessionStatus.PARTIALLY_COMPLETED, volumeKg = 600.0),
            sampleSummary("wed_active", wednesdayActive, SessionStatus.ACTIVE, volumeKg = 5000.0),
        )

        val result = WorkoutAnalytics.calculateWeeklySummary(
            summaries = summaries,
            weeklyTarget = 5,
            referenceDate = refDate,
            zoneId = nyZone,
        )

        assertEquals(1, result.completedWorkoutsCount)
        assertEquals(1, result.partialWorkoutsCount)
        assertEquals(5, result.weeklyTarget)
        assertEquals(1800.0, result.totalVolumeKg, 0.01)
        assertEquals(1200.0, result.dailyVolumeKg[0], 0.01)
        assertEquals(600.0, result.dailyVolumeKg[2], 0.01)
        assertEquals(3, result.dayOfWeekNumber)
        assertEquals(0.2f, result.progressFraction, 0.001f)
    }

    @Test
    fun workoutAnalytics_volumeExcludesPartialRepsAndNullLoads() {
        val sets = listOf(
            PerformedSet(
                id = "s1",
                sessionId = "sess1",
                sessionExerciseId = "e1",
                setIndex = 1,
                fullReps = 10,
                partialReps = 3,
                actualLoad = WeightLoad.fromKilograms(50.0),
                completedAtEpochMs = 1L,
                trackingMode = TrackingMode.MANUAL,
                origin = MeasurementOrigin.MANUAL_INPUT,
            ),
            PerformedSet(
                id = "s2",
                sessionId = "sess1",
                sessionExerciseId = "e1",
                setIndex = 2,
                fullReps = 8,
                partialReps = 2,
                actualLoad = null,
                completedAtEpochMs = 2L,
                trackingMode = TrackingMode.MANUAL,
                origin = MeasurementOrigin.MANUAL_INPUT,
            ),
        )
        assertEquals(500.0, WorkoutAnalytics.calculateVolumeKg(sets), 0.01)
    }

    @Test
    fun homeViewModel_handlesNoPlans_plansNoSessions_populated_andAccountSwitch() = runBlocking {
        val auth = FakeAuthRepo(
            AuthState.Authenticated(
                AuthUser("user_a", "Alex Miller", "alex@pulse.test", false, false, 1_700_000_000_000L),
            ),
        )
        val profiles = FakeProfileRepo()
        val plans = FakePlanRepo()
        val sessions = FakeSessionRepo()
        val history = FakeHistoryRepo()

        profiles.seed(sampleProfile("user_a", "Alex Miller", weeklyWorkouts = 3))

        val vm = HomeViewModel(
            authRepository = auth,
            userProfileRepository = profiles,
            workoutPlanRepository = plans,
            workoutSessionRepository = sessions,
            workoutHistoryRepository = history,
            zoneIdProvider = { ZoneId.of("UTC") },
            dateProvider = { LocalDate.of(2026, 11, 25) },
            timeProvider = { LocalTime.of(9, 0) },
        )
        val collectJob = launch { vm.uiState.collect {} }

        val noPlansState = awaitHomeLoaded(vm) { it.ownerId == "user_a" && it.isNoPlansEmptyState }
        assertNull(noPlansState.featuredPlan)
        assertEquals("Good morning, Alex", noPlansState.greetingTitle)
        assertEquals("Welcome", noPlansState.welcomeOverline)
        assertEquals(3, noPlansState.weeklyProgress.weeklyTarget)

        val plan1 = samplePlan("p1", "user_a", "Push Day", updatedAt = 100L)
        val plan2 = samplePlan("p2", "user_a", "Pull Day", updatedAt = 200L)
        plans.setPlans("user_a", listOf(plan1, plan2))

        val firstWorkoutState = awaitHomeLoaded(vm) { it.plans.size == 2 }
        assertFalse(firstWorkoutState.isNoPlansEmptyState)
        assertFalse(firstWorkoutState.hasCompletedHistory)
        assertEquals("Pull Day", firstWorkoutState.featuredPlan?.name)

        sessions.setActive("user_a", WorkoutSession("sess_act", "user_a", "p1", "Push Day", 10L, null, SessionStatus.ACTIVE, emptyList()))
        val activeState = awaitHomeLoaded(vm) { it.featuredPlan?.name == "Push Day" }
        assertNotNull(activeState.activeSession)

        val wedEpoch = ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        history.setHistory("user_a", listOf(sampleSummary("s_done", wedEpoch, SessionStatus.COMPLETED, volumeKg = 2400.0)))
        val populated = awaitHomeLoaded(vm) { it.hasCompletedHistory }
        assertEquals("Welcome back", populated.welcomeOverline)
        assertEquals(1, populated.weeklyProgress.completedWorkoutsCount)
        assertEquals(2400.0, populated.weeklyProgress.totalVolumeKg, 0.01)
        assertNotNull(populated.lastSession)

        profiles.seed(sampleProfile("user_b", "Jordan Lee", weeklyWorkouts = 5))
        auth.state.value = AuthState.Authenticated(
            AuthUser("user_b", "Jordan Lee", "jordan@pulse.test", false, false, 1_700_000_000_000L),
        )
        val userBState = awaitHomeLoaded(vm) { it.ownerId == "user_b" }
        assertEquals("Jordan Lee", userBState.displayName)
        assertTrue(userBState.isNoPlansEmptyState)
        assertFalse(userBState.hasCompletedHistory)
        assertNull(userBState.lastSession)
        assertEquals(5, userBState.weeklyProgress.weeklyTarget)

        collectJob.cancel()
    }

    @Test
    fun profileAndEditProfile_guestIdentity_validation_preservation_andOwnerIsolation() = runBlocking {
        val guestUser = AuthUser("guest_123", "Guest", "", isDemoUser = false, isGuest = true, createdAtEpochMs = null)
        val auth = FakeAuthRepo(AuthState.Authenticated(guestUser))
        val profiles = FakeProfileRepo()
        val prefs = FakePrefsRepo()

        profiles.seed(sampleProfile("guest_123", "Guest Runner", weeklyWorkouts = 4, age = 31))
        profiles.seed(sampleProfile("owner_b", "Other User", weeklyWorkouts = 5, age = 40))

        val profileVm = ProfileViewModel(auth, profiles, prefs)
        val pJob = launch { profileVm.uiState.collect {} }

        val loaded = withTimeout(2_000) {
            profileVm.uiState.first { it is ProfileUiState.Loaded } as ProfileUiState.Loaded
        }
        assertTrue(loaded.isGuest)
        assertEquals("Offline Guest Profile", loaded.subtitleIdentity)
        assertEquals("GR", loaded.initials)

        profileVm.openSheet(ProfileModalSheet.UNITS_AND_MEASUREMENTS)
        withTimeout(2_000) {
            profileVm.uiState.first {
                (it as? ProfileUiState.Loaded)?.activeSheet == ProfileModalSheet.UNITS_AND_MEASUREMENTS
            }
        }
        profileVm.confirmMetricUnits()
        withTimeout(2_000) {
            profileVm.uiState.first {
                (it as? ProfileUiState.Loaded)?.activeSheet == null
            }
        }

        val editVm = EditProfileViewModel(auth, profiles)
        withTimeout(2_000) { editVm.uiState.first { !it.isLoading } }

        editVm.onFullNameChanged("   ")
        var saved = false
        editVm.saveChanges { saved = true }
        assertFalse(saved)
        assertNotNull(editVm.uiState.value.validationError)

        editVm.onFullNameChanged("Alex Taylor")
        editVm.onWeightChanged("NaN")
        editVm.saveChanges { saved = true }
        assertFalse(saved)
        assertNotNull(editVm.uiState.value.validationError)

        editVm.onWeightChanged("82.5")
        editVm.onHeightChanged("185")
        editVm.onFitnessGoalSelected(FitnessGoal.STRENGTH)
        editVm.saveChanges { saved = true }
        withTimeout(2_000) {
            while (!saved) delay(10)
        }

        val updatedGuest = profiles.getProfile("guest_123")!!
        assertEquals("Alex Taylor", updatedGuest.fullName)
        assertEquals(82.5, updatedGuest.weightKg, 0.01)
        assertEquals(185, updatedGuest.heightCm)
        assertEquals(FitnessGoal.STRENGTH, updatedGuest.fitnessGoal)
        assertEquals(31, updatedGuest.age)
        assertEquals(4, updatedGuest.weeklyWorkouts)
        assertEquals("Other User", profiles.getProfile("owner_b")!!.fullName)

        profiles.failNextSave = true
        editVm.onFullNameChanged("Should Fail")
        saved = false
        editVm.saveChanges { saved = true }
        withTimeout(2_000) {
            editVm.uiState.first { it.saveError != null }
        }
        assertFalse(saved)

        pJob.cancel()
    }

    @Test
    fun editProfileViewModel_missingProfileBlocksSaveAndNeverInventsDefaults() = runBlocking {
        val user = AuthUser("user_missing", "No Profile", "noprofile@pulse.test", false, false, 1_700_000_000_000L)
        val auth = FakeAuthRepo(AuthState.Authenticated(user))
        val profiles = FakeProfileRepo()

        val editVm = EditProfileViewModel(auth, profiles)
        withTimeout(2_000) { editVm.uiState.first { !it.isLoading } }

        val state = editVm.uiState.value
        assertFalse(state.hasExistingProfile)
        assertFalse(state.canSave)
        assertNotNull(state.missingProfileError)

        var saved = false
        editVm.saveChanges { saved = true }
        assertFalse(saved)
        assertNotNull(editVm.uiState.value.saveError)
        assertNull(profiles.getProfile("user_missing"))
    }

    @Test
    fun editProfileViewModel_ownerSwitchMidEditReloadsTargetAndNeverCrossContaminates() = runBlocking {
        val userA = AuthUser("user_a", "Alice", "alice@pulse.test", false, false, 1_700_000_000_000L)
        val userB = AuthUser("user_b", "Bob", "bob@pulse.test", false, false, 1_700_000_000_000L)
        val auth = FakeAuthRepo(AuthState.Authenticated(userA))
        val profiles = FakeProfileRepo()
        profiles.seed(sampleProfile("user_a", "Alice", weeklyWorkouts = 3, age = 26))
        profiles.seed(sampleProfile("user_b", "Bob", weeklyWorkouts = 6, age = 35))

        val editVm = EditProfileViewModel(auth, profiles)
        withTimeout(2_000) { editVm.uiState.first { !it.isLoading && it.ownerId == "user_a" } }
        editVm.onFullNameChanged("Alice Edited")

        // Account switches mid-edit to userB -> ViewModel must reload Bob's profile and never overwrite Bob with Alice's draft
        auth.state.value = AuthState.Authenticated(userB)
        withTimeout(2_000) { editVm.uiState.first { !it.isLoading && it.ownerId == "user_b" } }

        assertEquals("Bob", editVm.uiState.value.fullName)
        assertEquals("Alice", profiles.getProfile("user_a")!!.fullName)
        assertEquals("Bob", profiles.getProfile("user_b")!!.fullName)
    }

    private suspend fun awaitHomeLoaded(
        vm: HomeViewModel,
        predicate: (HomeUiState.Loaded) -> Boolean,
    ): HomeUiState.Loaded = withTimeout(2_000) {
        vm.uiState.first { state ->
            state is HomeUiState.Loaded && predicate(state)
        } as HomeUiState.Loaded
    }

    private fun sampleSummary(id: String, startedAt: Long, status: SessionStatus, volumeKg: Double) = SessionSummary(
        sessionId = id,
        planNameSnapshot = "Upper Body",
        startedAtEpochMs = startedAt,
        endedAtEpochMs = startedAt + 1_800_000L,
        status = status,
        completedSetsCount = 6,
        totalPlannedSetsCount = 6,
        totalFullReps = 48,
        totalPartialReps = 0,
        totalVolumeKg = volumeKg,
        durationMs = 1_800_000L,
    )

    private fun sampleProfile(ownerId: String, name: String, weeklyWorkouts: Int, age: Int = 28) = UserProfile(
        ownerId = ownerId,
        fullName = name,
        gender = GenderIdentity.MALE,
        age = age,
        heightCm = 180,
        weightKg = 78.0,
        measurementUnit = WeightUnit.KG,
        fitnessGoal = FitnessGoal.HYPERTROPHY,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        weeklyWorkouts = weeklyWorkouts,
        setupCompleted = true,
        updatedAtEpochMs = 1_000L,
    )

    private fun samplePlan(id: String, ownerId: String, name: String, updatedAt: Long) = WorkoutPlan(
        id = id,
        ownerProfileId = ownerId,
        name = name,
        createdAtEpochMs = updatedAt,
        updatedAtEpochMs = updatedAt,
        exercises = listOf(
            PlanExercise(
                id = "${id}_ex1",
                planId = id,
                exerciseId = "bench_press",
                orderIndex = 0,
                plannedSets = listOf(PlannedSet("${id}_s1", "${id}_ex1", 1, 10)),
            ),
        ),
    )

    private class FakeAuthRepo(initial: AuthState) : AuthRepository {
        val state = MutableStateFlow(initial)
        override fun observeAuthState(): Flow<AuthState> = state
        override suspend fun restoreSession(): AuthState = state.value
        override suspend fun continueAsGuest() = throw UnsupportedOperationException()
        override suspend fun registerLocalAccount(fullName: String, email: String, password: String) = throw UnsupportedOperationException()
        override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean) = throw UnsupportedOperationException()
        override suspend fun resetPassword(newPassword: String) = throw UnsupportedOperationException()
        override suspend fun signInDemo(displayName: String, email: String) = throw UnsupportedOperationException()
        override suspend fun signOut() { state.value = AuthState.Unauthenticated }
    }

    private class FakeProfileRepo : UserProfileRepository {
        private val map = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
        var failNextSave = false
        fun seed(profile: UserProfile) { map.value = map.value + (profile.ownerId to profile) }
        override fun observeProfile(ownerId: String): Flow<UserProfile?> = map.map { it[ownerId] }
        override suspend fun getProfile(ownerId: String): UserProfile? = map.value[ownerId]
        override suspend fun saveCompletedProfile(ownerId: String, personalInfo: PersonalInfoDraft, goals: FitnessGoalsDraft): DomainResult<UserProfile> {
            if (failNextSave) {
                failNextSave = false
                return DomainResult.Failure(DataError.StorageError("Disk write failed"))
            }
            val updated = UserProfile(
                ownerId = ownerId,
                fullName = personalInfo.fullName,
                gender = personalInfo.gender,
                age = personalInfo.age,
                heightCm = personalInfo.heightCm,
                weightKg = personalInfo.weightKg,
                measurementUnit = personalInfo.measurementUnit,
                fitnessGoal = goals.fitnessGoal,
                experienceLevel = goals.experienceLevel,
                weeklyWorkouts = goals.weeklyWorkouts,
                setupCompleted = true,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
            seed(updated)
            return DomainResult.Success(updated)
        }
        override suspend fun updateExistingProfile(ownerId: String, draft: EditProfileDraft): DomainResult<UserProfile> {
            if (failNextSave) {
                failNextSave = false
                return DomainResult.Failure(DataError.StorageError("Disk write failed"))
            }
            val existing = map.value[ownerId]
            if (existing == null || !existing.setupCompleted) {
                return DomainResult.Failure(DataError.NotFound("No completed profile exists"))
            }
            val updated = existing.copy(
                fullName = draft.fullName.trim(),
                gender = draft.gender,
                heightCm = draft.heightCm,
                weightKg = draft.weightKg,
                fitnessGoal = draft.fitnessGoal,
                experienceLevel = draft.experienceLevel,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
            seed(updated)
            return DomainResult.Success(updated)
        }
        override suspend fun isProfileSetupCompleted(ownerId: String): Boolean = map.value[ownerId]?.setupCompleted == true
    }

    private class FakePrefsRepo : UserPreferencesRepository {
        private val prefs = MutableStateFlow(UserPreferences())
        override fun observePreferences(): Flow<UserPreferences> = prefs
        override suspend fun updatePreferredWeightUnit(unit: WeightUnit) { prefs.value = prefs.value.copy(preferredWeightUnit = unit) }
        override suspend fun setOnboardingCompleted(completed: Boolean) { prefs.value = prefs.value.copy(onboardingCompleted = completed) }
        override suspend fun updateDefaultRestSeconds(seconds: Int) { prefs.value = prefs.value.copy(defaultRestSeconds = seconds) }
    }

    private class FakePlanRepo : WorkoutPlanRepository {
        private val plans = MutableStateFlow<Map<String, List<WorkoutPlan>>>(emptyMap())
        fun setPlans(ownerId: String, list: List<WorkoutPlan>) { plans.value = plans.value + (ownerId to list) }
        override fun observePlans(ownerId: String): Flow<List<WorkoutPlan>> = plans.map { it[ownerId].orEmpty() }
        override fun observePlan(ownerId: String, planId: String): Flow<WorkoutPlan?> = flowOf(null)
        override suspend fun getPlan(ownerId: String, planId: String): WorkoutPlan? = null
        override suspend fun createPlan(ownerId: String, draft: WorkoutPlanDraft) = throw UnsupportedOperationException()
        override suspend fun updatePlan(ownerId: String, planId: String, draft: WorkoutPlanDraft) = throw UnsupportedOperationException()
        override suspend fun deletePlan(ownerId: String, planId: String) = throw UnsupportedOperationException()
    }

    private class FakeSessionRepo : WorkoutSessionRepository {
        private val active = MutableStateFlow<Map<String, WorkoutSession?>>(emptyMap())
        fun setActive(ownerId: String, session: WorkoutSession?) { active.value = active.value + (ownerId to session) }
        override fun observeActiveSession(ownerId: String): Flow<WorkoutSession?> = active.map { it[ownerId] }
        override fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?> = flowOf(null)
        override suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession? = null
        override suspend fun startSessionFromPlan(ownerId: String, planId: String) = throw UnsupportedOperationException()
        override suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest): RecordSetResult = throw UnsupportedOperationException()
        override suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest) = throw UnsupportedOperationException()
        override suspend fun completeSession(ownerId: String, sessionId: String) = throw UnsupportedOperationException()
        override suspend fun endSessionEarly(ownerId: String, sessionId: String) = throw UnsupportedOperationException()
        override fun observeRestState(ownerId: String, sessionId: String): Flow<com.ayasheta.pulsesmartgym.core.model.session.SessionRestState?> = flowOf(null)
        override suspend fun startRestTimer(
            ownerId: String,
            sessionId: String,
            exerciseId: String,
            exerciseName: String,
            completedSetIndex: Int,
            durationSeconds: Int,
            nextExerciseId: String?,
            nextSetIndex: Int?,
        ): DomainResult<com.ayasheta.pulsesmartgym.core.model.session.SessionRestState> = throw UnsupportedOperationException()
        override suspend fun extendRestTimer(ownerId: String, sessionId: String, additionalSeconds: Int): DomainResult<com.ayasheta.pulsesmartgym.core.model.session.SessionRestState> = throw UnsupportedOperationException()
        override suspend fun clearRestTimer(ownerId: String, sessionId: String): DomainResult<Unit> = throw UnsupportedOperationException()
    }

    private class FakeHistoryRepo : WorkoutHistoryRepository {
        private val history = MutableStateFlow<Map<String, List<SessionSummary>>>(emptyMap())
        fun setHistory(ownerId: String, list: List<SessionSummary>) { history.value = history.value + (ownerId to list) }
        override fun observeHistory(ownerId: String, statusFilter: SessionStatus?): Flow<List<SessionSummary>> = history.map { it[ownerId].orEmpty() }
        override fun observeAggregateMetrics(ownerId: String): Flow<HistoryAggregateMetrics> = flowOf(HistoryAggregateMetrics(0, 0, 0, 0, 0, 0.0))
        override suspend fun getSessionDetails(ownerId: String, sessionId: String): WorkoutSession? = null
    }
}
