package com.ayasheta.pulsesmartgym.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Back
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.HistoryClock
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun WorkoutHistoryScreen(
    viewModel: WorkoutHistoryViewModel,
    onSessionClick: (String) -> Unit,
    onGoToWorkoutsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutHistoryScreenContent(
        uiState = uiState,
        onSessionClick = onSessionClick,
        onGoToWorkoutsClick = onGoToWorkoutsClick,
        onRetryClick = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun WorkoutHistoryScreenContent(
    uiState: WorkoutHistoryUiState,
    onSessionClick: (String) -> Unit,
    onGoToWorkoutsClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeLabel = when (uiState) {
        WorkoutHistoryUiState.Empty -> "0 Saved"
        else -> "Local Storage"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background)
            .testTag("workout_history_screen"),
    ) {
        HistoryTopBar(badgeLabel = badgeLabel)

        when (uiState) {
            WorkoutHistoryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("history_loading"))
                }
            }

            WorkoutHistoryUiState.SignedOut -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        text = "Sign in to view your saved workout history.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 15.sp,
                            color = WorkoutPalette.TextMuted,
                        ),
                        modifier = Modifier.testTag("history_signed_out"),
                    )
                }
            }

            is WorkoutHistoryUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("history_error"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 15.sp,
                            color = WorkoutPalette.ErrorText,
                        ),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PulsePrimaryButton(
                        caption = "Retry",
                        onClick = onRetryClick,
                        modifier = Modifier.fillMaxWidth(0.6f).height(46.dp),
                    )
                }
            }

            WorkoutHistoryUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 24.dp, vertical = 36.dp)
                            .testTag("history_empty_card"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                imageVector = HistoryClock,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(PulseTheme.color.primary),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        BasicText(
                            text = "No Workouts Recorded Yet",
                            style = PulseTheme.typography.sectionTitle.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        BasicText(
                            text = "Complete a workout and save it on your device to see your training history, volume trends, and set breakdowns here.",
                            style = PulseTheme.typography.contentSecondary.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = WorkoutPalette.TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            ),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PulsePrimaryButton(
                            caption = "Go to Workouts",
                            onClick = onGoToWorkoutsClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("button_history_go_to_workouts"),
                        )
                    }
                }
            }

            is WorkoutHistoryUiState.Populated -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "history_aggregate_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .testTag("history_aggregate_card"),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            BasicText(
                                text = uiState.savedSessionsHeadline,
                                style = PulseTheme.typography.cardTitle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                            )
                            BasicText(
                                text = uiState.aggregateMetricsLine,
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WorkoutPalette.TextMuted,
                                ),
                            )
                        }
                    }

                    uiState.sections.forEach { section ->
                        item(key = "section_${section.key}") {
                            BasicText(
                                text = section.title,
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(
                            items = section.items,
                            key = { it.sessionId },
                        ) { item ->
                            HistorySessionCard(
                                item = item,
                                onClick = { onSessionClick(item.sessionId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(badgeLabel: String) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Workout History",
                style = PulseTheme.typography.sectionTitle.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            Box(
                modifier = Modifier
                    .clip(PulseTheme.corners.fullRounded)
                    .background(Color(0xFFF6F4F2))
                    .border(BorderStroke(1.dp, Color(0xFFE4E0DC)), PulseTheme.corners.fullRounded)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("history_header_badge"),
            ) {
                BasicText(
                    text = badgeLabel,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFECE8E3)),
        )
    }
}

@Composable
private fun HistorySessionCard(
    item: HistorySessionItemUiModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "${item.planName}, ${item.subtitleLine}, ${item.metricsLine}, ${item.statusBadgeLine}"
            }
            .padding(14.dp)
            .testTag("history_session_card_${item.sessionId}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = item.thumbnailRes),
            contentDescription = item.planName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = item.planName,
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = item.subtitleLine,
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 13.sp,
                    color = WorkoutPalette.TextMuted,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = item.metricsLine,
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = WorkoutPalette.TextPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = item.statusBadgeLine,
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B8A5A),
                ),
            )
        }
        Image(
            imageVector = Back,
            contentDescription = null,
            colorFilter = ColorFilter.tint(WorkoutPalette.TextMuted),
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = 180f },
        )
    }
}
