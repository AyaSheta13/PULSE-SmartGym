package com.ayasheta.pulsesmartgym.feature.workout.editor

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Trash
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import kotlin.math.roundToInt

@Composable
fun WorkoutPlanEditorScreen(
    viewModel: WorkoutPlanEditorViewModel,
    onOpenExercisePicker: (planId: String?) -> Unit,
    onPlanCreated: (planId: String) -> Unit,
    onPlanUpdated: (planId: String) -> Unit,
    onPlanDeleted: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkoutPlanEditorEvent.Created -> onPlanCreated(event.planId)
                is WorkoutPlanEditorEvent.Updated -> onPlanUpdated(event.planId)
                WorkoutPlanEditorEvent.Deleted -> onPlanDeleted()
                WorkoutPlanEditorEvent.Discarded -> onNavigateBack()
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            uiState.showDeleteConfirmation -> viewModel.dismissDeleteConfirmation()
            uiState.showDiscardConfirmation -> viewModel.dismissDiscardConfirmation()
            else -> viewModel.requestBackOrCancel()
        }
    }

    WorkoutPlanEditorScreenContent(
        uiState = uiState,
        onBackClick = viewModel::requestBackOrCancel,
        onDeleteClick = viewModel::requestDeleteConfirmation,
        onNameChanged = viewModel::onNameChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onAddExerciseClick = {
            if (viewModel.prepareOpenExercisePicker()) {
                onOpenExercisePicker(uiState.planId)
            }
        },
        onRemoveExercise = viewModel::removeExercise,
        onAdjustSets = viewModel::adjustSets,
        onAdjustReps = viewModel::adjustReps,
        onAdjustRest = viewModel::adjustRest,
        onToggleSetDetails = viewModel::toggleSetDetails,
        onUpdateSingleSet = viewModel::updateSingleSet,
        onSaveClick = viewModel::savePlan,
        onCancelClick = viewModel::requestBackOrCancel,
        onDismissDelete = viewModel::dismissDeleteConfirmation,
        onConfirmDelete = viewModel::confirmDeletePlan,
        onDismissDiscard = viewModel::dismissDiscardConfirmation,
        onConfirmDiscard = viewModel::confirmDiscardChanges,
        modifier = modifier,
    )
}

