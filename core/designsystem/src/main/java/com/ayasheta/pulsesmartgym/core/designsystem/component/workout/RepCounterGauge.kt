package com.ayasheta.pulsesmartgym.core.designsystem.component.workout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.card.Card
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Minus
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus

@Composable
fun PulseRepCounterGauge(
    currentReps: Int,
    targetReps: Int,
    onDecrementRep: () -> Unit,
    onIncrementRep: () -> Unit,
    modifier: Modifier = Modifier,
    overlineLabel: String = "REPS DONE",
    goalPrefixLabel: String = "GOAL",
    adjustLabel: String = "Adjust count",
) {
    val targetProgress = if (targetReps <= 0) 0f else (currentReps.toFloat() / targetReps.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "rep_counter_progress",
    )
    val trackColor = PulseTheme.color.surfaceMuted
    val arcColor = PulseTheme.color.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = PulseTheme.corners.xLarge,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("rep_counter_card_decorations"),
            ) {
                val haloRadius = 96.dp.toPx()
                drawCircle(
                    color = Color(0x16FF623E),
                    radius = haloRadius,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width - 48.dp.toPx(),
                        y = 48.dp.toPx(),
                    ),
                )
                drawCircle(
                    color = Color(0x1C39B8FD),
                    radius = haloRadius,
                    center = androidx.compose.ui.geometry.Offset(
                        x = 48.dp.toPx(),
                        y = size.height - 48.dp.toPx(),
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PulseTheme.space.xLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Box(
                modifier = Modifier
                    .size(224.dp)
                    .testTag("rep_counter_gauge_arc")
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = animatedProgress,
                            range = 0f..1f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke,
                    )
                    if (animatedProgress > 0f) {
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = stroke,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        text = overlineLabel,
                        style = PulseTheme.typography.label.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                    BasicText(
                        text = currentReps.toString(),
                        style = PulseTheme.typography.numericGauge.copy(
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                    Row(
                        modifier = Modifier
                            .clip(PulseTheme.corners.fullRounded)
                            .background(PulseTheme.color.surfaceMuted)
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = goalPrefixLabel,
                            style = PulseTheme.typography.label.copy(
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.color.shadePrimary,
                            ),
                        )
                        BasicText(
                            text = targetReps.toString(),
                            style = PulseTheme.typography.label.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = PulseTheme.color.primaryDark,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(PulseTheme.space.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.large, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepAdjustButton(
                    icon = Minus,
                    contentDescription = "Subtract one rep",
                    onClick = onDecrementRep,
                )
                BasicText(
                    text = adjustLabel,
                    style = PulseTheme.typography.label.copy(
                        fontWeight = FontWeight.Medium,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
                RepAdjustButton(
                    icon = Plus,
                    contentDescription = "Add one rep",
                    onClick = onIncrementRep,
                )
            }
        }
        }
    }
}

@Composable
private fun RepAdjustButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(64.dp)
            .height(PulseTheme.size.large)
            .clip(PulseTheme.corners.large)
            .background(PulseTheme.color.surfaceMuted)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(PulseTheme.color.shadePrimary),
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        BasicText(
            text = "1",
            style = PulseTheme.typography.label.copy(
                fontWeight = FontWeight.Bold,
                color = PulseTheme.color.shadePrimary,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseRepCounterGaugePreview() {
    PulseTheme {
        Box(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp),
        ) {
            PulseRepCounterGauge(
                currentReps = 8,
                targetReps = 10,
                onDecrementRep = {},
                onIncrementRep = {},
            )
        }
    }
}
