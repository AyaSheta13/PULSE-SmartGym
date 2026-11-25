package com.ayasheta.pulsesmartgym.core.designsystem.component.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark

enum class SetRowStatus {
    UNSTARTED,
    IN_PROGRESS,
    COMPLETED,
}

@Composable
fun PulseExerciseSetRow(
    setNumber: Int,
    repsText: String,
    status: SetRowStatus,
    modifier: Modifier = Modifier,
    weightText: String? = null,
    actionLabel: String = "Start",
    onActionClick: (() -> Unit)? = null,
) {
    val backgroundColor = when (status) {
        SetRowStatus.COMPLETED -> PulseTheme.color.completedSetBackground
        SetRowStatus.IN_PROGRESS -> PulseTheme.color.primaryContainer
        SetRowStatus.UNSTARTED -> PulseTheme.color.surfaceSubtle
    }
    val borderColor = when (status) {
        SetRowStatus.COMPLETED -> PulseTheme.color.completedSetBorder
        SetRowStatus.IN_PROGRESS -> PulseTheme.color.primary
        SetRowStatus.UNSTARTED -> PulseTheme.color.border
    }
    val rowHeight = if (status == SetRowStatus.COMPLETED) 46.dp else 49.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(PulseTheme.corners.medium)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = PulseTheme.corners.medium)
            .padding(horizontal = PulseTheme.space.medium, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            SetStatusIndicator(status = status)
            if (status == SetRowStatus.COMPLETED) {
                BasicText(
                    text = "Set $setNumber • $repsText",
                    style = PulseTheme.typography.caption.copy(
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                if (weightText != null) {
                    BasicText(
                        text = "@ $weightText",
                        style = PulseTheme.typography.supportingContent.copy(
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                }
            } else {
                BasicText(
                    text = "Set $setNumber",
                    style = PulseTheme.typography.caption.copy(
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                BasicText(
                    text = "• $repsText",
                    style = PulseTheme.typography.supportingContent.copy(
                        fontWeight = FontWeight.Normal,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
            }
        }

        if (status == SetRowStatus.UNSTARTED) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(PulseTheme.corners.small)
                    .background(PulseTheme.color.primary)
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = actionLabel,
                    style = PulseTheme.typography.supportingContent.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PulseTheme.color.onPrimary,
                    ),
                )
            }
        } else if (status == SetRowStatus.IN_PROGRESS) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(PulseTheme.corners.small)
                    .background(PulseTheme.color.primary.copy(alpha = 0.14f))
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Active Set",
                    style = PulseTheme.typography.label.copy(
                        color = PulseTheme.color.primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SetStatusIndicator(status: SetRowStatus) {
    when (status) {
        SetRowStatus.COMPLETED -> {
            Box(
                modifier = Modifier
                    .size(PulseTheme.size.tiny)
                    .clip(PulseTheme.corners.fullRounded)
                    .background(PulseTheme.color.success),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = CheckMark,
                    contentDescription = "Completed",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        SetRowStatus.IN_PROGRESS -> {
            Box(
                modifier = Modifier
                    .size(PulseTheme.size.tiny)
                    .clip(PulseTheme.corners.fullRounded)
                    .background(PulseTheme.color.primary.copy(alpha = 0.2f))
                    .border(2.dp, PulseTheme.color.primary, PulseTheme.corners.fullRounded),
            )
        }
        SetRowStatus.UNSTARTED -> {
            Box(
                modifier = Modifier
                    .size(PulseTheme.size.tiny)
                    .clip(PulseTheme.corners.fullRounded)
                    .background(Color(0xFFEBEDF2)),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseExerciseSetRowPreview() {
    PulseTheme {
        Column(
            modifier = Modifier
                .background(PulseTheme.color.container)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseExerciseSetRow(
                setNumber = 1,
                repsText = "10 reps done",
                weightText = "80 kg",
                status = SetRowStatus.COMPLETED,
            )
            PulseExerciseSetRow(
                setNumber = 2,
                repsText = "10 reps",
                status = SetRowStatus.IN_PROGRESS,
                onActionClick = {},
            )
            PulseExerciseSetRow(
                setNumber = 3,
                repsText = "10 reps",
                status = SetRowStatus.UNSTARTED,
                onActionClick = {},
            )
        }
    }
}
