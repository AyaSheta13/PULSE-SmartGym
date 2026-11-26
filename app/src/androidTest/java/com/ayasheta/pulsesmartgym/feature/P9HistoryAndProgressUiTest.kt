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
import androidx.compose.ui.test.performScrollTo
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P9HistoryAndProgressUiTest {

    private val isolatedRule = P9IsolatedContainerRule()
    private val activityRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(isolatedRule).around(activityRule)

    @Test
    fun p7p8ToP9EndToEnd_emptyStates_completeWorkout_viewHistory_details_progress_andRepeat() {
        // 1. Empty History & Empty Progress states
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText(PulseTab.HISTORY.label).assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText(PulseTab.HISTORY.label).performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("history_empty_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("0 Saved").assertIsDisplayed()

        activityRule.onNodeWithText(PulseTab.PROGRESS.label).performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("progress_empty_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_progress_start_workout").performClick()

        // 2. Start Workout -> ReadyStartSet -> ManualRepCounter -> Finish Set -> Finish Workout
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Upper Body Power").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText(PulseTab.HOME.label).performClick()
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
        val exId = activeSession.exercises.first().id

        activityRule.onNodeWithTag("session_set_start_${exId}_1").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_ready_start_set").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_ready_start_set").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_finish_set").assertExists(); true }.getOrDefault(false)
        }
        repeat(8) { activityRule.onNodeWithContentDescription("Add one rep").performClick() }
        activityRule.onNodeWithContentDescription("Increase weight").performClick()
        activityRule.onNodeWithTag("button_finish_set").performClick()

        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_finish_workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_finish_workout").performClick()

        // 3. Summary -> View History -> Populated History
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_summary_view_history").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_summary_view_history").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("history_aggregate_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("1 saved session").assertIsDisplayed()

        // 4. Open Session Details -> expand recorded set -> Repeat This Workout
        activityRule.onNodeWithTag("history_session_card_${activeSession.id}").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("session_details_breakdown_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("details_exercise_row_$exId").performScrollTo().performClick()
        activityRule.onNodeWithTag("details_exercise_sets_$exId", useUnmergedTree = true).assertIsDisplayed()

        // Check Training Progress first by navigating back -> Progress tab -> Activity recreate
        activityRule.onNodeWithTag("session_details_back_button").performClick()
        activityRule.onNodeWithText(PulseTab.PROGRESS.label).performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("progress_pr_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("82.5 kg").performScrollTo().assertIsDisplayed()
        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("progress_pr_card").assertExists(); true }.getOrDefault(false)
        }

        // Open Session Details again -> Repeat This Workout -> fresh ACTIVE session
        activityRule.onNodeWithText(PulseTab.HISTORY.label).performClick()
        activityRule.onNodeWithTag("history_session_card_${activeSession.id}").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_repeat_workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_repeat_workout").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("0 / 1 Sets Completed").assertIsDisplayed()
        val newActive = runBlocking {
            isolatedRule.container.workoutSessionRepository.observeActiveSession(ownerId).first()!!
        }
        assertEquals(0, newActive.exercises.sumOf { it.performedSets.size })
    }
}

class P9IsolatedContainerRule : ExternalResource() {
    lateinit var container: PulseDataContainer
        private set
    private var isolated: PulseDataContainer.IsolatedTestContainer? = null

    override fun before() {
        val app = ApplicationProvider.getApplicationContext<PulseApplication>()
        val created = PulseDataContainer.createIsolatedForTesting(app)
        isolated = created
        container = created.container
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
                    description = "P9 E2E plan",
                    exercises = listOf(
                        PlanExerciseDraft(bench.id, 0, plannedSets = listOf(PlannedSetDraft(1, 10, WeightLoad.fromKilograms(80.0), 0))),
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
