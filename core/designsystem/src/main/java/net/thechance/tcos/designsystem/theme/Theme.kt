package net.thechance.tcos.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import net.thechance.tcos.designsystem.semanticToken.ColorScheme
import net.thechance.tcos.designsystem.semanticToken.Corners
import net.thechance.tcos.designsystem.semanticToken.Size
import net.thechance.tcos.designsystem.semanticToken.Space
import net.thechance.tcos.designsystem.semanticToken.Typography

object Theme {
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