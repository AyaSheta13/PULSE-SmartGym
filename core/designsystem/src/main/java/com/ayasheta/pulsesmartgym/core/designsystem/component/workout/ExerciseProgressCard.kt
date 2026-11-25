package com.ayasheta.pulsesmartgym.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.card.Card
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseProgressBar
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun PulseExerciseProgressCard(
    exerciseName: String,
    summaryText: String,
    progressFraction: Float,
    progressBadgeText: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = PulseTheme.corners.xLarge,
        contentPadding = PaddingValues(PulseTheme.space.large),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    BasicText(
                        text = exerciseName,
                        style = PulseTheme.typography.sectionHeader.copy(
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                    BasicText(
                        text = summaryText,
                        style = PulseTheme.typography.supportingContent.copy(
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(PulseTheme.corners.fullRounded)
                        .background(PulseTheme.color.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    BasicText(
                        text = progressBadgeText,
                        style = PulseTheme.typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.color.primary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(PulseTheme.space.medium))
            PulseProgressBar(progress = progressFraction)
            Spacer(Modifier.height(PulseTheme.space.large))
            Column(
                verticalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                content = content,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseExerciseProgressCardPreview() {
    PulseTheme {
        Box(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp),
        ) {
            PulseExerciseProgressCard(
                exerciseName = "Barbell Bench Press",
                summaryText = "2/4 sets completed · Rest 90s",
                progressFraction = 0.5f,
                progressBadgeText = "2/4",
            ) {
                PulseExerciseSetRow(
                    setNumber = 1,
                    repsText = "10 reps done",
                    weightText = "80 kg",
                    status = SetRowStatus.COMPLETED,
                )
                PulseExerciseSetRow(
                    setNumber = 2,
                    repsText = "10 reps done",
                    weightText = "80 kg",
                    status = SetRowStatus.COMPLETED,
                )
                PulseExerciseSetRow(
                    setNumber = 3,
                    repsText = "10 reps",
                    status = SetRowStatus.IN_PROGRESS,
                    onActionClick = {},
                )
                PulseExerciseSetRow(
                    setNumber = 4,
                    repsText = "10 reps",
                    status = SetRowStatus.UNSTARTED,
                    onActionClick = {},
                )
            }
        }
    }
}
