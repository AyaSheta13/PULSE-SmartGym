package com.ayasheta.pulsesmartgym.core.designsystem.component.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun PulseProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = PulseTheme.color.surfaceMuted,
    indicatorColor: Color = PulseTheme.color.primary,
    height: Dp = 6.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PulseTheme.corners.fullRounded)
            .background(trackColor),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(PulseTheme.corners.fullRounded)
                    .background(indicatorColor),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseProgressBarPreview() {
    PulseTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PulseProgressBar(progress = 0f)
            PulseProgressBar(progress = 0.4f)
            PulseProgressBar(progress = 1f)
        }
    }
}
