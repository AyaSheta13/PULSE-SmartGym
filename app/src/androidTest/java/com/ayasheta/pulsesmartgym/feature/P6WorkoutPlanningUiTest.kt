package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
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
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P6WorkoutPlanningUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setupSignedInUser(container: PulseDataContainer): String = runBlocking {
        container.userPreferencesRepository.setOnboardingCompleted(true)
        val guest = (container.authRepository.continueAsGuest() as DomainResult.Success).data
        container.userProfileRepository.saveCompletedProfile(
            ownerId = guest.id,
            personalInfo = PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
            goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
        )
        guest.id
    }

    @Test
    fun fullWorkoutPlanningFlow_createPickDetailsEditDeleteAndStartDeferredNotice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PulseDataContainer.createIsolatedForTesting(context).use { isolated ->
            val container = isolated.container
            val ownerId = setupSignedInUser(container)

            composeRule.setContent {
                PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.WORKOUTS,
                    dataContainer = container,
                )
            }

        // 1. My Workouts empty state
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("workouts_empty_state").assertExists()
                true
            }.getOrDefault(false)
        }

        // 2. Open Create Workout Plan
        composeRule.onNodeWithTag("button_empty_create_plan").performClick()
        composeRule.onNodeWithTag("editor_empty_exercises").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_name_input").performTextInput("Push Strength")
        composeRule.onNodeWithTag("editor_description_input").performTextInput("Chest and shoulders")

        // 3. Open Exercise Picker, search & filter & select 2 exercises
        composeRule.onNodeWithTag("editor_add_exercise_button").performClick()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("picker_exercise_list").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("picker_search_input").performTextInput("bench")
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("picker_toggle_ex_bench_press").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("picker_toggle_ex_bench_press").performClick()
        composeRule.onNodeWithTag("picker_clear_search").performClick()
        composeRule.onNodeWithTag("picker_filter_SHOULDERS").performClick()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("picker_toggle_ex_lateral_raise").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("picker_toggle_ex_lateral_raise").performClick()
        composeRule.onNodeWithText("2 Selected").assertIsDisplayed()
        composeRule.onNodeWithTag("picker_confirm_button").performClick()

        // 4. Back on Create Plan -> draft preserved and 2 exercises configured
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Push Strength").assertExists()
                composeRule.onNodeWithText("Barbell Bench Press").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("editor_scroll_list")
            .performScrollToNode(hasTestTag("editor_save_button"))
        composeRule.onNodeWithTag("editor_save_button").performClick()

        // 5. Navigated to Workout Details -> verify plan name and Start Workout CTA
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("details_plan_name").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Push Strength").assertIsDisplayed()
        composeRule.onNodeWithTag("details_start_workout_button").assertIsDisplayed()

        // 6. Open Edit Plan -> Delete Plan confirmation -> Keep Plan -> Delete Plan
        composeRule.onNodeWithTag("details_edit_plan_button").performClick()
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithText("Edit Workout Plan").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.onNodeWithTag("modal_delete_plan").assertIsDisplayed()
        composeRule.onNodeWithTag("button_keep_plan").performClick()
        composeRule.onNodeWithTag("modal_delete_plan").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.onNodeWithTag("button_confirm_delete_plan").performClick()

        // 7. Back on My Workouts -> empty state restored and Room has 0 plans
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("workouts_empty_state").assertExists()
                true
            }.getOrDefault(false)
        }
        val remaining = runBlocking { container.workoutPlanRepository.observePlans(ownerId).first() }
        assertTrue(remaining.isEmpty())
        assertEquals(0, remaining.size)
        }
    }
}

@RunWith(AndroidJUnit4::class)
class P6EditorRecreationUiTest {

    private class IsolatedPulseDataContainerRule : org.junit.rules.TestWatcher() {
        private var isolated: PulseDataContainer.IsolatedTestContainer? = null

        val container: PulseDataContainer
            get() = requireNotNull(isolated) { "Isolated container not initialized" }.container

        override fun starting(description: org.junit.runner.Description) {
            val app = ApplicationProvider.getApplicationContext<com.ayasheta.pulsesmartgym.PulseApplication>()
            val created = PulseDataContainer.createIsolatedForTesting(app)
            isolated = created
            app.setTestDataContainerOverride(created.container)
            runBlocking {
                created.container.userPreferencesRepository.setOnboardingCompleted(true)
                val guest = (created.container.authRepository.continueAsGuest() as DomainResult.Success).data
                created.container.userProfileRepository.saveCompletedProfile(
                    ownerId = guest.id,
                    personalInfo = PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
                    goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
                )
            }
        }

        override fun finished(description: org.junit.runner.Description) {
            val app = ApplicationProvider.getApplicationContext<com.ayasheta.pulsesmartgym.PulseApplication>()
            app.setTestDataContainerOverride(null)
            isolated?.close()
            isolated = null
        }
    }

    private val isolatedRule = IsolatedPulseDataContainerRule()
    private val activityRule = androidx.compose.ui.test.junit4.createAndroidComposeRule<com.ayasheta.pulsesmartgym.MainActivity>()

    @get:Rule
    val ruleChain: org.junit.rules.RuleChain = org.junit.rules.RuleChain
        .outerRule(isolatedRule)
        .around(activityRule)