@Composable
fun WorkoutPlanEditorScreenContent(
    uiState: WorkoutPlanEditorUiState,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAddExerciseClick: () -> Unit,
    onRemoveExercise: (String) -> Unit,
    onAdjustSets: (String, Int) -> Unit,
    onAdjustReps: (String, Int) -> Unit,
    onAdjustRest: (String, Int) -> Unit,
    onToggleSetDetails: (String) -> Unit,
    onUpdateSingleSet: (String, Int, Int?, Int?, WeightLoad?, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDiscard: () -> Unit,
    onConfirmDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    inWindowModal: Boolean = false,
) {
    val bgColor = if (uiState.mode == WorkoutPlanEditorMode.CREATE) {
        WorkoutPalette.CreateBackground
    } else {
        WorkoutPalette.Background
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .imePadding(),
    ) {
        PulseTopAppBar(
            style = if (uiState.mode == WorkoutPlanEditorMode.CREATE) {
                PulseTopAppBarStyle.BackWithTitle(
                    title = "Create Workout Plan",
                    onBackClick = onBackClick,
                )
            } else {
                PulseTopAppBarStyle.BackWithTitleAndDelete(
                    title = "Edit Workout Plan",
                    onBackClick = onBackClick,
                    onDeleteClick = onDeleteClick,
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WorkoutPalette.Divider),
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("editor_loading"))
                }
            }

            uiState.isSignedOut -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Session expired. Please sign in again to manage workout plans.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            color = WorkoutPalette.TextMuted,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("editor_signed_out"),
                    )
                }
            }

            uiState.isPlanMissing -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    BasicText(
                        text = uiState.actionError ?: "Workout plan not found.",
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 16.sp,
                            color = WorkoutPalette.TextPrimary,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.testTag("editor_plan_missing"),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WorkoutPalette.PrimaryCoral)
                            .clickable(onClick = onBackClick)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        BasicText(
                            text = "Go Back",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("editor_scroll_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "overview_card") {
                        WorkoutOverviewCard(
                            name = uiState.draft.name,
                            description = uiState.draft.description,
                            onNameChanged = onNameChanged,
                            onDescriptionChanged = onDescriptionChanged,
                        )
                    }

                    item(key = "exercises_header") {
                        ExercisesSectionHeader(
                            count = uiState.draft.exercises.size,
                            onAddExerciseClick = onAddExerciseClick,
                        )
                    }

                    if (uiState.draft.exercises.isEmpty()) {
                        item(key = "empty_exercises_card") {
                            EmptyExercisesCard(onClick = onAddExerciseClick)
                        }
                    } else {
                        items(
                            items = uiState.draft.exercises,
                            key = { it.occurrenceKey },
                        ) { item ->
                            ConfiguredExerciseCard(
                                item = item,
                                preferredUnit = uiState.preferredWeightUnit,
                                isSetDetailsExpanded = item.occurrenceKey in uiState.expandedSetDetailsKeys,
                                onRemove = { onRemoveExercise(item.occurrenceKey) },
                                onAdjustSets = { delta -> onAdjustSets(item.occurrenceKey, delta) },
                                onAdjustReps = { delta -> onAdjustReps(item.occurrenceKey, delta) },
                                onAdjustRest = { delta -> onAdjustRest(item.occurrenceKey, delta) },
                                onToggleSetDetails = { onToggleSetDetails(item.occurrenceKey) },
                                onUpdateSingleSet = { setIdx, reps, rest, load, updateLoad ->
                                    onUpdateSingleSet(item.occurrenceKey, setIdx, reps, rest, load, updateLoad)
                                },
                            )
                        }

                        item(key = "readiness_banner") {
                            OfflinePlanReadinessBanner(
                                totalSets = uiState.draft.totalSetsCount,
                                estimatedMinutes = uiState.estimatedMinutes,
                            )
                        }
                    }

                    val errorMsg = uiState.validationError ?: uiState.actionError
                    if (errorMsg != null) {
                        item(key = "error_banner") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WorkoutPalette.ErrorBg)
                                    .padding(12.dp)
                                    .testTag("editor_error_banner"),
                            ) {
                                BasicText(
                                    text = errorMsg,
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WorkoutPalette.ErrorText,
                                    ),
                                )
                            }
                        }
                    }
                }

                // Bottom Actions Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WorkoutPalette.Background.copy(alpha = 0.96f))
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val primaryLabel = if (uiState.mode == WorkoutPlanEditorMode.CREATE) {
                        "Create Workout Plan"
                    } else {
                        "Save Changes"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = Color(0x1A000000),
                                spotColor = Color(0x1A000000),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(WorkoutPalette.PrimaryCoral)
                            .clickable(enabled = !uiState.isSaving && !uiState.isDeleting, onClick = onSaveClick)
                            .testTag("editor_save_button"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (uiState.isSaving) {
                            PulseLoadingIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            BasicText(
                                text = primaryLabel,
                                style = PulseTheme.typography.ctaPrimary.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WorkoutPalette.NeutralButtonBg)
                            .clickable(enabled = !uiState.isSaving && !uiState.isDeleting, onClick = onCancelClick)
                            .testTag("editor_cancel_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Cancel",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        DeletePlanConfirmationModal(
            planName = uiState.draft.name.ifBlank { "this workout plan" },
            isDeleting = uiState.isDeleting,
            onKeepPlan = onDismissDelete,
            onDeletePlan = onConfirmDelete,
            inWindow = inWindowModal,
        )
    }

    if (uiState.showDiscardConfirmation) {
        DiscardChangesDialog(
            onKeepEditing = onDismissDiscard,
            onDiscard = onConfirmDiscard,
        )
    }
}

@Composable
private fun WorkoutOverviewCard(
    name: String,
    description: String,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x0D000000),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = "Workout Overview",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                text = "WORKOUT NAME",
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextBrownMuted,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WorkoutPalette.InputBackground)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (name.isEmpty()) {
                        BasicText(
                            text = "e.g. Upper Body Push",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = WorkoutPalette.TextPlaceholder,
                            ),
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = onNameChanged,
                        singleLine = true,
                        textStyle = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = WorkoutPalette.TextPrimary,
                        ),
                        cursorBrush = SolidColor(WorkoutPalette.PrimaryCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_name_input"),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                BasicText(
                    text = "✎",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 15.sp,
                        color = WorkoutPalette.TextBrownMuted,
                    ),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                text = "DESCRIPTION (OPTIONAL)",
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextBrownMuted,
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WorkoutPalette.InputBackground)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (description.isEmpty()) {
                    BasicText(
                        text = "e.g. Chest, shoulders & triceps focus",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF999999),
                        ),
                    )
                }
                BasicTextField(
                    value = description,
                    onValueChange = onDescriptionChanged,
                    singleLine = true,
                    textStyle = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    cursorBrush = SolidColor(WorkoutPalette.PrimaryCoral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_description_input"),
                )
            }
        }
    }
}

