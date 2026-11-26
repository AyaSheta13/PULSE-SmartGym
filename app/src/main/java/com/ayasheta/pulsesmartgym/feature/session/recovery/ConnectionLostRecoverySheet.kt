package com.ayasheta.pulsesmartgym.feature.session.recovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun ConnectionLostRecoverySheet(
    visible: Boolean,
    setIndex: Int,
    fullReps: Int,
    partialReps: Int,
    recordedSetsCount: Int,
    isReconnecting: Boolean,
    reconnectErrorMessage: String?,
    onReconnectClick: () -> Unit,
    onContinueManuallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .testTag("connection_lost_recovery_overlay"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .testTag("connection_lost_recovery_sheet"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF4DC)),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "!",
                        style = PulseTheme.typography.sectionTitle.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                        ),
                    )
                }

                BasicText(
                    text = "Device Disconnected",
                    style = PulseTheme.typography.sectionTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )

                val partialSuffix = if (partialReps > 0) " + $partialReps partial" else ""
                val priorNote = if (recordedSetsCount > 0) "$recordedSetsCount completed set(s) saved, " else ""
                BasicText(
                    text = "Connection signal lost. All progress (${priorNote}Set $setIndex: $fullReps full reps$partialSuffix) and elapsed time are safely preserved. Reconnect to resume tracking, or continue manually without interruption.",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFF6B6763),
                        textAlign = TextAlign.Center,
                    ),
                )

                if (reconnectErrorMessage != null) {
                    BasicText(
                        text = reconnectErrorMessage,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WorkoutPalette.ErrorText,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("recovery_reconnect_error"),
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                PulsePrimaryButton(
                    caption = if (isReconnecting) "Reconnecting..." else "Reconnect Device",
                    onClick = { if (!isReconnecting) onReconnectClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("button_recovery_reconnect"),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE5DFD9)), RoundedCornerShape(14.dp))
                        .clickable(enabled = !isReconnecting, onClick = onContinueManuallyClick)
                        .testTag("button_recovery_continue_manually"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Continue Manually (Keep All Reps)",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }
            }
        }
    }
}
