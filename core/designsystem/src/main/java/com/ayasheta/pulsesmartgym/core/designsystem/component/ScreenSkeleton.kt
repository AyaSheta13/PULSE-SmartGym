package com.ayasheta.pulsesmartgym.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseBottomNavigationBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.ToastMessage
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.ToastStatus
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun ScreenSkeleton(
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = true,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    toast: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val insetsModifier = if (applyStatusBarPadding) {
        Modifier.padding(WindowInsets.statusBars.asPaddingValues())
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.color.background)
            .then(insetsModifier),
    ) {
        Column(Modifier.fillMaxSize()) {
            topBar()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }
            bottomBar()
        }
        Box(
            modifier = Modifier
                .padding(horizontal = PulseTheme.space.large)
                .padding(top = PulseTheme.space.medium),
        ) {
            toast()
        }
    }
}

@Preview
@Composable
private fun ScreenSkeletonPreview() {
    PulseTheme {
        ScreenSkeleton(
            topBar = {
                PulseTopAppBar(
                    style = PulseTopAppBarStyle.HomeGreetingHeader(
                        greeting = "Welcome back",
                        userName = "Good morning, Alex",
                        bluetoothStatus = PulseBluetoothChipStatus.CONNECTED,
                        avatarInitial = "A",
                        onBluetoothClick = {},
                        onAvatarClick = {},
                    ),
                )
            },
            bottomBar = {
                PulseBottomNavigationBar(
                    selectedTab = PulseTab.HOME,
                    onTabSelected = {},
                )
            },
            toast = {
                ToastMessage(
                    status = ToastStatus.SUCCESS,
                    isVisible = false,
                    title = "Success",
                    description = "Workout saved locally",
                    onClickClose = {},
                )
            },
            content = {},
        )
    }
}
