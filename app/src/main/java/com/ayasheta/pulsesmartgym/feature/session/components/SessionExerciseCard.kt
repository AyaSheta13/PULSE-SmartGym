package com.ayasheta.pulsesmartgym.feature.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionExerciseUiModel
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun SessionExerciseCard(
    exercise: SessionExerciseUiModel,
    preferredWeightUnit: WeightUnit,
    actionsEnabled: Boolean,
    onSetActionClick: (sessionExerciseId: String, setIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    onHowToPerformClick: (sessionExerciseId: String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("session_exercise_card_${exercise.sessionExerciseId}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = exercise.exerciseName,
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (exercise.isCompleted) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF6F3F2))
                        .border(BorderStroke(1.dp, Color(0xFFEBE7E2)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("session_exercise_complete_${exercise.sessionExerciseId}"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        imageVector = CheckMark,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(WorkoutPalette.TextPrimary),
                        modifier = Modifier.size(11.dp),
                    )
                    BasicText(
                        text = "${exercise.completedSetsCount}/${exercise.totalPlannedSetsCount}",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SessionProgressBar(
            progressFraction = exercise.progressFraction,
            heightDp = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = if (exercise.isCompleted) {
                    "${exercise.completedSetsCount} of ${exercise.totalPlannedSetsCount} completed ✓"
                } else {
                    "${exercise.completedSetsCount} of ${exercise.totalPlannedSetsCount} sets completed"
                },
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 12.sp,
                    fontWeight = if (exercise.isCompleted) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (exercise.isCompleted) Color(0xFF1F7A4D) else WorkoutPalette.TextMuted,
                ),
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(WorkoutPalette.PrimaryCoralTint)
                    .border(BorderStroke(1.dp, Color(0xFFFFD1C7)), CircleShape)
                    .clickable { onHowToPerformClick(exercise.sessionExerciseId) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("session_how_to_perform_${exercise.sessionExerciseId}"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(WorkoutPalette.PrimaryCoral),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "i",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
                BasicText(
                    text = "How to perform ›",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.PrimaryCoral,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF3F0EC)),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            exercise.sets.forEach { setRow ->
                SessionSetRow(
                    row = setRow,
                    preferredWeightUnit = preferredWeightUnit,
                    enabled = actionsEnabled,
                    onActionClick = { onSetActionClick(exercise.sessionExerciseId, setRow.setIndex) },
                )
            }
        }
    }
}
