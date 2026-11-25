package net.thechance.tcos.designsystem.component.selectable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.thechance.tcos.designsystem.theme.Theme

@Immutable
data class CheckBoxColors(
    val borderChecked: Color,
    val borderUnChecked: Color,
    val borderDisabled: Color,
    val containerChecked: Color,
    val containerUnChecked: Color,
    val containerDisabled: Color,
    val iconChecked: Color,
    val iconUnChecked: Color,
    val iconDisabled: Color,
)

@Composable
internal fun defaultCheckBoxColors() = CheckBoxColors(
    borderChecked = Theme.color.primary,
    borderUnChecked = Theme.color.shadeTertiary,
    borderDisabled = Theme.color.disable,
    containerChecked = Theme.color.primary,
    containerUnChecked = Theme.color.background,
    containerDisabled = Theme.color.disable,
    iconChecked = Theme.color.onPrimary,
    iconUnChecked = Color.Transparent,
    iconDisabled = Theme.color.onDisable,
)