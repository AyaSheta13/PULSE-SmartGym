package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.navigation.route.AuthEntry
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
}
