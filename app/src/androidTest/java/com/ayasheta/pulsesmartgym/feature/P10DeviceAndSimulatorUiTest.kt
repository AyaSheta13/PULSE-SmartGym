package com.ayasheta.pulsesmartgym.feature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.MainActivity
import com.ayasheta.pulsesmartgym.PulseApplication
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.PulseRepCounterGauge
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P10DeviceAndSimulatorUiTest {

    private val isolatedRule = P10IsolatedContainerRule()
    private val activityRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(isolatedRule).around(activityRule)

    @Test
    fun deviceSimulationE2E_connect_autoReps_disconnectRecovery_manualFallback_andGenuineHistoryUnchanged() {
        val genuineContainer = isolatedRule.container
        val ownerId = runBlocking {
            (genuineContainer.authRepository.restoreSession() as AuthState.Authenticated).user.id
        }

        // 1. Open My Device from Home Bluetooth chip -> Search & Connect -> Available Devices -> Connect PULSE-001
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("THIS WEEK'S PROGRESS").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("home_bluetooth_chip").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_search_connect_device").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_search_connect_device").performScrollTo().performClick()

        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_connect_sim_pulse_001").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_connect_sim_pulse_001").performClick()

        // 2. Connected Success -> View in Device Center -> Verify Connected Demo State
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("connected_success_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_view_in_device_center").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("my_device_connected_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("PULSE-001 Connected (Simulated)").assertIsDisplayed()

        // Activity recreation preserves connected state
        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("my_device_connected_card").assertExists(); true }.getOrDefault(false)
        }

        // 3. Start Workout with Auto-Tracking -> Ready/Start Set -> Device-Assisted Counter
        activityRule.onNodeWithTag("button_start_workout_auto_tracking").performScrollTo().performClick()
        activityRule.waitUntil(10_000) {
            runCatching { activityRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_set_execution_primary").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_set_execution_primary").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
        }

        // 4. Emit 8 simulated reps -> simulate connection loss -> recovery sheet -> Continue Manually -> add 2 -> Finish Set
        repeat(8) {
            activityRule.onNodeWithTag("button_sim_full_rep").performScrollTo().performClick()
        }
        activityRule.onNodeWithText("8").assertIsDisplayed()

        activityRule.onNodeWithTag("button_sim_drop_ble").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("connection_lost_recovery_sheet").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_recovery_continue_manually").performClick()

        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("8").assertIsDisplayed()
        activityRule.onNodeWithContentDescription("Add one rep").performClick()
        activityRule.onNodeWithContentDescription("Add one rep").performClick()
        activityRule.onAllNodesWithText("10").onFirst().assertIsDisplayed()
        activityRule.onNodeWithTag("button_set_execution_primary").performScrollTo().performClick()

        // 5. Rest timer -> Skip Rest -> End Workout Early -> Summary -> Back to Home (exits simulation)
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Skip Rest").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("Skip Rest").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_end_workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_end_workout").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_confirm_end_workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_confirm_end_workout").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("summary_headline").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onAllNodesWithText("10").onFirst().assertIsDisplayed()
        activityRule.onNodeWithTag("button_summary_back_home").performScrollTo().performClick()

        // 6. Genuine History and Progress remain 100% uncontaminated!
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText(PulseTab.HISTORY.label).assertExists(); true }.getOrDefault(false)
        }
        val genuineHistory = runBlocking { genuineContainer.workoutHistoryRepository.observeHistory(ownerId).first() }
        assertTrue(genuineHistory.isEmpty())
        activityRule.onNodeWithText(PulseTab.HISTORY.label).performClick()
        activityRule.onNodeWithText("No Workouts Recorded Yet").assertIsDisplayed()
    }

    @Test
    fun discoveryErrorFlows_noDevices_handshakeTimeoutRetry_andDisconnect() {
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("THIS WEEK'S PROGRESS").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("home_bluetooth_chip").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_search_connect_device").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_search_connect_device").performScrollTo().performClick()

        // Simulate Handshake Timeout -> Connection Failed -> Retry -> Connected
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_connect_sim_pulse_001").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_connect_sim_pulse_001").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_simulate_handshake_timeout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_simulate_handshake_timeout").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Connection Failed").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_retry_connection").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("connected_success_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_view_in_device_center").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_disconnect_device").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_disconnect_device").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("No Device Connected").assertExists(); true }.getOrDefault(false)
        }
        assertEquals(DeviceConnectionState.DISCONNECTED, isolatedRule.container.deviceRepository.linkState.value.phase)
    }

    @Test
    fun genuineActiveWorkoutHistoryAndPrs_surviveDemoRecreationManualFallbackAndExit() {
        val genuine = isolatedRule.container
        val ownerId = runBlocking {
            (genuine.authRepository.restoreSession() as AuthState.Authenticated).user.id
        }
        // Seed genuine History + PR + Active Session BEFORE entering simulation
        val activeGenuineSessionId = runBlocking {
            val bench = genuine.exerciseRepository.observeAll().first().first { it.name.contains("Bench", ignoreCase = true) }
            val plan = (genuine.workoutPlanRepository.createPlan(
                ownerId,
                com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft(
                    name = "Genuine Strength Plan",
                    exercises = listOf(
                        com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft(
                            bench.id,
                            0,
                            plannedSets = listOf(
                                com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft(1, 5, WeightLoad.fromKilograms(100.0), 60),
                            ),
                        ),
                    ),
                ),
            ) as DomainResult.Success).data
            val hist = (genuine.workoutSessionRepository.startSessionFromPlan(ownerId, plan.id) as DomainResult.Success).data
            genuine.workoutSessionRepository.recordPerformedSet(
                ownerId,
                com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest(hist.id, hist.exercises[0].id, 1, 5, 0, WeightLoad.fromKilograms(100.0)),
            )
            genuine.workoutSessionRepository.completeSession(ownerId, hist.id)

            // Now start an active genuine session
            val active = (genuine.workoutSessionRepository.startSessionFromPlan(ownerId, plan.id) as DomainResult.Success).data
            active.id
        }

        // Enter simulation -> connect PULSE-001 -> start demo workout -> recreate during unfinished demo set -> manual fallback -> recreate -> finish -> exit
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("THIS WEEK'S PROGRESS").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("home_bluetooth_chip").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_search_connect_device").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_search_connect_device").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_connect_sim_pulse_001").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_connect_sim_pulse_001").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("connected_success_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_view_in_device_center").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_start_workout_auto_tracking").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_start_workout_auto_tracking").performScrollTo().performClick()
        activityRule.waitUntil(10_000) {
            runCatching { activityRule.onNodeWithTag("session_progress_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_set_execution_primary").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_set_execution_primary").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
        }
        repeat(4) {
            activityRule.onNodeWithTag("button_sim_full_rep").performScrollTo().performClick()
        }
        // Recreate Activity during unfinished demo set
        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("set_execution_counter_card").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("4").performScrollTo().assertIsDisplayed()

        // Drop link -> Continue Manually -> recreate during manual fallback -> finish -> back to home
        activityRule.onNodeWithTag("button_sim_drop_ble").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_recovery_continue_manually").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_recovery_continue_manually").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("4").assertIsDisplayed()
        activityRule.activityRule.scenario.recreate()
        activityRule.waitForIdle()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Finish Set").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("4").performScrollTo().assertIsDisplayed()
        activityRule.onNodeWithTag("button_set_execution_primary").performScrollTo().performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithText("Skip Rest").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithText("Skip Rest").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_end_workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_end_workout").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_confirm_end_workout").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_confirm_end_workout").performClick()
        activityRule.waitUntil(5_000) {
            runCatching { activityRule.onNodeWithTag("button_summary_back_home").assertExists(); true }.getOrDefault(false)
        }
        activityRule.onNodeWithTag("button_summary_back_home").performScrollTo().performClick()

        // Verify genuine active session, History (1 session), and PR (100 kg) remain completely intact!
        val activeAfter = runBlocking { genuine.workoutSessionRepository.observeActiveSession(ownerId).first() }
        assertEquals(activeGenuineSessionId, activeAfter?.id)
        val historyAfter = runBlocking { genuine.workoutHistoryRepository.observeHistory(ownerId).first() }
        assertEquals(1, historyAfter.size)
        val prsAfter = runBlocking { genuine.workoutHistoryRepository.observePersonalRecords(ownerId).first() }
        assertEquals(1, prsAfter.size)
        assertEquals(100.0, prsAfter.first().maxLoad.toKilograms(), 0.01)
    }

    @Test
    fun demoEnvironmentController_handlesSlowInitFailureAccountSwitchAndRapidExitReentry() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<PulseApplication>()
        val authFlow = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(
            AuthState.Authenticated(com.ayasheta.pulsesmartgym.core.model.profile.AuthUser("user_a", "A", "a@p.fit", false, false)),
        )
        val fakeAuth = object : com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository {
            override fun observeAuthState() = authFlow
            override suspend fun restoreSession() = authFlow.value
            override suspend fun continueAsGuest() = throw UnsupportedOperationException()
            override suspend fun registerLocalAccount(fullName: String, email: String, password: String) = throw UnsupportedOperationException()
            override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean) = throw UnsupportedOperationException()
            override suspend fun resetPassword(newPassword: String) = throw UnsupportedOperationException()
            override suspend fun signInDemo(displayName: String, email: String) = throw UnsupportedOperationException()
            override suspend fun signOut() { authFlow.value = AuthState.Unauthenticated }
        }
        val deviceRepo = com.ayasheta.pulsesmartgym.core.data.device.DefaultDeviceRepository()
        val draftStore = com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftStore()

        val initGate = kotlinx.coroutines.CompletableDeferred<Unit>()
        var shouldFail = false
        val controller = com.ayasheta.pulsesmartgym.feature.device.env.DemoEnvironmentController(
            appContext = context,
            authRepository = fakeAuth,
            deviceRepository = deviceRepo,
            setDraftStore = draftStore,
            isolatedFactory = { ctx ->
                initGate.await()
                if (shouldFail) throw IllegalStateException("Disk I/O error")
                PulseDataContainer.createIsolatedForTesting(ctx)
            },
        )

        // 1. Slow initialization stays in DemoInitializing
        deviceRepo.setDemoSessionActive(true)
        var waited = 0
        while (controller.state.value !is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.DemoInitializing && waited < 50) {
            kotlinx.coroutines.delay(10)
            waited++
        }
        assertTrue(controller.state.value is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.DemoInitializing)

        // 2. Rapid exit cancels init and returns to Genuine
        deviceRepo.setDemoSessionActive(false)
        waited = 0
        while (controller.state.value !is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.Genuine && waited < 50) {
            kotlinx.coroutines.delay(10)
            waited++
        }
        assertTrue(controller.state.value is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.Genuine)

        // 3. Init failure transitions to DemoFailed (never Genuine)
        shouldFail = true
        initGate.complete(Unit)
        deviceRepo.setDemoSessionActive(true)
        waited = 0
        while (controller.state.value !is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.DemoFailed && waited < 50) {
            kotlinx.coroutines.delay(10)
            waited++
        }
        assertTrue(controller.state.value is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.DemoFailed)

        // 4. Reentry with healthy factory -> DemoReady; account switch -> closes & resets to Genuine
        deviceRepo.setDemoSessionActive(false)
        waited = 0
        while (controller.state.value !is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.Genuine && waited < 50) {
            kotlinx.coroutines.delay(10)
            waited++
        }
        shouldFail = false
        deviceRepo.setDemoSessionActive(true)
        waited = 0
        while (controller.state.value !is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.DemoReady && waited < 100) {
            kotlinx.coroutines.delay(20)
            waited++
        }
        assertTrue(controller.state.value is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.DemoReady)

        authFlow.value = AuthState.Authenticated(com.ayasheta.pulsesmartgym.core.model.profile.AuthUser("user_b", "B", "b@p.fit", false, false))
        waited = 0
        while (controller.state.value !is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.Genuine && waited < 50) {
            kotlinx.coroutines.delay(10)
            waited++
        }
        assertTrue(controller.state.value is com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState.Genuine)
    }
}

@RunWith(AndroidJUnit4::class)
class P10GaugeAnimationDeterministicTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun repCounterGauge_animatesProgressSmoothlyAcrossManualClockSteps() {
        var reps by mutableIntStateOf(0)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PulseTheme {
                PulseRepCounterGauge(
                    currentReps = reps,
                    targetReps = 10,
                    onDecrementRep = {},
                    onIncrementRep = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        reps = 8
        composeRule.mainClock.advanceTimeBy(160L)
        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(200L)
        composeRule.onNodeWithText("8").assertIsDisplayed()
    }
}

class P10IsolatedContainerRule : ExternalResource() {
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
        }
    }

    override fun after() {
        val app = ApplicationProvider.getApplicationContext<PulseApplication>()
        app.setTestDataContainerOverride(null)
        isolated?.close()
        isolated = null
    }
}
