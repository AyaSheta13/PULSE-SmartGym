package com.ayasheta.pulsesmartgym.core.designsystem.component.generic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.HistoryClock
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Home
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.HomeFilled
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Profile
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.ProfileFilled
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.ProgressChart
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Workouts

enum class PulseTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    WORKOUTS(label = "Workouts", icon = Workouts, selectedIcon = Workouts),
    PROGRESS(label = "Progress", icon = ProgressChart, selectedIcon = ProgressChart),
    HOME(label = "Home", icon = Home, selectedIcon = HomeFilled),
    HISTORY(label = "History", icon = HistoryClock, selectedIcon = HistoryClock),
    PROFILE(label = "Profile", icon = Profile, selectedIcon = ProfileFilled),
}

@Composable
fun PulseBottomNavigationBar(
    selectedTab: PulseTab,
    onTabSelected: (PulseTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PulseTheme.color.container)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PulseTheme.color.divider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PulseTheme.size.giant)
                .padding(top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: PulseTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (isSelected) PulseTheme.color.primary else PulseTheme.color.shadeTertiary
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clip(PulseTheme.corners.large)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
            contentDescription = "${tab.label} tab",
            modifier = Modifier.size(PulseTheme.size.small),
            colorFilter = ColorFilter.tint(contentColor),
        )
        BasicText(
            text = tab.label,
            style = PulseTheme.typography.label.copy(color = contentColor),
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseBottomNavigationBarPreview() {
    var selectedTab by remember { mutableStateOf(PulseTab.HOME) }
    PulseTheme {
        Column(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(top = 24.dp),
        ) {
            PulseBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
    }
}
