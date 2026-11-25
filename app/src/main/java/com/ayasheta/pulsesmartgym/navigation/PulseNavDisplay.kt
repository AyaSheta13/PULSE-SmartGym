package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab

@Composable
fun PulseNavDisplay(
    navigationState: PulseNavigationState,
    navigator: PulseNavigator,
    isDebugEntryEnabled: Boolean,
    dataContainer: PulseDataContainer,
    modifier: Modifier = Modifier,
) {
    val entryProvider = rememberPulseEntryProvider(
        navigator = navigator,
        isDebugEntryEnabled = isDebugEntryEnabled,
        dataContainer = dataContainer,
    )

    val authEntries = rememberStackEntries(
        backStack = navigationState.authBackStack,
        entryProvider = entryProvider,
    )
    val workoutsEntries = rememberStackEntries(
        backStack = navigationState.tabBackStacks.getValue(PulseTab.WORKOUTS),
        entryProvider = entryProvider,
    )
    val progressEntries = rememberStackEntries(
        backStack = navigationState.tabBackStacks.getValue(PulseTab.PROGRESS),
        entryProvider = entryProvider,
    )
    val homeEntries = rememberStackEntries(
        backStack = navigationState.tabBackStacks.getValue(PulseTab.HOME),
        entryProvider = entryProvider,
    )
    val historyEntries = rememberStackEntries(
        backStack = navigationState.tabBackStacks.getValue(PulseTab.HISTORY),
        entryProvider = entryProvider,
    )
    val profileEntries = rememberStackEntries(
        backStack = navigationState.tabBackStacks.getValue(PulseTab.PROFILE),
        entryProvider = entryProvider,
    )

    val activeEntries = when (navigationState.activeFlow) {
        AppEntryFlow.AUTH -> authEntries
        AppEntryFlow.MAIN -> when (navigationState.selectedTab) {
            PulseTab.WORKOUTS -> workoutsEntries
            PulseTab.PROGRESS -> progressEntries
            PulseTab.HOME -> homeEntries
            PulseTab.HISTORY -> historyEntries
            PulseTab.PROFILE -> profileEntries
        }
    }

    key(navigationState.activeFlow, navigationState.selectedTab) {
        NavDisplay(
            entries = activeEntries,
            modifier = modifier,
            onBack = { navigator.navigateBack() },
            transitionSpec = PulseNavigationTransitions.forwardTransition(),
            popTransitionSpec = PulseNavigationTransitions.popTransition(),
            predictivePopTransitionSpec = { PulseNavigationTransitions.popTransition().invoke(this) },
        )
    }
}

@Composable
private fun rememberStackEntries(
    backStack: List<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
    backStack = backStack,
    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    entryProvider = entryProvider,
)
