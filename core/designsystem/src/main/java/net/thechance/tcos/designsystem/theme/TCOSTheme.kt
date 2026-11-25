package net.thechance.tcos.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import net.thechance.tcos.designsystem.semanticToken.ColorScheme
import net.thechance.tcos.designsystem.semanticToken.Corners
import net.thechance.tcos.designsystem.semanticToken.Size
import net.thechance.tcos.designsystem.semanticToken.Space
import net.thechance.tcos.designsystem.semanticToken.Typography
import net.thechance.tcos.designsystem.semanticToken.defaultCorners
import net.thechance.tcos.designsystem.semanticToken.defaultLightColors
import net.thechance.tcos.designsystem.semanticToken.defaultSizing
import net.thechance.tcos.designsystem.semanticToken.defaultSpacing
import net.thechance.tcos.designsystem.semanticToken.defaultTypography

@Composable
fun TCOSTheme(
    colorScheme: ColorScheme = defaultLightColors,
    typography: Typography = defaultTypography,
    spacing: Space = defaultSpacing,
    corners: Corners = defaultCorners,
    sizing: Size = defaultSizing,
    content: @Composable () -> Unit
){
    CompositionLocalProvider(
        LocalColor provides colorScheme,
        LocalTypography provides typography,
        LocalCorners provides corners,
        LocalSpace provides spacing,
        LocalSize provides sizing
    ) {
        content()
    }
}