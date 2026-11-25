package com.ayasheta.pulsesmartgym.feature.session.logging

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.feature.session.dashboard.ManualSetEntryTarget
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun ManualSetEntrySheet(
    target: ManualSetEntryTarget,
    preferredWeightUnit: WeightUnit,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (fullRepsInput: String, partialRepsInput: String, loadInput: String) -> Unit,
    inWindow: Boolean = false,
) {
    val unitLabel = if (preferredWeightUnit == WeightUnit.KG) "kg" else "lbs"
    val initialFull = (target.existingFullReps ?: target.targetReps).toString()
    val initialPartial = (target.existingPartialReps ?: 0).toString()
    val initialLoad = SessionUiMapper.formatEditableWeight(
        target.existingLoad ?: target.targetLoad,
        preferredWeightUnit,
    )

    var fullRepsText by rememberSaveable(target.sessionExerciseId, target.setIndex, target.isCorrection) {
        mutableStateOf(initialFull)
    }
    var partialRepsText by rememberSaveable(target.sessionExerciseId, target.setIndex, target.isCorrection) {
        mutableStateOf(initialPartial)
    }
    var loadText by rememberSaveable(target.sessionExerciseId, target.setIndex, target.isCorrection) {
        mutableStateOf(initialLoad)
    }

    val targetLoadLabel = SessionUiMapper.formatWeightLoad(target.targetLoad, preferredWeightUnit)
    val sheetBody: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(enabled = !isSaving, onClick = onDismiss)
                .testTag("modal_manual_set_entry_scrim"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.White)
                    .clickable(enabled = false, onClick = {})
                    .let { if (inWindow) it else it.navigationBarsPadding().imePadding() }
                    .padding(horizontal = 16.dp, vertical = 22.dp)
                    .testTag("modal_manual_set_entry"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BasicText(
                    text = if (target.isCorrection) {
                        "Edit Set ${target.setIndex}"
                    } else {
                        "Log Set ${target.setIndex}"
                    },
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )
                BasicText(
                    text = buildString {
                        append(target.exerciseName)
                        append(" • Target: ${target.targetReps} reps")
                        if (targetLoadLabel != null) {
                            append(" @ $targetLoadLabel")
                        }
                    },
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = WorkoutPalette.TextMuted,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ManualNumberInputField(
                        label = "Full Reps",
                        value = fullRepsText,
                        onValueChange = { fullRepsText = it },
                        keyboardType = KeyboardType.Number,
                        testTag = "input_manual_full_reps",
                        modifier = Modifier.weight(1f),
                    )
                    ManualNumberInputField(
                        label = "Partial Reps",
                        value = partialRepsText,
                        onValueChange = { partialRepsText = it },
                        keyboardType = KeyboardType.Number,
                        testTag = "input_manual_partial_reps",
                        modifier = Modifier.weight(1f),
                    )
                }

                ManualNumberInputField(
                    label = "Actual Load ($unitLabel, optional)",
                    value = loadText,
                    onValueChange = { loadText = it },
                    keyboardType = KeyboardType.Decimal,
                    testTag = "input_manual_load",
                    modifier = Modifier.fillMaxWidth(),
                )

                if (errorMessage != null) {
                    BasicText(
                        text = errorMessage,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = WorkoutPalette.ErrorText,
                        ),
                        modifier = Modifier.testTag("manual_set_entry_error"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEEAE6))
                            .clickable(enabled = !isSaving, onClick = onDismiss)
                            .testTag("button_manual_set_cancel"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Cancel",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .alpha(if (isSaving) 0.65f else 1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WorkoutPalette.PrimaryCoral)
                            .clickable(enabled = !isSaving) {
                                onConfirm(fullRepsText, partialRepsText, loadText)
                            }
                            .testTag("button_manual_set_confirm"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSaving) {
                            PulseLoadingIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            BasicText(
                                text = if (target.isCorrection) "Save Correction" else "Confirm Set",
                                style = PulseTheme.typography.ctaPrimary.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    if (inWindow) {
        sheetBody()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            sheetBody()
        }
    }
}

@Composable
private fun ManualNumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextMuted,
            ),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(WorkoutPalette.PrimaryCoral),
            textStyle = PulseTheme.typography.contentPrimary.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFAF8F7))
                .border(BorderStroke(1.dp, Color(0xFFE4DFDA)), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .testTag(testTag),
        )
    }
}
