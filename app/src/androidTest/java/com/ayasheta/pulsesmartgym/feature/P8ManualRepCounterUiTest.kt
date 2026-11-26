package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.MainActivity
import com.ayasheta.pulsesmartgym.PulseApplication
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P8ManualRepCounterUiTest {

    private val isolatedRule = P8IsolatedContainerRule()
    private val activityRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(isolatedRule).around(activityRule)

    @Test
    fun fullP8Flow_readyStartSet_manualRepCounter_restTimer_recreation_andCompletion() {
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Start Workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("Start Workout").performClick()

        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
        }

        val ownerId = runBlocking {
            (isolatedRule.container.authRepository.restoreSession() as AuthState.Authenticated).user.id
        }
        val activeSession = runBlocking {
            isolatedRule.container.workoutSessionRepository.observeActiveSession(ownerId).first()!!
        }
        val ex1Id = activeSession.exercises.first().id

        // 1. Start Set 1 -> ReadyStartSet
        activityRule.onNodeWithTag("session_set_start_${ex1Id}_1").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("ready_start_set_screen").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("Barbell Bench Press").assertIsDisplayed()
        activityRule.onNodeWithText("Set 1 of 2 • Ready").assertIsDisplayed()
        activityRule.onNodeWithTag("ready_rest_info").assertIsDisplayed()

        // 2. Start Set -> ManualRepCounter (starts at 0)
        activityRule.onNodeWithTag("button_ready_start_set").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("manual_rep_counter_screen").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("0").assertIsDisplayed()

        // Increment reps to 8 and adjust weight, then recreate Activity -> unsaved reps & weight survive!
        repeat(9) { activityRule.onNodeWithContentDescription("Add one rep").performClick() }
        activityRule.onNodeWithContentDescription("Subtract one rep").performClick()
        activityRule.onNodeWithContentDescription("Increase weight").performClick()
        activityRule.onNodeWithText("8").assertIsDisplayed()

        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("manual_rep_counter_screen").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("8").assertIsDisplayed()
        activityRule.onNodeWithText("82.5").assertIsDisplayed()

        // 3. Finish Set -> returns to WorkoutSession with Rest Timer visible
        activityRule.onNodeWithTag("button_finish_set").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("session_rest_timer_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("1 / 2 Sets Completed").assertIsDisplayed()
        activityRule.onNodeWithText("Rest after Barbell Bench Press").assertIsDisplayed()

        // 4. Extend rest +30s and recreate Activity -> Rest Timer survives!
        activityRule.onNodeWithText("+30s").performClick()
        activityRule.waitUntil(5_000) {
            runBlocking {
                isolatedRule.container.workoutSessionRepository.observeRestState(ownerId, activeSession.id).first()?.durationSeconds == 120
            }
        }
        val persistedRest = runBlocking {
            isolatedRule.container.workoutSessionRepository.observeRestState(ownerId, activeSession.id).first()
        }
        assertNotNull(persistedRest)
        assertEquals(120, persistedRest!!.durationSeconds)

        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("session_rest_timer_card").assertExists(); true }.getOrDefault(false)
        }

        // 5. Skip Rest -> stays on WorkoutSession dashboard; user freely starts Set 2
        activityRule.onNodeWithText("Skip Rest").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("session_rest_timer_card").assertDoesNotExist(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("session_set_start_${ex1Id}_2").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("ready_start_set_screen").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("Set 2 of 2 • Ready").assertIsDisplayed()

        // Complete Set 2
        activityRule.onNodeWithTag("button_ready_start_set").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_finish_set").assertExists(); true }.getOrDefault(false)
        }
        repeat(10) { activityRule.onNodeWithContentDescription("Add one rep").performClick() }
        activityRule.onNodeWithTag("button_finish_set").performClick()

        // All sets complete -> back on WorkoutSession -> Finish Workout -> WorkoutComplete
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("2 / 2 Sets Completed").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_finish_workout").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Workout Complete!").assertExists(); true }.getOrDefault(false)
        }
    }
}

class P8IsolatedContainerRule : ExternalResource() {
    private var isolated: PulseDataContainer.IsolatedTestContainer? = null
    val container: PulseDataContainer get() = isolated!!.container

    override fun before() {
        val app = ApplicationProvider.getApplicationContext<PulseApplication>()
        val created = PulseDataContainer.createIsolatedForTesting(app)
        isolated = created
        app.setTestDataContainerOverride(created.container)
        runBlocking {
            val user = (created.container.authRepository.continueAsGuest() as DomainResult.Success).data
            created.container.userProfileRepository.saveCompletedProfile(
                ownerId = user.id,
                personalInfo = PersonalInfoDraft("Aya Sheta", GenderIdentity.FEMALE, 25, 168, 62.0, WeightUnit.KG),
                goals = FitnessGoalsDraft(FitnessGoal.STRENGTH, ExperienceLevel.INTERMEDIATE, 4),
            )
            created.container.userPreferencesRepository.setOnboardingCompleted(true)
            val bench = created.container.exerciseRepository.observeAll().first().first { it.name.contains("Bench", ignoreCase = true) }
            created.container.workoutPlanRepository.createPlan(
                ownerId = user.id,
                draft = WorkoutPlanDraft(
                    name = "Upper Body Power",
                    description = "P8 test split",
                    exercises = listOf(
                        PlanExerciseDraft(
                            exerciseId = bench.id,
                            orderIndex = 0,
                            plannedSets = listOf(
                                PlannedSetDraft(1, 10, WeightLoad.fromKilograms(80.0), 90),
                                PlannedSetDraft(2, 10, WeightLoad.fromKilograms(80.0), 90),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    override fun after() {
        val app = ApplicationProvider.getApplicationContext<PulseApplication>()
        app.setTestDataContainerOverride(null)
        isolated?.close()
        isolated = null
    }
}
