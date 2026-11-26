package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseBottomNavigationBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.PulseRepCounterGauge
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginContent
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginUiState
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpContent
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpUiState
import com.ayasheta.pulsesmartgym.feature.device.components.BluetoothRadarIndicator
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalHost
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalSheet
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailExerciseUiModel
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PreP11VisualQaCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAllPreP11VisualQaScreens() {
        var step by mutableIntStateOf(0)
        var activeSheet by mutableStateOf<ProfileModalSheet?>(null)

        val detailExercises = listOf(
            WorkoutDetailExerciseUiModel("d1", 1, "ex_back_squat", "Barbell Squat", "Quads & Glutes", R.drawable.ex_thumb_back_squat, 4, "8", "120s", "4 × 8", emptyList(), false),
            WorkoutDetailExerciseUiModel("d2", 2, "ex_leg_press", "Leg Press", "Quads & Hamstrings", R.drawable.ex_thumb_leg_press, 4, "10", "90s", "4 × 10", emptyList(), false),
        )

        val detailsUiState = WorkoutDetailsUiState.Loaded(
            planId = "p1",
            categoryTag = "UPPER BODY POWER",
            planName = "Upper Body Power",
            description = "Chest, back, and arms strength routine.",
            exerciseCount = 2,
            totalSetsCount = 7,
            estimatedMinutes = 35,
            exercises = detailExercises,
            expandedOccurrenceId = null,
        )

        val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
            "08_home_nav_selected.png" to {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7)), contentAlignment = Alignment.BottomCenter) {
                    PulseBottomNavigationBar(selectedTab = PulseTab.HOME, onTabSelected = {})
                }
            },
            "09_home_nav_unselected.png" to {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7)), contentAlignment = Alignment.BottomCenter) {
                    PulseBottomNavigationBar(selectedTab = PulseTab.WORKOUTS, onTabSelected = {})
                }
            },
            "10_workout_details.png" to {
                WorkoutDetailsScreenContent(
                    uiState = detailsUiState,
                    onBackClick = {},
                    onToggleExercise = {},
                    onEditPlanClick = {},
                )
            },
            "11_rep_counter_halos_manual.png" to {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7)).padding(16.dp), contentAlignment = Alignment.Center) {
                    PulseRepCounterGauge(
                        currentReps = 6,
                        targetReps = 10,
                        onDecrementRep = {},
                        onIncrementRep = {},
                    )
                }
            },
            "12_rep_counter_halos_device.png" to {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7)).padding(16.dp), contentAlignment = Alignment.Center) {
                    PulseRepCounterGauge(
                        currentReps = 8,
                        targetReps = 10,
                        overlineLabel = "AUTO REPS",
                        adjustLabel = "Manual adjust",
                        onDecrementRep = {},
                        onIncrementRep = {},
                    )
                }
            },
            "13_device_discovery_scanning.png" to {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7)), contentAlignment = Alignment.Center) {
                    BluetoothRadarIndicator(isAnimating = true)
                }
            },
            "14_login_inline_validation.png" to {
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
                    onSubmitClick = {},
                    onSignUpClick = {},
                )
            },
            "15_signup_inline_validation.png" to {
                SignUpContent(
                    uiState = SignUpUiState(
                        fullName = "",
                        email = "invalid.email@",
                        password = "12345",
                        confirmPassword = "abcdef",
                        bannerTitle = "Registration Incomplete (4 Errors)",
                        bannerMessage = "Please correct the highlighted fields below to create your account.",
                        fullNameError = "✕ Missing required field: Name cannot be blank.",
                        emailError = "✕ Invalid email format: Missing domain extension.",
                        passwordError = "✕ Weak password: Minimum 8 characters required with symbol & number.",
                        confirmPasswordError = "✕ Confirmation mismatch: Passwords do not match.",
                    ),
                    onBackClick = {},
                    onFullNameChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onToggleConfirmPasswordVisibility = {},
                    onSubmitClick = {},
                    onLogInClick = {},
                )
            },
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = 390.dp, height = 884.dp)) {
                    if (step < screens.size) {
                        screens[step].second()
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7))) {
                            ProfileModalHost(
                                activeSheet = activeSheet,
                                isGuest = false,
                                preferredUnit = WeightUnit.KG,
                                onDismiss = { activeSheet = null },
                                onConfirmUnits = {},
                                inWindow = true,
                            )
                        }
                    }
                }
            }
        }

        val outDir = File("/sdcard/Download/pre_p11").apply { mkdirs() }

        screens.forEachIndexed { index, (filename, _) ->
            composeRule.runOnIdle { step = index }
            composeRule.mainClock.advanceTimeBy(400L)
            val bmp = composeRule.onRoot().captureToImage().asAndroidBitmap()
            FileOutputStream(File(outDir, filename)).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        // Capture bottom sheet open and mid-exit
        composeRule.runOnIdle {
            step = screens.size
            activeSheet = ProfileModalSheet.UNITS_AND_MEASUREMENTS
        }
        composeRule.mainClock.advanceTimeBy(350L)
        val openBmp = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(outDir, "16_bottom_sheet_open.png")).use { out ->
            openBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        composeRule.runOnIdle { activeSheet = null }
        composeRule.mainClock.advanceTimeBy(32L)
        composeRule.mainClock.advanceTimeBy(120L)
        val exitBmp = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(outDir, "17_bottom_sheet_mid_exit.png")).use { out ->
            exitBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
