package com.ayasheta.pulsesmartgym.feature.auth.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthColors
import com.ayasheta.pulsesmartgym.feature.auth.components.PulsePrimaryCta
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseValidationBanner

@Composable
fun AuthEntryScreen(
    viewModel: AuthEntryViewModel,
    onCreateAccountClick: () -> Unit,
    onLogInClick: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigationTarget) {
        when (uiState.navigationTarget) {
            AuthEntryNavigationTarget.ProfileSetup -> {
                viewModel.consumeNavigation()
                onNavigateToProfileSetup()
            }
            AuthEntryNavigationTarget.MainHome -> {
                viewModel.consumeNavigation()
                onNavigateToMain()
            }
            null -> Unit
        }
    }

    AuthEntryContent(
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onCreateAccountClick = onCreateAccountClick,
        onLogInClick = onLogInClick,
        onContinueAsGuestClick = viewModel::continueAsGuest,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthEntryContent(
    isLoading: Boolean,
    errorMessage: String?,
    onCreateAccountClick: () -> Unit,
    onLogInClick: () -> Unit,
    onContinueAsGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseAuthColors.EntrySurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        // Brand Header (Figma 607:345)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_pulse_logo_mark),
                contentDescription = "PULSE Logo",
                modifier = Modifier.size(32.dp),
            )
            BasicText(
                text = "PULSE",
                style = TextStyle(
                    fontFamily = PulseTheme.typography.sectionTitle.fontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color(0xFF141414),
                ),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PulseAuthColors.BadgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                BasicText(
                    text = "SMART GYM",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.label.fontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = PulseAuthColors.BadgeText,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hero Visual Card (Figma 607:350)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x14000000),
                    spotColor = Color(0x14000000),
                )
                .clip(RoundedCornerShape(24.dp)),
        ) {
            Image(
                painter = painterResource(id = R.drawable.onboarding_hero_1),
                contentDescription = "PULSE Smart Gym",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xE00D0D0D),
                            ),
                        ),
                    ),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Value Proposition Section (Figma 607:357)
        BasicText(
            text = "Elevate Every Rep",
            style = TextStyle(
                fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                fontSize = 26.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = Color(0xFF141414),
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )

        Spacer(modifier = Modifier.height(6.dp))

        BasicText(
            text = "Smart workout tracking built for focused strength. Log every set with precision, track progressive overload, and keep all data private.",
            style = TextStyle(
                fontFamily = PulseTheme.typography.contentSecondary.fontFamily,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                color = PulseAuthColors.EntryMutedText,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Feature Pills Row (Figma 607:360)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeaturePill("✦ Custom Routines")
            FeaturePill("✦ Precision Logging")
            FeaturePill("✦ Local Analytics")
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            PulseValidationBanner(
                title = "Offline Guest Error",
                message = errorMessage,
            )
        }

        Spacer(modifier = Modifier.weight(0.615f, fill = true))
        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons (Figma 607:367)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PulsePrimaryCta(
                text = "Create Free Account",
                onClick = onCreateAccountClick,
                height = 52.dp,
                fontSizeSp = 16,
                containerColor = PulseAuthColors.EntryPrimaryCta,
                enabled = !isLoading,
                withElevationShadow = true,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(
                        width = 1.5.dp,
                        color = PulseAuthColors.EntrySecondaryBorder,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable(enabled = !isLoading, role = Role.Button, onClick = onLogInClick),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Log In",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.ctaCompact.fontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PulseAuthColors.EntrySecondaryText,
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !isLoading, role = Role.Button, onClick = onContinueAsGuestClick),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    PulseLoadingIndicator(
                        size = 18.dp,
                        strokeWidth = 2.dp,
                        color = PulseAuthColors.EntryMutedText,
                    )
                } else {
                    BasicText(
                        text = "Continue as Guest (Offline Mode) →",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.contentSecondary.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PulseAuthColors.EntryMutedText,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.385f, fill = true))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeaturePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PulseAuthColors.PillBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontFamily = PulseTheme.typography.label.fontFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = PulseAuthColors.PillText,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun AuthEntryPreview() {
    PulseTheme {
        AuthEntryContent(
            isLoading = false,
            errorMessage = null,
            onCreateAccountClick = {},
            onLogInClick = {},
            onContinueAsGuestClick = {},
        )
    }
}
