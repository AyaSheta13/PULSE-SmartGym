package com.ayasheta.pulsesmartgym.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseProgressBar
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun PulseRestTimerCard(
    remainingFormatted: String,
    progress: Float,
    subtitle: String,
    onAddTimeClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeLabel: String = "REST",
    addTimeLabel: String = "+30s",
    skipLabel: String = "Skip Rest",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PulseTheme.corners.xLarge)
            .background(PulseTheme.color.container)
            .border(width = 1.5.dp, color = PulseTheme.color.primary, shape = PulseTheme.corners.xLarge)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(PulseTheme.corners.xSmall)
                    .background(PulseTheme.color.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                BasicText(
                    text = badgeLabel,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = PulseTheme.color.primary,
                    ),
                )
            }
            BasicText(
                text = subtitle,
                style = PulseTheme.typography.label.copy(
                    fontWeight = FontWeight.Normal,
                    color = PulseTheme.color.shadeSecondary,
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = remainingFormatted,
                style = PulseTheme.typography.numericMetric.copy(
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    color = PulseTheme.color.shadePrimary,
                ),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(PulseTheme.corners.small)
                        .background(PulseTheme.color.background)
                        .border(1.dp, PulseTheme.color.border, PulseTheme.corners.small)
                        .clickable(onClick = onAddTimeClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = addTimeLabel,
                        style = PulseTheme.typography.label.copy(
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(PulseTheme.corners.small)
                        .background(PulseTheme.color.primary)
                        .clickable(onClick = onSkipClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = skipLabel,
                        style = PulseTheme.typography.label.copy(
                            color = PulseTheme.color.onPrimary,
                        ),
                    )
                }
            }
        }

        PulseProgressBar(
            progress = progress,
            height = 4.dp,
            trackColor = PulseTheme.color.surfaceMuted,
            indicatorColor = PulseTheme.color.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseRestTimerCardPreview() {
    PulseTheme {
        Box(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp),
        ) {
            PulseRestTimerCard(
                remainingFormatted = "01:28",
                progress = 0.4f,
                subtitle = "Rest after Barbell Bench Press",
                onAddTimeClick = {},
                onSkipClick = {},
            )
        }
    }
}
