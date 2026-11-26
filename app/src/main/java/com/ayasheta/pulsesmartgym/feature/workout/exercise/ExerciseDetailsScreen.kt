package com.ayasheta.pulsesmartgym.feature.workout.exercise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun ExerciseDetailsScreen(
    viewModel: ExerciseDetailsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseDetailsScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailsScreenContent(
    uiState: ExerciseDetailsUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background)
            .testTag("exercise_details_screen"),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.BackWithTitle(
                title = "Exercise Details",
                onBackClick = onBackClick,
            ),
        )

        when (uiState) {
            ExerciseDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("exercise_details_loading"))
                }
            }

            is ExerciseDetailsUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Exercise details not found.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            color = WorkoutPalette.TextMuted,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("exercise_details_not_found"),
                    )
                }
            }

            is ExerciseDetailsUiState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Hero Card: Image, Category/Body Part Badges, Title & Summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(WorkoutPalette.CardSurface)
                            .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            painter = painterResource(id = uiState.imageRes),
                            contentDescription = uiState.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(204.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .testTag("exercise_details_image"),
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExerciseBadgePill(
                                label = uiState.bodyPartLabel.uppercase(),
                                backgroundColor = WorkoutPalette.PrimaryCoralTint,
                                contentColor = WorkoutPalette.PrimaryCoralDark,
                            )
                            ExerciseBadgePill(
                                label = uiState.equipmentLabel.uppercase(),
                                backgroundColor = Color(0xFFF6F3F2),
                                contentColor = WorkoutPalette.TextPrimary,
                            )
                            ExerciseBadgePill(
                                label = uiState.categoryLabel.uppercase(),
                                backgroundColor = Color(0xFFE7F6EF),
                                contentColor = Color(0xFF1B8A5A),
                            )
                        }

                        BasicText(
                            text = uiState.name,
                            style = PulseTheme.typography.sectionTitle.copy(
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                            modifier = Modifier.testTag("exercise_details_title"),
                        )

                        BasicText(
                            text = uiState.guide.summary,
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = WorkoutPalette.TextMuted,
                            ),
                        )
                    }

                    // Target Muscles Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(WorkoutPalette.CardSurface)
                            .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BasicText(
                            text = "Target Muscles",
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BasicText(
                                text = "PRIMARY",
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.PrimaryCoralDark,
                                ),
                            )
                            BasicText(
                                text = uiState.guide.primaryMuscles,
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                                modifier = Modifier.testTag("exercise_details_primary_muscles"),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BasicText(
                                text = "SECONDARY & STABILIZERS",
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextMuted,
                                ),
                            )
                            BasicText(
                                text = uiState.guide.secondaryMuscles,
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = WorkoutPalette.TextMuted,
                                ),
                            )
                        }
                    }

                    // Step-by-Step Instructions Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(WorkoutPalette.CardSurface)
                            .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                            .testTag("exercise_details_instructions"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BasicText(
                            text = "Step-by-Step Instructions",
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )

                        uiState.guide.instructions.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(WorkoutPalette.PrimaryCoralTint),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    BasicText(
                                        text = "${index + 1}",
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WorkoutPalette.PrimaryCoral,
                                        ),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    BasicText(
                                        text = step.title,
                                        style = PulseTheme.typography.cardTitle.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WorkoutPalette.TextPrimary,
                                        ),
                                    )
                                    BasicText(
                                        text = step.detail,
                                        style = PulseTheme.typography.contentPrimary.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            color = WorkoutPalette.TextMuted,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    // Form Cues Card
                    if (uiState.guide.formCues.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(WorkoutPalette.CardSurface)
                                .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(18.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BasicText(
                                text = "Form Cues & Safety",
                                style = PulseTheme.typography.cardTitle.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                            )
                            uiState.guide.formCues.forEach { cue ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    BasicText(
                                        text = "•",
                                        style = PulseTheme.typography.contentPrimary.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WorkoutPalette.PrimaryCoral,
                                        ),
                                    )
                                    BasicText(
                                        text = cue,
                                        style = PulseTheme.typography.contentPrimary.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            color = WorkoutPalette.TextMuted,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExerciseBadgePill(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            ),
        )
    }
}
