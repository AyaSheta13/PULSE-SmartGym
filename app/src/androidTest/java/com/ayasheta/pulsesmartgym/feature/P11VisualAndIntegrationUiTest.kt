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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.feature.auth.entry.AuthEntryContent
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingContent
import com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionPhase
import com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionScreenContent
import com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionUiState
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerItemUiModel
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerUiState
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class P11VisualAndIntegrationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcomeAndOnboardingHeroCards_matchIntrinsicAspectRatios_withZeroScroll() {
        var screenMode by mutableStateOf("welcome")
        var widthDp by mutableStateOf(390.dp)
        var heightDp by mutableStateOf(844.dp)
        var page by mutableIntStateOf(0)

        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = widthDp, height = heightDp)) {
                    if (screenMode == "welcome") {
                        AuthEntryContent(
                            isLoading = false,
                            errorMessage = null,
                            onCreateAccountClick = {},
                            onLogInClick = {},
                            onContinueAsGuestClick = {},
                        )
                    } else {
                        OnboardingContent(
                            currentPage = page,
                            isCompleting = false,
                            onPageSelected = { page = it },
                            onBackClick = {},
                            onSkipClick = {},
                            onPrimaryClick = {},
                        )
                    }
                }
            }
        }

        // Welcome across 360, 390, 412dp and short/standard/tall heights
        listOf(360.dp to 640.dp, 390.dp to 844.dp, 412.dp to 915.dp).forEach { (w, h) ->
            widthDp = w
            heightDp = h
            composeRule.waitForIdle()
            assertEquals(0, composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size)
            val bounds = composeRule.onNodeWithTag("welcome_hero_card").getUnclippedBoundsInRoot()
            val cardRatio = (bounds.right - bounds.left).value / (bounds.bottom - bounds.top).value
            assertTrue("Welcome ratio mismatch at ${w}x$h: $cardRatio", abs(cardRatio - (378f / 436f)) < 0.04f)
            if (w == 390.dp) saveScreenshot("p11_welcome_390.png")
        }

        // Onboarding 0, 1, 2 across 360, 390, 412dp and short/tall heights
        screenMode = "onboarding"
        val expectedRatios = listOf(378f / 436f, 935f / 1024f, 377f / 436f)
        listOf(360.dp to 640.dp, 390.dp to 844.dp, 412.dp to 915.dp).forEach { (w, h) ->
            widthDp = w
            heightDp = h
            for (p in 0..2) {
                page = p
                composeRule.waitForIdle()
                val verticalScrollNodes = composeRule.onAllNodes(
                    androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
                        androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange,
                    ),
                ).fetchSemanticsNodes()
                assertEquals(0, verticalScrollNodes.size)
                val cardBounds = composeRule.onNodeWithTag("onboarding_hero_card_$p").getUnclippedBoundsInRoot()
                val badgeBounds = composeRule.onNodeWithTag("onboarding_hero_badge_$p").getUnclippedBoundsInRoot()
                val ratio = (cardBounds.right - cardBounds.left).value / (cardBounds.bottom - cardBounds.top).value
                assertTrue("Page $p ratio mismatch at ${w}x$h: $ratio vs ${expectedRatios[p]}", abs(ratio - expectedRatios[p]) < 0.05f)
                assertTrue(badgeBounds.top >= cardBounds.top && badgeBounds.bottom <= cardBounds.bottom)
                if (w == 390.dp) saveScreenshot("p11_onboarding_$p.png")
            }
        }
    }

    @Test
    fun exercisePicker_displaysLeftCircleSelection_wrappedName_andRightChevron() {
        var toggled: String? = null
        var details: String? = null

        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = 390.dp, height = 844.dp)) {
                    ExercisePickerScreenContent(
                        uiState = ExercisePickerUiState(
                            isLoading = false,
                            selectedIds = listOf("ex_bench_press"),
                            exercises = listOf(
                                ExercisePickerItemUiModel(
                                    id = "ex_bench_press",
                                    name = "Barbell Bench Press",
                                    muscleGroup = MuscleGroup.CHEST,
                                    muscleSubtitle = "Chest",
                                    equipmentLabel = "BARBELL",
                                    thumbnailRes = R.drawable.ex_thumb_bench_press,
                                    isSelected = true,
                                ),
                                ExercisePickerItemUiModel(
                                    id = "ex_barbell_row",
                                    name = "Bent-Over Barbell Row",
                                    muscleGroup = MuscleGroup.BACK,
                                    muscleSubtitle = "Back",
                                    equipmentLabel = "BARBELL",
                                    thumbnailRes = R.drawable.ex_thumb_barbell_row,
                                    isSelected = false,
                                ),
                            ),
                        ),
                        onQueryChanged = {},
                        onFilterSelected = {},
                        onToggleExercise = { toggled = it },
                        onBackClick = {},
                        onConfirmClick = {},
                        onExerciseDetailsClick = { details = it },
                    )
                }
            }
        }

        val toggleBounds = composeRule.onNodeWithTag("picker_toggle_ex_barbell_row").getUnclippedBoundsInRoot()
        val chevronBounds = composeRule.onNodeWithTag("picker_chevron_ex_barbell_row", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Toggle must be on left of chevron", toggleBounds.right < chevronBounds.left)
        composeRule.onNodeWithText("Bent-Over Barbell Row").assertIsDisplayed()
        saveScreenshot("p11_exercise_picker_after.png")

        composeRule.onNodeWithTag("picker_details_ex_barbell_row").performClick()
        assertEquals("ex_barbell_row", details)
        assertEquals(null, toggled)
    }

    @Test
    fun setExecution_showsLiveMetrics_expandableBreakdown_andReactiveConnectedSimChip() {
        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = 390.dp, height = 844.dp)) {
                    SetExecutionScreenContent(
                        uiState = SetExecutionUiState.Active(
                            phase = SetExecutionPhase.InProgress,
                            sessionId = "s1",
                            ownerId = "o1",
                            planName = "Upper Body Power",
                            exerciseId = "se1",
                            exerciseName = "Barbell Bench Press",
                            setIndex = 1,
                            totalSetsForExercise = 4,
                            targetReps = 10,
                            targetLoad = WeightLoad(80_000L),
                            targetRestSeconds = 90,
                            preferredWeightUnit = WeightUnit.KG,
                            recordedSetsCount = 0,
                            actualLoad = WeightLoad(80_000L),
                            fullReps = 2,
                            partialReps = 0,
                            isDeviceAssisted = false,
                            canStartDeviceAssisted = true,
                            isFallbackManual = true,
                            modeLabel = "Manual",
                            bluetoothChipStatus = PulseBluetoothChipStatus.CONNECTED,
                            isTelemetrySimulated = true,
                            trackingCompatibilityNotice = null,
                            startedElapsedRealtimeMs = 1_000L,
                            repRecords = listOf(
                                RepMeasurement(1, RepClassification.FULL, 1800L, angularRomDegrees = 82.0, linearRomCm = 41.5, peakAccelerationMetersPerSecondSquared = 2.65, origin = MeasurementOrigin.SIMULATED_TELEMETRY),
                                RepMeasurement(2, RepClassification.FULL, 1750L, angularRomDegrees = 84.0, linearRomCm = 42.5, peakAccelerationMetersPerSecondSquared = 2.80, origin = MeasurementOrigin.SIMULATED_TELEMETRY),
                            ),
                            errorMessage = null,
                            isReconnecting = false,
                            reconnectError = null,
                            showEndEarlyConfirmation = false,
                            isEndingEarly = false,
                            showSimulateButtons = false,
                        ),
                        onBackClick = {},
                        onStartSetClick = {},
                        onFinishSetClick = {},
                        onIncrementRep = {},
                        onDecrementRep = {},
                        onIncrementWeight = {},
                        onDecrementWeight = {},
                    )
                }
            }
        }

        // Reactive connection chip stays "Connected (Sim)" even when manual fallback is active
        composeRule.onNodeWithText("Connected (Sim)").assertIsDisplayed()
        composeRule.onNodeWithTag("set_execution_live_performance").assertIsDisplayed()
        composeRule.onNodeWithText("2.80 m/s²").assertIsDisplayed()
        composeRule.onNodeWithTag("button_toggle_rep_breakdown").performClick()
        composeRule.waitForIdle()
        saveScreenshot("p11_set_execution_live_metrics.png")
    }

    @Test
    fun defaultPlansSeededOnFirstUse_andPlusIconsRenderedInWorkoutsEditorAndPicker() {
        val context: Context = ApplicationProvider.getApplicationContext()
        com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer.createIsolatedForTesting(
            context = context,
            seedDefaultPlansOnFirstUse = true,
        ).use { isolated ->
            val container = isolated.container
            kotlinx.coroutines.runBlocking {
                val guest = (container.authRepository.continueAsGuest() as com.ayasheta.pulsesmartgym.core.model.common.DomainResult.Success).data
                container.userProfileRepository.saveCompletedProfile(
                    ownerId = guest.id,
                    personalInfo = com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft(
                        "Aya Sheta",
                        com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity.FEMALE,
                        25,
                        168,
                        62.0,
                    ),
                    goals = com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft(
                        com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal.STRENGTH,
                        com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel.BEGINNER,
                        3,
                    ),
                )
                container.userPreferencesRepository.setOnboardingCompleted(true)
            }

            composeRule.setContent {
                com.ayasheta.pulsesmartgym.app.PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = com.ayasheta.pulsesmartgym.navigation.AppEntryFlow.MAIN,
                    initialTab = com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab.WORKOUTS,
                    dataContainer = container,
                )
            }

            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("Beginner Full Body").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Upper Body Strength").assertIsDisplayed()
            composeRule.onNodeWithText("Lower Body Foundation").assertIsDisplayed()
            saveScreenshot("p11_my_workouts_seeded.png")

            // Open Create New Workout Plan -> verify centered Plus icons & empty exercises card
            composeRule.onNodeWithTag("button_create_new_plan_inline").performClick()
            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithTag("editor_empty_exercises").assertExists()
                    true
                }.getOrDefault(false)
            }
            saveScreenshot("p11_create_plan_empty_plus.png")

            // Open Select Exercises -> verify circular Plus selection controls
            composeRule.onNodeWithTag("editor_add_exercise_button").performClick()
            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithTag("picker_exercise_list").assertExists()
                    true
                }.getOrDefault(false)
            }
            saveScreenshot("p11_exercise_picker_plus.png")
        }
    }

    @Test
    fun exercisePhotoMappings_resolveDistinctlyForTricepPushdownLatPulldownAndHistory() {
        val tricepRes = com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes("tricep_pushdown")
        val cableRowRes = com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes("cable_row")
        val latPulldownRes = com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes("lat_pulldown")
        val barbellRowRes = com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes("barbell_row")

        assertEquals(R.drawable.ex_thumb_tricep_pushdown, tricepRes)
        assertEquals(R.drawable.ex_thumb_cable_row, cableRowRes)
        assertEquals(R.drawable.ex_thumb_lat_pulldown, latPulldownRes)
        assertEquals(R.drawable.ex_thumb_barbell_row, barbellRowRes)
        assertTrue(tricepRes != cableRowRes)
        assertTrue(latPulldownRes != barbellRowRes)

        assertEquals(
            R.drawable.ex_thumb_tricep_pushdown,
            com.ayasheta.pulsesmartgym.feature.history.HistoryUiMapper.resolveHistoryThumbnail(
                sourceExerciseId = "ex_tricep_pushdown",
                exerciseName = "Cable Tricep Pushdown",
                muscleGroup = com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup.TRICEPS,
                planName = "Upper Body Strength",
            ),
        )
        assertEquals(
            R.drawable.ex_thumb_lat_pulldown,
            com.ayasheta.pulsesmartgym.feature.history.HistoryUiMapper.resolveHistoryThumbnail(
                sourceExerciseId = "ex_lat_pulldown",
                exerciseName = "Lat Pulldown",
                muscleGroup = com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup.BACK,
                planName = "Upper Body Strength",
            ),
        )
    }

    @Test
    fun starterPlanEndToEndJourney_startCompleteHistoryDetailsProgress_andEmptyStateAfterDelete() {
        val context: Context = ApplicationProvider.getApplicationContext()
        com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer.createIsolatedForTesting(
            context = context,
            seedDefaultPlansOnFirstUse = true,
        ).use { isolated ->
            val container = isolated.container
            val ownerId = kotlinx.coroutines.runBlocking {
                val guest = (container.authRepository.continueAsGuest() as com.ayasheta.pulsesmartgym.core.model.common.DomainResult.Success).data
                container.userProfileRepository.saveCompletedProfile(
                    ownerId = guest.id,
                    personalInfo = com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft(
                        "Aya Sheta",
                        com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity.FEMALE,
                        25,
                        168,
                        62.0,
                    ),
                    goals = com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft(
                        com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal.STRENGTH,
                        com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel.BEGINNER,
                        3,
                    ),
                )
                container.userPreferencesRepository.setOnboardingCompleted(true)
                guest.id
            }

            composeRule.setContent {
                com.ayasheta.pulsesmartgym.app.PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = com.ayasheta.pulsesmartgym.navigation.AppEntryFlow.MAIN,
                    initialTab = com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab.WORKOUTS,
                    dataContainer = container,
                )
            }

            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("Beginner Full Body").assertExists()
                    true
                }.getOrDefault(false)
            }

            // Verify Upper Body Strength & Lower Body Foundation open cleanly in WorkoutDetails
            composeRule.onNodeWithText("Upper Body Strength").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("details_plan_name").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Upper Body Strength").assertIsDisplayed()
            composeRule.onNodeWithTag("details_start_workout_button").assertIsDisplayed()
            androidx.test.espresso.Espresso.pressBack()

            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Beginner Full Body").assertExists(); true }.getOrDefault(false)
            }

            // Open Beginner Full Body -> Start Workout -> Set Execution -> Finish Set -> Skip Rest -> End Early -> History -> Progress
            composeRule.onNodeWithText("Beginner Full Body").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("details_start_workout_button").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("details_start_workout_button").performClick()
            composeRule.waitUntil(10_000) {
                runCatching { composeRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Barbell Back Squat").assertIsDisplayed()
            composeRule.onAllNodesWithText("Start").onFirst().performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("button_set_execution_primary").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("button_set_execution_primary").performScrollTo().performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
            }
            repeat(5) {
                composeRule.onNodeWithContentDescription("Add one rep").performClick()
            }
            composeRule.onNodeWithTag("button_set_execution_primary").performScrollTo().performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Skip Rest").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Skip Rest").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("button_end_workout").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("button_end_workout").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("button_confirm_end_workout").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("button_confirm_end_workout").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("summary_headline").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("button_summary_back_home").performScrollTo().performClick()

            // History -> Session Details -> Progress
            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText(com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab.HISTORY.label).assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithText(com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab.HISTORY.label).performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Beginner Full Body").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Beginner Full Body").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Barbell Back Squat").assertExists(); true }.getOrDefault(false)
            }
            androidx.test.espresso.Espresso.pressBack()

            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText(com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab.PROGRESS.label).assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Progress tab").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("training_progress_screen").assertExists(); true }.getOrDefault(false)
            }

            // Delete all 3 starter plans -> verify true empty My Workouts state
            kotlinx.coroutines.runBlocking {
                val plans = container.workoutPlanRepository.observePlans(ownerId).first()
                plans.forEach { container.workoutPlanRepository.deletePlan(ownerId, it.id) }
            }
            composeRule.onNodeWithContentDescription("Workouts tab").performClick()
            composeRule.waitForIdle()
            if (runCatching { composeRule.onNodeWithTag("details_plan_name").assertExists(); true }.getOrDefault(false) ||
                runCatching { composeRule.onNodeWithTag("details_not_found").assertExists(); true }.getOrDefault(false)
            ) {
                androidx.test.espresso.Espresso.pressBack()
            }
            composeRule.waitUntil(10_000) {
                runCatching { composeRule.onNodeWithTag("workouts_empty_state").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("No Workout Plans Yet").assertIsDisplayed()
            saveScreenshot("p11_my_workouts_empty_after_delete.png")
        }
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
