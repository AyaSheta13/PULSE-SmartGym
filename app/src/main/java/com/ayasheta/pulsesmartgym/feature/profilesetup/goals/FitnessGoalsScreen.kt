package com.ayasheta.pulsesmartgym.feature.profilesetup.goals

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthColors
import com.ayasheta.pulsesmartgym.feature.auth.components.PulsePrimaryCta
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseValidationBanner

@Composable
fun FitnessGoalsScreen(
    viewModel: FitnessGoalsViewModel,
    onBackClick: () -> Unit,
    onSetupCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.setupCompleted) {
        if (uiState.setupCompleted) {
            viewModel.consumeCompletion()
            onSetupCompleted()
        }
    }

    FitnessGoalsContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onGoalSelect = viewModel::selectGoal,
        onExperienceSelect = viewModel::selectExperience,
        onWeeklyWorkoutsSelect = viewModel::selectWeeklyWorkouts,
        onCompleteClick = viewModel::completeSetup,
        modifier = modifier,
    )
}

@Composable
fun FitnessGoalsContent(
    uiState: FitnessGoalsUiState,
    onBackClick: () -> Unit,
    onGoalSelect: (FitnessGoal) -> Unit,
    onExperienceSelect: (ExperienceLevel) -> Unit,
    onWeeklyWorkoutsSelect: (Int) -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseAuthColors.ScreenSurface),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.BackWithTitle(
                title = "Training Goals (2/2)",
                onBackClick = onBackClick,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 6dp Progress Bar at 100% (Figma 282:62)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PulseTheme.color.primary),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Your Fitness Goals",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                BasicText(
                    text = "Select your training focus to tailor routines and targets.",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
            }

            if (uiState.errorMessage != null) {
                PulseValidationBanner(
                    title = "Unable to Save Profile",
                    message = uiState.errorMessage,
                )
            }

            // Goals Group (Figma 282:67)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FitnessGoal.entries.forEach { goal ->
                    val isSelected = uiState.selectedGoal == goal
                    GoalOptionCard(
                        goal = goal,
                        iconResId = when (goal) {
                            FitnessGoal.HYPERTROPHY -> R.drawable.ic_goal_hypertrophy
                            FitnessGoal.STRENGTH -> R.drawable.ic_goal_strength
                            FitnessGoal.FITNESS -> R.drawable.ic_goal_fitness
                        },
                        isSelected = isSelected,
                        onClick = { onGoalSelect(goal) },
                    )
                }
            }

            // Experience Level (Figma 282:93)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    text = "EXPERIENCE LEVEL",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.label.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExperienceLevel.entries.forEach { level ->
                        SelectableChipOption(
                            label = level.label,
                            isSelected = uiState.selectedExperience == level,
                            onClick = { onExperienceSelect(level) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Weekly Workouts (Figma 282:102)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    text = "WEEKLY WORKOUTS",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.label.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(3, 4, 5, 6).forEach { days ->
                        SelectableChipOption(
                            label = "$days Days",
                            isSelected = uiState.selectedWeeklyWorkouts == days,
                            onClick = { onWeeklyWorkoutsSelect(days) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            PulsePrimaryCta(
                text = "Complete Setup & Start →",
                onClick = onCompleteClick,
                isLoading = uiState.isSaving,
            )
        }
    }
}

@Composable
private fun GoalOptionCard(
    goal: FitnessGoal,
    @DrawableRes iconResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) PulseTheme.color.primary else PulseAuthColors.CardBorderLight,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) PulseAuthColors.SelectedIconBg else PulseTheme.color.background,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (isSelected) PulseTheme.color.primary else PulseTheme.color.shadePrimary,
                    ),
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = goal.title,
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.cardTitle.fontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                BasicText(
                    text = goal.subtitle,
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) PulseTheme.color.primary else Color(0xFFE5E0DB)),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                BasicText(
                    text = "✓",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.cardTitle.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SelectableChipOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (isSelected) PulseAuthColors.SelectedTintBg else Color.White)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PulseTheme.color.primary else PulseAuthColors.CardBorderLight,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = PulseTheme.typography.caption.fontFamily,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PulseTheme.color.primary else PulseTheme.color.shadePrimary,
            ),
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun FitnessGoalsPreview() {
    PulseTheme {
        FitnessGoalsContent(
            uiState = FitnessGoalsUiState(),
            onBackClick = {},
            onGoalSelect = {},
            onExperienceSelect = {},
            onWeeklyWorkoutsSelect = {},
            onCompleteClick = {},
        )
    }
}