    @Test
    fun createPlanDraft_survivesActivityRecreationAfterReturningFromPicker() {
        val container = isolatedRule.container
        runBlocking {
            container.userPreferencesRepository.setOnboardingCompleted(true)
            val guest = (container.authRepository.continueAsGuest() as DomainResult.Success).data
            container.userProfileRepository.saveCompletedProfile(
                ownerId = guest.id,
                personalInfo = PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
                goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
            )
        }

        activityRule.activityRule.scenario.recreate()
        activityRule.waitUntil(5_000) {
            runCatching {
                activityRule.onNodeWithText(PulseTab.WORKOUTS.label).assertExists()
                true
            }.getOrDefault(false)
        }

        activityRule.onNodeWithText(PulseTab.WORKOUTS.label).performClick()
        activityRule.waitUntil(5_000) {
            runCatching {
                activityRule.onNodeWithTag("workouts_empty_state").assertExists()
                true
            }.getOrDefault(false)
        }

        activityRule.onNodeWithTag("button_empty_create_plan").performClick()
        activityRule.onNodeWithTag("editor_name_input").performTextInput("Restored Split")
        activityRule.onNodeWithTag("editor_description_input").performTextInput("Survived recreation")
        activityRule.onNodeWithTag("editor_add_exercise_button").performClick()

        activityRule.waitUntil(5_000) {
            runCatching {
                activityRule.onNodeWithTag("picker_toggle_ex_bench_press").assertExists()
                true
            }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("picker_toggle_ex_bench_press").performClick()
        activityRule.onNodeWithTag("picker_confirm_button").performClick()

        activityRule.waitUntil(5_000) {
            runCatching {
                activityRule.onNodeWithText("Barbell Bench Press").assertExists()
                true
            }.getOrDefault(false)
        }

        // Trigger Activity recreation
        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()

        activityRule.waitUntil(5_000) {
            runCatching {
                activityRule.onNodeWithText("Restored Split").assertExists()
                activityRule.onNodeWithText("Survived recreation").assertExists()
                activityRule.onNodeWithText("Barbell Bench Press").assertExists()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun editPlanDraft_withMixedPerSetConfigSurvivesActivityRecreation() {
        val container = isolatedRule.container
        val planId = runBlocking {
            container.userPreferencesRepository.setOnboardingCompleted(true)
            val guest = (container.authRepository.continueAsGuest() as DomainResult.Success).data
            container.userProfileRepository.saveCompletedProfile(
                ownerId = guest.id,
                personalInfo = PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
                goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
            )
            container.exerciseRepository.observeAll().first()
            val created = (container.workoutPlanRepository.createPlan(
                guest.id,
                com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft(
                    name = "Mixed Set Plan",
                    description = "Original",
                    exercises = listOf(
                        com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft(
                            exerciseId = "ex_bench_press",
                            orderIndex = 0,
                            plannedSets = listOf(
                                com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft(1, 12, com.ayasheta.pulsesmartgym.core.model.common.WeightLoad.fromKilograms(60.0), 90),
                                com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft(2, 8, com.ayasheta.pulsesmartgym.core.model.common.WeightLoad.fromKilograms(75.0), 120),
                            ),
                        ),
                    ),
                ),
            ) as DomainResult.Success).data
            created.id
        }

        activityRule.activityRule.scenario.recreate()
            activityRule.waitUntil(5_000) {
                runCatching {
                    activityRule.onNodeWithText(PulseTab.WORKOUTS.label).assertExists()
                    true
                }.getOrDefault(false)
            }

            activityRule.onNodeWithText(PulseTab.WORKOUTS.label).performClick()
            activityRule.waitUntil(5_000) {
                runCatching {
                    activityRule.onNodeWithTag("workout_plan_card_$planId").assertExists()
                    true
                }.getOrDefault(false)
            }
            activityRule.onNodeWithTag("workout_plan_card_$planId").performClick()

            activityRule.waitUntil(5_000) {
                runCatching {
                    activityRule.onNodeWithTag("details_edit_plan_button").assertExists()
                    true
                }.getOrDefault(false)
            }
            activityRule.onNodeWithTag("details_edit_plan_button").performClick()

            activityRule.waitUntil(5_000) {
                runCatching {
                    activityRule.onNodeWithText("Edit Workout Plan").assertExists()
                    true
                }.getOrDefault(false)
            }
            activityRule.onNodeWithTag("editor_name_input").performTextReplacement("Mixed Set Plan Edited")

            // Recreate Activity -> unsaved Edit name & mixed per-set configuration survive
            activityRule.activityRule.scenario.recreate()
            activityRule.waitForIdle()

            activityRule.waitUntil(5_000) {
                runCatching {
                    activityRule.onNodeWithText("Edit Workout Plan").assertExists()
                    activityRule.onNodeWithText("Mixed Set Plan Edited").assertExists()
                    true
                }.getOrDefault(false)
            }
            activityRule.onNodeWithTag("editor_scroll_list")
                .performScrollToNode(hasText("Per-set targets (custom)"))
            activityRule.onNodeWithText("Barbell Bench Press").assertExists()
            activityRule.onNodeWithText("Per-set targets (custom)").assertExists()
    }
}
