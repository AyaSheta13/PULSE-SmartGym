package net.thechance.tcos.designsystem.component.generic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.component.text.CardTitleText
import net.thechance.tcos.designsystem.component.text.SubTitleText
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme
import net.thechance.tcos.designsystem.vectors.Back
import net.thechance.tcos.designsystem.vectors.Notification

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    leadingButtonIcon: ImageVector? = null,
    onClickLeadingButton: (() -> Unit)? = null,
    trailingButtonIcon: ImageVector? = null,
    onClickTrailingButton: (() -> Unit)? = null,
    logo: Painter? = null,
){
    Row(
        modifier = modifier.fillMaxWidth()
            .height(Theme.size.massive)
            .background(Theme.color.background)
            .padding(
                vertical = Theme.space.large,
                horizontal = Theme.space.huge,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.space.medium)
    ) {
        leadingButtonIcon?.let {
            ClickableIcon (
                icon = it,
                onClick = { onClickLeadingButton?.invoke() },
                enabled = onClickTrailingButton != null
            )
        } ?: logo?.let {
            Image(
                painter = it,
                contentDescription = "Logo",
                modifier = Modifier.size(Theme.size.large)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            title?.let { CardTitleText(it) }
            subtitle?.let { SubTitleText(it) }
        }
        trailingButtonIcon?.let {
            ClickableIcon (
                icon = it,
                onClick = { onClickTrailingButton?.invoke() },
                enabled = onClickTrailingButton != null
            )
        }
    }
}

@Composable
private fun ClickableIcon(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
){
    Box(
        Modifier.size(Theme.size.large)
            .clip(Theme.corners.large)
            .background(Theme.color.container)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ){
        Image(
            imageVector = icon,
            contentDescription = "App bar icon",
            colorFilter = ColorFilter.tint(Theme.color.shadeSecondary),
            modifier = Modifier.size(Theme.size.tiny)
        )
    }
}

@Preview
@Composable
private fun TopBarPreview(){
    TCOSTheme {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TopBar(
                title = "Home",
                subtitle = "Hi Ahmed, wassup?"
            )

            TopBar(
                title = "Home",
                subtitle = "Hi Ahmed, wassup?",
                logo = painterResource(R.drawable.tc_logo)
            )
            TopBar(
                title = "Home",
                subtitle = "Hi Ahmed, wassup?",
                leadingButtonIcon = Back,
                onClickLeadingButton = {}
            )
            TopBar(
                title = "Home",
                trailingButtonIcon = Notification,
                onClickTrailingButton = {}
            )
            TopBar(
                title = "Home",
                subtitle = "Hi Ahmed, wassup?",
                logo = painterResource(R.drawable.tc_logo),
                trailingButtonIcon = Notification,
                onClickTrailingButton = {}
            )
        }

    }

}