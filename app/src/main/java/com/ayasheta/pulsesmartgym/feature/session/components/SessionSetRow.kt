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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionSetRowUiModel
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun SessionSetRow(
    row: SessionSetRowUiModel,
    preferredWeightUnit: WeightUnit,
    enabled: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val performed = row.performed
    if (performed == null) {
        val targetLoadText = SessionUiMapper.formatWeightLoad(row.targetLoad, preferredWeightUnit)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAF8F7))
                .border(BorderStroke(1.dp, Color(0xFFECE8E3)), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("session_set_row_${row.sessionExerciseId}_${row.setIndex}"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.5.dp, Color(0xFFD1CCC7)), CircleShape),
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "Set ${row.setIndex}",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )
                BasicText(
                    text = if (targetLoadText != null) {
                        "• ${row.targetReps} reps @ $targetLoadText"
                    } else {
                        "• ${row.targetReps} reps"
                    },
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7A7570),
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .widthIn(min = 68.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WorkoutPalette.PrimaryCoral)
                    .clickable(enabled = enabled, onClick = onActionClick)
                    .padding(horizontal = 16.dp)
                    .testTag("session_set_start_${row.sessionExerciseId}_${row.setIndex}"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Start",
                    style = PulseTheme.typography.ctaPrimary.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }
        }
    } else {
        val actualLoadText = SessionUiMapper.formatWeightLoad(performed.actualLoad, preferredWeightUnit)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF5F2))
                .border(BorderStroke(1.dp, Color(0xFFFF8A6E)), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp)
                .testTag("session_set_row_${row.sessionExerciseId}_${row.setIndex}"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00B87C)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = CheckMark,
                    contentDescription = "Completed",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(12.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = "Set ${row.setIndex} • ${performed.fullReps} reps done",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    if (actualLoadText != null) {
                        BasicText(
                            text = "@ $actualLoadText",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF7A7570),
                            ),
                        )
                    }
                }
                if (performed.partialReps > 0) {
                    BasicText(
                        text = "+ ${performed.partialReps} partial reps",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7A7570),
                        ),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFEAE3DC)), RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled, onClick = onActionClick)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("session_set_edit_${row.sessionExerciseId}_${row.setIndex}"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Edit",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5A5754),
                    ),
                )
            }
        }
    }
}
