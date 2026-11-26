package com.ayasheta.pulsesmartgym.navigation

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.MainActivity
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.app.PulseAppShell
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.navigation.route.ExercisePicker
import com.ayasheta.pulsesmartgym.navigation.route.MyDevice
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PulseNavigationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabSwitching_preservesSaveableStateAcrossRootTabs() {
        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                initialTab = PulseTab.PROGRESS,
            )
        }

        composeRule.onNodeWithText("Training Progress").assertIsDisplayed()

        // Switch to HistoryTab
        composeRule.onNodeWithText(PulseTab.HISTORY.label).performClick()
        composeRule.onNodeWithText("Workout History").assertIsDisplayed()

        // Switch back to ProgressTab -> Training Progress is preserved
        composeRule.onNodeWithText(PulseTab.PROGRESS.label).performClick()
        composeRule.onNodeWithText("Training Progress").assertIsDisplayed()

        // Switch back to HistoryTab -> Workout History is preserved
        composeRule.onNodeWithText(PulseTab.HISTORY.label).performClick()
        composeRule.onNodeWithText("Workout History").assertIsDisplayed()
    }

    @Test
    fun sameDestinationUnderDifferentTabs_preservesIndependentSaveableState() {
        lateinit var navigator: PulseNavigator
        composeRule.setContent {
            PulseTheme {
                val state = rememberPulseNavigationState(
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.PROGRESS,
                )
                navigator = remember(state) { PulseNavigator(state) }
                PulseAppShell(
                    navigationState = state,
                    navigator = navigator,
                    isDebugEntryEnabled = true,
                )
            }
        }

        // Open ExercisePicker under ProgressTab and select Chest filter
        composeRule.runOnIdle { navigator.navigate(ExercisePicker()) }
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithTag("picker_filter_CHEST").assertExists(); true }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("picker_filter_CHEST").performClick()
        composeRule.onNodeWithText("Barbell Bench Press").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell Back Squat").assertDoesNotExist()

        // Switch to HistoryTab while ProgressTab keeps ExercisePicker on its stack
        composeRule.runOnIdle { navigator.selectTab(PulseTab.HISTORY) }
        composeRule.onNodeWithText("Workout History").assertIsDisplayed()
        composeRule.runOnIdle { navigator.navigate(ExercisePicker()) }
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithTag("picker_filter_LEGS").assertExists(); true }.getOrDefault(false)
        }
        // Fresh ExercisePicker defaults to ALL, so Barbell Bench Press is visible
        composeRule.onNodeWithText("Barbell Bench Press").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_filter_LEGS").performClick()
        composeRule.onNodeWithText("Barbell Back Squat").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell Bench Press").assertDoesNotExist()

        // Switch back to ProgressTab -> restores ProgressTab's ExercisePicker with Chest filter
        composeRule.runOnIdle { navigator.selectTab(PulseTab.PROGRESS) }
        composeRule.onNodeWithText("Barbell Bench Press").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell Back Squat").assertDoesNotExist()
    }

    @Test
    fun releaseMode_hidesDebugAuthBypassAcrossAuthFlow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = PulseDataContainer(context)
        runBlocking {
            container.userPreferencesRepository.setOnboardingCompleted(false)
            container.authRepository.signOut()
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = false,
                initialFlow = AppEntryFlow.AUTH,
                dataContainer = container,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Welcome to PULSE").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.onNodeWithText("Elevate Every Rep").assertIsDisplayed()

        composeRule.onNodeWithText("Log In").performClick()
        composeRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeRule.onNodeWithText("⚠ Test Invalid Login & Error Feedback").assertDoesNotExist()
    }

    @Test
    fun modalSheet_dismissesWithSystemBackWithoutPoppingUnderlyingRoute() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = PulseDataContainer(context)
        runBlocking {
            val guest = (container.authRepository.continueAsGuest() as com.ayasheta.pulsesmartgym.core.model.common.DomainResult.Success).data
            container.userProfileRepository.saveCompletedProfile(
                ownerId = guest.id,
                personalInfo = PersonalInfoDraft("Alex Miller", GenderIdentity.MALE, 28, 182, 78.0),
                goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
            )
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                initialTab = PulseTab.PROFILE,
                dataContainer = container,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("settings_row_account").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("settings_row_account").performClick()
        composeRule.onNodeWithTag("sheet_account_preferences").assertIsDisplayed()
        composeRule.onNodeWithText("Got It").assertIsDisplayed()
        composeRule.waitForIdle()
        Thread.sleep(600)

        val pfd = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("input keyevent 4")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("sheet_account_preferences").assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Profile & Settings").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class PulseActivityRecreationUiTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun activityRecreation_preservesSelectedTabAndSaveableState() {
        activityRule.waitUntil(timeoutMillis = 5_000) {
            val onOnboarding = runCatching {
                activityRule.onNodeWithText("Skip").assertExists()
                true
            }.getOrDefault(false)
            val onAuthEntry = runCatching {
                activityRule.onNodeWithText("Elevate Every Rep").assertExists()
                true
            }.getOrDefault(false)
            val onPersonalInfo = runCatching {
                activityRule.onNodeWithText("About You").assertExists()
                true
            }.getOrDefault(false)
            val onHome = runCatching {
                activityRule.onNodeWithText("THIS WEEK'S PROGRESS").assertExists()
                true
            }.getOrDefault(false)
            onOnboarding || onAuthEntry || onPersonalInfo || onHome
        }

        if (runCatching { activityRule.onNodeWithText("Skip").assertExists(); true }.getOrDefault(false)) {
            activityRule.onNodeWithText("Skip").performClick()
        }
        if (runCatching { activityRule.onNodeWithText("Elevate Every Rep").assertExists(); true }.getOrDefault(false)) {
            activityRule.onNodeWithText("Continue as Guest (Offline Mode) →").performClick()
        }
        if (runCatching { activityRule.onNodeWithText("About You").assertExists(); true }.getOrDefault(false)) {
            if (runCatching { activityRule.onNodeWithTag("field_YOUR NAME").assertExists(); true }.getOrDefault(false)) {
                activityRule.onNodeWithTag("field_YOUR NAME").performTextInput("Guest Athlete")
            }
            if (runCatching { activityRule.onNodeWithTag("metric_AGE").assertExists(); true }.getOrDefault(false)) {
                activityRule.onNodeWithTag("metric_AGE").performTextInput("28")
            }
            if (runCatching { activityRule.onNodeWithTag("metric_HEIGHT").assertExists(); true }.getOrDefault(false)) {
                activityRule.onNodeWithTag("metric_HEIGHT").performTextInput("182")
            }
            if (runCatching { activityRule.onNodeWithTag("metric_WEIGHT").assertExists(); true }.getOrDefault(false)) {
                activityRule.onNodeWithTag("metric_WEIGHT").performTextInput("78")
            }
            activityRule.onNodeWithText("Continue to Goals →").performScrollTo().performClick()
            activityRule.onNodeWithText("Complete Setup & Start →").performScrollTo().performClick()
        }

        activityRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                activityRule.onNodeWithText("THIS WEEK'S PROGRESS").assertExists()
                true
            }.getOrDefault(false)
        }
        activityRule.onNodeWithText(PulseTab.PROGRESS.label).performClick()
        activityRule.onNodeWithText("Training Progress").assertIsDisplayed()

        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()

        activityRule.onNodeWithText("Training Progress").assertIsDisplayed()
    }
}
