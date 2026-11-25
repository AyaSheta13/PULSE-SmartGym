package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.navigation.route.Splash
import com.ayasheta.pulsesmartgym.navigation.route.toRootRoute

enum class AppEntryFlow {
    AUTH,
    MAIN,
}

@Stable
class PulseNavigationState(
    initialFlow: AppEntryFlow,
    initialTab: PulseTab,
    val authBackStack: MutableList<NavKey>,
    val tabBackStacks: Map<PulseTab, MutableList<NavKey>>,
) {
    var activeFlow by mutableStateOf(initialFlow)
    var selectedTab by mutableStateOf(initialTab)

    val activeBackStack: MutableList<NavKey>
        get() = when (activeFlow) {
            AppEntryFlow.AUTH -> authBackStack
            AppEntryFlow.MAIN -> tabBackStacks.getValue(selectedTab)
        }

    val currentDestination: NavKey?
        get() = activeBackStack.lastOrNull()

    val showBottomBar: Boolean
        get() = activeFlow == AppEntryFlow.MAIN &&
            NavigationPolicy.shouldShowBottomBar(currentDestination)

    companion object {
        fun createForTesting(
            initialFlow: AppEntryFlow = AppEntryFlow.MAIN,
            initialTab: PulseTab = PulseTab.HOME,
            initialAuthRoute: NavKey = Splash,
        ): PulseNavigationState {
            val authStack = mutableStateListOf(initialAuthRoute)
            val tabs = PulseTab.entries.associateWith { tab ->
                mutableStateListOf<NavKey>(tab.toRootRoute())
            }
            return PulseNavigationState(
                initialFlow = initialFlow,
                initialTab = initialTab,
                authBackStack = authStack,
                tabBackStacks = tabs,
            )
        }
    }
}

@Composable
fun rememberPulseNavigationState(
    initialFlow: AppEntryFlow = AppEntryFlow.AUTH,
    initialTab: PulseTab = PulseTab.HOME,
    initialAuthRoute: NavKey = Splash,
): PulseNavigationState {
    var savedFlow by rememberSaveable { mutableStateOf(initialFlow) }
    var savedTab by rememberSaveable { mutableStateOf(initialTab) }

    val authBackStack = rememberNavBackStack(initialAuthRoute)
    val workoutsStack = rememberNavBackStack(PulseTab.WORKOUTS.toRootRoute())
    val progressStack = rememberNavBackStack(PulseTab.PROGRESS.toRootRoute())
    val homeStack = rememberNavBackStack(PulseTab.HOME.toRootRoute())
    val historyStack = rememberNavBackStack(PulseTab.HISTORY.toRootRoute())
    val profileStack = rememberNavBackStack(PulseTab.PROFILE.toRootRoute())

    return androidx.compose.runtime.remember(
        authBackStack,
        workoutsStack,
        progressStack,
        homeStack,
        historyStack,
        profileStack,
    ) {
        PulseNavigationState(
            initialFlow = savedFlow,
            initialTab = savedTab,
            authBackStack = authBackStack,
            tabBackStacks = mapOf(
                PulseTab.WORKOUTS to workoutsStack,
                PulseTab.PROGRESS to progressStack,
                PulseTab.HOME to homeStack,
                PulseTab.HISTORY to historyStack,
                PulseTab.PROFILE to profileStack,
            ),
        )
    }.also { state ->
        savedFlow = state.activeFlow
        savedTab = state.selectedTab
    }
}
