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
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterScreenContent
import com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterUiState
import com.ayasheta.pulsesmartgym.feature.session.dashboard.PerformedSetUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionExerciseUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionRestTimerUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionSetRowUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionScreenContent
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionUiState
import com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetScreenContent
import com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetUiState
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class P8ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureP8ScreensAt390x884() {
        var screenMode by mutableStateOf(0)

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
                        when (screenMode) {
                            0 -> ReadyStartSetScreenContent(
                                uiState = ReadyStartSetUiState.Ready(
                                    sessionId = "s1",
                                    exerciseId = "e1",
                                    setIndex = 1,
                                    totalSetsForExercise = 4,
                                    planName = "Upper Body Power",
                                    exerciseName = "Barbell Bench Press",
                                    targetReps = 10,
                                    targetLoad = WeightLoad.fromKilograms(80.0),
                                    targetRestSeconds = 90,
                                    preferredWeightUnit = WeightUnit.KG,
                                ),
                                onBackClick = {},
                                onStartSetClick = {},
                                onRequestEndEarly = {},
                                onDismissEndEarly = {},
                                onConfirmEndEarly = {},
                            )
                            1 -> ManualRepCounterScreenContent(
                                uiState = ManualRepCounterUiState.Active(
                                    sessionId = "s1",
                                    exerciseId = "e1",
                                    setIndex = 2,
                                    totalSetsForExercise = 4,
                                    planName = "Upper Body Power",
                                    exerciseName = "Barbell Bench Press",
                                    targetReps = 10,
                                    targetLoad = WeightLoad.fromKilograms(80.0),
                                    targetRestSeconds = 90,
                                    preferredWeightUnit = WeightUnit.KG,
                                    currentReps = 8,
                                    actualLoad = WeightLoad.fromKilograms(80.0),
                                ),
                                onBackClick = {},
                                onIncrementRep = {},
                                onDecrementRep = {},
                                onIncrementWeight = {},
                                onDecrementWeight = {},
                                onFinishSet = {},
                                onRequestEndEarly = {},
                                onDismissEndEarly = {},
                                onConfirmEndEarly = {},
                            )
                            else -> WorkoutSessionScreenContent(
                                uiState = WorkoutSessionUiState.Active(
                                    sessionId = "s1",
                                    ownerId = "u1",
                                    planName = "Upper Body Power",
                                    startedAtEpochMs = 0L,
                                    elapsedSeconds = 252L,
                                    totalPlannedSets = 16,
                                    totalRecordedSets = 1,
                                    progressFraction = 1f / 16f,
                                    allSetsCompleted = false,
                                    preferredWeightUnit = WeightUnit.KG,
                                    restTimer = SessionRestTimerUiModel(
                                        exerciseId = "e1",
                                        exerciseName = "Barbell Bench Press",
                                        completedSetIndex = 1,
                                        remainingSeconds = 88,
                                        remainingFormatted = "01:28",
                                        progress = 88f / 90f,
                                        deadlineEpochMs = 88_000L,
                                        nextExerciseId = "e1",
                                        nextSetIndex = 2,
                                    ),
                                    exercises = listOf(
                                        SessionExerciseUiModel(
                                            sessionExerciseId = "e1",
                                            sourceExerciseId = "cat_bench",
                                            exerciseName = "Barbell Bench Press",
                                            completedSetsCount = 1,
                                            totalPlannedSetsCount = 4,
                                            isCompleted = false,
                                            progressFraction = 0.25f,
                                            sets = listOf(
                                                SessionSetRowUiModel(
                                                    sessionExerciseId = "e1",
                                                    setIndex = 1,
                                                    targetReps = 10,
                                                    targetLoad = WeightLoad.fromKilograms(80.0),
                                                    targetRestSeconds = 90,
                                                    performed = PerformedSetUiModel(
                                                        fullReps = 10,
                                                        partialReps = 0,
                                                        actualLoad = WeightLoad.fromKilograms(80.0),
                                                        trackingMode = com.ayasheta.pulsesmartgym.core.model.device.TrackingMode.MANUAL,
                                                    ),
                                                ),
                                                SessionSetRowUiModel(
                                                    sessionExerciseId = "e1",
                                                    setIndex = 2,
                                                    targetReps = 10,
                                                    targetLoad = WeightLoad.fromKilograms(80.0),
                                                    targetRestSeconds = 90,
                                                    performed = null,
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
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
                    }
                }
            }
        }

        val outDir = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "p8_screenshots").apply {
            mkdirs()
        }

        composeRule.waitForIdle()
        saveBitmap(composeRule.onRoot().captureToImage().asAndroidBitmap(), File(outDir, "01_ready_start_set.png"))

        screenMode = 1
        composeRule.waitForIdle()
        saveBitmap(composeRule.onRoot().captureToImage().asAndroidBitmap(), File(outDir, "02_manual_rep_counter.png"))

        screenMode = 2
        composeRule.waitForIdle()
        saveBitmap(composeRule.onRoot().captureToImage().asAndroidBitmap(), File(outDir, "03_session_rest_visible.png"))
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
