package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

@Composable
fun PulseNavDisplay(
    navigationState: PulseNavigationState,
    navigator: PulseNavigator,
    isDebugEntryEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val entryProvider = rememberPulseEntryProvider(
        navigator = navigator,
        isDebugEntryEnabled = isDebugEntryEnabled,
    )

    NavDisplay(
        backStack = navigationState.activeBackStack,
        modifier = modifier,
        onBack = { navigator.navigateBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
        ),
        transitionSpec = PulseNavigationTransitions.forwardTransition(),
        popTransitionSpec = PulseNavigationTransitions.popTransition(),
        predictivePopTransitionSpec = { PulseNavigationTransitions.popTransition().invoke(this) },
        entryProvider = entryProvider,
    )
}