@Composable
private fun ExercisesSectionHeader(
    count: Int,
    onAddExerciseClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Exercises",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(WorkoutPalette.CountBadgeBg),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = count.toString(),
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.CountBadgeText,
                    ),
                    modifier = Modifier.testTag("editor_exercise_count"),
                )
            }
        }

        Row(
            modifier = Modifier
                .height(40.dp)
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = Color(0x0D000000),
                    spotColor = Color(0x0D000000),
                )
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .clickable(onClick = onAddExerciseClick)
                .padding(horizontal = 12.dp)
                .testTag("editor_add_exercise_button"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.5.dp, WorkoutPalette.PrimaryCoralDark), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "+",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.PrimaryCoralDark,
                    ),
                )
            }
            BasicText(
                text = "Add Exercise",
                style = PulseTheme.typography.ctaPrimary.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WorkoutPalette.PrimaryCoralDark,
                ),
            )
        }
    }
}

@Composable
private fun EmptyExercisesCard(
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.5.dp, Color(0xFFE0E0E0)), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 32.dp)
            .testTag("editor_empty_exercises"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF0EB)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "+",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5733),
                ),
            )
        }
        BasicText(
            text = "No Exercises Added Yet",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF262626),
            ),
        )
        BasicText(
            text = "Tap Add Exercise above to select movements, configure sets, target reps, and rest time.",
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF808080),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun ConfiguredExerciseCard(
    item: EditorExerciseDraftItem,
    preferredUnit: WeightUnit,
    isSetDetailsExpanded: Boolean,
    onRemove: () -> Unit,
    onAdjustSets: (Int) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onAdjustRest: (Int) -> Unit,
    onToggleSetDetails: () -> Unit,
    onUpdateSingleSet: (Int, Int?, Int?, WeightLoad?, Boolean) -> Unit,
) {
    val showPerSetDetails = isSetDetailsExpanded || item.hasMixedReps || item.hasMixedRest || item.hasAnyLoad

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x0D000000),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, WorkoutPalette.EditorCardBorder), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("editor_exercise_card_${item.occurrenceKey}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = item.thumbnailRes),
                contentDescription = item.exercise.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = item.exercise.name,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextEditorPrimary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = item.metadataLine,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2400D),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = "•",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 10.sp,
                            color = Color(0xFFB2B2BF),
                        ),
                    )
                    BasicText(
                        text = item.equipmentLabel,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF70737A),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF7F7FA))
                    .clickable(onClick = onRemove)
                    .testTag("editor_remove_${item.occurrenceKey}"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = Trash,
                    contentDescription = "Remove ${item.exercise.name}",
                    colorFilter = ColorFilter.tint(Color(0xFF70737A)),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            StepperTile(
                label = "SETS",
                valueText = item.setsCount.toString(),
                onDecrease = { onAdjustSets(-1) },
                onIncrease = { onAdjustSets(1) },
                decreaseTag = "editor_sets_dec_${item.occurrenceKey}",
                increaseTag = "editor_sets_inc_${item.occurrenceKey}",
                modifier = Modifier.weight(1f),
            )
            StepperTile(
                label = "REPS",
                valueText = item.repsDisplay,
                onDecrease = { onAdjustReps(-1) },
                onIncrease = { onAdjustReps(1) },
                decreaseTag = "editor_reps_dec_${item.occurrenceKey}",
                increaseTag = "editor_reps_inc_${item.occurrenceKey}",
                modifier = Modifier.weight(1f),
            )
            StepperTile(
                label = "REST (SEC)",
                valueText = item.restDisplay,
                onDecrease = { onAdjustRest(-15) },
                onIncrease = { onAdjustRest(15) },
                decreaseTag = "editor_rest_dec_${item.occurrenceKey}",
                increaseTag = "editor_rest_inc_${item.occurrenceKey}",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggleSetDetails)
                .padding(vertical = 2.dp)
                .testTag("editor_toggle_per_set_${item.occurrenceKey}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = if (item.hasMixedReps || item.hasMixedRest) {
                    "Per-set targets (custom)"
                } else {
                    "Per-set & load targets"
                },
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WorkoutPalette.TextBrownMuted,
                ),
            )
            BasicText(
                text = if (showPerSetDetails) "Hide" else "Customize",
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.PrimaryCoralDark,
                ),
            )
        }

        if (showPerSetDetails) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WorkoutPalette.MetricTileBackground)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item.sets.forEach { set ->
                    PerSetEditorRow(
                        occurrenceKey = item.occurrenceKey,
                        set = set,
                        preferredUnit = preferredUnit,
                        onUpdate = { reps, rest, load, updateLoad ->
                            onUpdateSingleSet(set.setIndex, reps, rest, load, updateLoad)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperTile(
    label: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseTag: String,
    increaseTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(WorkoutPalette.StepperTileBackground)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF70737A),
            ),
            maxLines = 1,
        )
        BasicText(
            text = valueText,
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = if (valueText.length > 4) 18.sp else 26.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WorkoutPalette.TextEditorPrimary,
            ),
            maxLines = 1,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE0E3E8)), RoundedCornerShape(6.dp))
                    .clickable(onClick = onDecrease)
                    .testTag(decreaseTag),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "−",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextEditorPrimary,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE0E3E8)), RoundedCornerShape(6.dp))
                    .clickable(onClick = onIncrease)
                    .testTag(increaseTag),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "+",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextEditorPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PerSetEditorRow(
    occurrenceKey: String,
    set: PlannedSetDraft,
    preferredUnit: WeightUnit,
    onUpdate: (Int?, Int?, WeightLoad?, Boolean) -> Unit,
) {
    val unitSuffix = if (preferredUnit == WeightUnit.KG) "kg" else "lbs"
    val initialLoadText = set.targetLoad?.let { load ->
        val raw = if (preferredUnit == WeightUnit.KG) load.toKilograms() else load.toPounds()
        if (raw % 1.0 == 0.0) raw.roundToInt().toString() else String.format("%.1f", raw)
    } ?: ""
    var loadText by remember(occurrenceKey, set.setIndex, set.targetLoad?.grams, preferredUnit) {
        mutableStateOf(initialLoadText)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "Set ${set.setIndex}",
            style = PulseTheme.typography.label.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoutPalette.TextBrownMuted,
            ),
            modifier = Modifier.width(40.dp),
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE0E3E8)), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "−",
                style = PulseTheme.typography.label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable { onUpdate((set.targetReps - 1).coerceAtLeast(1), null, null, false) }
                    .padding(horizontal = 4.dp),
            )
            BasicText(
                text = "${set.targetReps}r",
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = "+",
                style = PulseTheme.typography.label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable { onUpdate(set.targetReps + 1, null, null, false) }
                    .padding(horizontal = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE0E3E8)), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "−",
                style = PulseTheme.typography.label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable {
                        val currentRest = set.targetRestSeconds ?: EditorExerciseDraftItem.DEFAULT_REST_SECONDS
                        onUpdate(null, (currentRest - 15).coerceAtLeast(0), null, false)
                    }
                    .padding(horizontal = 4.dp),
            )
            BasicText(
                text = "${set.targetRestSeconds ?: EditorExerciseDraftItem.DEFAULT_REST_SECONDS}s",
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = "+",
                style = PulseTheme.typography.label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clickable {
                        val currentRest = set.targetRestSeconds ?: EditorExerciseDraftItem.DEFAULT_REST_SECONDS
                        onUpdate(null, currentRest + 15, null, false)
                    }
                    .padding(horizontal = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE0E3E8)), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (loadText.isEmpty()) {
                    BasicText(
                        text = "Load",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 11.sp,
                            color = WorkoutPalette.TextSubtle,
                        ),
                    )
                }
                BasicTextField(
                    value = loadText,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }
                        loadText = filtered
                        val parsed = filtered.toDoubleOrNull()
                        val load = when {
                            filtered.isBlank() -> null
                            parsed != null && parsed >= 0.0 -> {
                                if (preferredUnit == WeightUnit.KG) {
                                    WeightLoad.fromKilograms(parsed)
                                } else {
                                    WeightLoad.fromPounds(parsed)
                                }
                            }
                            else -> return@BasicTextField
                        }
                        onUpdate(null, null, load, true)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    cursorBrush = SolidColor(WorkoutPalette.PrimaryCoral),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BasicText(
                text = unitSuffix,
                style = PulseTheme.typography.label.copy(
                    fontSize = 10.sp,
                    color = WorkoutPalette.TextMuted,
                ),
            )
        }
    }
}

