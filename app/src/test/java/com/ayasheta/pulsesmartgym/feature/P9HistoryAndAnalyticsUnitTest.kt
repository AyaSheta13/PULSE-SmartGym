package com.ayasheta.pulsesmartgym.feature

import androidx.lifecycle.SavedStateHandle
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.ExercisePersonalRecord
import com.ayasheta.pulsesmartgym.core.model.history.HistoryAggregateMetrics
import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.history.VolumeCoverage
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
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
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.history.HistoryUiMapper
import com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryUiState
import com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryViewModel
import com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsUiState
import com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsViewModel
import com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressUiState
import com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class P9HistoryAndAnalyticsUnitTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val zone = ZoneId.of("UTC")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun historyAggregation_excludesActiveAndAbandoned_separatesPartialReps_andGroupsByMonthAndYear() {
        val oct24 = epoch(2026, 10, 24, 10, 45)
        val sep15 = epoch(2026, 9, 15, 9, 0)
        val dec20PrevYear = epoch(2025, 12, 20, 18, 0)

        val completed = makeSession("s1", SessionStatus.COMPLETED, oct24, oct24 + 2_535_000L, fullReps = 10, partialReps = 2, loadKg = 80.0)
        val partial = makeSession("s2", SessionStatus.PARTIALLY_COMPLETED, sep15, sep15 + 255_000L, fullReps = 8, partialReps = 0, loadKg = 60.0)
        val prevYear = makeSession("s3", SessionStatus.COMPLETED, dec20PrevYear, dec20PrevYear + 1_200_000L, fullReps = 12, partialReps = 0, loadKg = 50.0)
        val active = makeSession("s4", SessionStatus.ACTIVE, oct24 + 1000L, null, fullReps = 20, partialReps = 0, loadKg = 100.0)
        val abandoned = makeSession("s5", SessionStatus.ABANDONED, oct24 + 2000L, oct24 + 5000L, fullReps = 0, partialReps = 0, loadKg = null)

        val summaries = listOf(completed, partial, prevYear, active, abandoned).map { WorkoutAnalytics.summarizeSession(it) }
        val aggregates = WorkoutAnalytics.aggregateSummaries(summaries)

        assertEquals(3, aggregates.totalSessions)
        assertEquals(2, aggregates.completedSessions)
        assertEquals(1, aggregates.partialSessions)
        assertEquals(30, aggregates.totalFullReps)
        assertEquals(2, aggregates.totalPartialReps)
        assertEquals(3, aggregates.totalCompletedSets)

        val ui = HistoryUiMapper.mapToUiState(
            summaries = summaries,
            aggregates = aggregates,
            today = LocalDate.of(2026, 10, 25),
            zoneId = zone,
        ) as WorkoutHistoryUiState.Populated

        assertEquals("3 saved sessions", ui.savedSessionsHeadline)
        assertTrue(ui.aggregateMetricsLine.contains("30 reps (+2 partial)"))
        assertEquals(3, ui.sections.size)
        assertEquals("RECENT", ui.sections[0].title)
        assertEquals("SEPTEMBER", ui.sections[1].title)
        assertEquals("DECEMBER 2025", ui.sections[2].title)
    }

    @Test
    fun weeklyAnalytics_computesMondaySundayWindow_previousComparison_partialPolicy_andUnknownVolume() {
        // Current week: Mon Oct 19 .. Sun Oct 25, 2026
        val mon = epoch(2026, 10, 19, 10, 0)
        val wed = epoch(2026, 10, 21, 18, 0)
        // Previous week: Mon Oct 12 .. Sun Oct 18, 2026
        val prevTue = epoch(2026, 10, 13, 12, 0)

        val sMon = makeSession("s_mon", SessionStatus.COMPLETED, mon, mon + 3_600_000L, fullReps = 10, partialReps = 0, loadKg = 100.0) // 1000 kg, 60 min
        val sWedPartial = makeSession("s_wed", SessionStatus.PARTIALLY_COMPLETED, wed, wed + 1_200_000L, fullReps = 5, partialReps = 2, loadKg = 80.0) // 400 kg, 20 min
        val sPrev = makeSession("s_prev", SessionStatus.COMPLETED, prevTue, prevTue + 1_800_000L, fullReps = 10, partialReps = 0, loadKg = 70.0) // 700 kg

        val metrics = WorkoutAnalytics.calculatePeriodMetrics(
            sessions = listOf(sMon, sWedPartial, sPrev),
            weeklyTarget = 4,
            referenceDate = LocalDate.of(2026, 10, 22),
            currentToday = LocalDate.of(2026, 10, 22),
            zoneId = zone,
        )

        assertEquals(LocalDate.of(2026, 10, 19), metrics.weekStart)
        assertEquals(LocalDate.of(2026, 10, 25), metrics.weekEnd)
        // Only COMPLETED counts toward completedWorkoutsCount
        assertEquals(1, metrics.completedWorkoutsCount)
        assertEquals(1, metrics.partialWorkoutsCount)
        assertEquals(0, metrics.completedWorkoutsDelta) // 1 - 1
        assertEquals(25, metrics.targetCompletionPercent) // 1 / 4 = 25%
        // Partial session IS included in actual volume & sets logged
        assertEquals(1400.0, metrics.totalVolumeKg, 0.01)
        assertEquals(700.0, metrics.previousWeekVolumeKg, 0.01)
        assertEquals(100, metrics.volumeChangePercent) // (1400 - 700) / 700 = +100%
        assertEquals(2, metrics.setsLoggedCount)
        assertEquals(1, metrics.setsLoggedDelta) // 2 - 1
        assertEquals(40, metrics.avgDurationMinutes) // (60 + 20) / 2
        assertEquals(40.0, metrics.paceMinutesPerSet!!, 0.01) // 80 min / 2 sets
        assertEquals(3, metrics.highlightDayIndex) // Thursday Oct 22 -> index 3

        // Unknown load coverage
        val unknownOnly = makeSession("s_unk", SessionStatus.COMPLETED, mon, mon + 600_000L, fullReps = 10, partialReps = 0, loadKg = null)
        val unknownMetrics = WorkoutAnalytics.calculatePeriodMetrics(
            sessions = listOf(unknownOnly),
            weeklyTarget = null,
            referenceDate = LocalDate.of(2026, 10, 22),
            currentToday = LocalDate.of(2026, 10, 22),
            zoneId = zone,
        )
        assertEquals(VolumeCoverage.NO_MEASURED_VOLUME, unknownMetrics.volumeCoverage)
        assertNull(unknownMetrics.volumeChangePercent)
        assertNull(unknownMetrics.targetCompletionPercent)
    }

    @Test
    fun personalRecords_enforcesFullReps_weightedOnly_tieBreaker_andImprovement() {
        val t1 = epoch(2026, 10, 10, 10, 0)
        val t2 = epoch(2026, 10, 15, 10, 0)
        val t3 = epoch(2026, 10, 20, 10, 0)

        val s1 = makeSession("s1", SessionStatus.COMPLETED, t1, t1 + 1000L, fullReps = 8, partialReps = 0, loadKg = 80.0, sourceExId = "ex_bench")
        val s2 = makeSession("s2", SessionStatus.PARTIALLY_COMPLETED, t2, t2 + 1000L, fullReps = 5, partialReps = 0, loadKg = 90.0, sourceExId = "ex_bench")
        // Partial-only reps (0 full reps) and active session must NOT count as PR
        val sPartialOnly = makeSession("s4", SessionStatus.COMPLETED, t3 + 5000L, t3 + 6000L, fullReps = 0, partialReps = 5, loadKg = 120.0, sourceExId = "ex_bench")
        val sActive = makeSession("s5", SessionStatus.ACTIVE, t3 + 7000L, null, fullReps = 10, partialReps = 0, loadKg = 150.0, sourceExId = "ex_bench")

        val prs = WorkoutAnalytics.calculatePersonalRecords(listOf(s1, s2, sPartialOnly, sActive))
        assertEquals(1, prs.size)
        val pr = prs.single()
        assertEquals(90.0, pr.maxLoad.toKilograms(), 0.01)
        assertEquals(5, pr.repsAtMaxLoad)
        assertEquals(80.0, pr.previousBestLoad!!.toKilograms(), 0.01)
        assertEquals(10.0, pr.improvementKg!!, 0.01)

        // Equal load tie picks higher reps (6 > 5) and does not fabricate a weight increase
        val s3 = makeSession("s3", SessionStatus.COMPLETED, t3, t3 + 1000L, fullReps = 6, partialReps = 0, loadKg = 90.0, sourceExId = "ex_bench")
        val tiedPr = WorkoutAnalytics.calculatePersonalRecords(listOf(s1, s2, s3)).single()
        assertEquals(6, tiedPr.repsAtMaxLoad)
        assertNull(tiedPr.improvementKg)
    }

    @Test
    fun viewModels_handleAccountSwitch_unavailableActiveSession_andWeekPersistence(): Unit = runBlocking {
        val authRepo = FakeP9AuthRepo("user_a")
        val historyRepo = FakeP9HistoryRepo()
        val profileRepo = FakeP9ProfileRepo()
        val prefsRepo = FakeP9PrefsRepo()

        val oct20 = epoch(2026, 10, 20, 10, 0)
        val compA = makeSession("sess_a", SessionStatus.COMPLETED, oct20, oct20 + 1_800_000L, fullReps = 10, partialReps = 0, loadKg = 80.0)
        val activeA = makeSession("sess_active", SessionStatus.ACTIVE, oct20 + 5000L, null, fullReps = 5, partialReps = 0, loadKg = 60.0)
        historyRepo.setSessions("user_a", listOf(compA, activeA))

        // HistoryViewModel shows user_a's completed session, then empties on user_b switch
        val historyVm = WorkoutHistoryViewModel(authRepo, historyRepo, todayProvider = { LocalDate.of(2026, 10, 22) }, zoneProvider = { zone })
        val pop = withTimeout(5_000) { historyVm.uiState.first { it is WorkoutHistoryUiState.Populated } as WorkoutHistoryUiState.Populated }
        assertEquals("1 saved session", pop.savedSessionsHeadline)

        authRepo.switchUser("user_b")
        withTimeout(5_000) { historyVm.uiState.first { it is WorkoutHistoryUiState.Empty } }

        // SessionDetailsViewModel rejects ACTIVE session as Unavailable
        authRepo.switchUser("user_a")
        val detailsActiveVm = SessionDetailsViewModel("sess_active", authRepo, historyRepo, prefsRepo)
        withTimeout(5_000) { detailsActiveVm.uiState.first { it is SessionDetailsUiState.Unavailable } }

        // TrainingProgressViewModel persists selectedWeekOffset in SavedStateHandle & updates KG/LBS
        val savedState = SavedStateHandle()
        val progressVm1 = TrainingProgressViewModel(
            authRepo, historyRepo, profileRepo, prefsRepo, savedState,
            todayProvider = { LocalDate.of(2026, 10, 22) }, zoneProvider = { zone },
        )
        withTimeout(5_000) { progressVm1.uiState.first { it is TrainingProgressUiState.Populated } }
        progressVm1.selectWeekOffset(1)
        prefsRepo.updatePreferredWeightUnit(WeightUnit.LBS)

        val progressVm2 = TrainingProgressViewModel(
            authRepo, historyRepo, profileRepo, prefsRepo, savedState,
            todayProvider = { LocalDate.of(2026, 10, 22) }, zoneProvider = { zone },
        )
        val restored = withTimeout(5_000) { progressVm2.uiState.first { it is TrainingProgressUiState.Populated } as TrainingProgressUiState.Populated }
        assertEquals(1, restored.selectedWeekOffset)
        assertTrue(restored.personalRecords.single().weightLabel.contains("lbs"))
    }

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun makeSession(
        id: String,
        status: SessionStatus,
        startedAtMs: Long,
        endedAtMs: Long?,
        fullReps: Int,
        partialReps: Int,
        loadKg: Double?,
        sourceExId: String = "ex_bench_press",
    ): WorkoutSession {
        val performed = if (status == SessionStatus.ABANDONED) {
            emptyList()
        } else {
            listOf(
                PerformedSet(
                    id = "perf_$id",
                    sessionId = id,
                    sessionExerciseId = "se_$id",
                    setIndex = 1,
                    fullReps = fullReps,
                    partialReps = partialReps,
                    actualLoad = loadKg?.let { WeightLoad.fromKilograms(it) },
                    completedAtEpochMs = endedAtMs ?: startedAtMs,
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_INPUT,
                    durationMs = 45_000L,
                ),
            )
        }
        return WorkoutSession(
            id = id,
            ownerProfileId = "user_a",
            sourcePlanId = "plan_1",
            planNameSnapshot = "Upper Body Power",
            startedAtEpochMs = startedAtMs,
            endedAtEpochMs = endedAtMs,
            status = status,
            exercises = listOf(
                SessionExercise(
                    id = "se_$id",
                    sessionId = id,
                    sourceExerciseId = sourceExId,
                    exerciseNameSnapshot = "Barbell Bench Press",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.BARBELL,
                    trackingCategorySnapshot = TrackingCategory.LINEAR,
                    orderIndex = 0,
                    plannedSets = listOf(SessionPlannedSet("sp_$id", "se_$id", 1, 10, WeightLoad.fromKilograms(80.0), 90)),
                    performedSets = performed,
                ),
            ),
        )
    }
}

