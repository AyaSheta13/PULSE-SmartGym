package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.navigation.route.AuthEntry
import com.ayasheta.pulsesmartgym.navigation.route.CreateWorkoutPlan
import com.ayasheta.pulsesmartgym.navigation.route.DeviceAssistedRepCounter
import com.ayasheta.pulsesmartgym.navigation.route.EditWorkoutPlan
import com.ayasheta.pulsesmartgym.navigation.route.ExercisePicker
import com.ayasheta.pulsesmartgym.navigation.route.ManualRepCounter
import com.ayasheta.pulsesmartgym.navigation.route.ReadyStartSet
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutDetails
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutSession
import com.ayasheta.pulsesmartgym.navigation.route.toRootRoute

@Stable
class PulseNavigator(
    val state: PulseNavigationState,
) {
    fun navigate(route: NavKey) {
        val stack = state.activeBackStack
        if (stack.lastOrNull() == route) return
        stack.add(route)
    }

    fun replaceAuthRoot(route: NavKey) {
        state.authBackStack.clear()
        state.authBackStack.add(route)
    }

    fun navigateBack(): Boolean {
        val stack = state.activeBackStack
        return when (state.activeFlow) {
            AppEntryFlow.AUTH -> {
                if (stack.size > 1) {
                    stack.removeAt(stack.lastIndex)
                    true
                } else {
                    false
                }
            }
            AppEntryFlow.MAIN -> {
                if (stack.size > 1) {
                    stack.removeAt(stack.lastIndex)
                    true
                } else if (state.selectedTab != PulseTab.HOME) {
                    state.selectedTab = PulseTab.HOME
                    true
                } else {
                    false
                }
            }
        }
    }

    fun selectTab(tab: PulseTab) {
        if (state.activeFlow != AppEntryFlow.MAIN) {
            state.activeFlow = AppEntryFlow.MAIN
        }
        if (state.selectedTab == tab) {
            val stack = state.tabBackStacks.getValue(tab)
            while (stack.size > 1) {
                stack.removeAt(stack.lastIndex)
            }
        } else {
            state.selectedTab = tab
        }
    }

    fun completeDebugAuthBypass(
        isDebugEntryEnabled: Boolean,
        initialTab: PulseTab = PulseTab.HOME,
    ): Boolean {
        if (!isDebugEntryEnabled) return false
        completeAuthAndEnterMain(initialTab = initialTab)
        return true
    }

    fun completeAuthAndEnterMain(initialTab: PulseTab = PulseTab.HOME) {
        state.selectedTab = initialTab
        state.activeFlow = AppEntryFlow.MAIN
        state.authBackStack.clear()
        state.authBackStack.add(AuthEntry)
    }

    fun resetToAuth(entryRoute: NavKey = AuthEntry) {
        state.authBackStack.clear()
        state.authBackStack.add(entryRoute)
        state.tabBackStacks.forEach { (tab, stack) ->
            stack.clear()
            stack.add(tab.toRootRoute())
        }
        state.selectedTab = PulseTab.HOME
        state.activeFlow = AppEntryFlow.AUTH
    }

    fun openWorkoutDetails(planId: String) {
        navigate(WorkoutDetails(planId = planId))
    }

    fun replaceTopWithWorkoutDetails(planId: String) {
        val stack = state.activeBackStack
        if (stack.size > 1 && stack.lastOrNull() is CreateWorkoutPlan) {
            stack.removeAt(stack.lastIndex)
        }
        val target = WorkoutDetails(planId = planId)
        if (stack.lastOrNull() != target) {
            stack.add(target)
        }
    }

    fun popWorkoutPlanFlowAfterDelete(deletedPlanId: String) {
        state.tabBackStacks.values.forEach { stack ->
            while (stack.size > 1) {
                val top = stack.lastOrNull()
                val belongsToDeletedPlan = (top is EditWorkoutPlan && top.planId == deletedPlanId) ||
                    (top is WorkoutDetails && top.planId == deletedPlanId) ||
                    (top is ExercisePicker && top.planId == deletedPlanId)
                if (belongsToDeletedPlan) {
                    stack.removeAt(stack.lastIndex)
                } else {
                    break
                }
            }
        }
    }

    fun startWorkoutSession(planId: String, sessionId: String? = null) {
        navigate(WorkoutSession(planId = planId, sessionId = sessionId))
    }

    fun completeWorkoutFlow(summaryRoute: NavKey) {
        val stack = state.activeBackStack
        while (stack.size > 1 && NavigationPolicy.isFullScreenWorkoutRoute(stack.lastOrNull())) {
            stack.removeAt(stack.lastIndex)
        }
        if (stack.lastOrNull() != summaryRoute) {
            stack.add(summaryRoute)
        }
    }

    fun finishSetAndReturnToSession(
        sessionId: String,
        nextExerciseId: String? = null,
        nextSetIndex: Int? = null,
        openNextSetImmediately: Boolean = false,
    ) {
        val stack = state.activeBackStack
        while (stack.size > 1) {
            val top = stack.lastOrNull()
            if (top is ManualRepCounter || top is DeviceAssistedRepCounter || top is ReadyStartSet) {
                stack.removeAt(stack.lastIndex)
            } else {
                break
            }
        }
        if (openNextSetImmediately && nextExerciseId != null && nextSetIndex != null) {
            navigate(ReadyStartSet(sessionId = sessionId, exerciseId = nextExerciseId, setIndex = nextSetIndex))
        }
    }

    fun switchDeviceCounterToManual(
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
    ) {
        val stack = state.activeBackStack
        while (stack.size > 1 && stack.lastOrNull() is DeviceAssistedRepCounter) {
            stack.removeAt(stack.lastIndex)
        }
        navigate(ManualRepCounter(sessionId = sessionId, exerciseId = exerciseId, setIndex = setIndex))
    }

    fun exitWorkoutSummaryToHome() {
        val stack = state.activeBackStack
        while (stack.size > 1 && NavigationPolicy.isFullScreenWorkoutRoute(stack.lastOrNull())) {
            stack.removeAt(stack.lastIndex)
        }
        state.selectedTab = PulseTab.HOME
        val homeStack = state.activeBackStack
        while (homeStack.size > 1) {
            homeStack.removeAt(homeStack.lastIndex)
        }
        state.activeFlow = AppEntryFlow.MAIN
    }

    fun exitWorkoutSummaryToHistory() {
        val stack = state.activeBackStack
        while (stack.size > 1 && NavigationPolicy.isFullScreenWorkoutRoute(stack.lastOrNull())) {
            stack.removeAt(stack.lastIndex)
        }
        val historyStack = state.tabBackStacks.getValue(PulseTab.HISTORY)
        while (historyStack.size > 1) {
            historyStack.removeAt(historyStack.lastIndex)
        }
        state.selectedTab = PulseTab.HISTORY
        state.activeFlow = AppEntryFlow.MAIN
    }
}
