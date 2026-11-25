package com.ayasheta.pulsesmartgym.core.designsystem.component.selectable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun ToggleButton(
    isToggled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    thumbToggledColor: Color = PulseTheme.color.onPrimary,
    thumbUnToggledColor: Color = PulseTheme.color.shadeQuaternary,
    thumbDisableColor: Color = PulseTheme.color.onDisable,
    containerToggledColor: Color = PulseTheme.color.primary,
    containerUnToggledColor: Color = PulseTheme.color.primaryVariant,
    containerDisableColor: Color = PulseTheme.color.disable
){
    val toggleAlignment = if (isToggled) Alignment.CenterEnd else Alignment.CenterStart

    val thumbColor = if (!isEnabled) {
        thumbDisableColor
    } else if (isToggled){
        thumbToggledColor
    } else {
        thumbUnToggledColor
    }

    val containerColor = if (!isEnabled) {
        containerDisableColor
    } else if (isToggled){
        containerToggledColor
    } else {
        containerUnToggledColor
    }

    Box(
        modifier = modifier.width(PulseTheme.size.large)
            .height(PulseTheme.size.small)
            .clip(PulseTheme.corners.xxLarge)
            .background(containerColor)
            .clickable(isEnabled) { onClick() }
            .padding(PulseTheme.space.small)
    ){
        Box(
            modifier = Modifier.size(PulseTheme.size.tiny)
                .clip(PulseTheme.corners.fullRounded)
                .background(thumbColor)
                .align(toggleAlignment)
        )
    }
}


@Preview
@Composable
private fun ToggleButtonPreview() {
    PulseTheme {
        var isChecked: Boolean by remember { mutableStateOf(false) }
        Row (
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(PulseTheme.color.background).padding(16.dp)
        ) {
            ToggleButton(
                isToggled = isChecked,
                onClick = { isChecked = !isChecked },
            )
            BasicText("Switch Me")
        }
    }
}