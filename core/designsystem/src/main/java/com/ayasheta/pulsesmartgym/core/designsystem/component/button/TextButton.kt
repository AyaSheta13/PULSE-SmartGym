package com.ayasheta.pulsesmartgym.core.designsystem.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus

@Composable
fun TextButton(
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
        containerColor = Color.Transparent,
        pressedContainerColor = PulseTheme.color.primaryContainer,
        disableContainerColor = Color.Transparent,
        contentColor = PulseTheme.color.primary,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.large,
        shape = PulseTheme.corners.large,
        textStyle = PulseTheme.typography.ctaCompact,
        contentPadding = PaddingValues(horizontal = PulseTheme.space.small, vertical = PulseTheme.space.tiny),
    )
}

@Composable
fun TextIconButton(
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
        containerColor = Color.Transparent,
        pressedContainerColor = PulseTheme.color.primaryContainer,
        disableContainerColor = Color.Transparent,
        contentColor = PulseTheme.color.primary,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.large,
        shape = PulseTheme.corners.large,
        contentPadding = PaddingValues(PulseTheme.space.small),
    )
}

@Preview(name = "Text Buttons")
@Composable
private fun TextButtonsPreview() {
    PulseTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(caption = "View All", onClick = {})
            TextButton(caption = "Add Exercise", icon = rememberVectorPainter(Plus), onClick = {})
            TextButton(caption = "Disabled", onClick = {}, isEnabled = false)
            TextIconButton(icon = rememberVectorPainter(Plus), onClick = {})
        }
    }
}
