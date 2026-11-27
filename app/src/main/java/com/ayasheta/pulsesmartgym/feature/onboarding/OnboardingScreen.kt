package com.ayasheta.pulsesmartgym.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Back
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthColors
import com.ayasheta.pulsesmartgym.feature.auth.components.PulsePrimaryCta

private data class OnboardingPageSpec(
    @param:DrawableRes val heroResId: Int,
    val aspectRatio: Float,
    val badgeLabel: String,
    val title: String,
    val description: String,
    val primaryCtaLabel: String,
    val showBackButton: Boolean,
    val showSkipButton: Boolean,
    val skipFontSizeSp: Int,
    val skipBold: Boolean,
)

private val onboardingPages = listOf(
    OnboardingPageSpec(
        heroResId = R.drawable.onboarding_hero_1,
        aspectRatio = 378f / 436f,
        badgeLabel = "STRENGTH",
        title = "Welcome to PULSE",
        description = "Smart workout tracking built for focused strength training. Log every rep and build consistent gym momentum.",
        primaryCtaLabel = "Next →",
        showBackButton = false,
        showSkipButton = true,
        skipFontSizeSp = 14,
        skipBold = false,
    ),
    OnboardingPageSpec(
        heroResId = R.drawable.onboarding_hero_2,
        aspectRatio = 935f / 1024f,
        badgeLabel = "SMART LOGGING",
        title = "Track Every Workout",
        description = "Log sets, weights, and reps with clean, fluid controls. Track your training volume and personal bests with zero friction.",
        primaryCtaLabel = "Next →",
        showBackButton = true,
        showSkipButton = true,
        skipFontSizeSp = 12,
        skipBold = true,
    ),
    OnboardingPageSpec(
        heroResId = R.drawable.onboarding_hero_3,
        aspectRatio = 377f / 436f,
        badgeLabel = "DATA & ANALYTICS",
        title = "Analyze Your Progress",
        description = "Visualize workout volume, personal records, and strength progress. All data stays fast, private, and always available.",
        primaryCtaLabel = "Get Started →",
        showBackButton = true,
        showSkipButton = false,
        skipFontSizeSp = 12,
        skipBold = true,
    ),
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit,
    onExitAppBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) {
            onCompleted()
        }
    }

    BackHandler {
        if (!viewModel.previousPage()) {
            onExitAppBack()
        }
    }

    OnboardingContent(
        currentPage = uiState.currentPage,
        isCompleting = uiState.isCompleting,
        onPageSelected = viewModel::selectPage,
        onBackClick = {
            if (!viewModel.previousPage()) {
                onExitAppBack()
            }
        },
        onSkipClick = viewModel::skipOnboarding,
        onPrimaryClick = viewModel::nextPage,
        modifier = modifier,
    )
}

@Composable
fun OnboardingContent(
    currentPage: Int,
    isCompleting: Boolean,
    onPageSelected: (Int) -> Unit = {},
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clampedPage = currentPage.coerceIn(0, onboardingPages.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = clampedPage,
        pageCount = { onboardingPages.size },
    )

    LaunchedEffect(clampedPage) {
        if (pagerState.currentPage != clampedPage) {
            pagerState.animateScrollToPage(
                page = clampedPage,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )
        }
    }

    val latestPage by rememberUpdatedState(clampedPage)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage to pagerState.isScrollInProgress }.collect { (settled, scrolling) ->
            if (!scrolling && settled != latestPage) {
                onPageSelected(settled)
            }
        }
    }

    val spec = onboardingPages[clampedPage]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseAuthColors.ScreenSurface),
    ) {
        // 64dp Top Navigation Bar matching Figma 471:365, 471:347, 471:358
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PulseTheme.color.container)
                .statusBarsPadding()
                .height(64.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (spec.showBackButton) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button, onClick = onBackClick)
                            .semantics { contentDescription = "Back" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            imageVector = Back,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(PulseTheme.color.shadePrimary),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }

                if (spec.showSkipButton) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable(
                                enabled = !isCompleting,
                                role = Role.Button,
                                onClick = onSkipClick,
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        BasicText(
                            text = "Skip",
                            style = TextStyle(
                                fontFamily = PulseTheme.typography.contentSecondary.fontFamily,
                                fontSize = spec.skipFontSizeSp.sp,
                                fontWeight = if (spec.skipBold) FontWeight.SemiBold else FontWeight.Medium,
                                color = PulseTheme.color.shadePrimary,
                            ),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PulseTheme.color.divider),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = !isCompleting,
        ) { pageIndex ->
            val pageSpec = onboardingPages[pageIndex]
            val fontScale = LocalConfiguration.current.fontScale

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Standard viewports fit without vertical scroll so horizontal swipe stays crisp.
                // Exceptionally short heights or large accessibility font scales fall back to scroll.
                val useScrollFallback = maxHeight < 360.dp || fontScale > 1.35f
                val pageColumnModifier = Modifier
                    .fillMaxSize()
                    .let { base ->
                        if (useScrollFallback) base.verticalScroll(rememberScrollState()) else base
                    }
                    .padding(horizontal = 20.dp)

                Column(
                    modifier = pageColumnModifier,
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Hero Visual Module — container matches intrinsic image ratio with zero blank strips
                    val heroWrapperModifier = if (useScrollFallback) {
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .heightIn(min = 160.dp)
                    }
                    Box(
                        modifier = heroWrapperModifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(
                                    ratio = pageSpec.aspectRatio,
                                    matchHeightConstraintsFirst = pageSpec.aspectRatio < 1f,
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFEFE2DE),
                                    shape = RoundedCornerShape(24.dp),
                                )
                                .testTag("onboarding_hero_card_$pageIndex"),
                        ) {
                            Image(
                                painter = painterResource(id = pageSpec.heroResId),
                                contentDescription = pageSpec.badgeLabel,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("onboarding_hero_image_$pageIndex"),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .padding(start = 14.dp, top = 14.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xBF191919))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("onboarding_hero_badge_$pageIndex"),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PulseTheme.color.primary),
                                )
                                BasicText(
                                    text = pageSpec.badgeLabel,
                                    style = TextStyle(
                                        fontFamily = PulseTheme.typography.label.fontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White,
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    BasicText(
                        text = pageSpec.title,
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.sectionTitle.fontFamily,
                            fontSize = 25.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191919),
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BasicText(
                        text = pageSpec.description,
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF66615C),
                        ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Animated Progress Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(onboardingPages.size) { index ->
                    val isActive = index == clampedPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 7.dp,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        label = "onboarding_dot_width_$index",
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isActive) PulseTheme.color.primary else Color(0xFFD9D4CF),
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        label = "onboarding_dot_color_$index",
                    )
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(dotColor),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PulsePrimaryCta(
                text = spec.primaryCtaLabel,
                onClick = onPrimaryClick,
                height = 54.dp,
                fontSizeSp = 16,
                isLoading = isCompleting,
                enabled = !isCompleting,
                withElevationShadow = true,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun OnboardingPage1Preview() {
    PulseTheme {
        OnboardingContent(
            currentPage = 0,
            isCompleting = false,
            onBackClick = {},
            onSkipClick = {},
            onPrimaryClick = {},
        )
    }
}
