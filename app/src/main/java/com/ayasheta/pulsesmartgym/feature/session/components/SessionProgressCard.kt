package com.ayasheta.pulsesmartgym.feature.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.HistoryClock
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun SessionProgressCard(
    planName: String,
    elapsedTimeText: String,
    recordedSetsCount: Int,
    plannedSetsCount: Int,
    progressFraction: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("session_progress_card"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = planName,
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("session_plan_name"),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF6F3F2))
                    .border(BorderStroke(1.dp, Color(0xFFEBE7E2)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("session_elapsed_timer"),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.Image(
                    imageVector = HistoryClock,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF6E6A66)),
                    modifier = Modifier.size(13.dp),
                )
                BasicText(
                    text = elapsedTimeText,
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5A5754),
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        BasicText(
            text = "$recordedSetsCount / $plannedSetsCount Sets Completed",
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
            modifier = Modifier.testTag("session_overall_progress_text"),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SessionProgressBar(
            progressFraction = progressFraction,
            heightDp = 6,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SessionProgressBar(
    progressFraction: Float,
    heightDp: Int,
    modifier: Modifier = Modifier,
) {
    val clamped = progressFraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(heightDp.dp)
            .clip(RoundedCornerShape((heightDp / 2).dp))
            .background(Color(0xFFEFECE9)),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape((heightDp / 2).dp))
                    .background(WorkoutPalette.PrimaryCoral),
            )
        }
    }
}
