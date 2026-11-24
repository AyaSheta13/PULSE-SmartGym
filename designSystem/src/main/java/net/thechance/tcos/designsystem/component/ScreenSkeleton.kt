package net.thechance.tcos.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.component.generic.BottomNavigationBar
import net.thechance.tcos.designsystem.component.generic.BottomNavigationBarTab
import net.thechance.tcos.designsystem.component.generic.ToastMessage
import net.thechance.tcos.designsystem.component.generic.ToastStatus
import net.thechance.tcos.designsystem.component.generic.TopBar
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme
import net.thechance.tcos.designsystem.vectors.Chat
import net.thechance.tcos.designsystem.vectors.ChatFilled
import net.thechance.tcos.designsystem.vectors.Favourite
import net.thechance.tcos.designsystem.vectors.FavouriteFilled
import net.thechance.tcos.designsystem.vectors.Home
import net.thechance.tcos.designsystem.vectors.HomeFilled
import net.thechance.tcos.designsystem.vectors.Notification
import net.thechance.tcos.designsystem.vectors.Profile
import net.thechance.tcos.designsystem.vectors.ProfileFilled

@Composable
fun ScreenSkeleton(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    toast: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
){
    Box(
        Modifier
            .background(Theme.color.background)
            .padding(WindowInsets.systemBars.asPaddingValues())
    ){
        Column(Modifier.fillMaxSize()) {
            topBar()
            Box(
                modifier = Modifier.weight(1f)
                .fillMaxWidth()
            ) {
                content()
            }
            bottomBar()
        }
        Box(
            modifier = Modifier.padding(horizontal = Theme.space.huge)
                .padding(top = Theme.space.large)
        ) {
            toast()
        }
    }

}

@Preview
@Composable
private fun ScreenSkeletonPreview(){
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
        ScreenSkeleton(
            topBar = {
                TopBar(
                    title = "Home",
                    subtitle = "Hi Ahmed, wassup?",
                    logo = painterResource(R.drawable.tc_logo),
                    trailingButtonIcon = Notification,
                    onClickTrailingButton = {}
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    tabs = tabs,
                    onTabChange = {}
                )
            },
            content = {},
            toast = {
                ToastMessage(
                    status = ToastStatus.SUCCESS,
                    isVisible = false,
                    title = "Success",
                    description = "Book Added to favourites",
                    onClickClose = {}
                )
            }
        )
    }
}