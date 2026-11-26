package com.ayasheta.pulsesmartgym.feature.workout.details

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.workout.components.StartWorkoutDeferredDialog
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun WorkoutDetailsScreen(
    viewModel: WorkoutDetailsViewModel,
    onBackClick: () -> Unit,
    onEditPlanClick: (String) -> Unit,
    onStartWorkoutClick: (String) -> Unit = {},
    onExerciseDetailsClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutDetailsScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onToggleExercise = viewModel::toggleExercise,
        onEditPlanClick = onEditPlanClick,
        onStartWorkoutClick = onStartWorkoutClick,
        onExerciseDetailsClick = onExerciseDetailsClick,
        modifier = modifier,
    )
}

@Composable
fun WorkoutDetailsScreenContent(
    uiState: WorkoutDetailsUiState,
    onBackClick: () -> Unit,
    onToggleExercise: (String) -> Unit,
    onEditPlanClick: (String) -> Unit,
    onStartWorkoutClick: (String) -> Unit = {},
    onExerciseDetailsClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.BackWithTitle(
                title = "Workout Details",
                onBackClick = onBackClick,
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WorkoutPalette.Divider),
        )

        when (uiState) {
            WorkoutDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("details_loading"))
                }
            }

            WorkoutDetailsUiState.SignedOut -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Session expired. Please sign in again.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            color = WorkoutPalette.TextMuted,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("details_signed_out"),
                    )
                }
            }

            is WorkoutDetailsUiState.NotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 16.sp,
                            color = WorkoutPalette.TextPrimary,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("details_not_found"),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WorkoutPalette.PrimaryCoral)
                            .clickable(onClick = onBackClick)
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                            .testTag("details_not_found_back"),
                    ) {
                        BasicText(
                            text = "Back to My Workouts",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }

            is WorkoutDetailsUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("details_scroll_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "overview_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("details_overview_card"),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WorkoutPalette.PrimaryCoralTint)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                BasicText(
                                    text = uiState.categoryTag,
                                    style = PulseTheme.typography.label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutPalette.PrimaryCoral,
                                        letterSpacing = 0.5.sp,
                                    ),
                                )
                            }
                            BasicText(
                                text = uiState.planName,
                                style = PulseTheme.typography.cardTitle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                                modifier = Modifier.testTag("details_plan_name"),
                            )
                            BasicText(
                                text = uiState.metaLine,
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WorkoutPalette.TextMuted,
                                ),
                                modifier = Modifier.testTag("details_meta_line"),
                            )
                            if (!uiState.description.isNullOrBlank()) {
                                BasicText(
                                    text = uiState.description,
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 12.sp,
                                        color = WorkoutPalette.TextBrownMuted,
                                    ),
                                )
                            }
                        }
                    }

                    item(key = "exercises_header") {
                        BasicText(
                            text = "Exercises (${uiState.exerciseCount})",
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }

                    items(
                        items = uiState.exercises,
                        key = { it.occurrenceId },
                    ) { exercise ->
                        val isExpanded = exercise.occurrenceId == uiState.expandedOccurrenceId
                        WorkoutDetailExerciseCard(
                            exercise = exercise,
                            isExpanded = isExpanded,
                            onClick = { onToggleExercise(exercise.occurrenceId) },
                            onExerciseDetailsClick = { onExerciseDetailsClick(exercise.exerciseId) },
                        )
                    }
                }

                // Fixed Bottom Action Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            ambientColor = Color(0x0D000000),
                            spotColor = Color(0x0D000000),
                        )
                        .background(Color.White),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(WorkoutPalette.CardBorder),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(232f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(WorkoutPalette.PrimaryCoral)
                                .clickable { onStartWorkoutClick(uiState.planId) }
                                .testTag("details_start_workout_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "Start Workout",
                                style = PulseTheme.typography.ctaPrimary.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(112f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFE5E0DC)), RoundedCornerShape(16.dp))
                                .clickable { onEditPlanClick(uiState.planId) }
                                .testTag("details_edit_plan_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "Edit Plan",
                                style = PulseTheme.typography.ctaPrimary.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutDetailExerciseCard(
    exercise: WorkoutDetailExerciseUiModel,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExerciseDetailsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = if (isExpanded) 14.dp else 12.dp,
            )
            .testTag("details_exercise_card_${exercise.occurrenceId}"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isExpanded) WorkoutPalette.PrimaryCoralTint else Color(0xFFF8F6F4),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = exercise.orderNumber.toString(),
                        style = PulseTheme.typography.label.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpanded) WorkoutPalette.PrimaryCoral else WorkoutPalette.TextPrimary,
                        ),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    BasicText(
                        text = exercise.name,
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.SemiBold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = exercise.muscleSubtitle,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isExpanded) WorkoutPalette.PrimaryCoral else WorkoutPalette.TextMuted,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isExpanded) {
                    BasicText(
                        text = exercise.collapsedSummary,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoutPalette.TextSubtle,
                        ),
                    )
                }
                BasicText(
                    text = if (isExpanded) "⌃" else "⌄",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) WorkoutPalette.PrimaryCoral else WorkoutPalette.TextSubtle,
                    ),
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_expanded_body_${exercise.occurrenceId}"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = exercise.photoRes),
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailsMetricTile(
                        label = "SETS",
                        value = exercise.setsCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    DetailsMetricTile(
                        label = "TARGET REPS",
                        value = exercise.targetRepsLabel,
                        modifier = Modifier.weight(1f),
                    )
                    DetailsMetricTile(
                        label = "REST",
                        value = exercise.restLabel,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (exercise.hasMixedOrLoadDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(WorkoutPalette.MetricTileBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        exercise.perSetRows.forEach { row ->
                            val restText = row.restSeconds?.let { "${it}s rest" } ?: "Default rest"
                            val loadPart = row.loadLabel?.let { " • $it" }.orEmpty()
                            BasicText(
                                text = "Set ${row.setIndex}: ${row.targetReps} reps$loadPart • $restText",
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WorkoutPalette.TextBrownMuted,
                                ),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(WorkoutPalette.PrimaryCoralTint)
                            .clickable(onClick = onExerciseDetailsClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("details_exercise_guide_${exercise.exerciseId}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "View Exercise Guide ›",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.PrimaryCoralDark,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WorkoutPalette.MetricTileBackground)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = WorkoutPalette.TextSubtle,
            ),
            maxLines = 1,
        )
        BasicText(
            text = value,
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoutPalette.TextPrimary,
            ),
            maxLines = 1,
        )
    }
}