@Composable
private fun OfflinePlanReadinessBanner(
    totalSets: Int,
    estimatedMinutes: Int?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x0D000000),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutPalette.ReadyBannerBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(WorkoutPalette.ReadyIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = CheckMark,
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            BasicText(
                text = "$totalSets total sets planned • Offline Plan Ready",
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WorkoutPalette.ReadyTextPrimary,
                ),
            )
            if (estimatedMinutes != null) {
                BasicText(
                    text = "Estimating ~$estimatedMinutes minutes session duration",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.ReadyTextSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
fun DeletePlanConfirmationModal(
    planName: String,
    isDeleting: Boolean,
    onKeepPlan: () -> Unit,
    onDeletePlan: () -> Unit,
    inWindow: Boolean = false,
) {
    val modalBody: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(enabled = !isDeleting, onClick = onKeepPlan)
                .testTag("modal_delete_plan_scrim"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, WorkoutPalette.InputBackground), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable(enabled = false, onClick = {})
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 25.dp, bottom = 24.dp)
                    .testTag("modal_delete_plan"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BasicText(
                    text = "Delete this workout plan?",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191919),
                    ),
                )
                BasicText(
                    text = "Delete $planName from your plans? Completed workout history will remain on your device.",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF191919),
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkoutPalette.PrimaryCoral)
                        .clickable(enabled = !isDeleting, onClick = onKeepPlan)
                        .testTag("button_keep_plan"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Keep Plan",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(if (isDeleting) 0.6f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEAE6))
                        .clickable(enabled = !isDeleting, onClick = onDeletePlan)
                        .testTag("button_confirm_delete_plan"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDeleting) {
                        PulseLoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        BasicText(
                            text = "Delete Plan",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191919),
                            ),
                        )
                    }
                }
            }
        }
    }

    if (inWindow) {
        modalBody()
    } else {
        Dialog(
            onDismissRequest = onKeepPlan,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = !isDeleting,
                dismissOnClickOutside = !isDeleting,
            ),
        ) {
            modalBody()
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    Dialog(
        onDismissRequest = onKeepEditing,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onKeepEditing),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.White)
                    .clickable(enabled = false, onClick = {})
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .testTag("modal_discard_changes"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BasicText(
                    text = "Discard unsaved changes?",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )
                BasicText(
                    text = "Your unsaved edits to this workout plan will be lost.",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        color = WorkoutPalette.TextBrownMuted,
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkoutPalette.PrimaryCoral)
                        .clickable(onClick = onKeepEditing)
                        .testTag("button_keep_editing"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Keep Editing",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkoutPalette.NeutralButtonBg)
                        .clickable(onClick = onDiscard)
                        .testTag("button_discard_changes"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Discard Changes",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }
            }
        }
    }
}
