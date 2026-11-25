package com.ayasheta.pulsesmartgym.core.designsystem.component.chips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Workouts

@Composable
fun Chip(
    caption: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: Painter? = null,
    containerColor: Color = PulseTheme.color.container,
    selectedContainerColor: Color = PulseTheme.color.primary,
    contentColor: Color = PulseTheme.color.shadeSecondary,
    selectedContentColor: Color = PulseTheme.color.onPrimary,
    borderColor: Color = PulseTheme.color.border,
) {
    val backgroundColor = if (isSelected) selectedContainerColor else containerColor
    val textAndIconColor = if (isSelected) selectedContentColor else contentColor
    val resolvedBorder = if (isSelected) Color.Transparent else borderColor

    Row(
        modifier = modifier
            .height(PulseTheme.size.medium)
            .clip(PulseTheme.corners.fullRounded)
            .background(backgroundColor)
            .border(width = 1.dp, color = resolvedBorder, shape = PulseTheme.corners.fullRounded)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = PulseTheme.space.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = caption,
            style = PulseTheme.typography.label.copy(color = textAndIconColor),
        )
        icon?.let {
            Image(
                painter = it,
                contentDescription = null,
                colorFilter = ColorFilter.tint(textAndIconColor),
                modifier = Modifier
                    .padding(start = PulseTheme.space.small)
                    .size(14.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChipPreview() {
    PulseTheme {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(
                caption = "Chest",
                icon = rememberVectorPainter(Workouts),
            )
            Chip(
                caption = "Auto Sensor Mode",
                icon = rememberVectorPainter(Workouts),
                isSelected = true,
            )
            Chip(
                caption = "Manual Rep Counter",
                isSelected = false,
                onClick = {},
            )
        }
    }
}
