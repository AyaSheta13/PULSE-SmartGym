package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    private val activeFlowState: MutableState<AppEntryFlow>,
    private val selectedTabState: MutableState<PulseTab>,
    val authBackStack: MutableList<NavKey>,
    val tabBackStacks: Map<PulseTab, MutableList<NavKey>>,
) {
    var activeFlow: AppEntryFlow
        get() = activeFlowState.value
        set(value) {
            activeFlowState.value = value
        }

    var selectedTab: PulseTab
        get() = selectedTabState.value
        set(value) {
            selectedTabState.value = value
        }

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
                activeFlowState = mutableStateOf(initialFlow),
                selectedTabState = mutableStateOf(initialTab),
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
    val activeFlowState = rememberSaveable { mutableStateOf(initialFlow) }
    val selectedTabState = rememberSaveable { mutableStateOf(initialTab) }

    val authBackStack = rememberNavBackStack(initialAuthRoute)
    val workoutsStack = rememberNavBackStack(PulseTab.WORKOUTS.toRootRoute())
    val progressStack = rememberNavBackStack(PulseTab.PROGRESS.toRootRoute())
    val homeStack = rememberNavBackStack(PulseTab.HOME.toRootRoute())
    val historyStack = rememberNavBackStack(PulseTab.HISTORY.toRootRoute())
    val profileStack = rememberNavBackStack(PulseTab.PROFILE.toRootRoute())

    return remember(
        activeFlowState,
        selectedTabState,
        authBackStack,
        workoutsStack,
        progressStack,
        homeStack,
        historyStack,
        profileStack,
    ) {
        PulseNavigationState(
            activeFlowState = activeFlowState,
            selectedTabState = selectedTabState,
            authBackStack = authBackStack,
            tabBackStacks = mapOf(
                PulseTab.WORKOUTS to workoutsStack,
                PulseTab.PROGRESS to progressStack,
                PulseTab.HOME to homeStack,
                PulseTab.HISTORY to historyStack,
                PulseTab.PROFILE to profileStack,
            ),
        )
    }
}
