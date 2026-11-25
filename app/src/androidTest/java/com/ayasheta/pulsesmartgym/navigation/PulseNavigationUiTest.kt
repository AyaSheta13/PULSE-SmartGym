package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PulseNavigationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun authFlow_debugEntryEntersMainShellAndSwitchesTabs() {
        composeRule.setContent {
            PulseApp(isDebugEntryEnabled = true, initialFlow = AppEntryFlow.AUTH)
        }

        composeRule.onNodeWithText("Splash").assertIsDisplayed()
        composeRule.onNodeWithText("Debug: Enter Main App Shell").performClick()

        // Main shell shows Home and 5 bottom tabs
        composeRule.onNodeWithText("Route: HomeTab").assertIsDisplayed()
        composeRule.onNodeWithText(PulseTab.WORKOUTS.label).performClick()
        composeRule.onNodeWithText("Route: WorkoutsTab").assertIsDisplayed()

        // Navigate to nested Workout Details -> bottom bar hidden
        composeRule.onNodeWithText("Open Workout Details").performClick()
        composeRule.onNodeWithText("Route: WorkoutDetails").assertIsDisplayed()

        // Back returns to WorkoutsTab
        composeRule.onNodeWithText("Navigate Back").performClick()
        composeRule.onNodeWithText("Route: WorkoutsTab").assertIsDisplayed()
    }

    @Test
    fun modalSheet_displaysAboveDestinationAndDismissesWithoutPoppingRoute() {
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

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Route: ProfileTab").assertIsDisplayed()
    }
}
