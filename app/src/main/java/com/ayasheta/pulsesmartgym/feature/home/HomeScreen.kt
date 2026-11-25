package com.ayasheta.pulsesmartgym.feature.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseBottomSheet
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.HistoryClock
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Play
import com.ayasheta.pulsesmartgym.core.model.history.WeeklyProgressSummary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val HomeBackground = Color(0xFFFCF9F8)
private val CardBorderLight = Color(0xFFF1EEEA)
private val CardBorderSubtle = Color(0xFFF0F0F0)
private val TextPrimaryDark = Color(0xFF1C1B1B)
private val TextSecondaryMuted = Color(0xFF7A7570)
private val TextBrownMuted = Color(0xFF5A413B)
private val AccentOrange = Color(0xFFFF623E)
private val AccentGreen = Color(0xFF00AB75)
private val AccentDarkGreen = Color(0xFF006C49)
private val PillSurface = Color(0xFFF6F3F2)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAvatarClick: () -> Unit,
    onDeviceStatusClick: () -> Unit,
    onSeeAllPlansClick: () -> Unit,
    onPlanClick: (String) -> Unit,
    onProgressClick: () -> Unit,
    onLastSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenContent(
        uiState = uiState,
        onAvatarClick = onAvatarClick,
        onDeviceStatusClick = onDeviceStatusClick,
        onSeeAllPlansClick = onSeeAllPlansClick,
        onPlanClick = onPlanClick,
        onProgressClick = onProgressClick,
        onLastSessionClick = onLastSessionClick,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onAvatarClick: () -> Unit,
    onDeviceStatusClick: () -> Unit,
    onSeeAllPlansClick: () -> Unit,
    onPlanClick: (String) -> Unit,
    onProgressClick: () -> Unit,
    onLastSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingStartPlan by remember { mutableStateOf<HomePlanItemUiModel?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        when (uiState) {
            HomeUiState.Loading, HomeUiState.SignedOut -> {
                PulseTopAppBar(
                    style = PulseTopAppBarStyle.HomeGreetingHeader(
                        greeting = "Welcome",
                        userName = "PULSE",
                        bluetoothStatus = PulseBluetoothChipStatus.DISCONNECTED,
                        avatarInitial = "P",
                        onBluetoothClick = onDeviceStatusClick,
                        onAvatarClick = onAvatarClick,
                    ),
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseLoadingIndicator()
                }
            }

            is HomeUiState.Error -> {
                PulseTopAppBar(
                    style = PulseTopAppBarStyle.HomeGreetingHeader(
                        greeting = "Welcome",
                        userName = "PULSE",
                        bluetoothStatus = PulseBluetoothChipStatus.DISCONNECTED,
                        avatarInitial = "P",
                        onBluetoothClick = onDeviceStatusClick,
                        onAvatarClick = onAvatarClick,
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.contentPrimary.copy(color = PulseTheme.color.error),
                    )
                }
            }

            is HomeUiState.Loaded -> {
                PulseTopAppBar(
                    style = PulseTopAppBarStyle.HomeGreetingHeader(
                        greeting = uiState.welcomeOverline,
                        userName = uiState.greetingTitle,
                        bluetoothStatus = PulseBluetoothChipStatus.DISCONNECTED,
                        avatarInitial = uiState.avatarInitial,
                        onBluetoothClick = onDeviceStatusClick,
                        onAvatarClick = onAvatarClick,
                    ),
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_scroll_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(key = "featured_hero") {
                        if (uiState.isPopulatedState && uiState.featuredPlan != null) {
                            PopulatedFeaturedWorkoutCard(
                                plan = uiState.featuredPlan,
                                hasActiveSession = uiState.activeSession != null,
                                onStartClick = { pendingStartPlan = uiState.featuredPlan },
                            )
                        } else {
                            FirstWorkoutHeroCard(
                                featuredPlan = uiState.featuredPlan,
                                onStartClick = {
                                    val plan = uiState.featuredPlan
                                    if (plan != null) {
                                        pendingStartPlan = plan
                                    } else {
                                        onSeeAllPlansClick()
                                    }
                                },
                            )
                        }
                    }

                    item(key = "weekly_progress") {
                        if (uiState.isPopulatedState) {
                            PopulatedWeeklyProgressCard(
                                summary = uiState.weeklyProgress,
                                onClick = onProgressClick,
                            )
                        } else {
                            FirstWorkoutWeeklyProgressCard(
                                summary = uiState.weeklyProgress,
                                onClick = onProgressClick,
                            )
                        }
                    }

                    item(key = "plans_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                text = if (uiState.isPopulatedState) "Your Workouts" else "Your Workout Plans",
                                style = PulseTheme.typography.sectionTitle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                ),
                            )
                            BasicText(
                                text = "See all ›",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onSeeAllPlansClick)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isPopulatedState) Color(0xFFB22C0B) else AccentOrange,
                                ),
                            )
                        }
                    }

                    if (uiState.isNoPlansEmptyState) {
                        item(key = "no_plans_empty") {
                            NoPlansEmptyCard(onExploreWorkouts = onSeeAllPlansClick)
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.plans.take(3),
                            key = { _, plan -> plan.id },
                        ) { index, plan ->
                            HomePlanPreviewRow(
                                plan = plan,
                                thumbnailResId = resolvePlanThumbnailRes(plan.name, index),
                                onClick = { onPlanClick(plan.id) },
                            )
                        }
                    }

                    uiState.lastSession?.let { lastSession ->
                        item(key = "last_session") {
                            LastSessionCard(
                                session = lastSession,
                                onClick = { onLastSessionClick(lastSession.sessionId) },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingStartPlan?.let { plan ->
        PulseBottomSheet(
            visible = true,
            onDismissRequest = { pendingStartPlan = null },
            title = "Workout Session Unavailable",
            description = "Live workout execution for \"${plan.name}\" is planned for a future release. You can inspect this plan's exercises and sets in Workout Details.",
            primaryActionLabel = "View Plan Details",
            onPrimaryAction = {
                pendingStartPlan = null
                onPlanClick(plan.id)
            },
            secondaryActionLabel = "Close",
            onSecondaryAction = { pendingStartPlan = null },
        )
    }
}

@Composable
private fun FirstWorkoutHeroCard(
    featuredPlan: HomePlanItemUiModel?,
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TextPrimaryDark)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF333333))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            BasicText(
                text = if (featuredPlan != null) "READY FOR YOUR FIRST WORKOUT" else "GET STARTED WITH PULSE",
                style = PulseTheme.typography.label.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                ),
            )
        }

        BasicText(
            text = featuredPlan?.name ?: "No Workout Plans Yet",
            style = PulseTheme.typography.displayHero.copy(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        BasicText(
            text = if (featuredPlan != null) {
                "${featuredPlan.exerciseCount} exercises • ${featuredPlan.plannedSetsCount} planned sets"
            } else {
                "Open the Workouts tab to build your first routine."
            },
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFBFBAB5),
            ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentOrange)
                .clickable(onClick = onStartClick),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (featuredPlan != null) {
                    Image(
                        imageVector = Play,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
                BasicText(
                    text = if (featuredPlan != null) "Start Workout" else "Explore Workouts",
                    style = PulseTheme.typography.ctaPrimary.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PopulatedFeaturedWorkoutCard(
    plan: HomePlanItemUiModel,
    hasActiveSession: Boolean,
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.home_hero_upper_body),
                contentDescription = plan.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.5f to Color.Black.copy(alpha = 0.40f),
                                1f to Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.90f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentOrange),
                )
                BasicText(
                    text = if (hasActiveSession) "ACTIVE WORKOUT" else "TODAY'S WORKOUT",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.28.sp,
                        color = TextPrimaryDark,
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = plan.name,
                    style = PulseTheme.typography.displayHero.copy(
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = "${plan.exerciseCount} exercises • ${plan.plannedSetsCount} planned sets",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE6F3F0EF),
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentOrange)
                    .clickable(onClick = onStartClick),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        imageVector = Play,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                    BasicText(
                        text = if (hasActiveSession) "Resume Workout" else "Start Workout",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstWorkoutWeeklyProgressCard(
    summary: WeeklyProgressSummary,
    onClick: () -> Unit,
) {
    val targetText = summary.weeklyTarget?.toString() ?: "—"
    val volumeText = formatKg(summary.totalVolumeKg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "THIS WEEK'S PROGRESS",
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted,
                ),
            )
            BasicText(
                text = "Day ${summary.dayOfWeekNumber} of 7",
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                ),
            )
        }

        BasicText(
            text = "${summary.completedWorkoutsCount} of $targetText Workouts Completed",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
            ),
        )

        BasicText(
            text = if (summary.partialWorkoutsCount > 0) {
                "$volumeText lifted • ${summary.partialWorkoutsCount} partial session logged this week."
            } else {
                "$volumeText lifted • Complete your first session to build training trends."
            },
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryMuted,
            ),
        )

        BasicText(
            text = "View Training Progress & PRs →",
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
            style = PulseTheme.typography.label.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentOrange,
            ),
        )
    }
}

