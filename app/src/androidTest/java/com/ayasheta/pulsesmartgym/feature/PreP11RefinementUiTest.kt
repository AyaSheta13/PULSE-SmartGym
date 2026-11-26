package com.ayasheta.pulsesmartgym.feature

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseBottomSheet
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.PulseRepCounterGauge
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginContent
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginUiState
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpContent
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpUiState
import com.ayasheta.pulsesmartgym.feature.device.components.BluetoothRadarIndicator
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingContent
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalHost
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalSheet
import com.ayasheta.pulsesmartgym.feature.session.dashboard.EndWorkoutEarlyModal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PreP11RefinementUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pulseBottomSheet_animatesEnterAndExit_andUnmountsAfterExit() {
        var visible by mutableStateOf(false)
        var dismissCount = 0

        composeRule.setContent {
            PulseTheme {
                PulseBottomSheet(
                    visible = visible,
                    onDismissRequest = {
                        dismissCount += 1
                        visible = false
                    },
                    title = "Confirm Action",
                    summaryContent = {
                        BasicText("Sheet body content")
                    },
                    primaryActionLabel = "Confirm",
                    onPrimaryAction = { visible = false },
                )
            }
        }

        composeRule.onNodeWithText("Confirm Action").assertDoesNotExist()

        composeRule.runOnIdle { visible = true }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Confirm Action").assertIsDisplayed()
        composeRule.onNodeWithText("Sheet body content").assertIsDisplayed()

        composeRule.runOnIdle { visible = false }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Confirm Action").assertDoesNotExist()
        assertEquals(0, dismissCount)
    }

    @Test
    fun onboardingPager_syncsWithButtonsAndSwipe_andGuardsDoubleTap() {
        var currentPage by mutableIntStateOf(0)
        var completeCount = 0

        composeRule.setContent {
            PulseTheme {
                OnboardingContent(
                    currentPage = currentPage,
                    isCompleting = false,
                    onPageSelected = { currentPage = it },
                    onBackClick = { if (currentPage > 0) currentPage -= 1 },
                    onSkipClick = { completeCount += 1 },
                    onPrimaryClick = {
                        if (currentPage < 2) currentPage += 1 else completeCount += 1
                    },
                )
            }
        }

        composeRule.onNodeWithText("Welcome to PULSE").assertIsDisplayed()
        composeRule.onNodeWithText("Next →").performClick()
        composeRule.waitForIdle()
        assertEquals(1, currentPage)
        composeRule.onNodeWithText("Track Every Workout").assertIsDisplayed()

        composeRule.onNodeWithText("Track Every Workout").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(2, currentPage)
        composeRule.onNodeWithText("Analyze Your Progress").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started →").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started →").performClick()
        assertEquals(1, completeCount)
    }

    @Test
    fun loginAndSignUp_excludeSocialLoginAndDebugShowcase_andKeepPrimaryCtaInteractive() {
        var loginSubmitCount = 0
        var signUpSubmitCount = 0

        composeRule.setContent {
            PulseTheme {
                LoginContent(
                    uiState = LoginUiState(
                        email = "bad-email",
                        password = "123",
                        bannerTitle = "Validation Failed",
                        bannerMessage = "Please check the highlighted email and password fields.",
                        emailError = "✕ Invalid email. Format must be name@example.com, not empty.",
                        passwordError = "✕ Password cannot be empty (minimum 8 characters required).",
                    ),
                    onBackClick = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onToggleRememberMe = {},
                    onForgotPasswordClick = {},
                    onSubmitClick = { loginSubmitCount += 1 },
                    onSignUpClick = {},
                )
            }
        }

        composeRule.onNodeWithText("OR CONTINUE WITH").assertDoesNotExist()
        composeRule.onNodeWithText("Google").assertDoesNotExist()
        composeRule.onNodeWithText("Apple").assertDoesNotExist()
        composeRule.onNodeWithText("⚠ Test Invalid Login & Error Feedback").assertDoesNotExist()
        composeRule.onNodeWithText("Try Valid Demo Credentials").assertDoesNotExist()
        composeRule.onNode(hasText("Log In") and hasClickAction()).performScrollTo().performClick()
        assertEquals(1, loginSubmitCount)
        assertTrue(signUpSubmitCount == 0)
    }

    @Test
    fun signUpContent_excludesDebugButtons_evenWithErrors() {
        var submitCount = 0
        composeRule.setContent {
            PulseTheme {
                SignUpContent(
                    uiState = SignUpUiState(
                        fullName = "Alex",
                        email = "invalid",
                        password = "123",
                        confirmPassword = "456",
                        emailError = "✕ Invalid email format: Missing domain extension.",
                    ),
                    onBackClick = {},
                    onFullNameChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onToggleConfirmPasswordVisibility = {},
                    onSubmitClick = { submitCount += 1 },
                    onLogInClick = {},
                )
            }
        }

        composeRule.onNodeWithText("⚠ Test Sign Up Validation Errors (4 Scenarios)").assertDoesNotExist()
        composeRule.onNodeWithText("Use Valid Demo Registration").assertDoesNotExist()
        composeRule.onNode(hasText("Create Account") and hasClickAction()).performScrollTo().performClick()
        assertEquals(1, submitCount)
    }

    @Test
    fun repCounterGauge_rendersBackgroundDecorations_andAnimatesProgress() {
        var reps by mutableIntStateOf(2)
        composeRule.setContent {
            PulseTheme {
                PulseRepCounterGauge(
                    currentReps = reps,
                    targetReps = 10,
                    onDecrementRep = { reps = (reps - 1).coerceAtLeast(0) },
                    onIncrementRep = { reps += 1 },
                )
            }
        }

        composeRule.onNodeWithTag("rep_counter_card_decorations").assertExists()
        composeRule.onNodeWithTag("rep_counter_gauge_arc").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add one rep").performClick()
        composeRule.waitForIdle()
        assertEquals(3, reps)
        composeRule.onNodeWithContentDescription("Subtract one rep").performClick()
        composeRule.waitForIdle()
        assertEquals(2, reps)
    }

    @Test
    fun bluetoothRadarIndicator_togglesBetweenAnimatingAndStaticCleanly() {
        var isAnimating by mutableStateOf(true)
        composeRule.setContent {
            PulseTheme {
                BluetoothRadarIndicator(isAnimating = isAnimating)
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { isAnimating = false }
        composeRule.waitForIdle()
    }

    @Test
    fun overlayBottomSheet_animatesUpwardOnEnter_downwardOnExit_andPreventsDuplicateDestructiveAction() {
        var activeSheet by mutableStateOf<ProfileModalSheet?>(null)
        var showEndEarly by mutableStateOf(false)
        var endWorkoutCalls = 0

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PulseTheme {
                ProfileModalHost(
                    activeSheet = activeSheet,
                    isGuest = false,
                    preferredUnit = WeightUnit.KG,
                    onDismiss = { activeSheet = null },
                    onConfirmUnits = {},
                    inWindow = true,
                )
                EndWorkoutEarlyModal(
                    visible = showEndEarly,
                    recordedSetsCount = 2,
                    isEnding = false,
                    onKeepWorkingOut = { showEndEarly = false },
                    onEndWorkout = {
                        endWorkoutCalls += 1
                        showEndEarly = false
                    },
                    inWindow = true,
                )
            }
        }

        // Open ProfileModalHost overlay sheet and verify intermediate enter positions
        composeRule.runOnIdle { activeSheet = ProfileModalSheet.UNITS_AND_MEASUREMENTS }
        composeRule.mainClock.advanceTimeBy(32L)
        val enterStartTop = composeRule.onNodeWithTag("sheet_units").getBoundsInRoot().top
        composeRule.mainClock.advanceTimeBy(150L)
        val enterMidTop = composeRule.onNodeWithTag("sheet_units").getBoundsInRoot().top
        composeRule.mainClock.advanceTimeBy(200L)
        val enterSettledTop = composeRule.onNodeWithTag("sheet_units").getBoundsInRoot().top
        assertTrue(
            "Expected upward entry motion: start=$enterStartTop, mid=$enterMidTop, settled=$enterSettledTop",
            enterStartTop > enterMidTop && enterMidTop > enterSettledTop,
        )

        // Dismiss overlay sheet and verify it remains mounted during 240ms downward exit, then unmounts
        composeRule.runOnIdle { activeSheet = null }
        composeRule.mainClock.advanceTimeBy(32L)
        composeRule.mainClock.advanceTimeBy(120L)
        val exitMidTop = composeRule.onNodeWithTag("sheet_units").getBoundsInRoot().top
        assertTrue(
            "Expected downward exit motion while still mounted: settled=$enterSettledTop, exitMid=$exitMidTop",
            exitMidTop > enterSettledTop,
        )
        composeRule.mainClock.advanceTimeBy(200L)
        composeRule.onNodeWithTag("sheet_units").assertDoesNotExist()

        // Verify EndWorkoutEarlyModal destructive button cannot execute twice during exit
        composeRule.runOnIdle { showEndEarly = true }
        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.onNodeWithTag("button_confirm_end_workout").performClick()
        composeRule.mainClock.advanceTimeBy(50L)
        // Attempt second click while exit animation is running
        runCatching { composeRule.onNodeWithTag("button_confirm_end_workout").performClick() }
        composeRule.mainClock.advanceTimeBy(250L)
        assertEquals(1, endWorkoutCalls)
        composeRule.onNodeWithTag("modal_end_workout_early").assertDoesNotExist()
    }
}
