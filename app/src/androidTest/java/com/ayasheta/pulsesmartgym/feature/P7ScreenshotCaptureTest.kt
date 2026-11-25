package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
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
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.feature.session.dashboard.ManualSetEntryTarget
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionScreenContent
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultScreenContent
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultViewModel
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class P7ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAllP7ReferenceStates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PulseDataContainer.createIsolatedForTesting(context).use { isolated ->
            runBlocking {
                val c = isolated.container
                c.userPreferencesRepository.setOnboardingCompleted(true)
                val guest = (c.authRepository.continueAsGuest() as DomainResult.Success).data
                c.userProfileRepository.saveCompletedProfile(
                    guest.id,
                    PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
                    FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
                )
                c.exerciseRepository.observeAll().first()
                c.workoutPlanRepository.createPlan(
                    guest.id,
                    WorkoutPlanDraft("Upper Body Power", exercises = listOf(PlanExerciseDraft("ex_bench_press", 0, plannedSets = (1..4).map { PlannedSetDraft(it, 10, WeightLoad.fromKilograms(80.0), 90) }))),
                )
            }

            val baseSession = buildSampleSession()
            val ex1 = baseSession.exercises[0]
            val ex2 = baseSession.exercises[1]
            val ex3 = baseSession.exercises[2]
            val ex4 = baseSession.exercises[3]

            val s0 = baseSession
            val s1 = baseSession.copy(
                exercises = listOf(
                    ex1.copy(performedSets = listOf(perf("p1", ex1.id, 1, 10, 80.0))),
                    ex2, ex3, ex4,
                ),
            )
            val sNonSeq = baseSession.copy(
                exercises = listOf(
                    ex1.copy(performedSets = listOf(perf("p1", ex1.id, 1, 10, 80.0))),
                    ex2.copy(performedSets = listOf(perf("p2", ex2.id, 2, 10, 28.0))),
                    ex3.copy(performedSets = listOf(perf("p3", ex3.id, 3, 10, 70.0))),
                    ex4,
                ),
            )
            val sEx1Done = baseSession.copy(
                exercises = listOf(
                    ex1.copy(performedSets = (1..4).map { perf("p1_$it", ex1.id, it, 10, 80.0) }),
                    ex2, ex3, ex4,
                ),
            )
            val sAllDone = baseSession.copy(
                exercises = listOf(
                    ex1.copy(performedSets = (1..4).map { perf("pa1_$it", ex1.id, it, 10, 80.0) }),
                    ex2.copy(performedSets = (1..4).map { perf("pa2_$it", ex2.id, it, 10, 28.0) }),
                    ex3.copy(performedSets = (1..4).map { perf("pa3_$it", ex3.id, it, 10, 70.0) }),
                    ex4.copy(performedSets = (1..4).map { perf("pa4_$it", ex4.id, it, 10, 32.0) }),
                ),
            )

            val completeResult = WorkoutResultViewModel.mapSessionToResult(
                sAllDone.copy(status = SessionStatus.COMPLETED, startedAtEpochMs = 0L, endedAtEpochMs = 2_535_000L),
                WeightUnit.KG,
            )
            val partialResult = WorkoutResultViewModel.mapSessionToResult(
                s1.copy(status = SessionStatus.PARTIALLY_COMPLETED, startedAtEpochMs = 0L, endedAtEpochMs = 255_000L),
                WeightUnit.KG,
            )
            val abandonedResult = WorkoutResultViewModel.mapSessionToResult(
                s0.copy(status = SessionStatus.ABANDONED, startedAtEpochMs = 0L, endedAtEpochMs = 45_000L),
                WeightUnit.KG,
            )

            var currentScreen by mutableStateOf<@Composable () -> Unit>({})
            composeRule.setContent {
                PulseTheme {
                    CompositionLocalProvider(
                        LocalDensity provides Density(density = 1f, fontScale = 1f),
                        LocalRenderModalsInWindow provides true,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 390.dp, height = 884.dp)
                                .consumeWindowInsets(WindowInsets.systemBars),
                        ) {
                            currentScreen()
                        }
                    }
                }
            }

            fun capture(fileName: String, content: @Composable () -> Unit) {
                composeRule.runOnIdle { currentScreen = content }
                composeRule.waitForIdle()
                val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
                saveBitmap(bitmap, fileName)
            }

            capture("01_session_0_of_16.png") {
                SessionDashboardCapture(SessionUiMapper.mapToActiveUiState(s0, WeightUnit.KG, nowEpochMs = 0L))
            }
            capture("02_session_1_of_16.png") {
                SessionDashboardCapture(SessionUiMapper.mapToActiveUiState(s1, WeightUnit.KG, nowEpochMs = 255_000L))
            }
            capture("03_session_non_sequential.png") {
                SessionDashboardCapture(SessionUiMapper.mapToActiveUiState(sNonSeq, WeightUnit.KG, nowEpochMs = 760_000L))
            }
            capture("04_session_exercise_1_complete.png") {
                SessionDashboardCapture(SessionUiMapper.mapToActiveUiState(sEx1Done, WeightUnit.KG, nowEpochMs = 1_122_000L))
            }
            capture("05_session_all_complete.png") {
                SessionDashboardCapture(SessionUiMapper.mapToActiveUiState(sAllDone, WeightUnit.KG, nowEpochMs = 2_895_000L))
            }
            capture("06_end_workout_early_modal.png") {
                SessionDashboardCapture(
                    SessionUiMapper.mapToActiveUiState(s1, WeightUnit.KG, nowEpochMs = 255_000L)
                        .copy(showEndEarlyConfirmation = true),
                )
            }
            capture("07_manual_set_logging_sheet.png") {
                SessionDashboardCapture(
                    SessionUiMapper.mapToActiveUiState(s0, WeightUnit.KG, nowEpochMs = 45_000L)
                        .copy(
                            manualEntryTarget = ManualSetEntryTarget(
                                sessionExerciseId = ex1.id,
                                exerciseName = ex1.exerciseNameSnapshot,
                                setIndex = 1,
                                targetReps = 10,
                                targetLoad = WeightLoad.fromKilograms(80.0),
                                isCorrection = false,
                            ),
                        ),
                )
            }
            capture("08_workout_complete_summary.png") {
                WorkoutResultScreenContent(uiState = completeResult, onBackToHome = {})
            }
            capture("09_workout_early_termination_summary.png") {
                WorkoutResultScreenContent(uiState = partialResult, onBackToHome = {})
            }
            capture("10_workout_abandoned_summary.png") {
                WorkoutResultScreenContent(uiState = abandonedResult, onBackToHome = {})
            }
        }
    }

    @Composable
    private fun SessionDashboardCapture(state: com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionUiState) {
        WorkoutSessionScreenContent(
            uiState = state,
            onBackClick = {},
            onSetActionClick = { _, _ -> },
            onDismissSetEntry = {},
            onConfirmSetEntry = { _, _, _ -> },
            onFinishWorkout = {},
            onRequestEndEarly = {},
            onDismissEndEarly = {},
            onConfirmEndEarly = {},
        )
    }

    private fun buildSampleSession(): WorkoutSession {
        fun ex(id: String, name: String, order: Int, loadKg: Double) = SessionExercise(
            id = id,
            sessionId = "s_cap",
            sourceExerciseId = id,
            exerciseNameSnapshot = name,
            muscleGroupSnapshot = MuscleGroup.CHEST,
            equipmentSnapshot = EquipmentCategory.BARBELL,
            trackingCategorySnapshot = TrackingCategory.LINEAR,
            orderIndex = order,
            plannedSets = (1..4).map { idx ->
                SessionPlannedSet("sp_${id}_$idx", id, idx, 10, WeightLoad.fromKilograms(loadKg), 90)
            },
            performedSets = emptyList(),
        )
        return WorkoutSession(
            id = "s_cap",
            ownerProfileId = "guest",
            sourcePlanId = "p1",
            planNameSnapshot = "Upper Body Power",
            startedAtEpochMs = 0L,
            status = SessionStatus.ACTIVE,
            exercises = listOf(
                ex("se_1", "Barbell Bench Press", 0, 80.0),
                ex("se_2", "Incline Dumbbell Press", 1, 28.0),
                ex("se_3", "Bent-Over Barbell Row", 2, 70.0),
                ex("se_4", "Overhead Triceps Extension", 3, 32.0),
            ),
        )
    }

    private fun perf(id: String, exId: String, setIndex: Int, reps: Int, loadKg: Double) = PerformedSet(
        id = id,
        sessionId = "s_cap",
        sessionExerciseId = exId,
        setIndex = setIndex,
        fullReps = reps,
        partialReps = 0,
        actualLoad = WeightLoad.fromKilograms(loadKg),
        completedAtEpochMs = 1_000L,
        trackingMode = TrackingMode.MANUAL,
        origin = MeasurementOrigin.MANUAL_INPUT,
    )

    private fun saveBitmap(bitmap: Bitmap, fileName: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.filesDir, "p7_screenshots").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        require(file.exists() && file.length() > 0L) {
            "Captured screenshot $fileName was missing or empty"
        }
    }
}
