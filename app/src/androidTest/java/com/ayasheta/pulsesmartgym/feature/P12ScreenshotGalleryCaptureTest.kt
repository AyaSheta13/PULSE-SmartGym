package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P12ScreenshotGalleryCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAuthLoginAndSignUpScreenshots() {
        val context: Context = ApplicationProvider.getApplicationContext()
        PulseDataContainer.createIsolatedForTesting(
            context = context,
            seedDefaultPlansOnFirstUse = false,
        ).use { isolated ->
            runBlocking { isolated.container.userPreferencesRepository.setOnboardingCompleted(true) }
            composeRule.setContent {
                PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = AppEntryFlow.AUTH,
                    dataContainer = isolated.container,
                )
            }
            composeRule.waitUntil(8_000) {
                runCatching { composeRule.onNodeWithText("Log In").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Log In").performClick()
            composeRule.waitForIdle()
            saveScreenshot("login.png")

            composeRule.onNodeWithText("Sign Up").performClick()
            composeRule.waitForIdle()
            saveScreenshot("signup.png")
        }
    }

    @Test
    fun captureMainAppJourneyScreenshots() {
        val context: Context = ApplicationProvider.getApplicationContext()
        PulseDataContainer.createIsolatedForTesting(
            context = context,
            seedDefaultPlansOnFirstUse = true,
        ).use { isolated ->
            val container = isolated.container
            runBlocking {
                val account = (
                    container.authRepository.registerLocalAccount(
                        fullName = "Alex Morgan",
                        email = "alex.morgan@pulse.test",
                        password = "Password123!",
                    ) as DomainResult.Success<AuthUser>
                    ).data
                container.userProfileRepository.saveCompletedProfile(
                    ownerId = account.id,
                    personalInfo = PersonalInfoDraft("Alex Morgan", GenderIdentity.MALE, 27, 178, 76.0),
                    goals = FitnessGoalsDraft(FitnessGoal.STRENGTH, ExperienceLevel.BEGINNER, 3),
                )
                container.userPreferencesRepository.setOnboardingCompleted(true)
                container.workoutPlanRepository.observePlans(account.id).first { it.isNotEmpty() }
            }

            composeRule.setContent {
                PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.WORKOUTS,
                    dataContainer = container,
                )
            }

            // 1. Wait for Workouts, then capture Home & Profile
            composeRule.waitUntil(8_000) {
                runCatching { composeRule.onNodeWithText("Beginner Full Body").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Home tab").performClick()
            composeRule.waitForIdle()
            saveScreenshot("home.png")

            // 2. Profile
            composeRule.onNodeWithContentDescription("Profile tab").performClick()
            composeRule.waitForIdle()
            saveScreenshot("profile_settings.png")

            // 3. Workouts -> Details -> Session -> Set Ready -> Rest -> Results -> History -> Session Details -> Progress
            composeRule.onNodeWithContentDescription("Workouts tab").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Beginner Full Body").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Beginner Full Body").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("details_start_workout_button").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("details_start_workout_button").performClick()
            composeRule.waitUntil(10_000) {
                runCatching { composeRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
            }
            saveScreenshot("workout_session.png")

            composeRule.onAllNodesWithText("Start").onFirst().performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("button_set_execution_primary").assertExists(); true }.getOrDefault(false)
            }
            saveScreenshot("set_execution_ready.png")

            composeRule.onNodeWithTag("button_set_execution_primary").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
            }
            repeat(5) { composeRule.onNodeWithContentDescription("Add one rep").performClick() }
            composeRule.onNodeWithTag("button_set_execution_primary").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Skip Rest").assertExists(); true }.getOrDefault(false)
            }
            saveScreenshot("rest_timer.png")

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
            saveScreenshot("workout_results.png")

            composeRule.onNodeWithTag("button_summary_back_home").performScrollTo().performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText(PulseTab.HISTORY.label).assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText(PulseTab.HISTORY.label).performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Beginner Full Body").assertExists(); true }.getOrDefault(false)
            }
            saveScreenshot("history.png")

            composeRule.onNodeWithText("Beginner Full Body").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Barbell Back Squat").assertExists(); true }.getOrDefault(false)
            }
            saveScreenshot("session_details.png")
            androidx.test.espresso.Espresso.pressBack()

            composeRule.onNodeWithContentDescription("Progress tab").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("training_progress_screen").assertExists(); true }.getOrDefault(false)
            }
            saveScreenshot("training_progress.png")
        }
    }

    @Test
    fun captureDeviceConnectionScreenshot() {
        composeRule.setContent {
            com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme {
                com.ayasheta.pulsesmartgym.feature.device.center.MyDeviceScreenContent(
                    uiState = com.ayasheta.pulsesmartgym.feature.device.DeviceUiState(
                        linkState = com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState(
                            phase = com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState.CONNECTED,
                            providerType = com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType.SIMULATED,
                            selectedDevice = com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor(
                                id = "SIM-PULSE-01",
                                name = "PULSE Pod (Sim)",
                                profileLabel = "Simulated IMU + Linear",
                                rssiDbm = -48,
                                signalLabel = "Strong (Sim)",
                                providerType = com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType.SIMULATED,
                                isSimulated = true,
                            ),
                            batteryPercent = 88,
                            isTelemetrySimulated = true,
                        ),
                        activeProviderType = com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType.SIMULATED,
                        isDemoSessionActive = true,
                    ),
                    onBackClick = {},
                    onSelectProvider = {},
                    onSearchAndConnectClick = {},
                    onEnterDemoSimulationClick = {},
                    onStartWorkoutWithAutoTracking = {},
                    onDisconnectClick = {},
                )
            }
        }
        composeRule.waitForIdle()
        saveScreenshot("device_connection.png")
    }

    private fun saveScreenshot(fileName: String) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(context.filesDir, "screenshots_gallery").apply { mkdirs() }
        FileOutputStream(File(dir, fileName)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
