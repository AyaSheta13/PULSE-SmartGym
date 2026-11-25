package com.ayasheta.pulsesmartgym.core.designsystem.component.selectable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

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
    borderChecked = PulseTheme.color.primary,
    borderUnChecked = PulseTheme.color.shadeTertiary,
    borderDisabled = PulseTheme.color.disable,
    containerChecked = PulseTheme.color.primary,
    containerUnChecked = PulseTheme.color.background,
    containerDisabled = PulseTheme.color.disable,
    iconChecked = PulseTheme.color.onPrimary,
    iconUnChecked = Color.Transparent,
    iconDisabled = PulseTheme.color.onDisable,
)