package com.ayasheta.pulsesmartgym.core.designsystem.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Play
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus

@Composable
fun Button(
    caption: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
    isLoading: Boolean = false,
    loading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    BaseButton(
        modifier = modifier,
        onClick = onClick,
        icon = icon,
        loading = loading,
        caption = caption,
        isLoading = isLoading,
        isEnabled = isEnabled,
        hasBorder = false,
        containerColor = PulseTheme.color.primary,
        pressedContainerColor = PulseTheme.color.primaryDark,
        disableContainerColor = PulseTheme.color.disable,
        contentColor = PulseTheme.color.onPrimary,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.huge,
        shape = PulseTheme.corners.xLarge,
        textStyle = PulseTheme.typography.ctaPrimary,
    )
}

@Composable
fun CompactButton(
    caption: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
    isLoading: Boolean = false,
    loading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    BaseButton(
        modifier = modifier,
        onClick = onClick,
        icon = icon,
        loading = loading,
        caption = caption,
        isLoading = isLoading,
        isEnabled = isEnabled,
        hasBorder = false,
        containerColor = PulseTheme.color.primary,
        pressedContainerColor = PulseTheme.color.primaryDark,
        disableContainerColor = PulseTheme.color.disable,
        contentColor = PulseTheme.color.onPrimary,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.large,
        shape = PulseTheme.corners.large,
        textStyle = PulseTheme.typography.ctaCompact,
        contentPadding = PaddingValues(
            horizontal = PulseTheme.space.xLarge,
            vertical = PulseTheme.space.medium,
        ),
    )
}

@Composable
fun IconButton(
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
        hasBorder = false,
        containerColor = PulseTheme.color.primary,
        pressedContainerColor = PulseTheme.color.primaryDark,
        disableContainerColor = PulseTheme.color.disable,
        contentColor = PulseTheme.color.onPrimary,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.large,
        shape = PulseTheme.corners.large,
        contentPadding = PaddingValues(PulseTheme.space.medium),
    )
}

@Preview(name = "Primary Buttons")
@Composable
private fun PrimaryButtonsPreview() {
    PulseTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                caption = "Start Workout",
                icon = rememberVectorPainter(Play),
                onClick = {},
                modifier = Modifier.width(232.dp),
            )
            CompactButton(
                caption = "Start Workout",
                onClick = {},
                modifier = Modifier.width(232.dp),
            )
            Button(
                caption = "Disabled",
                onClick = {},
                isEnabled = false,
                modifier = Modifier.width(232.dp),
            )
            IconButton(
                icon = rememberVectorPainter(Plus),
                onClick = {},
            )
        }
    }
}
