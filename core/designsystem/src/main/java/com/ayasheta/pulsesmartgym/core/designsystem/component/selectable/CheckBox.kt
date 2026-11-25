package com.ayasheta.pulsesmartgym.core.designsystem.component.selectable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Indeterminate

@Composable
fun CheckBox(
    status: CheckBoxStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    colors: CheckBoxColors = defaultCheckBoxColors()
){
    val borderColor = if (!isEnabled) {
        colors.borderDisabled
    } else if (status != CheckBoxStatus.UN_CHECKED) {
        colors.borderChecked
    } else {
        colors.borderUnChecked
    }

    val containerColor = if (!isEnabled) {
        colors.containerDisabled
    } else if (status != CheckBoxStatus.UN_CHECKED) {
        colors.containerChecked
    } else {
        colors.containerUnChecked
    }

    val iconColor = if (!isEnabled) {
        colors.iconDisabled
    } else if (status != CheckBoxStatus.UN_CHECKED) {
        colors.iconChecked
    } else {
        colors.iconUnChecked
    }

    val icon: ImageVector? = when(status){
        CheckBoxStatus.CHECKED -> CheckMark
        CheckBoxStatus.UN_CHECKED -> null
        CheckBoxStatus.INDETERMINATE -> Indeterminate
    }

    Box(
        modifier = modifier.size(PulseTheme.size.tiny)
            .clip(PulseTheme.corners.small)
            .background(containerColor)
            .border(
                width = 1.dp,
                shape = PulseTheme.corners.small,
                color = borderColor
            )
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ){
        icon?.let {
            Image(
                imageVector = icon,
                contentDescription = "${status.name} icon",
                colorFilter = ColorFilter.tint(iconColor),
                modifier = Modifier.size(PulseTheme.size.mini)
            )
        }

    }
}

@Preview
@Composable
private fun CheckBoxPreview(){
    PulseTheme {
        var checkBoxStatus: CheckBoxStatus by remember { mutableStateOf(CheckBoxStatus.UN_CHECKED) }
        CheckBox(
            status = checkBoxStatus,
            onClick = {
                checkBoxStatus = if(checkBoxStatus == CheckBoxStatus.CHECKED){
                    CheckBoxStatus.UN_CHECKED
                } else {
                    CheckBoxStatus.CHECKED
                }
            },
        )
    }
}