package com.ayasheta.pulsesmartgym.navigation

import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.navigation.route.AuthEntry
import com.ayasheta.pulsesmartgym.navigation.route.CreateWorkoutPlan
import com.ayasheta.pulsesmartgym.navigation.route.DeviceDiscovery
import com.ayasheta.pulsesmartgym.navigation.route.EditWorkoutPlan
import com.ayasheta.pulsesmartgym.navigation.route.ExercisePicker
import com.ayasheta.pulsesmartgym.navigation.route.ForgotPassword
import com.ayasheta.pulsesmartgym.navigation.route.HistoryTab
import com.ayasheta.pulsesmartgym.navigation.route.HomeTab
import com.ayasheta.pulsesmartgym.navigation.route.Login
import com.ayasheta.pulsesmartgym.navigation.route.ManualRepCounter
import com.ayasheta.pulsesmartgym.navigation.route.MyDevice
import com.ayasheta.pulsesmartgym.navigation.route.ProfileTab
import com.ayasheta.pulsesmartgym.navigation.route.ProgressTab
import com.ayasheta.pulsesmartgym.navigation.route.ReadyStartSet
import com.ayasheta.pulsesmartgym.navigation.route.SignUp
import com.ayasheta.pulsesmartgym.navigation.route.Splash
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutComplete
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutDetails
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutSession
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutsTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseNavigatorTest {

    @Test
    fun authFlow_splashToLoginAndForgotPasswordBack_worksCorrectly() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.AUTH)
        val navigator = PulseNavigator(state)

        assertEquals(Splash, state.currentDestination)
        navigator.navigate(AuthEntry)
        navigator.navigate(Login)
        navigator.navigate(ForgotPassword)
        assertEquals(ForgotPassword, state.currentDestination)

        assertTrue(navigator.navigateBack())
        assertEquals(Login, state.currentDestination)

        navigator.navigateBack()
        assertEquals(AuthEntry, state.currentDestination)

        navigator.navigate(SignUp)
        assertEquals(SignUp, state.currentDestination)
    }

    @Test
    fun completeAuthAndEnterMain_switchesFlowAndClearsAuthStack() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.AUTH)
        val navigator = PulseNavigator(state)
        navigator.navigate(AuthEntry)
        navigator.navigate(Login)

        navigator.completeAuthAndEnterMain()

        assertEquals(AppEntryFlow.MAIN, state.activeFlow)
        assertEquals(PulseTab.HOME, state.selectedTab)
        assertEquals(HomeTab, state.currentDestination)
        assertEquals(1, state.authBackStack.size)
    }

    @Test
    fun mainTabs_repeatedSwitching_doesNotAccumulateDuplicateRoots() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.MAIN)
        val navigator = PulseNavigator(state)

        val tabs = listOf(
            PulseTab.WORKOUTS to WorkoutsTab,
            PulseTab.PROGRESS to ProgressTab,
            PulseTab.HISTORY to HistoryTab,
            PulseTab.PROFILE to ProfileTab,
            PulseTab.HOME to HomeTab,
        )

        repeat(3) {
            tabs.forEach { (tab, expectedRoute) ->
                navigator.selectTab(tab)
                assertEquals(tab, state.selectedTab)
                assertEquals(expectedRoute, state.currentDestination)
                assertEquals(1, state.activeBackStack.size)
            }
        }
    }

    @Test
    fun multipleBackStacks_preservesNestedDestinationAcrossTabSwitch() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.MAIN)
        val navigator = PulseNavigator(state)

        navigator.selectTab(PulseTab.WORKOUTS)
        navigator.openWorkoutDetails("plan_42")
        navigator.navigate(EditWorkoutPlan("plan_42"))
        assertEquals(EditWorkoutPlan("plan_42"), state.currentDestination)
        assertFalse(state.showBottomBar)

        // Switch to History and back to Workouts
        navigator.selectTab(PulseTab.HISTORY)
        assertEquals(HistoryTab, state.currentDestination)
        assertTrue(state.showBottomBar)

        navigator.selectTab(PulseTab.WORKOUTS)
        assertEquals(EditWorkoutPlan("plan_42"), state.currentDestination)
        assertEquals(3, state.activeBackStack.size)
    }

    @Test
    fun systemBackPolicy_popsNestedThenReturnsToHomeThenExitsAtHomeRoot() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.MAIN)
        val navigator = PulseNavigator(state)

        navigator.selectTab(PulseTab.WORKOUTS)
        navigator.navigate(CreateWorkoutPlan)
        navigator.navigate(ExercisePicker(planId = null))

        assertTrue(navigator.navigateBack())
        assertEquals(CreateWorkoutPlan, state.currentDestination)

        assertTrue(navigator.navigateBack())
        assertEquals(WorkoutsTab, state.currentDestination)

        // At non-Home tab root -> returns to HomeTab
        assertTrue(navigator.navigateBack())
        assertEquals(PulseTab.HOME, state.selectedTab)
        assertEquals(HomeTab, state.currentDestination)

        // At HomeTab root -> allows system exit
        assertFalse(navigator.navigateBack())
    }

    @Test
    fun workoutJourney_preservesRouteArgumentsAndClearsLiveStackOnComplete() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.MAIN)
        val navigator = PulseNavigator(state)

        navigator.openWorkoutDetails("plan_10")
        navigator.startWorkoutSession(planId = "plan_10", sessionId = "session_99")
        navigator.navigate(ReadyStartSet(sessionId = "session_99", exerciseId = "ex_3", setIndex = 2))
        navigator.navigate(ManualRepCounter(sessionId = "session_99", exerciseId = "ex_3", setIndex = 2))

        val top = state.currentDestination as ManualRepCounter
        assertEquals("session_99", top.sessionId)
        assertEquals("ex_3", top.exerciseId)
        assertEquals(2, top.setIndex)

        navigator.completeWorkoutFlow(WorkoutComplete(sessionId = "session_99"))
        assertEquals(WorkoutComplete(sessionId = "session_99"), state.currentDestination)

        // Back from WorkoutComplete returns to WorkoutDetails, not back into live counter
        assertTrue(navigator.navigateBack())
        assertEquals(WorkoutDetails("plan_10"), state.currentDestination)
    }

    @Test
    fun hardwareJourney_navigatesAndPopsCleanly() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.MAIN)
        val navigator = PulseNavigator(state)

        navigator.navigate(MyDevice)
        navigator.navigate(DeviceDiscovery)
        assertEquals(DeviceDiscovery, state.currentDestination)
        assertFalse(state.showBottomBar)

        assertTrue(navigator.navigateBack())
        assertEquals(MyDevice, state.currentDestination)

        assertTrue(navigator.navigateBack())
        assertEquals(HomeTab, state.currentDestination)
        assertTrue(state.showBottomBar)
    }

    @Test
    fun doubleTapNavigate_preventsDuplicateConsecutiveRoutes() {
        val state = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.MAIN)
        val navigator = PulseNavigator(state)

        navigator.openWorkoutDetails("plan_1")
        navigator.openWorkoutDetails("plan_1")

        assertEquals(2, state.activeBackStack.size)
        assertEquals(WorkoutDetails("plan_1"), state.currentDestination)
    }

    @Test
    fun debugAuthBypass_isBlockedWhenDebugDisabledAndAllowedWhenEnabled() {
        val releaseState = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.AUTH)
        val releaseNavigator = PulseNavigator(releaseState)

        assertFalse(releaseNavigator.completeDebugAuthBypass(isDebugEntryEnabled = false))
        assertEquals(AppEntryFlow.AUTH, releaseState.activeFlow)
        assertEquals(Splash, releaseState.currentDestination)

        val debugState = PulseNavigationState.createForTesting(initialFlow = AppEntryFlow.AUTH)
        val debugNavigator = PulseNavigator(debugState)

        assertTrue(debugNavigator.completeDebugAuthBypass(isDebugEntryEnabled = true))
        assertEquals(AppEntryFlow.MAIN, debugState.activeFlow)
        assertEquals(HomeTab, debugState.currentDestination)
    }

    @Test
    fun bottomBarPolicy_onlyVisibleOnFiveRootTabs() {
        assertTrue(NavigationPolicy.shouldShowBottomBar(HomeTab))
        assertTrue(NavigationPolicy.shouldShowBottomBar(WorkoutsTab))
        assertTrue(NavigationPolicy.shouldShowBottomBar(ProgressTab))
        assertTrue(NavigationPolicy.shouldShowBottomBar(HistoryTab))
        assertTrue(NavigationPolicy.shouldShowBottomBar(ProfileTab))

        assertFalse(NavigationPolicy.shouldShowBottomBar(Splash))
        assertFalse(NavigationPolicy.shouldShowBottomBar(Login))
        assertFalse(NavigationPolicy.shouldShowBottomBar(WorkoutDetails("1")))
        assertFalse(NavigationPolicy.shouldShowBottomBar(WorkoutSession("1")))
        assertFalse(NavigationPolicy.shouldShowBottomBar(MyDevice))
    }
}
