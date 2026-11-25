package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P4AuthAndProfileUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var dataContainer: PulseDataContainer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataContainer = PulseDataContainer(context)
        runBlocking {
            dataContainer.userPreferencesRepository.setOnboardingCompleted(false)
            dataContainer.authRepository.signOut()
        }
    }

    @Test
    fun onboardingToGuestProfileSetup_preservesPersonalInfoOnBack_andEntersMain() {
        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.AUTH,
                dataContainer = dataContainer,
            )
        }

        // Splash resolves to Onboarding page 1
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Welcome to PULSE").assertExists()
                true
            }.getOrDefault(false)
        }

        // Advance Page 1 -> Page 2 -> Page 3 -> Auth Entry
        composeRule.onNodeWithText("Next →").performClick()
        composeRule.onNodeWithText("Track Every Workout").assertIsDisplayed()
        composeRule.onNodeWithText("Next →").performClick()
        composeRule.onNodeWithText("Analyze Your Progress").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started →").performClick()

        // Auth Entry -> Continue as Guest (Offline Mode)
        composeRule.onNodeWithText("Elevate Every Rep").assertIsDisplayed()
        composeRule.onNodeWithText("Continue as Guest (Offline Mode) →").performClick()

        // Personal Info Setup
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("About You").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("field_YOUR NAME").performTextInput("Sam Rivera")
        composeRule.onNodeWithTag("metric_AGE").performTextInput("29")
        composeRule.onNodeWithTag("metric_HEIGHT").performTextInput("178")
        composeRule.onNodeWithTag("metric_WEIGHT").performTextInput("75")
        composeRule.onNodeWithText("Female").performClick()
        composeRule.onNodeWithText("Continue to Goals →").performScrollTo().performClick()

        // Fitness Goals Setup -> Navigate back to verify Personal Info is preserved
        composeRule.onNodeWithText("Your Fitness Goals").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Sam Rivera").assertIsDisplayed()

        // Proceed forward and complete setup
        composeRule.onNodeWithText("Continue to Goals →").performClick()
        composeRule.onNodeWithText("Increase Strength & Power").performClick()
        composeRule.onNodeWithText("Advanced").performClick()
        composeRule.onNodeWithText("5 Days").performClick()
        composeRule.onNodeWithText("Complete Setup & Start →").performClick()

        // Enters Main Shell (HomeTab)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Route: HomeTab").assertExists()
                true
            }.getOrDefault(false)
        }

        // Logout from ProfileTab returns to AuthEntry
        composeRule.onNodeWithText(PulseTab.PROFILE.label).performClick()
        composeRule.onNodeWithText("Return to Auth Entry").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Elevate Every Rep").assertExists()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun loginAndSignUp_displayFigmaValidationErrors_andForgotPasswordIsHonest() {
        runBlocking {
            dataContainer.userPreferencesRepository.setOnboardingCompleted(true)
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.AUTH,
                dataContainer = dataContainer,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Elevate Every Rep").assertExists()
                true
            }.getOrDefault(false)
        }

        // Check Sign Up 4-scenario validation errors
        composeRule.onNodeWithText("Create Free Account").performClick()
        composeRule.onNodeWithText("Start Your Training").assertIsDisplayed()
        composeRule.onNode(hasText("Create Account") and hasClickAction()).performClick()
        composeRule.onNodeWithText("Registration Incomplete (4 Errors)").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Missing required field: Name cannot be blank.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Invalid email format: Missing domain extension.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Weak password: Minimum 8 characters required with symbol & number.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Confirmation mismatch: Passwords do not match.").assertIsDisplayed()

        // Go back to Auth Entry -> Log In -> Validation Errors + Forgot Password
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Log In").performClick()
        composeRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeRule.onNodeWithText("⚠ Test Invalid Login & Error Feedback").performClick()
        composeRule.onNodeWithText("Incorrect Email or Password").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Invalid email. Format must be name@example.com, not empty.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Password cannot be empty (minimum 8 characters required).").assertIsDisplayed()

        // Navigate to Forgot Password and verify unauthenticated reset is blocked honestly
        composeRule.onNodeWithText("Forgot Password? Reset it here →").performScrollTo().performClick()
        composeRule.onNodeWithText("Save New Password").assertIsDisplayed()
        composeRule.onNodeWithTag("field_NEW PASSWORD").performTextInput("Pulse@2026!")
        composeRule.onNodeWithText("Save New Password").performClick()
        composeRule.onNodeWithText("Password Reset Unavailable").assertIsDisplayed()
    }
}