private class FakeP9AuthRepo(initialId: String) : AuthRepository {
    private val state = MutableStateFlow<AuthState>(
        AuthState.Authenticated(AuthUser(initialId, "User $initialId", "$initialId@pulse.local", false, false)),
    )
    fun switchUser(id: String) {
        state.value = AuthState.Authenticated(AuthUser(id, "User $id", "$id@pulse.local", false, false))
    }
    override fun observeAuthState(): Flow<AuthState> = state
    override suspend fun restoreSession(): AuthState = state.value
    override suspend fun continueAsGuest() = throw UnsupportedOperationException()
    override suspend fun registerLocalAccount(fullName: String, email: String, password: String) = throw UnsupportedOperationException()
    override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean) = throw UnsupportedOperationException()
    override suspend fun resetPassword(newPassword: String) = throw UnsupportedOperationException()
    override suspend fun signInDemo(displayName: String, email: String) = throw UnsupportedOperationException()
    override suspend fun signOut() { state.value = AuthState.Unauthenticated }
}

private class FakeP9HistoryRepo : WorkoutHistoryRepository {
    private val sessionsByOwner = MutableStateFlow<Map<String, List<WorkoutSession>>>(emptyMap())
    fun setSessions(ownerId: String, list: List<WorkoutSession>) {
        sessionsByOwner.value = sessionsByOwner.value + (ownerId to list)
    }
    override fun observeHistory(ownerId: String, statusFilter: SessionStatus?): Flow<List<SessionSummary>> =
        observeFinishedSessions(ownerId).map { list ->
            list.filter { statusFilter == null || it.status == statusFilter }.map { WorkoutAnalytics.summarizeSession(it) }
        }
    override fun observeAggregateMetrics(ownerId: String): Flow<HistoryAggregateMetrics> =
        observeHistory(ownerId, null).map { WorkoutAnalytics.aggregateSummaries(it) }
    override fun observeFinishedSessions(ownerId: String): Flow<List<WorkoutSession>> =
        sessionsByOwner.map { map ->
            map[ownerId].orEmpty().filter { it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED }
        }
    override fun observeSessionDetails(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        observeFinishedSessions(ownerId).map { list -> list.firstOrNull { it.id == sessionId } }
    override fun observePersonalRecords(ownerId: String): Flow<List<ExercisePersonalRecord>> =
        observeFinishedSessions(ownerId).map { WorkoutAnalytics.calculatePersonalRecords(it) }
    override suspend fun getSessionDetails(ownerId: String, sessionId: String): WorkoutSession? =
        observeSessionDetails(ownerId, sessionId).first()
}

private class FakeP9ProfileRepo : UserProfileRepository {
    private val profile = MutableStateFlow<UserProfile?>(null)
    override fun observeProfile(ownerId: String): Flow<UserProfile?> = profile
    override suspend fun getProfile(ownerId: String): UserProfile? = profile.value
    override suspend fun saveCompletedProfile(ownerId: String, personalInfo: PersonalInfoDraft, goals: FitnessGoalsDraft) = throw UnsupportedOperationException()
    override suspend fun updateExistingProfile(ownerId: String, draft: com.ayasheta.pulsesmartgym.core.model.profile.EditProfileDraft) = throw UnsupportedOperationException()
    override suspend fun isProfileSetupCompleted(ownerId: String): Boolean = false
}

private class FakeP9PrefsRepo : UserPreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences())
    override fun observePreferences(): Flow<UserPreferences> = prefs
    override suspend fun updatePreferredWeightUnit(unit: WeightUnit) { prefs.value = prefs.value.copy(preferredWeightUnit = unit) }
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun updateDefaultRestSeconds(seconds: Int) {}
}
