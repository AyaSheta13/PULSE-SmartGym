package com.ayasheta.pulsesmartgym.feature.profile.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseBottomSheet
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity

private val EditBg = Color(0xFFFCF9F8)
private val EditBorder = Color(0xFFF1EEEA)
private val EditTextPrimary = Color(0xFF1C1B1B)
private val EditTextSecondary = Color(0xFF7A7570)
private val EditOrange = Color(0xFFFF623E)

private enum class EditPickerSheet {
    GOAL,
    EXPERIENCE,
}

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EditProfileScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onFullNameChanged = viewModel::onFullNameChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onHeightChanged = viewModel::onHeightChanged,
        onGenderSelected = viewModel::onGenderSelected,
        onFitnessGoalSelected = viewModel::onFitnessGoalSelected,
        onExperienceLevelSelected = viewModel::onExperienceLevelSelected,
        onChangeAvatarClick = viewModel::onChangeAvatarClick,
        onDismissAvatarNotice = viewModel::dismissAvatarNotice,
        onSaveClick = { viewModel.saveChanges(onSaveSuccess) },
        onDiscardClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun EditProfileScreenContent(
    uiState: EditProfileUiState,
    onBackClick: () -> Unit,
    onFullNameChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onGenderSelected: (GenderIdentity) -> Unit,
    onFitnessGoalSelected: (FitnessGoal) -> Unit,
    onExperienceLevelSelected: (ExperienceLevel) -> Unit,
    onChangeAvatarClick: () -> Unit,
    onDismissAvatarNotice: () -> Unit,
    onSaveClick: () -> Unit,
    onDiscardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activePicker by remember { mutableStateOf<EditPickerSheet?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditBg),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.BackWithTitle(
                title = "Edit Profile",
                onBackClick = onBackClick,
            ),
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                PulseLoadingIndicator()
            }
            return
        }

        val missingError = uiState.missingProfileError
        if (!uiState.hasExistingProfile && missingError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PulseTheme.color.errorVariant)
                        .border(1.dp, PulseTheme.color.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .testTag("edit_profile_missing_error"),
                ) {
                    BasicText(
                        text = missingError,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            color = PulseTheme.color.error,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditOrange)
                        .clickable(onClick = onBackClick)
                        .testTag("edit_profile_missing_back_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Go Back",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp)
                .testTag("edit_profile_scroll"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EditOrange),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = uiState.initials,
                        style = PulseTheme.typography.displayHero.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
                BasicText(
                    text = "Change Avatar",
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onChangeAvatarClick)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EditOrange,
                    ),
                )
            }

            if (uiState.avatarUnavailableNotice) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF1EE))
                        .border(1.dp, EditOrange.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismissAvatarNotice)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    BasicText(
                        text = "Custom photo avatars are not available in this offline version. Your initials update automatically from your full name.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 12.sp,
                            color = Color(0xFF5A413B),
                        ),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("FULL NAME")
                EditableFieldBox(
                    value = uiState.fullName,
                    onValueChange = onFullNameChanged,
                    placeholder = "Enter your full name",
                    testTag = "edit_profile_name_input",
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("EMAIL ADDRESS")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF7F6F2))
                        .border(1.dp, EditBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicText(
                        text = uiState.emailDisplay,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EditTextSecondary,
                        ),
                        maxLines = 1,
                    )
                }
                BasicText(
                    text = if (uiState.isGuest) {
                        "Guest sessions do not have an email address."
                    } else {
                        "Account email is managed by local authentication and is read-only here."
                    },
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        color = EditTextSecondary,
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FieldLabel("WEIGHT (KG)")
                    EditableFieldBox(
                        value = uiState.weightInput,
                        onValueChange = onWeightChanged,
                        placeholder = "75.0",
                        keyboardType = KeyboardType.Decimal,
                        testTag = "edit_profile_weight_input",
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FieldLabel("HEIGHT (CM)")
                    EditableFieldBox(
                        value = uiState.heightInput,
                        onValueChange = onHeightChanged,
                        placeholder = "175",
                        keyboardType = KeyboardType.Number,
                        testTag = "edit_profile_height_input",
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("GENDER IDENTITY")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GenderIdentity.entries.forEach { option ->
                        val selected = uiState.gender == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) EditOrange.copy(alpha = 0.12f) else Color.White,
                                )
                                .border(
                                    BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) EditOrange else EditBorder,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onGenderSelected(option) }
                                .testTag("edit_gender_${option.name}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = option.label,
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) EditOrange else EditTextSecondary,
                                ),
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("FITNESS GOAL")
                SelectorFieldBox(
                    value = uiState.fitnessGoal.title,
                    onClick = { activePicker = EditPickerSheet.GOAL },
                    testTag = "edit_goal_selector",
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("EXPERIENCE LEVEL")
                SelectorFieldBox(
                    value = uiState.experienceLevel.displayLabel(),
                    onClick = { activePicker = EditPickerSheet.EXPERIENCE },
                    testTag = "edit_experience_selector",
                )
            }

            val errorBanner = uiState.validationError ?: uiState.saveError
            if (errorBanner != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PulseTheme.color.errorVariant)
                        .border(1.dp, PulseTheme.color.error.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .testTag("edit_profile_error"),
                ) {
                    BasicText(
                        text = errorBanner,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 13.sp,
                            color = PulseTheme.color.error,
                        ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (uiState.canSave) EditOrange else EditOrange.copy(alpha = 0.55f))
                    .clickable(enabled = uiState.canSave, onClick = onSaveClick)
                    .testTag("edit_profile_save_button"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = if (uiState.isSaving) "Saving..." else "Save Changes",
                    style = PulseTheme.typography.ctaPrimary.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFD9D4CF), RoundedCornerShape(10.dp))
                    .clickable(enabled = !uiState.isSaving, onClick = onDiscardClick)
                    .testTag("edit_profile_discard_button"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Cancel / Discard Changes",
                    style = PulseTheme.typography.ctaPrimary.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EditTextSecondary,
                    ),
                )
            }
        }
    }

    when (activePicker) {
        EditPickerSheet.GOAL -> {
            PulseBottomSheet(
                visible = true,
                onDismissRequest = { activePicker = null },
                title = "Select Fitness Goal",
                summaryContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FitnessGoal.entries.forEach { goal ->
                            val selected = goal == uiState.fitnessGoal
                            PickerOptionRow(
                                title = goal.title,
                                subtitle = goal.subtitle,
                                selected = selected,
                                onClick = {
                                    onFitnessGoalSelected(goal)
                                    activePicker = null
                                },
                                testTag = "picker_goal_${goal.name}",
                            )
                        }
                    }
                },
                primaryActionLabel = "Close",
                onPrimaryAction = { activePicker = null },
            )
        }

        EditPickerSheet.EXPERIENCE -> {
            PulseBottomSheet(
                visible = true,
                onDismissRequest = { activePicker = null },
                title = "Select Experience Level",
                summaryContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExperienceLevel.entries.forEach { level ->
                            val selected = level == uiState.experienceLevel
                            PickerOptionRow(
                                title = level.displayLabel(),
                                subtitle = null,
                                selected = selected,
                                onClick = {
                                    onExperienceLevelSelected(level)
                                    activePicker = null
                                },
                                testTag = "picker_level_${level.name}",
                            )
                        }
                    }
                },
                primaryActionLabel = "Close",
                onPrimaryAction = { activePicker = null },
            )
        }

        null -> Unit
    }
}

