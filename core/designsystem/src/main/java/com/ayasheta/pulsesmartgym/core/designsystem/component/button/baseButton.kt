package com.ayasheta.pulsesmartgym.core.designsystem.component.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Play

@Composable
internal fun BaseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
    leadingIcon: Boolean = true,
    loading: (@Composable () -> Unit)? = null,
    caption: String? = null,
    isLoading: Boolean = false,
    isEnabled: Boolean = true,
    hasBorder: Boolean = false,
    borderColor: Color = PulseTheme.color.border,
    containerColor: Color = PulseTheme.color.primary,
    pressedContainerColor: Color = PulseTheme.color.primaryDark,
    disableContainerColor: Color = PulseTheme.color.disable,
    contentColor: Color = PulseTheme.color.onPrimary,
    disableContentColor: Color = PulseTheme.color.onDisable,
    height: Dp = PulseTheme.size.huge,
    shape: Shape = PulseTheme.corners.xLarge,
    textStyle: TextStyle = PulseTheme.typography.ctaPrimary,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = PulseTheme.space.huge,
        vertical = PulseTheme.space.medium,
    ),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        !isEnabled -> disableContainerColor
        isPressed -> pressedContainerColor
        else -> containerColor
    }
    val textAndIconColor = if (isEnabled) contentColor else disableContentColor
    val hasContentSpacing = icon != null && caption != null
    val outlineColor = if (!hasBorder) {
        Color.Transparent
    } else if (isEnabled) {
        borderColor
    } else {
        PulseTheme.color.disable
    }
    val isClickable = isEnabled && !isLoading

    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = outlineColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isClickable,
                onClick = onClick,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            loading?.invoke()
        } else {
            if (leadingIcon && icon != null) {
                Image(
                    painter = icon,
                    contentDescription = if (caption == null) "Button icon" else null,
                    colorFilter = ColorFilter.tint(textAndIconColor),
                    modifier = Modifier.size(16.dp),
                )
                if (hasContentSpacing) {
                    Spacer(Modifier.width(PulseTheme.space.small))
                }
            }
            caption?.let {
                BasicText(
                    text = it,
                    style = textStyle.copy(color = textAndIconColor),
                )
            }
            if (!leadingIcon && icon != null) {
                if (hasContentSpacing) {
                    Spacer(Modifier.width(PulseTheme.space.small))
                }
                Image(
                    painter = icon,
                    contentDescription = if (caption == null) "Button icon" else null,
                    colorFilter = ColorFilter.tint(textAndIconColor),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview("BaseButton States")
@Composable
private fun BaseButtonPreview() {
    PulseTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BaseButton(
                onClick = {},
                caption = "Start Workout",
                icon = rememberVectorPainter(Play),
            )
            BaseButton(
                isEnabled = false,
                onClick = {},
                caption = "Disabled",
            )
        }
    }
}
