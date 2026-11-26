package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseBottomNavigationBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.history.HistoryUiMapper
import com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryScreenContent
import com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryUiState
import com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsScreenContent
import com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsViewModel
import com.ayasheta.pulsesmartgym.feature.progress.ProgressUiMapper
import com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressScreenContent
import com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressUiState
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultScreenContent
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultViewModel
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class P9ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAllNineP9ReferenceScreensAt390x884() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 10, 24)
        val completeSession = sampleCompleteSession(zone)
        val partialSession = samplePartialSession(zone)
        val secondSession = sampleSecondSession(zone)
        val thirdSession = sampleThirdSession(zone)

        val populatedHistory = HistoryUiMapper.mapToUiState(
            summaries = listOf(completeSession, secondSession, thirdSession).map { WorkoutAnalytics.summarizeSession(it) },
            aggregates = WorkoutAnalytics.aggregateSummaries(
                listOf(completeSession, secondSession, thirdSession).map { WorkoutAnalytics.summarizeSession(it) },
            ),
            today = today,
            zoneId = zone,
        )
        val partialHistory = HistoryUiMapper.mapToUiState(
            summaries = listOf(partialSession, secondSession, thirdSession).map { WorkoutAnalytics.summarizeSession(it) },
            aggregates = WorkoutAnalytics.aggregateSummaries(
                listOf(partialSession, secondSession, thirdSession).map { WorkoutAnalytics.summarizeSession(it) },
            ),
            today = today,
            zoneId = zone,
        )
        val detailsComplete = SessionDetailsViewModel.mapToLoaded(completeSession, WeightUnit.KG, today, zone)
        val detailsPartial = SessionDetailsViewModel.mapToLoaded(partialSession, WeightUnit.KG, today, zone)
        val populatedProgress = ProgressUiMapper.mapToUiState(
            sessions = listOf(completeSession, secondSession, thirdSession),
            weeklyTarget = 4,
            unit = WeightUnit.KG,
            selectedWeekOffset = 0,
            today = today,
            zoneId = zone,
        )
        val completeSummary = WorkoutResultViewModel.mapSessionToResult(completeSession, WeightUnit.KG)
        val partialSummary = WorkoutResultViewModel.mapSessionToResult(partialSession, WeightUnit.KG)

        var mode by mutableStateOf(0)

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = baseDensity.fontScale),
                LocalRenderModalsInWindow provides false,
            ) {
                PulseTheme {
                    Box(
                        modifier = Modifier
                            .size(width = 390.dp, height = 884.dp)
                            .consumeWindowInsets(WindowInsets.systemBars),
                    ) {
                        when (mode) {
                            0, 1, 2 -> Column {
                                val state = when (mode) {
                                    0 -> populatedHistory
                                    1 -> WorkoutHistoryUiState.Empty
                                    else -> partialHistory
                                }
                                WorkoutHistoryScreenContent(
                                    uiState = state,
                                    onSessionClick = {},
                                    onGoToWorkoutsClick = {},
                                    onRetryClick = {},
                                    modifier = Modifier.weight(1f),
                                )
                                PulseBottomNavigationBar(selectedTab = PulseTab.HISTORY, onTabSelected = {})
                            }
                            3 -> SessionDetailsScreenContent(
                                uiState = detailsComplete,
                                onBackClick = {},
                                onRepeatWorkoutClick = {},
                                onRetryClick = {},
                            )
                            4 -> SessionDetailsScreenContent(
                                uiState = detailsPartial,
                                onBackClick = {},
                                onRepeatWorkoutClick = {},
                                onRetryClick = {},
                            )
                            5, 6 -> Column {
                                val state = if (mode == 5) populatedProgress else TrainingProgressUiState.Empty
                                TrainingProgressScreenContent(
                                    uiState = state,
                                    onStartWorkoutClick = {},
                                    onSelectWeekOffset = {},
                                    onRetryClick = {},
                                    modifier = Modifier.weight(1f),
                                )
                                PulseBottomNavigationBar(selectedTab = PulseTab.PROGRESS, onTabSelected = {})
                            }
                            7 -> WorkoutResultScreenContent(
                                uiState = completeSummary,
                                onBackToHome = {},
                                onViewHistory = {},
                            )
                            else -> WorkoutResultScreenContent(
                                uiState = partialSummary,
                                onBackToHome = {},
                                onViewHistory = {},
                            )
                        }
                    }
                }
            }
        }

        val outDir = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "p9_screenshots").apply {
            mkdirs()
        }
        val names = listOf(
            "01_history_populated.png",
            "02_history_empty.png",
            "03_history_partial.png",
            "04_session_details_complete.png",
            "05_session_details_partial.png",
            "06_progress_populated.png",
            "07_progress_empty.png",
            "08_workout_complete_summary.png",
            "09_partial_workout_summary.png",
        )
        names.forEachIndexed { idx, fileName ->
            mode = idx
            composeRule.waitForIdle()
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { out ->
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            assertTrue("Screenshot $fileName must exist and be non-empty", outFile.exists() && outFile.length() > 0L)
        }
    }

    private fun epoch(zone: ZoneId, day: Int, hour: Int, min: Int): Long =
        ZonedDateTime.of(2026, 10, day, hour, min, 0, 0, zone).toInstant().toEpochMilli()

    private fun sampleCompleteSession(zone: ZoneId): WorkoutSession {
        val start = epoch(zone, 24, 10, 45)
        return WorkoutSession(
            id = "s_comp",
            ownerProfileId = "u1",
            sourcePlanId = "p1",
            planNameSnapshot = "Upper Body Power",
            startedAtEpochMs = start,
            endedAtEpochMs = start + 2_535_000L,
            status = SessionStatus.COMPLETED,
            exercises = listOf(
                makeEx("e1", "s_comp", "bench_press", "Barbell Bench Press", 0, 4, 10, 80.0, completedAtEpochMs = start),
                makeEx("e2", "s_comp", "incline_db_press", "Incline Dumbbell Press", 1, 4, 9, 28.0, completedAtEpochMs = start),
                makeEx("e3", "s_comp", "barbell_row", "Bent-Over Barbell Row", 2, 4, 8, 70.0, completedAtEpochMs = start),
                makeEx("e4", "s_comp", "overhead_tricep", "Overhead Triceps Extension", 3, 4, 8, 32.0, completedAtEpochMs = start),
            ),
        )
    }

    private fun samplePartialSession(zone: ZoneId): WorkoutSession {
        val start = epoch(zone, 24, 14, 0)
        return WorkoutSession(
            id = "s_part",
            ownerProfileId = "u1",
            sourcePlanId = "p1",
            planNameSnapshot = "Upper Body Power",
            startedAtEpochMs = start,
            endedAtEpochMs = start + 255_000L,
            status = SessionStatus.PARTIALLY_COMPLETED,
            exercises = listOf(
                makeEx("e1", "s_part", "bench_press", "Barbell Bench Press", 0, 4, 10, 80.0, recordedSets = 1, completedAtEpochMs = start),
                makeEx("e2", "s_part", "incline_db_press", "Incline Dumbbell Press", 1, 4, 9, 28.0, recordedSets = 0, completedAtEpochMs = start),
                makeEx("e3", "s_part", "barbell_row", "Bent-Over Barbell Row", 2, 4, 8, 70.0, recordedSets = 0, completedAtEpochMs = start),
                makeEx("e4", "s_part", "overhead_tricep", "Overhead Triceps Extension", 3, 4, 8, 32.0, recordedSets = 0, completedAtEpochMs = start),
            ),
        )
    }

    private fun sampleSecondSession(zone: ZoneId): WorkoutSession {
        val start = epoch(zone, 22, 8, 30)
        return WorkoutSession(
            id = "s_2",
            ownerProfileId = "u1",
            sourcePlanId = "p2",
            planNameSnapshot = "Lower Body Strength",
            startedAtEpochMs = start,
            endedAtEpochMs = start + 2_900_000L,
            status = SessionStatus.COMPLETED,
            exercises = listOf(makeEx("e5", "s_2", "back_squat", "Barbell Back Squat", 0, 4, 10, 100.0, completedAtEpochMs = start)),
        )
    }

    private fun sampleThirdSession(zone: ZoneId): WorkoutSession {
        val start = epoch(zone, 19, 18, 10)
        return WorkoutSession(
            id = "s_3",
            ownerProfileId = "u1",
            sourcePlanId = "p3",
            planNameSnapshot = "Upper Hypertrophy",
            startedAtEpochMs = start,
            endedAtEpochMs = start + 2_710_000L,
            status = SessionStatus.COMPLETED,
            exercises = listOf(makeEx("e6", "s_3", "barbell_row", "Bent-Over Barbell Row", 0, 4, 10, 85.0, completedAtEpochMs = start)),
        )
    }

    private fun makeEx(
        id: String,
        sessId: String,
        sourceId: String,
        name: String,
        order: Int,
        plannedCount: Int,
        reps: Int,
        kg: Double,
        recordedSets: Int = plannedCount,
        completedAtEpochMs: Long = 1_000L,
    ): SessionExercise = SessionExercise(
        id = id,
        sessionId = sessId,
        sourceExerciseId = sourceId,
        exerciseNameSnapshot = name,
        muscleGroupSnapshot = MuscleGroup.CHEST,
        equipmentSnapshot = EquipmentCategory.BARBELL,
        trackingCategorySnapshot = TrackingCategory.LINEAR,
        orderIndex = order,
        plannedSets = (1..plannedCount).map { idx -> SessionPlannedSet("sp_${id}_$idx", id, idx, reps, WeightLoad.fromKilograms(kg), 90) },
        performedSets = (1..recordedSets).map { idx ->
            PerformedSet("pf_${id}_$idx", sessId, id, idx, reps, 0, WeightLoad.fromKilograms(kg), completedAtEpochMs, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT, 45_000L)
        },
    )
}
