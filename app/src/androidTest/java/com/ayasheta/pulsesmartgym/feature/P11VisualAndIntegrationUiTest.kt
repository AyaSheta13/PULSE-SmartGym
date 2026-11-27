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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
        var page by mutableIntStateOf(0)

        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = widthDp, height = 844.dp)) {
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

        // Welcome across 360, 390, 412dp
        listOf(360.dp, 390.dp, 412.dp).forEach { w ->
            widthDp = w
            composeRule.waitForIdle()
            assertEquals(0, composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size)
            val bounds = composeRule.onNodeWithTag("welcome_hero_card").getUnclippedBoundsInRoot()
            val cardRatio = (bounds.right - bounds.left).value / (bounds.bottom - bounds.top).value
            assertTrue("Welcome ratio mismatch: $cardRatio", abs(cardRatio - (378f / 436f)) < 0.04f)
            if (w == 390.dp) saveScreenshot("p11_welcome_390.png")
        }

        // Onboarding 0, 1, 2 at 390dp
        screenMode = "onboarding"
        widthDp = 390.dp
        val expectedRatios = listOf(378f / 436f, 1672f / 941f, 377f / 436f)
        for (p in 0..2) {
            page = p
            composeRule.waitForIdle()
            val cardBounds = composeRule.onNodeWithTag("onboarding_hero_card_$p").getUnclippedBoundsInRoot()
            val badgeBounds = composeRule.onNodeWithTag("onboarding_hero_badge_$p").getUnclippedBoundsInRoot()
            val ratio = (cardBounds.right - cardBounds.left).value / (cardBounds.bottom - cardBounds.top).value
            assertTrue("Page $p ratio mismatch: $ratio vs ${expectedRatios[p]}", abs(ratio - expectedRatios[p]) < 0.05f)
            assertTrue(badgeBounds.top >= cardBounds.top && badgeBounds.bottom <= cardBounds.bottom)
            saveScreenshot("p11_onboarding_$p.png")
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
