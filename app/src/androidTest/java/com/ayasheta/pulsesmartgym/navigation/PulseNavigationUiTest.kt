package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.MainActivity
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.app.PulseAppShell
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
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
                initialTab = PulseTab.HOME,
            )
        }

        // Mutate non-default Compose state on HomeTab
        composeRule.onNodeWithText("Route: HomeTab").assertIsDisplayed()
        composeRule.onNodeWithText("Increment Local State").performClick()
        composeRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()

        // Switch to WorkoutsTab and mutate its own state twice
        composeRule.onNodeWithText(PulseTab.WORKOUTS.label).performClick()
        composeRule.onNodeWithText("Route: WorkoutsTab").assertIsDisplayed()
        composeRule.onNodeWithText("Saved State Counter: 0").assertIsDisplayed()
        composeRule.onNodeWithText("Increment Local State").performClick()
        composeRule.onNodeWithText("Increment Local State").performClick()
        composeRule.onNodeWithText("Saved State Counter: 2").assertIsDisplayed()

        // Switch back to HomeTab -> state 1 is preserved
        composeRule.onNodeWithText(PulseTab.HOME.label).performClick()
        composeRule.onNodeWithText("Route: HomeTab").assertIsDisplayed()
        composeRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()

        // Switch back to WorkoutsTab -> state 2 is preserved
        composeRule.onNodeWithText(PulseTab.WORKOUTS.label).performClick()
        composeRule.onNodeWithText("Route: WorkoutsTab").assertIsDisplayed()
        composeRule.onNodeWithText("Saved State Counter: 2").assertIsDisplayed()
    }

    @Test
    fun sameDestinationUnderDifferentTabs_preservesIndependentSaveableState() {
        lateinit var navigator: PulseNavigator
        composeRule.setContent {
            PulseTheme {
                val state = rememberPulseNavigationState(
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.HOME,
                )
                navigator = androidx.compose.runtime.remember(state) { PulseNavigator(state) }
                PulseAppShell(
                    navigationState = state,
                    navigator = navigator,
                    isDebugEntryEnabled = true,
                )
            }
        }

        // Open WorkoutDetails("plan_sample_1") under HomeTab and increment to 1
        composeRule.onNodeWithText("Open Workout Details").performClick()
        composeRule.onNodeWithText("Route: WorkoutDetails").assertIsDisplayed()
        composeRule.onNodeWithText("Increment Local State").performClick()
        composeRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()

        // Switch to WorkoutsTab while HomeTab keeps WorkoutDetails on its stack
        composeRule.runOnIdle { navigator.selectTab(PulseTab.WORKOUTS) }
        composeRule.onNodeWithText("Route: WorkoutsTab").assertIsDisplayed()
        composeRule.onNodeWithText("Open Workout Details").performClick()
        composeRule.onNodeWithText("Route: WorkoutDetails").assertIsDisplayed()
        composeRule.onNodeWithText("Saved State Counter: 0").assertIsDisplayed()
        composeRule.onNodeWithText("Increment Local State").performClick()
        composeRule.onNodeWithText("Increment Local State").performClick()
        composeRule.onNodeWithText("Saved State Counter: 2").assertIsDisplayed()

        // Switch back to HomeTab -> restores HomeTab's WorkoutDetails with counter 1
        composeRule.runOnIdle { navigator.selectTab(PulseTab.HOME) }
        composeRule.onNodeWithText("Route: WorkoutDetails").assertIsDisplayed()
        composeRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()
    }

    @Test
    fun releaseMode_hidesDebugAuthBypassAcrossAuthFlow() {
        composeRule.setContent {
            PulseApp(isDebugEntryEnabled = false, initialFlow = AppEntryFlow.AUTH)
        }

        composeRule.onNodeWithText("Route: Splash").assertIsDisplayed()
        composeRule.onNodeWithText("Debug: Enter Main App Shell").assertDoesNotExist()

        composeRule.onNodeWithText("Go to Auth Entry").performClick()
        composeRule.onNodeWithText("Route: AuthEntry").assertIsDisplayed()
        composeRule.onNodeWithText("Debug: Enter Main App Shell").assertDoesNotExist()

        composeRule.onNodeWithText("Log In").performClick()
        composeRule.onNodeWithText("Route: Login").assertIsDisplayed()
        composeRule.onNodeWithText("Debug: Simulate Login -> Home").assertDoesNotExist()
    }

    @Test
    fun modalSheet_dismissesWithSystemBackWithoutPoppingUnderlyingRoute() {
        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                initialTab = PulseTab.PROFILE,
            )
        }

        composeRule.onNodeWithText("Route: ProfileTab").assertIsDisplayed()
        composeRule.onNodeWithText("Open Modal Sheet").performClick()
        composeRule.onNodeWithText("Sample Modal Sheet").assertIsDisplayed()

        Espresso.pressBack()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sample Modal Sheet").assertDoesNotExist()
        composeRule.onNodeWithText("Route: ProfileTab").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class PulseActivityRecreationUiTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun activityRecreation_preservesSelectedTabAndSaveableState() {
        activityRule.onNodeWithText("Debug: Enter Main App Shell").performClick()
        activityRule.onNodeWithText(PulseTab.WORKOUTS.label).performClick()
        activityRule.onNodeWithText("Increment Local State").performClick()
        activityRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()

        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()

        activityRule.onNodeWithText("Route: WorkoutsTab").assertIsDisplayed()
        activityRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()
    }
}
