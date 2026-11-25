package net.thechance.tcos.designsystem.component.generic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.thechance.tcos.designsystem.baseToken.ColorPalette
import net.thechance.tcos.designsystem.component.text.Hint
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme
import net.thechance.tcos.designsystem.vectors.Chat
import net.thechance.tcos.designsystem.vectors.ChatFilled
import net.thechance.tcos.designsystem.vectors.Favourite
import net.thechance.tcos.designsystem.vectors.FavouriteFilled
import net.thechance.tcos.designsystem.vectors.Home
import net.thechance.tcos.designsystem.vectors.HomeFilled
import net.thechance.tcos.designsystem.vectors.Profile
import net.thechance.tcos.designsystem.vectors.ProfileFilled

@Composable
fun BottomNavigationBar(
    tabs: List<BottomNavigationBarTab>,
    modifier: Modifier = Modifier,
    onTabChange: (BottomNavigationBarTab) -> Unit,
){
    var selectedTabIndex: Int by remember { mutableStateOf(0) }
    Row(
        modifier = modifier.fillMaxWidth()
            .height(Theme.size.massive)
            .dropShadow(
                shape = Theme.corners.notRounded,
                shadow = Shadow(
                    radius = 12.dp,
                    offset = DpOffset(x = 0.dp, y = -4.dp),
                    color = ColorPalette.black.copy(alpha = 0.08f)
                )
            )
            .background(Theme.color.container)
            .padding(
                vertical = Theme.space.large,
                horizontal = Theme.space.huge
            ),
    ) {
        tabs.forEachIndexed { index, tab ->
            TabItem(
                tab = tab,
                onClick = {
                    selectedTabIndex = index
                    onTabChange(tabs[index])
                },
                isSelected = selectedTabIndex == index
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: BottomNavigationBarTab,
    isSelected: Boolean,
    onClick: () -> Unit,
){
    val contentColor = if (isSelected) Theme.color.primary else Theme.color.shadeSecondary
    Column(
        modifier = Modifier.fillMaxHeight()
            .weight(1f)
            .clip(Theme.corners.medium)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
            contentDescription = "${tab.name} tab icon",
            modifier = Modifier.size(Theme.size.small),
            colorFilter = ColorFilter.tint(contentColor)
        )
        if (isSelected) {
            Hint(
                text = tab.name,
                color = contentColor
            )
        }
    }

}

@Preview
@Composable
private fun BottomNavigationBarPreview() {
    val tabs: List<BottomNavigationBarTab> by remember {
        mutableStateOf(listOf(
            BottomNavigationBarTab(
                name = "Home",
                icon = Home,
                selectedIcon = HomeFilled
            ),
            BottomNavigationBarTab(
                name = "Chat",
                icon = Chat,
                selectedIcon = ChatFilled
            ),
            BottomNavigationBarTab(
                name = "Favourite",
                icon = Favourite,
                selectedIcon = FavouriteFilled
            ),
            BottomNavigationBarTab(
                name = "Profile",
                icon = Profile,
                selectedIcon = ProfileFilled
            )
        ))
    }
    TCOSTheme {
        Column(
            Modifier.background(Theme.color.background).padding(top = 100.dp)
        ) {
            BottomNavigationBar(
                tabs = tabs,
                onTabChange = {}
            )
        }
    }
}