@Composable
private fun PopulatedWeeklyProgressCard(
    summary: WeeklyProgressSummary,
    onClick: () -> Unit,
) {
    val targetText = summary.weeklyTarget?.toString() ?: "—"
    val targetPercent = (summary.progressFraction * 100f).roundToInt()
    val volumeText = formatKg(summary.totalVolumeKg)
    val maxDayVolume = (summary.dailyVolumeKg.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val todayIndex = (summary.dayOfWeekNumber - 1).coerceIn(0, 6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "THIS WEEK'S PROGRESS",
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = TextSecondaryMuted,
                ),
            )
            if (summary.partialWorkoutsCount > 0) {
                BasicText(
                    text = "${summary.partialWorkoutsCount} partial",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryMuted,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = "${summary.completedWorkoutsCount} of $targetText Workouts Completed",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                    ),
                )
                BasicText(
                    text = if (summary.weeklyTarget != null) {
                        "$volumeText lifted • $targetPercent% of weekly target"
                    } else {
                        "$volumeText lifted"
                    },
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentGreen,
                    ),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(38.dp),
            ) {
                summary.dailyVolumeKg.forEachIndexed { index, dayVolume ->
                    val barHeight = if (dayVolume <= 0.0) {
                        6.dp
                    } else {
                        val ratio = (dayVolume / maxDayVolume).toFloat().coerceIn(0.2f, 1f)
                        (6f + ratio * 32f).dp
                    }
                    val barColor = when {
                        index == todayIndex && dayVolume > 0.0 -> AccentOrange
                        dayVolume > 0.0 -> Color(0xFFD1CCC7)
                        else -> Color(0xFFF1EEEA)
                    }
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "View Detailed Analytics & PRs",
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentOrange,
                ),
            )
            BasicText(
                text = "→",
                style = PulseTheme.typography.label.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                ),
            )
        }
    }
}

