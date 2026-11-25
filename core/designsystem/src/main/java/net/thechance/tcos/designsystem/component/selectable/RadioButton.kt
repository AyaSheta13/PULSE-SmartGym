package net.thechance.tcos.designsystem.component.selectable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme

@Composable
fun RadioButton(
    isChecked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    checkedColor: Color = Theme.color.primary,
    notCheckedColor: Color = Theme.color.shadeQuaternary,
    disableColor: Color = Theme.color.disable,
    centerColor: Color = Theme.color.background

){
    val borderColor: Color = if (!isEnabled) {
        disableColor
    } else if (isChecked) {
        checkedColor
    } else {
        notCheckedColor
    }
    val borderSize: Dp = if (isChecked) 6.dp else 1.dp

    Box(
        modifier = modifier.size(Theme.size.tiny)
            .clip(Theme.corners.fullRounded)
            .border(
                width = borderSize,
                color = borderColor,
                shape = Theme.corners.fullRounded
            ).background(centerColor)
            .clickable(enabled = isEnabled){ onClick() }
    )
}


@Preview
@Composable
private fun RadioButtonPreview() {
    TCOSTheme() {
        var isChecked: Boolean by remember { mutableStateOf(false) }
        Row (
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(Theme.color.background).padding(16.dp)
        ) {
            RadioButton(
                isChecked = isChecked,
                onClick = { isChecked = !isChecked },
            )
            BasicText("Enable me")
        }
    }
}