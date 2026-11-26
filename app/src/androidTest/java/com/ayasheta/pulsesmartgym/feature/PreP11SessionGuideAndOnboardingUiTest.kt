package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.feature.auth.entry.AuthEntryContent
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingContent
import com.ayasheta.pulsesmartgym.feature.session.dashboard.PerformedSetUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionExerciseUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionRestTimerUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionSetRowUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionScreenContent
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionUiState
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsUiState
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseGuideCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PreP11SessionGuideAndOnboardingUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun workoutSession_howToPerform_opensCorrectExercise_andPreservesSessionOnReturn() {
        val seeded = ExerciseCatalogSeed.defaultExercises.map { e ->
            ExerciseDefinition(
                id = e.id,
                name = e.name,
                muscleGroup = e.muscleGroup,
                equipment = e.equipment,
                trackingCategory = e.trackingCategory,
                description = e.description,
                imageKey = e.imageKey,
            )
        }
        val bench = seeded.first { it.id == "ex_bench_press" }
        val inclineDb = seeded.first { it.id == "ex_db_incline_press" }
        val row = seeded.first { it.id == "ex_barbell_row" }

        val activeSessionState = WorkoutSessionUiState.Active(
            sessionId = "sess_1",
            ownerId = "owner_1",
            planName = "Upper Body Power",
            startedAtEpochMs = 1_000L,
            elapsedSeconds = 255L,
            totalPlannedSets = 6,
            totalRecordedSets = 2,
            progressFraction = 2f / 6f,
            allSetsCompleted = false,
            preferredWeightUnit = WeightUnit.KG,
            connectionLabel = "Connected",
            restTimer = SessionRestTimerUiModel(
                exerciseId = "se_bench",
                exerciseName = bench.name,
                completedSetIndex = 1,
                remainingSeconds = 88,
                remainingFormatted = "01:28",
                progress = 88f / 90f,
                deadlineEpochMs = Long.MAX_VALUE,
                nextExerciseId = "se_bench",
                nextSetIndex = 2,
            ),
            exercises = listOf(
                SessionExerciseUiModel(
                    sessionExerciseId = "se_bench",
                    sourceExerciseId = bench.id,
                    exerciseName = bench.name,
                    completedSetsCount = 1,
                    totalPlannedSetsCount = 2,
                    isCompleted = false,
                    progressFraction = 0.5f,
                    sets = listOf(
                        SessionSetRowUiModel(
                            sessionExerciseId = "se_bench",
                            setIndex = 1,
                            targetReps = 10,
                            targetLoad = WeightLoad(grams = 80_000L),
                            targetRestSeconds = 90,
                            performed = PerformedSetUiModel(
                                fullReps = 10,
                                partialReps = 0,
                                actualLoad = WeightLoad(grams = 80_000L),
                                trackingMode = TrackingMode.MANUAL,
                            ),
                        ),
                        SessionSetRowUiModel(
                            sessionExerciseId = "se_bench",
                            setIndex = 2,
                            targetReps = 10,
                            targetLoad = WeightLoad(grams = 80_000L),
                            targetRestSeconds = 90,
                            performed = null,
                        ),
                    ),
                ),
                SessionExerciseUiModel(
                    sessionExerciseId = "se_incline",
                    sourceExerciseId = inclineDb.id,
                    exerciseName = inclineDb.name,
                    completedSetsCount = 1,
                    totalPlannedSetsCount = 1,
                    isCompleted = true,
                    progressFraction = 1f,
                    sets = listOf(
                        SessionSetRowUiModel(
                            sessionExerciseId = "se_incline",
                            setIndex = 1,
                            targetReps = 10,
                            targetLoad = WeightLoad(grams = 30_000L),
                            targetRestSeconds = 90,
                            performed = PerformedSetUiModel(
                                fullReps = 10,
                                partialReps = 0,
                                actualLoad = WeightLoad(grams = 30_000L),
                                trackingMode = TrackingMode.MANUAL,
                            ),
                        ),
                    ),
                ),
                SessionExerciseUiModel(
                    sessionExerciseId = "se_row",
                    sourceExerciseId = row.id,
                    exerciseName = "Very Long Bent-Over Barbell Row Exercise Name",
                    completedSetsCount = 0,
                    totalPlannedSetsCount = 3,
                    isCompleted = false,
                    progressFraction = 0f,
                    sets = listOf(
                        SessionSetRowUiModel("se_row", 1, 8, WeightLoad(grams = 70_000L), 90, null),
                        SessionSetRowUiModel("se_row", 2, 8, WeightLoad(grams = 70_000L), 90, null),
                        SessionSetRowUiModel("se_row", 3, 8, WeightLoad(grams = 70_000L), 90, null),
                    ),
                ),
            ),
        )

        var selectedDetailsId by mutableStateOf<String?>(null)
        var endEarlyRequested = false

        composeRule.setContent {
            PulseTheme {
                val openId = selectedDetailsId
                if (openId == null) {
                    WorkoutSessionScreenContent(
                        uiState = activeSessionState,
                        onBackClick = { endEarlyRequested = true },
                        onSetActionClick = { _, _ -> },
                        onDismissSetEntry = {},
                        onConfirmSetEntry = { _, _, _ -> },
                        onFinishWorkout = {},
                        onRequestEndEarly = { endEarlyRequested = true },
                        onDismissEndEarly = {},
                        onConfirmEndEarly = {},
                        onHowToPerformClick = { selectedDetailsId = it },
                    )
                } else {
                    val target = if (openId == "se_incline") inclineDb else bench
                    ExerciseDetailsScreenContent(
                        uiState = ExerciseDetailsUiState.Loaded(
                            exerciseId = target.id,
                            name = target.name,
                            bodyPartLabel = target.muscleGroup.displayLabel(),
                            equipmentLabel = target.equipment.displayLabel(),
                            categoryLabel = target.trackingCategory.displayLabel(),
                            imageRes = resolveExerciseThumbnailRes(target.imageKey, target.muscleGroup),
                            guide = ExerciseGuideCatalog.forExercise(target),
                        ),
                        onBackClick = { selectedDetailsId = null },
                    )
                }
            }
        }

        // Scroll to verify long exercise name & How to perform visibility + capture session screenshot
        composeRule.onNodeWithTag("session_how_to_perform_se_bench").assertIsDisplayed()
        composeRule.onNodeWithTag("session_how_to_perform_se_incline").assertIsDisplayed()
        composeRule.onNodeWithTag("session_exercise_complete_se_incline").assertIsDisplayed()
        saveScreenshot("android_workout_session_how_to_perform.png")

        // Open Incline DB Press guide (must NOT open Barbell Bench Press)
        composeRule.onNodeWithTag("session_how_to_perform_se_incline").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(inclineDb.name).assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_details_instructions").assertIsDisplayed()
        assertFalse(endEarlyRequested)
        saveScreenshot("android_exercise_details_from_session.png")

        // Return to active session and confirm elapsed time, rest timer, and completed sets remain intact
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("04:15").assertIsDisplayed()
        composeRule.onNodeWithTag("session_rest_timer_card").assertIsDisplayed()
        composeRule.onNodeWithText("2 / 6 Sets Completed").assertIsDisplayed()
        assertFalse(endEarlyRequested)
        saveScreenshot("android_workout_session_returned.png")
    }

    @Test
    fun welcomeScreen_fitsWithoutVerticalScroll_acrossSupportedWidths() {
        var guestClicked = false
        var createAccountClicked = false
        var loginClicked = false
        var currentWidth by mutableStateOf(360.dp)

        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = currentWidth, height = 844.dp)) {
                    AuthEntryContent(
                        isLoading = false,
                        errorMessage = null,
                        onCreateAccountClick = { createAccountClicked = true },
                        onLogInClick = { loginClicked = true },
                        onContinueAsGuestClick = { guestClicked = true },
                    )
                }
            }
        }

        val widths = listOf(360.dp, 390.dp, 412.dp)
        widths.forEach { width ->
            currentWidth = width
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("welcome_hero_card").assertIsDisplayed()
            composeRule.onNodeWithTag("welcome_hero_image").assertIsDisplayed()
            composeRule.onNodeWithText("Create Free Account").assertIsDisplayed()
            composeRule.onNodeWithText("Log In").assertIsDisplayed()
            composeRule.onNodeWithText("Continue as Guest (Offline Mode) →").assertIsDisplayed()
            // Verify zero vertical scroll container on standard sizes
            assertEquals(0, composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size)

            if (width == 390.dp) {
                saveScreenshot("android_welcome_after.png")
            }
        }

        composeRule.onNodeWithText("Continue as Guest (Offline Mode) →").performClick()
        composeRule.onNodeWithText("Create Free Account").performClick()
        composeRule.onNodeWithText("Log In").performClick()
        assertTrue(guestClicked && createAccountClicked && loginClicked)
    }

    @Test
    fun onboardingAllThreePages_fitWithoutVerticalScroll_andSwipeWorks() {
        var page by mutableIntStateOf(0)

        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = 390.dp, height = 844.dp)) {
                    OnboardingContent(
                        currentPage = page,
                        isCompleting = false,
                        onPageSelected = { page = it },
                        onBackClick = { if (page > 0) page-- },
                        onSkipClick = {},
                        onPrimaryClick = { if (page < 2) page++ },
                    )
                }
            }
        }

        // Page 0
        composeRule.onNodeWithTag("onboarding_hero_card_0").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome to PULSE").assertIsDisplayed()
        saveScreenshot("android_onboarding_1_after.png")

        // Swipe left -> Page 1
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(1, page)
        composeRule.onNodeWithTag("onboarding_hero_card_1").assertIsDisplayed()
        composeRule.onNodeWithText("Track Every Workout").assertIsDisplayed()
        saveScreenshot("android_onboarding_2_after.png")

        // Tap Next -> Page 2
        composeRule.onNodeWithText("Next →").performClick()
        composeRule.waitForIdle()
        assertEquals(2, page)
        composeRule.onNodeWithTag("onboarding_hero_card_2").assertIsDisplayed()
        composeRule.onNodeWithText("Analyze Your Progress").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started →").assertIsDisplayed()
        saveScreenshot("android_onboarding_3_after.png")
    }

    private fun saveScreenshot(fileName: String) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val outDir = File(context.filesDir, "visual_qa")
        if (!outDir.exists()) outDir.mkdirs()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, fileName).outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
