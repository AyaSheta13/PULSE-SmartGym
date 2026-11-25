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
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Trash

@Composable
fun NegativeButton(
    caption: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
    isLoading: Boolean = false,
    loading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
    isFilled: Boolean = false,
) {
    BaseButton(
        modifier = modifier,
        onClick = onClick,
        icon = icon,
        loading = loading,
        caption = caption,
        isLoading = isLoading,
        isEnabled = isEnabled,
        hasBorder = !isFilled,
        borderColor = PulseTheme.color.error.copy(alpha = 0.35f),
        containerColor = if (isFilled) PulseTheme.color.error else PulseTheme.color.errorVariant,
        pressedContainerColor = if (isFilled) PulseTheme.color.primaryDark else PulseTheme.color.error.copy(alpha = 0.2f),
        disableContainerColor = PulseTheme.color.disable,
        contentColor = if (isFilled) PulseTheme.color.onPrimary else PulseTheme.color.error,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.large,
        shape = PulseTheme.corners.large,
        textStyle = PulseTheme.typography.ctaCompact,
    )
}

@Composable
fun NegativeIconButton(
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
        containerColor = PulseTheme.color.errorVariant,
        pressedContainerColor = PulseTheme.color.error.copy(alpha = 0.2f),
        disableContainerColor = PulseTheme.color.disable,
        contentColor = PulseTheme.color.error,
        disableContentColor = PulseTheme.color.onDisable,
        height = PulseTheme.size.large,
        shape = PulseTheme.corners.large,
        contentPadding = PaddingValues(PulseTheme.space.medium),
    )
}

@Preview(name = "Negative Buttons")
@Composable
private fun NegativeButtonsPreview() {
    PulseTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NegativeButton(
                caption = "Delete Workout",
                icon = rememberVectorPainter(Trash),
                onClick = {},
            )
            NegativeButton(
                caption = "Save & End Workout",
                isFilled = true,
                onClick = {},
            )
            NegativeButton(
                caption = "Disabled",
                onClick = {},
                isEnabled = false,
            )
            NegativeIconButton(
                icon = rememberVectorPainter(Trash),
                onClick = {},
            )
        }
    }
}
