package com.ayasheta.pulsesmartgym.feature

import androidx.lifecycle.SavedStateHandle
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
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
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionRestState
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.history.HistoryUiMapper
import com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryUiState
import com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryViewModel
import com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsUiState
import com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsViewModel
import com.ayasheta.pulsesmartgym.feature.progress.ProgressUiMapper
import com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressUiState
import com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressViewModel
import com.ayasheta.pulsesmartgym.feature.session.WorkoutSessionLaunchCoordinator
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(90.0, tiedPr.previousBestLoad!!.toKilograms(), 0.01)
        assertEquals(0.0, tiedPr.improvementKg!!, 0.01)
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

    private fun session(
        id: String,
        status: SessionStatus,
        startedAtMs: Long,
        endedAtMs: Long?,
        setCount: Int = 1,
        fullReps: Int = 10,
        loadKg: Double? = 80.0,
        sourceExId: String = "ex_bench_press",
    ): WorkoutSession = makeSession(
        id = id,
        status = status,
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        fullReps = fullReps,
        partialReps = 0,
        loadKg = loadKg,
        sourceExId = sourceExId,
        setCount = setCount,
    )

    private fun makeSession(
        id: String,
        status: SessionStatus,
        startedAtMs: Long,
        endedAtMs: Long?,
        fullReps: Int,
        partialReps: Int,
        loadKg: Double?,
        sourceExId: String = "ex_bench_press",
        setCount: Int = 1,
    ): WorkoutSession {
        val performed = if (status == SessionStatus.ABANDONED) {
            emptyList()
        } else {
            (1..setCount).map { idx ->
                PerformedSet(
                    id = "perf_${id}_$idx",
                    sessionId = id,
                    sessionExerciseId = "se_$id",
                    setIndex = idx,
                    fullReps = fullReps,
                    partialReps = partialReps,
                    actualLoad = loadKg?.let { WeightLoad.fromKilograms(it) },
                    completedAtEpochMs = endedAtMs ?: startedAtMs,
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_INPUT,
                    durationMs = 45_000L,
                )
            }
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

    @Test
    fun personalRecords_distinguishFirst_improved_andMatched_withDeterministicTieBreak() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 10, 24)
        val t1 = ZonedDateTime.of(2026, 10, 10, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val t2 = ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val t3 = ZonedDateTime.of(2026, 10, 20, 10, 0, 0, 0, zone).toInstant().toEpochMilli()

        val firstOnly = session("s_first", SessionStatus.COMPLETED, t1, t1 + 1000L, setCount = 1, fullReps = 5, loadKg = 80.0, sourceExId = "ex_first")
        val improved1 = session("s_imp1", SessionStatus.COMPLETED, t1, t1 + 1000L, setCount = 1, fullReps = 5, loadKg = 80.0, sourceExId = "ex_imp")
        val improved2 = session("s_imp2", SessionStatus.COMPLETED, t2, t2 + 1000L, setCount = 1, fullReps = 5, loadKg = 92.5, sourceExId = "ex_imp")
        val matched1 = session("s_mat1", SessionStatus.COMPLETED, t1, t1 + 1000L, setCount = 1, fullReps = 6, loadKg = 90.0, sourceExId = "ex_mat")
        val matched2 = session("s_mat2", SessionStatus.COMPLETED, t3, t3 + 1000L, setCount = 1, fullReps = 6, loadKg = 90.0, sourceExId = "ex_mat")

        val state = ProgressUiMapper.mapToUiState(
            sessions = listOf(firstOnly, improved1, improved2, matched1, matched2),
            weeklyTarget = 4,
            unit = WeightUnit.KG,
            selectedWeekOffset = 0,
            today = today,
            zoneId = zone,
        ) as TrainingProgressUiState.Populated

        val firstRow = state.personalRecords.first { it.exerciseKey.endsWith("ex_first") }
        val impRow = state.personalRecords.first { it.exerciseKey.endsWith("ex_imp") }
        val matRow = state.personalRecords.first { it.exerciseKey.endsWith("ex_mat") }
        assertTrue(firstRow.detailLabel.endsWith("First PR"))
        assertTrue(impRow.detailLabel.endsWith("+12.5 kg PR"))
        assertTrue(matRow.detailLabel.endsWith("Matched PR"))
        assertEquals("Oct 20", matRow.dateLabel)
    }

    @Test
    fun unknownAndPartialVolume_suppressMisleadingPercentages_acrossProgressSummaryAndDetails() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 10, 24)
        val prevStart = ZonedDateTime.of(2026, 10, 14, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val currStart = ZonedDateTime.of(2026, 10, 21, 10, 0, 0, 0, zone).toInstant().toEpochMilli()

        // 1. Unknown current-week volume with known previous-week volume -> NOT -100%
        val prevKnown = session("s_pk", SessionStatus.COMPLETED, prevStart, prevStart + 600_000L, setCount = 2, fullReps = 10, loadKg = 50.0)
        val currUnknown = session("s_cu", SessionStatus.COMPLETED, currStart, currStart + 600_000L, setCount = 2, fullReps = 10, loadKg = null)

        val unknownCurrState = ProgressUiMapper.mapToUiState(
            sessions = listOf(prevKnown, currUnknown),
            weeklyTarget = null,
            unit = WeightUnit.KG,
            selectedWeekOffset = 0,
            today = today,
            zoneId = zone,
        ) as TrainingProgressUiState.Populated
        assertEquals("—", unknownCurrState.volumeCard.primaryValue)
        assertEquals("unmeasured", unknownCurrState.volumeCard.primaryUnit)
        assertEquals("Unmeasured loads", unknownCurrState.volumeCard.deltaText)
        assertEquals("Unmeasured (This Week)", unknownCurrState.chartHeaderVolumeText)

        // Summary & Details also show "— volume" rather than "0 kg volume"
        val resUnknown = WorkoutResultViewModel.mapSessionToResult(currUnknown, WeightUnit.KG)
        val detUnknown = SessionDetailsViewModel.mapToLoaded(currUnknown, WeightUnit.KG, today, zone)
        assertEquals("— volume", resUnknown.volumeChipText)
        assertEquals("— volume", detUnknown.volumeChipText)

        // 2. Known current-week volume with unknown previous-week volume
        val prevUnknown = session("s_pu", SessionStatus.COMPLETED, prevStart, prevStart + 600_000L, setCount = 2, fullReps = 10, loadKg = null)
        val currKnown = session("s_ck", SessionStatus.COMPLETED, currStart, currStart + 600_000L, setCount = 2, fullReps = 10, loadKg = 50.0)
        val unknownPrevState = ProgressUiMapper.mapToUiState(
            sessions = listOf(prevUnknown, currKnown),
            weeklyTarget = null,
            unit = WeightUnit.KG,
            selectedWeekOffset = 0,
            today = today,
            zoneId = zone,
        ) as TrainingProgressUiState.Populated
        assertEquals("Prior wk unmeasured", unknownPrevState.volumeCard.deltaText)

        // 3. Partial measurement coverage (mixed known and null loads)
        val currMixed = WorkoutSession(
            id = "s_mix",
            ownerProfileId = "user_a",
            sourcePlanId = "p1",
            planNameSnapshot = "Mixed",
            startedAtEpochMs = currStart,
            endedAtEpochMs = currStart + 600_000L,
            status = SessionStatus.COMPLETED,
            exercises = listOf(
                SessionExercise(
                    id = "se_mix",
                    sessionId = "s_mix",
                    sourceExerciseId = "bench",
                    exerciseNameSnapshot = "Bench",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.BARBELL,
                    trackingCategorySnapshot = TrackingCategory.LINEAR,
                    orderIndex = 0,
                    plannedSets = emptyList(),
                    performedSets = listOf(
                        PerformedSet("p1", "s_mix", "se_mix", 1, 10, 0, WeightLoad.fromKilograms(80.0), currStart, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT),
                        PerformedSet("p2", "s_mix", "se_mix", 2, 10, 0, null, currStart, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT),
                    ),
                ),
            ),
        )
        val partialState = ProgressUiMapper.mapToUiState(
            sessions = listOf(prevKnown, currMixed),
            weeklyTarget = null,
            unit = WeightUnit.KG,
            selectedWeekOffset = 0,
            today = today,
            zoneId = zone,
        ) as TrainingProgressUiState.Populated
        assertEquals("Partial coverage", partialState.volumeCard.deltaText)
        assertTrue(partialState.chartHeaderVolumeText.contains("(partial)"))
        assertTrue(WorkoutResultViewModel.mapSessionToResult(currMixed, WeightUnit.KG).volumeChipText.contains("(partial)"))
        assertTrue(SessionDetailsViewModel.mapToLoaded(currMixed, WeightUnit.KG, today, zone).volumeChipText.contains("(partial)"))

        // 4. Zero measured weight -> KNOWN_ZERO_VOLUME
        val currZero = session("s_zero", SessionStatus.COMPLETED, currStart, currStart + 600_000L, setCount = 1, fullReps = 10, loadKg = 0.0)
        assertEquals(VolumeCoverage.KNOWN_ZERO_VOLUME, WorkoutAnalytics.calculateVolumeCoverage(currZero.exercises.first().performedSets))
        val zeroState = ProgressUiMapper.mapToUiState(
            sessions = listOf(currZero),
            weeklyTarget = null,
            unit = WeightUnit.KG,
            selectedWeekOffset = 0,
            today = today,
            zoneId = zone,
        ) as TrainingProgressUiState.Populated
        assertEquals("0", zeroState.volumeCard.primaryValue)
        assertEquals("kg", zeroState.volumeCard.primaryUnit)
    }

    @Test
    fun largeVolume_dstTransition_invalidDuration_andHistoricalWeekLabel_areAccurate() {
        // Large volume > Int.MAX_VALUE without overflow
        val hugeKg = 2_500_000_000.0
        assertEquals("2,500,000,000 kg", SessionUiMapper.formatVolumeWithUnit(hugeKg, WeightUnit.KG))
        assertEquals("5,511,556,555 lbs", SessionUiMapper.formatVolumeWithUnit(hugeKg, WeightUnit.LBS))

        // Invalid / missing duration (endedAt < startedAt or null)
        val zone = ZoneId.of("America/New_York")
        // Spring forward DST week: 2026-03-08 is DST transition in New York
        val monStart = ZonedDateTime.of(2026, 3, 2, 0, 30, 0, 0, zone).toInstant().toEpochMilli()
        val sunLate = ZonedDateTime.of(2026, 3, 8, 23, 30, 0, 0, zone).toInstant().toEpochMilli()
        val sMon = session("s_dst1", SessionStatus.COMPLETED, monStart, monStart - 5_000L, setCount = 1, fullReps = 10, loadKg = 100.0)
        val sSun = session("s_dst2", SessionStatus.COMPLETED, sunLate, null, setCount = 1, fullReps = 10, loadKg = 100.0)

        assertEquals(0L, WorkoutAnalytics.summarizeSession(sMon).durationMs)
        assertNull(WorkoutAnalytics.summarizeSession(sSun).durationMs)
        assertEquals("—", SessionDetailsViewModel.mapToLoaded(sSun, WeightUnit.KG, LocalDate.of(2026, 3, 9), zone).durationText)

        // Selected historical week uses truthful date range, NOT "This Week"
        val today = LocalDate.of(2026, 3, 10)
        val histWeekState = ProgressUiMapper.mapToUiState(
            sessions = listOf(sMon, sSun),
            weeklyTarget = 4,
            unit = WeightUnit.LBS,
            selectedWeekOffset = 1,
            today = today,
            zoneId = zone,
        ) as TrainingProgressUiState.Populated
        assertFalse(histWeekState.chartHeaderVolumeText.contains("This Week"))
        assertTrue(histWeekState.chartHeaderVolumeText.contains("Mar 2–8"))
        assertTrue(histWeekState.chartHeaderVolumeText.contains("lbs"))
        assertEquals("4,409", histWeekState.volumeCard.primaryValue)
    }

    @Test
    fun workoutHistoryViewModel_derivesConsistentAggregates_andClearsPreviousOwnerImmediately() = runBlocking {
        val auth = FakeP9AuthRepo("user_a")
        val delayUserB = CompletableDeferred<Unit>()
        val historyRepo = DelayedFakeP9HistoryRepo(delayedOwnerId = "user_b", gate = delayUserB)
        val prefsRepo = FakeP9PrefsRepo()
        val profileRepo = FakeP9ProfileRepo()

        val t1 = 1_760_000_000_000L
        val sA = session("sa_1", SessionStatus.COMPLETED, t1, t1 + 600_000L, setCount = 2, fullReps = 10, loadKg = 60.0)
        historyRepo.setSessions("user_a", listOf(sA))
        val sB = session("sb_1", SessionStatus.COMPLETED, t1, t1 + 300_000L, setCount = 1, fullReps = 5, loadKg = 40.0)
        historyRepo.setSessions("user_b", listOf(sB))

        val historyVm = WorkoutHistoryViewModel(auth, historyRepo, { LocalDate.of(2026, 10, 24) }, { ZoneId.of("UTC") })
        val progressVm = TrainingProgressViewModel(auth, historyRepo, profileRepo, prefsRepo, SavedStateHandle(), { LocalDate.of(2026, 10, 24) }, { ZoneId.of("UTC") })
        val detailsVm = SessionDetailsViewModel("sa_1", auth, historyRepo, prefsRepo, { LocalDate.of(2026, 10, 24) }, { ZoneId.of("UTC") })
        yield()

        val histA = historyVm.uiState.value as WorkoutHistoryUiState.Populated
        assertEquals("1 saved session", histA.savedSessionsHeadline)
        assertTrue(progressVm.uiState.value is TrainingProgressUiState.Populated)
        assertTrue(detailsVm.uiState.value is SessionDetailsUiState.Loaded)

        // Reactive update: adding a second session updates BOTH rows and aggregates from the same snapshot
        val sA2 = session("sa_2", SessionStatus.PARTIALLY_COMPLETED, t1 + 1000L, t1 + 101_000L, setCount = 1, fullReps = 4, loadKg = 50.0)
        historyRepo.setSessions("user_a", listOf(sA, sA2))
        yield()
        val histA2 = historyVm.uiState.value as WorkoutHistoryUiState.Populated
        assertEquals("2 saved sessions", histA2.savedSessionsHeadline)
        assertEquals(2, histA2.sections.sumOf { it.items.size })

        // Switch to user_b whose emissions are delayed -> previous user_a data MUST clear immediately to Loading!
        auth.switchUser("user_b")
        yield()
        assertTrue(historyVm.uiState.value is WorkoutHistoryUiState.Loading)
        assertTrue(progressVm.uiState.value is TrainingProgressUiState.Loading)
        assertTrue(detailsVm.uiState.value is SessionDetailsUiState.Loading)

        // Complete delay -> user_b gets only user_b data, details for sa_1 is Unavailable
        delayUserB.complete(Unit)
        yield()
        val histB = historyVm.uiState.value as WorkoutHistoryUiState.Populated
        assertEquals(1, histB.sections.sumOf { it.items.size })
        assertEquals("sb_1", histB.sections.first().items.first().sessionId)
        assertTrue(detailsVm.uiState.value is SessionDetailsUiState.Unavailable)
    }

    @Test
    fun launchCoordinator_resetsLoadingOnFailure_andDoesNotNavigateIfOwnerChanges() = runBlocking {
        val auth = FakeP9AuthRepo("user_a")
        val gate = CompletableDeferred<Unit>()
        var navigated = false
        val fakeSessionRepo = object : WorkoutSessionRepository {
            override fun observeActiveSession(ownerId: String) = throw UnsupportedOperationException()
            override fun observeSession(ownerId: String, sessionId: String) = throw UnsupportedOperationException()
            override suspend fun getSession(ownerId: String, sessionId: String) = throw UnsupportedOperationException()
            override fun observeRestState(ownerId: String, sessionId: String): Flow<SessionRestState?> = throw UnsupportedOperationException()
            override suspend fun startSessionFromPlan(ownerId: String, planId: String) =
                DomainResult.Failure(DataError.StorageError("boom"))
            override suspend fun startSessionFromHistory(ownerId: String, historicalSessionId: String): DomainResult<WorkoutSession> {
                gate.await()
                return DomainResult.Success(session("new_s", SessionStatus.ACTIVE, 1000L, null))
            }
            override suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest) = throw UnsupportedOperationException()
            override suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest) = throw UnsupportedOperationException()
            override suspend fun startRestTimer(ownerId: String, sessionId: String, exerciseId: String, exerciseName: String, completedSetIndex: Int, durationSeconds: Int, nextExerciseId: String?, nextSetIndex: Int?) = throw UnsupportedOperationException()
            override suspend fun extendRestTimer(ownerId: String, sessionId: String, additionalSeconds: Int) = throw UnsupportedOperationException()
            override suspend fun clearRestTimer(ownerId: String, sessionId: String) = throw UnsupportedOperationException()
            override suspend fun completeSession(ownerId: String, sessionId: String) = throw UnsupportedOperationException()
            override suspend fun endSessionEarly(ownerId: String, sessionId: String) = throw UnsupportedOperationException()
        }
        val coordinator = WorkoutSessionLaunchCoordinator(this, auth, fakeSessionRepo) { _, _ -> navigated = true }

        // 1. Failure resets isStarting and sets errorMessage
        coordinator.startOrResumePlan("p1")
        yield()
        assertFalse(coordinator.isStarting)
        assertEquals("boom", coordinator.errorMessage)

        // 2. Owner changes mid-flight -> does not navigate and resets isStarting
        coordinator.repeatHistoricalSession("hist_1")
        yield()
        assertTrue(coordinator.isStarting)
        auth.switchUser("user_b")
        gate.complete(Unit)
        yield()
        assertFalse(coordinator.isStarting)
        assertFalse(navigated)
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

private class DelayedFakeP9HistoryRepo(
    private val delayedOwnerId: String? = null,
    private val gate: CompletableDeferred<Unit>? = null,
) : FakeP9HistoryRepo() {
    override fun observeHistory(ownerId: String, statusFilter: SessionStatus?): Flow<List<SessionSummary>> =
        flow {
            if (ownerId == delayedOwnerId && gate != null) gate.await()
            emitAll(super.observeHistory(ownerId, statusFilter))
        }
    override fun observeFinishedSessions(ownerId: String): Flow<List<WorkoutSession>> =
        flow {
            if (ownerId == delayedOwnerId && gate != null) gate.await()
            emitAll(super.observeFinishedSessions(ownerId))
        }
    override fun observeSessionDetails(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        flow {
            if (ownerId == delayedOwnerId && gate != null) gate.await()
            emitAll(super.observeSessionDetails(ownerId, sessionId))
        }
}

private open class FakeP9HistoryRepo : WorkoutHistoryRepository {
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
