package com.ayasheta.pulsesmartgym.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseBottomNavigationBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import com.ayasheta.pulsesmartgym.navigation.PulseNavDisplay
import com.ayasheta.pulsesmartgym.navigation.PulseNavigationState
import com.ayasheta.pulsesmartgym.navigation.PulseNavigator

@Composable
fun PulseAppShell(
    navigationState: PulseNavigationState,
    navigator: PulseNavigator,
    isDebugEntryEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val canHandleBack = when (navigationState.activeFlow) {
        AppEntryFlow.AUTH -> navigationState.authBackStack.size > 1
        AppEntryFlow.MAIN ->
            navigationState.activeBackStack.size > 1 || navigationState.selectedTab != PulseTab.HOME
    }

    BackHandler(enabled = canHandleBack) {
        navigator.navigateBack()
    }

    val contentBottomInsetModifier = if (navigationState.showBottomBar) {
        Modifier
    } else {
        Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.color.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(contentBottomInsetModifier),
        ) {
            PulseNavDisplay(
                navigationState = navigationState,
                navigator = navigator,
                isDebugEntryEnabled = isDebugEntryEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (navigationState.showBottomBar) {
            PulseBottomNavigationBar(
                selectedTab = navigationState.selectedTab,
                onTabSelected = navigator::selectTab,
            )
        }
    }
}
