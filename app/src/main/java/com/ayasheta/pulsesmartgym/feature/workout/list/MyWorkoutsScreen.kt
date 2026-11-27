package com.ayasheta.pulsesmartgym.feature.workout.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Play
import com.ayasheta.pulsesmartgym.feature.workout.components.StartWorkoutDeferredDialog
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun MyWorkoutsScreen(
    viewModel: MyWorkoutsViewModel,
    onNewPlanClick: () -> Unit,
    onPlanClick: (String) -> Unit,
    onStartPlanClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyWorkoutsScreenContent(
        uiState = uiState,
        onNewPlanClick = onNewPlanClick,
        onPlanClick = onPlanClick,
        onStartPlanClick = onStartPlanClick,
        modifier = modifier,
    )
}

@Composable
fun MyWorkoutsScreenContent(
    uiState: MyWorkoutsUiState,
    onNewPlanClick: () -> Unit,
    onPlanClick: (String) -> Unit,
    onStartPlanClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.RootNavigationHeader(
                overline = "CHOOSE YOUR NEXT SESSION",
                title = "My Workouts",
                actionLabel = "+ New Plan",
                onActionClick = onNewPlanClick,
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WorkoutPalette.Divider),
        )

        when (uiState) {
            MyWorkoutsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("workouts_loading"))
                }
            }

            MyWorkoutsUiState.SignedOut -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Sign in to view and manage your workout plans.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            color = WorkoutPalette.TextMuted,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("workouts_signed_out"),
                    )
                }
            }

            is MyWorkoutsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.contentPrimary.copy(
                            color = WorkoutPalette.ErrorText,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("workouts_error"),
                    )
                }
            }

            is MyWorkoutsUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("workouts_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (uiState.plans.isEmpty()) {
                        item(key = "empty_workouts") {
                            EmptyWorkoutsCard(onCreateClick = onNewPlanClick)
                        }
                    } else {
                        items(
                            items = uiState.plans,
                            key = { it.id },
                        ) { plan ->
                            WorkoutPlanListCard(
                                plan = plan,
                                onCardClick = { onPlanClick(plan.id) },
                                onStartClick = { onStartPlanClick(plan.id) },
                            )
                        }
                        item(key = "create_new_inline") {
                            CreateNewPlanInlineCard(onClick = onNewPlanClick)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutPlanListCard(
    plan: WorkoutPlanCardUiModel,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x14191919),
                spotColor = Color(0x14191919),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutPalette.CardSurface)
            .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(12.dp))
            .clickable(onClick = onCardClick)
            .padding(14.dp)
            .testTag("workout_plan_card_${plan.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = plan.thumbnailRes),
                contentDescription = plan.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = plan.name,
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCardClick)
                            .testTag("workout_plan_more_${plan.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(WorkoutPalette.TextBrownMuted),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                BasicText(
                    text = plan.subtitleText,
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = WorkoutPalette.TextBrownMuted,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    plan.categoryChips.forEach { category ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(WorkoutPalette.InputBackground)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            BasicText(
                                text = category,
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextBrownMuted,
                                ),
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(WorkoutPalette.PrimaryCoral)
                    .clickable(onClick = onStartClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("workout_plan_start_${plan.id}"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = Play,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(10.dp),
                )
                BasicText(
                    text = "Start",
                    style = PulseTheme.typography.ctaPrimary.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CreateNewPlanInlineCard(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutPalette.InlineCreateBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("button_create_new_plan_inline"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(WorkoutPalette.InputBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = Plus,
                contentDescription = null,
                colorFilter = ColorFilter.tint(WorkoutPalette.TextBrownMuted),
                modifier = Modifier.size(16.dp),
            )
        }
        BasicText(
            text = "Create New Workout Plan",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextBrownMuted,
            ),
        )
    }
}

@Composable
private fun EmptyWorkoutsCard(
    onCreateClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.5.dp, Color(0xFFE0E0E0)), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 32.dp)
            .testTag("workouts_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(WorkoutPalette.PrimaryCoralTint),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = Plus,
                contentDescription = null,
                colorFilter = ColorFilter.tint(WorkoutPalette.PrimaryCoral),
                modifier = Modifier.size(22.dp),
            )
        }
        BasicText(
            text = "No Workout Plans Yet",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoutPalette.TextPrimary,
            ),
        )
        BasicText(
            text = "Create your first custom workout plan to organize exercises, sets, target reps, and rest times.",
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = WorkoutPalette.TextMuted,
                textAlign = TextAlign.Center,
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WorkoutPalette.PrimaryCoral)
                .clickable(onClick = onCreateClick)
                .testTag("button_empty_create_plan"),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Create New Workout Plan",
                style = PulseTheme.typography.ctaPrimary.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
            )
        }
    }
}
