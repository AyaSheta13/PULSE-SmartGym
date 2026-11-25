package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.auth.entry.AuthEntryContent
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginContent
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginUiState
import com.ayasheta.pulsesmartgym.feature.auth.recovery.ForgotPasswordContent
import com.ayasheta.pulsesmartgym.feature.auth.recovery.ForgotPasswordUiState
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpContent
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpUiState
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingContent
import com.ayasheta.pulsesmartgym.feature.profilesetup.goals.FitnessGoalsContent
import com.ayasheta.pulsesmartgym.feature.profilesetup.goals.FitnessGoalsUiState
import com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo.PersonalInfoContent
import com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo.PersonalInfoUiState
import com.ayasheta.pulsesmartgym.feature.splash.SplashContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class P4ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAll12ReferenceScreens() {
        val screenNames = listOf(
            "01_splash",
            "02_onboarding_1",
            "03_onboarding_2",
            "04_onboarding_3",
            "05_auth_entry",
            "06_login",
            "07_login_errors",
            "08_signup",
            "09_signup_errors",
            "10_forgot_password",
            "11_profile_personal_info",
            "12_profile_fitness_goals",
        )
        var screenIndex by mutableIntStateOf(0)

        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(width = 390.dp, height = 884.dp)) {
                    when (screenIndex) {
                        0 -> SplashContent()
                        1 -> OnboardingContent(0, false, {}, {}, {})
                        2 -> OnboardingContent(1, false, {}, {}, {})
                        3 -> OnboardingContent(2, false, {}, {}, {})
                        4 -> AuthEntryContent(false, null, {}, {}, {})
                        5 -> LoginContent(
                            uiState = LoginUiState(email = "alex.miller@example.com", password = "••••••••••••"),
                            isDebugEntryEnabled = true,
                            onBackClick = {},
                            onEmailChange = {},
                            onPasswordChange = {},
                            onTogglePasswordVisibility = {},
                            onToggleRememberMe = {},
                            onForgotPasswordClick = {},
                            onSubmitClick = {},
                            onTestErrorsClick = {},
                            onFillValidDemoClick = {},
                            onSignUpClick = {},
                        )
                        6 -> LoginContent(
                            uiState = LoginUiState(
                                email = "invalid-email-format",
                                password = "",
                                bannerTitle = "Incorrect Email or Password",
                                bannerMessage = "The email or password does not match any local profile on this device.",
                                emailError = "✕ Invalid email. Format must be name@example.com, not empty.",
                                passwordError = "✕ Password cannot be empty (minimum 8 characters required).",
                            ),
                            isDebugEntryEnabled = true,
                            onBackClick = {},
                            onEmailChange = {},
                            onPasswordChange = {},
                            onTogglePasswordVisibility = {},
                            onToggleRememberMe = {},
                            onForgotPasswordClick = {},
                            onSubmitClick = {},
                            onTestErrorsClick = {},
                            onFillValidDemoClick = {},
                            onSignUpClick = {},
                        )
                        7 -> SignUpContent(
                            uiState = SignUpUiState(
                                fullName = "Alex Miller",
                                email = "alex.miller@example.com",
                                password = "••••••••••••",
                                confirmPassword = "••••••••••••",
                            ),
                            isDebugEntryEnabled = true,
                            onBackClick = {},
                            onFullNameChange = {},
                            onEmailChange = {},
                            onPasswordChange = {},
                            onConfirmPasswordChange = {},
                            onTogglePasswordVisibility = {},
                            onToggleConfirmPasswordVisibility = {},
                            onSubmitClick = {},
                            onTestErrorsClick = {},
                            onFillValidDemoClick = {},
                            onLogInClick = {},
                        )
                        8 -> SignUpContent(
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
                            isDebugEntryEnabled = true,
                            onBackClick = {},
                            onFullNameChange = {},
                            onEmailChange = {},
                            onPasswordChange = {},
                            onConfirmPasswordChange = {},
                            onTogglePasswordVisibility = {},
                            onToggleConfirmPasswordVisibility = {},
                            onSubmitClick = {},
                            onTestErrorsClick = {},
                            onFillValidDemoClick = {},
                            onLogInClick = {},
                        )
                        9 -> ForgotPasswordContent(
                            uiState = ForgotPasswordUiState(newPassword = "••••••••••••"),
                            onBackClick = {},
                            onNewPasswordChange = {},
                            onTogglePasswordVisibility = {},
                            onSaveClick = {},
                        )
                        10 -> PersonalInfoContent(
                            uiState = PersonalInfoUiState(
                                fullName = "Alex Miller",
                                ageText = "28",
                                heightText = "182",
                                weightText = "78",
                            ),
                            onBackClick = {},
                            onNameChange = {},
                            onGenderSelect = {},
                            onAgeChange = {},
                            onHeightChange = {},
                            onWeightChange = {},
                            onImperialClick = {},
                            onContinueClick = {},
                        )
                        11 -> FitnessGoalsContent(
                            uiState = FitnessGoalsUiState(),
                            onBackClick = {},
                            onGoalSelect = {},
                            onExperienceSelect = {},
                            onWeeklyWorkoutsSelect = {},
                            onCompleteClick = {},
                        )
                    }
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.getExternalFilesDir(null), "p4_screenshots").apply { mkdirs() }

        screenNames.forEachIndexed { idx, name ->
            composeRule.runOnIdle { screenIndex = idx }
            composeRule.waitForIdle()
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            val file = File(dir, "$name.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }
}
