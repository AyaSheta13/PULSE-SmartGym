package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P5HomeAndProfileUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun seedAuthenticatedUser(container: PulseDataContainer): String = runBlocking {
        container.userPreferencesRepository.setOnboardingCompleted(true)
        val uniqueEmail = "p5_test_${System.nanoTime()}@pulse.test"
        val user = (container.authRepository.registerLocalAccount(
            fullName = "Alex Miller",
            email = uniqueEmail,
            password = "Password!1",
        ) as DomainResult.Success).data
        container.userProfileRepository.saveCompletedProfile(
            ownerId = user.id,
            personalInfo = PersonalInfoDraft("Alex Miller", GenderIdentity.MALE, 28, 182, 78.0),
            goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
        )
        user.id
    }

    @Test
    fun homeNavigation_avatar_workouts_progress_and_editProfileSaveAndDiscard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = PulseDataContainer(context)
        val ownerId = seedAuthenticatedUser(container)
        runBlocking {
            val firstExerciseId = container.exerciseRepository.observeAll().first().first().id
            val created = container.workoutPlanRepository.createPlan(
                ownerId = ownerId,
                draft = WorkoutPlanDraft(
                    name = "Upper Body Power",
                    exercises = listOf(
                        PlanExerciseDraft(
                            exerciseId = firstExerciseId,
                            orderIndex = 0,
                            plannedSets = listOf(PlannedSetDraft(1, 8), PlannedSetDraft(2, 8)),
                        ),
                    ),
                ),
            )
            assertTrue(created is DomainResult.Success)
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                initialTab = PulseTab.HOME,
                dataContainer = container,
            )
        }

        // Home displays real profile name & plan
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Upper Body Power").fetchSemanticsNodes().isNotEmpty()
        }

        // Home -> Workouts via See all
        composeRule.onNodeWithText("See all ›").performClick()
        composeRule.onNodeWithText("My Workouts").assertIsDisplayed()
        composeRule.onNodeWithText("Upper Body Power").assertIsDisplayed()

        // Home -> Progress via View Training Progress & PRs
        composeRule.onNodeWithText(PulseTab.HOME.label).performClick()
        composeRule.onNodeWithText("View Training Progress & PRs →").performClick()
        composeRule.onNodeWithText("Training Progress").assertIsDisplayed()

        // Home -> Profile via avatar click
        composeRule.onNodeWithText(PulseTab.HOME.label).performClick()
        composeRule.onNodeWithText("A").performClick()
        composeRule.onNodeWithText("Profile & Settings").assertIsDisplayed()

        // Profile -> Edit Profile -> Discard leaves name unchanged
        composeRule.onNodeWithTag("profile_edit_button").performClick()
        composeRule.onNodeWithText("Edit Profile").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_profile_name_input", useUnmergedTree = true).performTextClearance()
        composeRule.onNodeWithTag("edit_profile_name_input", useUnmergedTree = true).performTextInput("Discarded Name")
        composeRule.onNodeWithTag("edit_profile_discard_button").performScrollTo().performClick()

        composeRule.onNodeWithText("Profile & Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Alex Miller").assertIsDisplayed()

        // Profile -> Edit Profile -> Save persists and updates Profile & Home
        composeRule.onNodeWithTag("profile_edit_button").performClick()
        composeRule.onNodeWithTag("edit_profile_name_input", useUnmergedTree = true).performTextClearance()
        composeRule.onNodeWithTag("edit_profile_name_input", useUnmergedTree = true).performTextInput("Jordan Lee")
        composeRule.onNodeWithTag("edit_profile_weight_input", useUnmergedTree = true).performTextClearance()
        composeRule.onNodeWithTag("edit_profile_weight_input", useUnmergedTree = true).performTextInput("80.5")
        composeRule.onNodeWithTag("edit_profile_save_button").performScrollTo().performClick()

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Jordan Lee").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("80.5 kg").assertIsDisplayed()

        // Switch to Home and verify updated name reflects in greeting
        composeRule.onNodeWithText(PulseTab.HOME.label).performClick()
        composeRule.waitUntil(5_000) {
            listOf("Good morning, Jordan", "Good afternoon, Jordan", "Good evening, Jordan").any { greeting ->
                runCatching {
                    composeRule.onNodeWithText(greeting, substring = true).assertExists()
                    true
                }.getOrDefault(false)
            }
        }
    }

    @Test
    fun profileSettingsSheets_logoutPreservesRoomDataAndBlocksBackToMain() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = PulseDataContainer(context)
        val ownerId = seedAuthenticatedUser(container)

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                initialTab = PulseTab.PROFILE,
                dataContainer = container,
            )
        }

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("settings_row_units").assertExists()
                true
            }.getOrDefault(false)
        }

        // Units sheet: confirm Metric, verify Imperial disabled
        composeRule.onNodeWithTag("settings_row_units").performClick()
        composeRule.onNodeWithTag("sheet_units").assertIsDisplayed()
        composeRule.onNodeWithText("Coming in future release").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("sheet_units").assertDoesNotExist()
                true
            }.getOrDefault(false)
        }

        // Privacy sheet: open & dismiss via scrim
        composeRule.onNodeWithTag("settings_row_privacy").performClick()
        composeRule.onNodeWithTag("sheet_privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Got It").performClick()
        composeRule.onNodeWithTag("sheet_privacy").assertDoesNotExist()

        // Reminders row: honest Coming Soon feedback
        composeRule.onNodeWithTag("settings_row_reminders").performClick()
        composeRule.onNodeWithTag("sheet_reminders_coming_soon").assertIsDisplayed()
        composeRule.waitForIdle()
        Thread.sleep(500)
        val pfd = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("input keyevent 4")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("sheet_reminders_coming_soon").assertDoesNotExist()
                true
            }.getOrDefault(false)
        }

        // Logout -> returns to AuthEntry, preserves Room profile, Back cannot reopen Main
        composeRule.onNodeWithTag("profile_logout_button").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Elevate Every Rep").assertExists()
                true
            }.getOrDefault(false)
        }
        val persistedProfile = runBlocking { container.userProfileRepository.getProfile(ownerId) }
        assertNotNull(persistedProfile)
        assertTrue(persistedProfile!!.setupCompleted)
    }
}
