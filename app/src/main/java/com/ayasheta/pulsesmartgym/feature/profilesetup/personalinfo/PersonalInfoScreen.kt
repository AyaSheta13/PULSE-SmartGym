package com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthColors
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthFormField
import com.ayasheta.pulsesmartgym.feature.auth.components.PulsePrimaryCta

@Composable
fun PersonalInfoScreen(
    viewModel: PersonalInfoViewModel,
    onBackClick: () -> Unit,
    onContinueToGoals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToGoals) {
        if (uiState.navigateToGoals) {
            viewModel.consumeNavigation()
            onContinueToGoals()
        }
    }

    PersonalInfoContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onGenderSelect = viewModel::onGenderSelect,
        onAgeChange = viewModel::onAgeChange,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onImperialClick = viewModel::onImperialClicked,
        onContinueClick = viewModel::continueToGoals,
        modifier = modifier,
    )
}

@Composable
fun PersonalInfoContent(
    uiState: PersonalInfoUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onGenderSelect: (GenderIdentity) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onImperialClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseAuthColors.ScreenSurface),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.BackWithTitle(
                title = "Profile Setup (1/2)",
                onBackClick = onBackClick,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 6dp Progress Bar at 50% (Figma 282:9)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PulseAuthColors.ProgressTrack),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PulseTheme.color.primary),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "About You",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                BasicText(
                    text = "Set up your profile and training preferences to personalize your experience.",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
            }

            PulseAuthFormField(
                label = "YOUR NAME",
                value = uiState.fullName,
                onValueChange = onNameChange,
                placeholder = "Enter your name",
                errorMessage = uiState.nameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            // Gender Identity Selector (Figma 282:18)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    text = "GENDER IDENTITY",
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GenderIdentity.entries.forEach { gender ->
                        val isSelected = uiState.gender == gender
                        val shape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(shape)
                                .background(
                                    if (isSelected) PulseAuthColors.SelectedTintBg else Color.White,
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PulseTheme.color.primary else PulseAuthColors.CardBorderLight,
                                    shape = shape,
                                )
                                .clickable(role = Role.RadioButton) { onGenderSelect(gender) },
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = gender.label,
                                style = TextStyle(
                                    fontFamily = PulseTheme.typography.contentSecondary.fontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) PulseTheme.color.primary else PulseTheme.color.shadeSecondary,
                                ),
                            )
                        }
                    }
                }
            }

            // Stats Row Card (92dp, Figma 282:29)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = if (uiState.metricsError != null) {
                                PulseAuthColors.ErrorBorder
                            } else {
                                PulseAuthColors.CardBorderLight
                            },
                            shape = RoundedCornerShape(16.dp),
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricCell(
                        label = "AGE",
                        value = uiState.ageText,
                        placeholder = "28",
                        unit = "yrs",
                        onValueChange = onAgeChange,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(PulseAuthColors.CardBorderLight),
                    )
                    MetricCell(
                        label = "HEIGHT",
                        value = uiState.heightText,
                        placeholder = "182",
                        unit = "cm",
                        onValueChange = onHeightChange,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(PulseAuthColors.CardBorderLight),
                    )
                    MetricCell(
                        label = "WEIGHT",
                        value = uiState.weightText,
                        placeholder = "78",
                        unit = "kg",
                        onValueChange = onWeightChange,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (uiState.metricsError != null) {
                    BasicText(
                        text = uiState.metricsError,
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.supportingContent.fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PulseAuthColors.ErrorText,
                        ),
                    )
                }
            }

            // Unit Toggle (44dp, Figma 282:47)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PulseTheme.color.background)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Metric (kg, cm)",
                            style = TextStyle(
                                fontFamily = PulseTheme.typography.cardTitle.fontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.color.shadePrimary,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClick = onImperialClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Imperial (Coming Soon)",
                            style = TextStyle(
                                fontFamily = PulseTheme.typography.supportingContent.fontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PulseTheme.color.shadeSecondary,
                            ),
                            modifier = Modifier.alpha(0.5f),
                        )
                    }
                }

                if (uiState.imperialNotice != null) {
                    BasicText(
                        text = uiState.imperialNotice,
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.supportingContent.fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PulsePrimaryCta(
                text = "Continue to Goals →",
                onClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    placeholder: String,
    unit: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = PulseTheme.typography.label.fontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PulseTheme.color.shadeSecondary,
            ),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        ) {
            Box(
                modifier = Modifier.width(52.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (value.isEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseAuthColors.Placeholder,
                            textAlign = TextAlign.End,
                        ),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.color.shadePrimary,
                        textAlign = TextAlign.End,
                    ),
                    cursorBrush = SolidColor(PulseTheme.color.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("metric_$label"),
                )
            }
            BasicText(
                text = unit,
                style = TextStyle(
                    fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = PulseTheme.color.shadeSecondary,
                ),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun PersonalInfoPreview() {
    PulseTheme {
        PersonalInfoContent(
            uiState = PersonalInfoUiState(
                fullName = "Alex Miller",
                ageText = "28",
                heightText = "182",
                weightText = "78",
            ),
            onBackClick = {},
            onNameChange = {},
            onGenderSelect = {},
            onAgeChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onImperialClick = {},
            onContinueClick = {},
        )
    }
}
