package net.thechance.tcos.designsystem.component.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.theme.Theme

@Composable
internal fun BaseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
    loading: (@Composable () -> Unit)? = null,
    caption: String? = null,
    isLoading: Boolean = false,
    isEnabled: Boolean = true,
    hasBorder: Boolean = false,
    borderColor: Color = Theme.color.shadeQuaternary,
    containerColor: Color = Theme.color.primary,
    disableContainerColor: Color = Theme.color.disable,
    contentColor: Color = Theme.color.onPrimary,
    disableContentColor: Color = Theme.color.onDisable
){
    val backgroundColor = if (isEnabled) containerColor else disableContainerColor
    val textAndIconColor = if (isEnabled) contentColor else disableContentColor
    val hasContentSpacing = icon != null && caption != null
    val outlineColor = if (!hasBorder) Color.Transparent else if (isEnabled) borderColor else Theme.color.disable
    val isClickable = isEnabled && !isLoading

    Row(
        modifier = modifier
            .height(Theme.size.large)
            .clip(Theme.corners.xLarge)
            .background(backgroundColor)
            .border(width = 1.dp, color = outlineColor, shape = Theme.corners.xLarge)
            .clickable(isClickable){
                onClick()
            }
            .padding(
                vertical = Theme.space.medium,
                horizontal = Theme.space.huge
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        caption?.let {
            BasicText(
                text = it,
                style = Theme.typography.label.copy(
                    color = textAndIconColor
                )
            )
        }
        if (hasContentSpacing){
            Spacer(Modifier.width(Theme.space.medium))
        }
        if (isLoading) {
            loading?.invoke()
        } else {
            icon?.let {
                Image(
                    painter = it,
                    contentDescription = "button icon",
                    colorFilter = ColorFilter.tint(textAndIconColor)
                )
            }
        }
    }
}

@Preview("normal")
@Composable
private fun NormalPreview(){
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BaseButton(
            onClick = {},
            caption = "Add",
        )
        BaseButton(
            onClick = {},
            caption = "Add",
            icon = painterResource(R.drawable.ic_cart)
        )
        BaseButton(
            onClick = {},
            icon = painterResource(R.drawable.ic_cart)
        )
    }

}

@Preview("disable")
@Composable
private fun DisablePreview(){
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BaseButton(
            isEnabled = false,
            onClick = {},
            caption = "Add",
        )
        BaseButton(
            isEnabled = false,
            onClick = {},
            caption = "Add",
            icon = painterResource(R.drawable.ic_cart)
        )
        BaseButton(
            isEnabled = false,
            onClick = {},
            icon = painterResource(R.drawable.ic_cart)
        )
    }

}