package com.ayasheta.pulsesmartgym.feature.workout.picker

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Close
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel

@Composable
fun ExercisePickerScreen(
    viewModel: ExercisePickerViewModel,
    onBackClick: () -> Unit,
    onSelectionConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerScreenContent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onToggleExercise = viewModel::toggleExercise,
        onBackClick = {
            viewModel.cancelSelection()
            onBackClick()
        },
        onConfirmClick = {
            if (viewModel.confirmSelection()) {
                onSelectionConfirmed()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ExercisePickerScreenContent(
    uiState: ExercisePickerUiState,
    onQueryChanged: (String) -> Unit,
    onFilterSelected: (MuscleGroup?) -> Unit,
    onToggleExercise: (String) -> Unit,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInsetOverride: Dp? = null,
) {
    val filterOptions: List<MuscleGroup?> = listOf(
        null,
        MuscleGroup.CHEST,
        MuscleGroup.BACK,
        MuscleGroup.LEGS,
        MuscleGroup.SHOULDERS,
        MuscleGroup.BICEPS,
        MuscleGroup.TRICEPS,
        MuscleGroup.CORE,
        MuscleGroup.FULL_BODY,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PulseTopAppBar(
                style = PulseTopAppBarStyle.BackWithTitleAndAction(
                    title = "Select Exercises",
                    actionLabel = "${uiState.selectedCount} Selected",
                    onBackClick = onBackClick,
                    onActionClick = {},
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WorkoutPalette.Divider),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WorkoutPalette.Background.copy(alpha = 0.96f))
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                BasicText(
                    text = "Choose exercises to add to your workout plan.",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = WorkoutPalette.TextBrownMuted,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                        .shadow(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color(0x0D191919),
                            spotColor = Color(0x0D191919),
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        imageVector = PickerSearchIcon,
                        contentDescription = "Search",
                        colorFilter = ColorFilter.tint(Color(0xFF8E7069)),
                        modifier = Modifier.size(16.dp),
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (uiState.query.isEmpty()) {
                            BasicText(
                                text = "Search exercises...",
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF8E7069),
                                ),
                            )
                        }
                        BasicTextField(
                            value = uiState.query,
                            onValueChange = onQueryChanged,
                            singleLine = true,
                            textStyle = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = WorkoutPalette.TextPrimary,
                            ),
                            cursorBrush = SolidColor(WorkoutPalette.PrimaryCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("picker_search_input"),
                        )
                    }
                    if (uiState.query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(WorkoutPalette.InputBackground)
                                .clickable { onQueryChanged("") }
                                .testTag("picker_clear_search"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                imageVector = Close,
                                contentDescription = "Clear search",
                                colorFilter = ColorFilter.tint(WorkoutPalette.TextBrownMuted),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = uiState.selectedMuscleFilter == filter
                        val label = filter?.displayLabel() ?: "All"
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(
                                    if (isSelected) WorkoutPalette.PrimaryCoral else Color.White,
                                )
                                .border(
                                    BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) WorkoutPalette.PrimaryCoral else WorkoutPalette.CardBorder,
                                    ),
                                    RoundedCornerShape(9999.dp),
                                )
                                .clickable { onFilterSelected(filter) }
                                .padding(horizontal = 16.dp)
                                .testTag("picker_filter_${filter?.name ?: "ALL"}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = label,
                                style = PulseTheme.typography.ctaPrimary.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFFFCF9F8) else WorkoutPalette.TextBrownMuted,
                                ),
                            )
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PulseLoadingIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = uiState.errorMessage,
                            style = PulseTheme.typography.contentPrimary.copy(
                                color = WorkoutPalette.ErrorText,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }

                uiState.exercises.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "No exercises match your search or filter.",
                            style = PulseTheme.typography.contentPrimary.copy(
                                color = WorkoutPalette.TextMuted,
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier.testTag("picker_empty_results"),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("picker_exercise_list"),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 0.dp,
                            bottom = 112.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.exercises,
                            key = { it.id },
                        ) { item ->
                            ExercisePickerRowCard(
                                item = item,
                                onToggle = { onToggleExercise(item.id) },
                            )
                        }
                    }
                }
            }
        }

        // Fixed Bottom Elevation Action Drawer
        val navBarBottom = bottomInsetOverride
            ?: WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val drawerBottomPadding = (navBarBottom + 8.dp).coerceAtLeast(24.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WorkoutPalette.Background.copy(alpha = 0f),
                            WorkoutPalette.Background.copy(alpha = 0.95f),
                            WorkoutPalette.Background,
                        ),
                    ),
                )
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = drawerBottomPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x1F191919),
                        spotColor = Color(0x1F191919),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(12.dp),
            ) {
                val canConfirm = uiState.selectedCount > 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(if (canConfirm) 1f else 0.45f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkoutPalette.PrimaryCoral)
                        .clickable(enabled = canConfirm, onClick = onConfirmClick)
                        .padding(horizontal = 20.dp)
                        .testTag("picker_confirm_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = "Add Selected Exercises (${uiState.selectedCount})  →",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerRowCard(
    item: ExercisePickerItemUiModel,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x10191919),
                spotColor = Color(0x10191919),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp)
            .testTag("picker_exercise_${item.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = item.thumbnailRes),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = item.name,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(WorkoutPalette.InputBackground)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        BasicText(
                            text = item.muscleSubtitle,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextBrownMuted,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(WorkoutPalette.EquipmentMintBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        BasicText(
                            text = item.equipmentLabel,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.EquipmentMintText,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (item.isSelected) WorkoutPalette.PrimaryCoral else WorkoutPalette.InputBackground,
                )
                .clickable(onClick = onToggle)
                .testTag("picker_toggle_${item.id}"),
            contentAlignment = Alignment.Center,
        ) {
            if (item.isSelected) {
                Image(
                    imageVector = CheckMark,
                    contentDescription = "Selected",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(16.dp),
                )
            } else {
                BasicText(
                    text = "+",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextBrownMuted,
                    ),
                )
            }
        }
    }
}

private val PickerSearchIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PickerSearchIcon",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // Circle centered at (11, 11) with radius 7
            moveTo(18f, 11f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 11f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18f, 11f)
            // Handle from (16, 16) to (21, 21)
            moveTo(16f, 16f)
            lineTo(21f, 21f)
        }
    }.build()
}
