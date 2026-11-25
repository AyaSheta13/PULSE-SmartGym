package com.ayasheta.pulsesmartgym.core.designsystem.component.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.CompactButton
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.HistoryClock

@Composable
fun PulseEmptyStateCard(
    title: String,
    description: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = HistoryClock,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = PulseTheme.corners.xLarge,
        contentPadding = PaddingValues(
            horizontal = PulseTheme.space.huge,
            vertical = 36.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(PulseTheme.corners.fullRounded)
                .background(PulseTheme.color.primaryContainer)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(PulseTheme.color.primary),
                modifier = Modifier.size(PulseTheme.size.medium),
            )
        }
        Spacer(Modifier.height(PulseTheme.space.xLarge))
        BasicText(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = PulseTheme.typography.sectionTitle.copy(
                color = PulseTheme.color.shadePrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = description,
            modifier = Modifier.fillMaxWidth(),
            style = PulseTheme.typography.contentSecondary.copy(
                color = PulseTheme.color.shadeSecondary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(28.dp))
        CompactButton(
            caption = actionLabel,
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseEmptyStateCardPreview() {
    PulseTheme {
        Box(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp),
        ) {
            PulseEmptyStateCard(
                title = "No Workouts Recorded Yet",
                description = "Complete a workout and save it on your device to see your training history, volume trends, and set breakdowns here.",
                actionLabel = "Go to Workouts",
                onActionClick = {},
            )
        }
    }
}
