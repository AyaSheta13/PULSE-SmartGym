package com.ayasheta.pulsesmartgym.core.designsystem.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus

@Composable
fun OutlineButton(
    caption: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
    isLoading: Boolean = false,
    loading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
    isCompact: Boolean = false,
) {
    BaseButton(
        modifier = modifier,
        onClick = onClick,
        icon = icon,
        loading = loading,
        caption = caption,
        isLoading = isLoading,
        isEnabled = isEnabled,
        hasBorder = true,
        borderColor = PulseTheme.color.border,
        containerColor = PulseTheme.color.container,
        pressedContainerColor = PulseTheme.color.surfaceMuted,
        disableContainerColor = PulseTheme.color.background,
        contentColor = PulseTheme.color.shadePrimary,
        disableContentColor = PulseTheme.color.onDisable,
        height = if (isCompact) PulseTheme.size.large else PulseTheme.size.huge,
        shape = if (isCompact) PulseTheme.corners.large else PulseTheme.corners.xLarge,
        textStyle = PulseTheme.typography.ctaCompact,
        contentPadding = PaddingValues(
            horizontal = PulseTheme.space.large,
            vertical = PulseTheme.space.medium,
        ),
    )
}

@Composable
fun OutlineIconButton(
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    loading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    BaseButton(
        modifier = modifier,
        onClick = onClick,
        icon = icon,
        loading = loading,
        isLoading = isLoading,
        isEnabled = isEnabled,
        hasBorder = true,
        borderColor = PulseTheme.color.border,
        containerColor = PulseTheme.color.container,
        pressedContainerColor = PulseTheme.color.surfaceMuted,
        disableContainerColor = PulseTheme.color.background,
        contentColor = PulseTheme.color.shadePrimary,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.large,
        shape = PulseTheme.corners.large,
        contentPadding = PaddingValues(PulseTheme.space.medium),
    )
}

@Preview(name = "Secondary / Outline Buttons")
@Composable
private fun OutlineButtonsPreview() {
    PulseTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton(
                caption = "Edit Plan",
                onClick = {},
            )
            OutlineButton(
                caption = "Resume Workout",
                isCompact = true,
                onClick = {},
            )
            OutlineButton(
                caption = "Disabled",
                onClick = {},
                isEnabled = false,
            )
            OutlineIconButton(
                icon = rememberVectorPainter(Plus),
                onClick = {},
            )
        }
    }
}