@Composable
private fun FieldLabel(text: String) {
    BasicText(
        text = text,
        style = PulseTheme.typography.label.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = EditTextSecondary,
        ),
    )
}

@Composable
private fun EditableFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, EditBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            BasicText(
                text = placeholder,
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 14.sp,
                    color = Color(0xFFA6A19C),
                ),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(EditOrange),
            textStyle = PulseTheme.typography.contentPrimary.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EditTextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
    }
}

@Composable
private fun SelectorFieldBox(
    value: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, EditBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = value,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EditTextPrimary,
            ),
        )
        BasicText(
            text = "›",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA6A19C),
            ),
        )
    }
}

@Composable
private fun PickerOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFFF1EE) else Color(0xFFF7F6F2))
            .border(
                BorderStroke(1.dp, if (selected) EditOrange else EditBorder),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = title,
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EditTextPrimary,
                ),
            )
            if (subtitle != null) {
                BasicText(
                    text = subtitle,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        color = EditTextSecondary,
                    ),
                )
            }
        }
        if (selected) {
            BasicText(
                text = "✓",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EditOrange,
                ),
            )
        }
    }
}

private fun ExperienceLevel.displayLabel(): String = when (this) {
    ExperienceLevel.BEGINNER -> "Beginner (< 1 year)"
    ExperienceLevel.INTERMEDIATE -> "Intermediate (2–4 years)"
    ExperienceLevel.ADVANCED -> "Advanced (4+ years)"
}
