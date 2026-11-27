package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.MainActivity
import com.ayasheta.pulsesmartgym.PulseApplication
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P7WorkoutSessionUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startFromDetails_nonSequentialLogCorrectCompleteAndSummaryBackHome() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PulseDataContainer.createIsolatedForTesting(context).use { isolated ->
            val container = isolated.container
            val (ownerId, planId, otherPlanId) = runBlocking {
                container.userPreferencesRepository.setOnboardingCompleted(true)
                val guest = (container.authRepository.continueAsGuest() as DomainResult.Success).data
                container.userProfileRepository.saveCompletedProfile(
                    ownerId = guest.id,
                    personalInfo = PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
                    goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
                )
                container.exerciseRepository.observeAll().first()
                val p1 = (container.workoutPlanRepository.createPlan(
                    guest.id,
                    WorkoutPlanDraft(
                        name = "Push Session",
                        exercises = listOf(
                            PlanExerciseDraft("ex_bench_press", 0, plannedSets = listOf(PlannedSetDraft(1, 10, WeightLoad.fromKilograms(80.0), 90))),
                            PlanExerciseDraft("ex_lateral_raise", 1, plannedSets = listOf(PlannedSetDraft(1, 12, WeightLoad.fromKilograms(12.0), 60))),
                        ),
                    ),
                ) as DomainResult.Success).data
                val p2 = (container.workoutPlanRepository.createPlan(
                    guest.id,
                    WorkoutPlanDraft(
                        name = "Pull Session",
                        exercises = listOf(PlanExerciseDraft("ex_barbell_row", 0, plannedSets = listOf(PlannedSetDraft(1, 8, null, 90)))),
                    ),
                ) as DomainResult.Success).data
                Triple(guest.id, p1.id, p2.id)
            }

            composeRule.setContent {
                PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.WORKOUTS,
                    dataContainer = container,
                )
            }

            // 1. Open Details and tap Start Workout
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("workout_plan_card_$planId").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("workout_plan_card_$planId").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("details_start_workout_button").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("details_start_workout_button").performClick()

            // 2. Verify Session Dashboard 0/2, bottom nav hidden, truthful connection status
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("0 / 2 Sets Completed").assertIsDisplayed()
            composeRule.onNodeWithText("Disconnected").assertIsDisplayed()
            composeRule.onNodeWithTag("pulse_bottom_bar").assertDoesNotExist()

            // 3. Header Back during unfinished workout opens End Workout Early confirmation without terminating
            composeRule.onNodeWithTag("session_back_button").performClick()
            composeRule.onNodeWithTag("modal_end_workout_early").assertIsDisplayed()
            composeRule.onNodeWithTag("button_keep_working_out").performClick()
            composeRule.onNodeWithTag("modal_end_workout_early").assertDoesNotExist()

            // Verify ActiveSessionConflict when attempting to start another plan while session is active
            val conflictResult = runBlocking {
                container.workoutSessionRepository.startSessionFromPlan(ownerId, otherPlanId)
            }
            org.junit.Assert.assertTrue(conflictResult is DomainResult.Failure)

            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
            }

            // 4. Non-sequential completion: record Exercise 2 Set 1 first, then Exercise 1 Set 1
            val activeSession = runBlocking { container.workoutSessionRepository.observeActiveSession(ownerId).first()!! }
            val ex1Id = activeSession.exercises[0].id
            val ex2Id = activeSession.exercises[1].id

            composeRule.onNodeWithTag("session_set_start_${ex2Id}_1").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("button_set_execution_primary").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("button_set_execution_primary").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
            }
            repeat(12) { composeRule.onNodeWithContentDescription("Add one rep").performClick() }
            composeRule.onNodeWithTag("button_set_execution_primary").performClick()

            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("1 / 2 Sets Completed").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("session_exercise_complete_$ex2Id", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

            // End Early modal -> Keep Working Out dismisses without ending
            composeRule.onNodeWithTag("button_end_workout").performClick()
            composeRule.onNodeWithTag("modal_end_workout_early").assertIsDisplayed()
            composeRule.onNodeWithTag("button_keep_working_out").performClick()
            composeRule.onNodeWithTag("modal_end_workout_early").assertDoesNotExist()

            // Record Exercise 1 Set 1
            composeRule.onNodeWithTag("session_set_start_${ex1Id}_1").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("button_set_execution_primary").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("button_set_execution_primary").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
            }
            repeat(10) { composeRule.onNodeWithContentDescription("Add one rep").performClick() }
            composeRule.onNodeWithTag("button_set_execution_primary").performClick()

            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("2 / 2 Sets Completed").assertExists(); true }.getOrDefault(false)
            }
            // Explicit correction on Ex1 Set 1
            composeRule.onNodeWithTag("session_set_edit_${ex1Id}_1").performClick()
            composeRule.onNodeWithTag("input_manual_full_reps").performTextReplacement("11")
            composeRule.onNodeWithTag("button_manual_set_confirm").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Set 1 • 11 reps done").assertExists(); true }.getOrDefault(false)
            }

            // Finish Workout -> Workout Complete Summary -> Back to Home
            composeRule.onNodeWithTag("button_finish_workout").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("summary_headline").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Workout Complete!").assertIsDisplayed()
            composeRule.onNodeWithTag("button_summary_back_home").performClick()

            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("home_scroll_list").assertExists(); true }.getOrDefault(false)
            }
        }
    }

    @Test
    fun endEarlyZeroSets_createsAbandonedAndLogoutPreservesActiveSessionForOwner() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PulseDataContainer.createIsolatedForTesting(context).use { isolated ->
            val container = isolated.container
            val (ownerId, planId) = runBlocking {
                container.userPreferencesRepository.setOnboardingCompleted(true)
                val acc = (container.authRepository.registerLocalAccount("User A", "a@pulse.local", "Password123!") as DomainResult.Success).data
                container.userProfileRepository.saveCompletedProfile(
                    ownerId = acc.id,
                    personalInfo = PersonalInfoDraft("User A", GenderIdentity.MALE, 25, 175, 70.0),
                    goals = FitnessGoalsDraft(FitnessGoal.STRENGTH, ExperienceLevel.INTERMEDIATE, 4),
                )
                container.exerciseRepository.observeAll().first()
                val p = (container.workoutPlanRepository.createPlan(
                    acc.id,
                    WorkoutPlanDraft("Squat Day", exercises = listOf(PlanExerciseDraft("ex_back_squat", 0, plannedSets = listOf(PlannedSetDraft(1, 5, null, 120))))),
                ) as DomainResult.Success).data
                acc.id to p.id
            }

            composeRule.setContent {
                PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.HOME,
                    dataContainer = container,
                )
            }

            // Start from Home CTA
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Start Workout").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Start Workout").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
            }

            // Sign out while session is ACTIVE -> session stays ACTIVE in Room for ownerId
            runBlocking { container.authRepository.signOut() }
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("session_signed_out").assertExists(); true }.getOrDefault(false)
            }
            assertNotNull(runBlocking { container.workoutSessionRepository.observeActiveSession(ownerId).first() })

            // Another owner has no active session
            val ownerB = runBlocking {
                (container.authRepository.registerLocalAccount("User B", "b@pulse.local", "Password123!") as DomainResult.Success).data
            }
            assertNull(runBlocking { container.workoutSessionRepository.observeActiveSession(ownerB.id).first() })

            // Original owner signs back in -> active session resumes and can be ended early with 0 sets (ABANDONED)
            runBlocking { container.authRepository.signInWithEmail("a@pulse.local", "Password123!", false) }
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("button_end_workout").assertExists(); true }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("button_end_workout").performClick()
            composeRule.onNodeWithTag("button_confirm_end_workout").performClick()
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("Workout Abandoned").assertExists(); true }.getOrDefault(false)
            }
            val saved = runBlocking { container.workoutSessionRepository.observeActiveSession(ownerId).first() }
            assertNull(saved)
        }
    }

    @Test
    fun resumeActiveSessionFromHome_whenSourcePlanDeletedAndAcrossSignOutSignIn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PulseDataContainer.createIsolatedForTesting(context).use { isolated ->
            val container = isolated.container
            val (ownerId, planId, sessionId) = runBlocking {
                container.userPreferencesRepository.setOnboardingCompleted(true)
                val acc = (container.authRepository.registerLocalAccount("Alex", "resume@pulse.local", "Password123!") as DomainResult.Success).data
                container.userProfileRepository.saveCompletedProfile(
                    ownerId = acc.id,
                    personalInfo = PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
                    goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
                )
                container.exerciseRepository.observeAll().first()
                val plan = (container.workoutPlanRepository.createPlan(
                    acc.id,
                    WorkoutPlanDraft(
                        name = "Deleted Source Plan",
                        exercises = listOf(
                            PlanExerciseDraft("ex_bench_press", 0, plannedSets = (1..2).map { PlannedSetDraft(it, 10, WeightLoad.fromKilograms(80.0), 90) }),
                        ),
                    ),
                ) as DomainResult.Success).data
                val started = (container.workoutSessionRepository.startSessionFromPlan(acc.id, plan.id) as DomainResult.Success).data
                container.workoutSessionRepository.recordPerformedSet(
                    acc.id,
                    com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest(
                        sessionId = started.id,
                        sessionExerciseId = started.exercises.first().id,
                        setIndex = 1,
                        fullReps = 10,
                        partialReps = 0,
                        actualLoad = WeightLoad.fromKilograms(80.0),
                    ),
                )
                // Delete original plan while session remains ACTIVE
                container.workoutPlanRepository.deletePlan(acc.id, plan.id)
                Triple(acc.id, plan.id, started.id)
            }

            composeRule.setContent {
                PulseApp(
                    isDebugEntryEnabled = true,
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.HOME,
                    dataContainer = container,
                )
            }

            // Home must still offer Resume Workout using planNameSnapshot even though source plan was deleted
            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("Resume Workout").assertExists()
                    composeRule.onNodeWithText("Deleted Source Plan").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Resume Workout").performClick()

            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("1 / 2 Sets Completed").assertExists()
                    composeRule.onNodeWithText("Set 1 • 10 reps done").assertExists()
                    true
                }.getOrDefault(false)
            }
            val currentSession = runBlocking { container.workoutSessionRepository.observeActiveSession(ownerId).first()!! }
            assertEquals(sessionId, currentSession.id)
            assertNull(currentSession.sourcePlanId)

            // Sign out & sign back in -> Home still offers Resume Workout for original sessionId
            runBlocking { container.authRepository.signOut() }
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithTag("session_signed_out").assertExists(); true }.getOrDefault(false)
            }
            runBlocking { container.authRepository.signInWithEmail("resume@pulse.local", "Password123!", false) }
            composeRule.waitUntil(5_000) {
                runCatching { composeRule.onNodeWithText("1 / 2 Sets Completed").assertExists(); true }.getOrDefault(false)
            }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class P7SessionRecreationUiTest {

    private class IsolatedRule : TestWatcher() {
        private var isolated: PulseDataContainer.IsolatedTestContainer? = null
        val container: PulseDataContainer get() = isolated!!.container
        var planId: String = ""

        override fun starting(description: Description) {
            val app = ApplicationProvider.getApplicationContext<PulseApplication>()
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
                created.container.exerciseRepository.observeAll().first()
                val plan = (created.container.workoutPlanRepository.createPlan(
                    guest.id,
                    WorkoutPlanDraft(
                        name = "Recreation Split",
                        exercises = listOf(
                            PlanExerciseDraft("ex_bench_press", 0, plannedSets = (1..2).map { PlannedSetDraft(it, 10, WeightLoad.fromKilograms(80.0), 90) }),
                        ),
                    ),
                ) as DomainResult.Success).data
                planId = plan.id
            }
        }

        override fun finished(description: Description) {
            val app = ApplicationProvider.getApplicationContext<PulseApplication>()
            app.setTestDataContainerOverride(null)
            isolated?.close()
            isolated = null
        }
    }

    private val isolatedRule = IsolatedRule()
    private val activityRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(isolatedRule).around(activityRule)

    @Test
    fun activeSessionProgress_survivesActivityRecreation() {
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Start Workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("Start Workout").performClick()

        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
        }
        val active = runBlocking {
            val auth = isolatedRule.container.authRepository.restoreSession() as com.ayasheta.pulsesmartgym.core.model.profile.AuthState.Authenticated
            isolatedRule.container.workoutSessionRepository.observeActiveSession(auth.user.id).first()!!
        }
        val exId = active.exercises.first().id
        activityRule.onNodeWithTag("session_set_start_${exId}_1").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_set_execution_primary").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_set_execution_primary").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
        }
        repeat(10) {
            activityRule.onNodeWithContentDescription("Add one rep").performClick()
        }
        activityRule.onNodeWithTag("button_set_execution_primary").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("1 / 2 Sets Completed").assertExists(); true }.getOrDefault(false)
        }

        // Trigger Activity recreation
        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()

        activityRule.waitUntil(5_000) {
            runCatching {
                activityRule.onNodeWithText("Recreation Split").assertExists()
                activityRule.onNodeWithText("1 / 2 Sets Completed").assertExists()
                activityRule.onNodeWithText("Set 1 • 10 reps done").assertExists()
                true
            }.getOrDefault(false)
        }
    }
}
