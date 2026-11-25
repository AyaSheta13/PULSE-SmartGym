package net.thechance.tcos.designsystem.component.chips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme

@Composable
fun Chip(
    caption: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: Painter? = null,
    containerColor: Color = Theme.color.container,
    selectedContainerColor: Color = Theme.color.primary,
    contentColor: Color = Theme.color.shadeSecondary,
    selectedContentColor: Color = Theme.color.onPrimary
){
    val backgroundColor: Color = if (isSelected) selectedContainerColor else  containerColor
    val textAndIconColor: Color = if (isSelected) selectedContentColor else  contentColor
    val horizontalSpacing: Dp = if (isSelected) Theme.space.huge else Theme.space.medium

    Row(
        modifier = modifier.clip(Theme.corners.fullRounded)
            .height(Theme.size.medium)
            .background(backgroundColor)
            .clickable(enabled = onClick != null){ onClick?.invoke() }
            .padding(horizontal = horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically
        ,
    ) {
        BasicText(
            text = caption,
            style = Theme.typography.label.copy(
                color = textAndIconColor
            )
        )
        icon?.let {
            Image(
                painter = it,
                contentDescription = "button icon",
                colorFilter = ColorFilter.tint(textAndIconColor),
                modifier = Modifier
                    .padding(start = Theme.space.medium).size(Theme.size.tiny)
            )
        }
    }

}

@Preview
@Composable
private fun ChipPreview(){
    TCOSTheme() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(
                caption = "chip",
                icon = painterResource(R.drawable.ic_cart)
            )
            Chip(
                caption = "chip",
                icon = painterResource(R.drawable.ic_cart),
                isSelected = true
            )
            Chip(
                caption = "No Icon",
                isSelected = true
            )
        }
    }
}