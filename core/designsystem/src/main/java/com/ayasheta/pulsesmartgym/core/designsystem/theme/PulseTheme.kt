package com.ayasheta.pulsesmartgym.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.ColorScheme
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.Corners
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.Size
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.Space
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.Typography
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.defaultCorners
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.defaultLightColors
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.defaultSizing
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.defaultSpacing
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.defaultTypography

@Composable
fun PulseTheme(
    colorScheme: ColorScheme = defaultLightColors,
    typography: Typography = defaultTypography,
    spacing: Space = defaultSpacing,
    corners: Corners = defaultCorners,
    sizing: Size = defaultSizing,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColor provides colorScheme,
        LocalTypography provides typography,
        LocalCorners provides corners,
        LocalSpace provides spacing,
        LocalSize provides sizing,
    ) {
        content()
    }
}

object PulseTheme {
    val color: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalColor.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = LocalTypography.current

    val corners: Corners
        @Composable @ReadOnlyComposable get() = LocalCorners.current

    val space: Space
        @Composable @ReadOnlyComposable get() = LocalSpace.current

    val size: Size
        @Composable @ReadOnlyComposable get() = LocalSize.current
}