@Composable
private fun HomePlanPreviewRow(
    plan: HomePlanItemUiModel,
    thumbnailResId: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = thumbnailResId),
                contentDescription = plan.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = plan.name,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1C),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = "${plan.exerciseCount} exercises • ${plan.plannedSetsCount} sets",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF70707A),
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PillSurface),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "›",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF70707A),
                ),
            )
        }
    }
}

@Composable
private fun NoPlansEmptyCard(
    onExploreWorkouts: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, CardBorderLight), RoundedCornerShape(14.dp))
            .clickable(onClick = onExploreWorkouts)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = "No workout plans yet",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
            ),
        )
        BasicText(
            text = "Create your first training plan in the Workouts tab to pin it here.",
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                color = TextSecondaryMuted,
            ),
        )
    }
}

@Composable
private fun LastSessionCard(
    session: HomeLastSessionUiModel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorderLight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = HistoryClock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(TextBrownMuted),
                )
                BasicText(
                    text = "LAST SESSION • ${session.dateLabel}",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.55.sp,
                        color = TextBrownMuted,
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (session.isPartial) Color(0xFFFFF4E5) else PillSurface)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                BasicText(
                    text = if (session.isPartial) "Ended early" else "Saved on device",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (session.isPartial) Color(0xFFB45309) else AccentDarkGreen,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = session.planName,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val durationPart = session.durationLabel?.let { " • $it" }.orEmpty()
                BasicText(
                    text = "${session.totalFullReps} reps$durationPart • ${session.completedSetsCount} completed sets",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBrownMuted,
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.isPartial) Color(0xFFF59E0B).copy(alpha = 0.25f)
                        else Color(0xFF4EDEA3).copy(alpha = 0.30f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!session.isPartial) {
                    Image(
                        imageVector = CheckMark,
                        contentDescription = "Completed",
                        modifier = Modifier.size(12.dp),
                        colorFilter = ColorFilter.tint(AccentDarkGreen),
                    )
                }
            }
        }
    }
}

private fun resolvePlanThumbnailRes(planName: String, index: Int): Int {
    val lower = planName.lowercase(Locale.US)
    return when {
        "lower" in lower || "leg" in lower || "squat" in lower -> R.drawable.plan_thumb_lower_body
        "full" in lower || "functional" in lower -> R.drawable.plan_thumb_full_body
        "upper" in lower || "push" in lower || "pull" in lower -> R.drawable.plan_thumb_upper_body
        index % 3 == 1 -> R.drawable.plan_thumb_lower_body
        index % 3 == 2 -> R.drawable.plan_thumb_full_body
        else -> R.drawable.plan_thumb_upper_body
    }
}

private fun formatKg(volumeKg: Double): String {
    val rounded = volumeKg.roundToInt()
    val formatted = NumberFormat.getIntegerInstance(Locale.US).format(rounded)
    return "$formatted kg"
}
