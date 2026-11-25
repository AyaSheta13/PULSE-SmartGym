package com.ayasheta.pulsesmartgym.core.designsystem.component.generic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChip
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Back
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Close
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.ShareIcon
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Trash

sealed interface PulseTopAppBarStyle {
    data class BackWithTitle(
        val title: String,
        val onBackClick: () -> Unit,
    ) : PulseTopAppBarStyle

    data class BackWithTitleAndAction(
        val title: String,
        val actionLabel: String,
        val onBackClick: () -> Unit,
        val onActionClick: () -> Unit,
    ) : PulseTopAppBarStyle

    data class TitleWithBadge(
        val title: String,
        val badgeLabel: String,
        val onBadgeClick: (() -> Unit)? = null,
    ) : PulseTopAppBarStyle

    data class RootNavigationHeader(
        val overline: String,
        val title: String,
        val actionLabel: String,
        val onActionClick: () -> Unit,
    ) : PulseTopAppBarStyle

    data class HomeGreetingHeader(
        val greeting: String,
        val userName: String,
        val bluetoothStatus: PulseBluetoothChipStatus,
        val avatarInitial: String,
        val onBluetoothClick: () -> Unit,
        val onAvatarClick: () -> Unit,
    ) : PulseTopAppBarStyle

    data class LiveWorkoutHeader(
        val exerciseTitle: String,
        val sessionSubtitle: String,
        val bluetoothStatus: PulseBluetoothChipStatus,
        val onBackClick: () -> Unit,
        val onBluetoothClick: (() -> Unit)? = null,
    ) : PulseTopAppBarStyle

    data class WorkoutSummaryHeader(
        val title: String,
        val onDismissClick: () -> Unit,
        val onShareClick: () -> Unit,
    ) : PulseTopAppBarStyle

    data class BackWithTitleAndDelete(
        val title: String,
        val onBackClick: () -> Unit,
        val onDeleteClick: () -> Unit,
    ) : PulseTopAppBarStyle

    data class TitleOnly(
        val title: String,
    ) : PulseTopAppBarStyle
}

@Composable
fun PulseTopAppBar(
    style: PulseTopAppBarStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PulseTheme.color.container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PulseTheme.size.giant)
                .padding(horizontal = PulseTheme.space.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            when (style) {
                is PulseTopAppBarStyle.BackWithTitle -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                    ) {
                        TopBarIconButton(
                            icon = Back,
                            contentDescription = "Back",
                            onClick = style.onBackClick,
                        )
                        TopBarTitle(style.title)
                    }
                }

                is PulseTopAppBarStyle.BackWithTitleAndAction -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                    ) {
                        TopBarIconButton(
                            icon = Back,
                            contentDescription = "Back",
                            onClick = style.onBackClick,
                        )
                        TopBarTitle(style.title)
                    }
                    TopBarPillButton(
                        label = style.actionLabel,
                        containerColor = PulseTheme.color.surfaceMuted,
                        contentColor = PulseTheme.color.shadePrimary,
                        onClick = style.onActionClick,
                    )
                }

                is PulseTopAppBarStyle.TitleWithBadge -> {
                    TopBarTitle(style.title, modifier = Modifier.weight(1f))
                    TopBarPillButton(
                        label = style.badgeLabel,
                        containerColor = PulseTheme.color.surfaceMuted,
                        contentColor = PulseTheme.color.shadePrimary,
                        onClick = style.onBadgeClick,
                    )
                }

                is PulseTopAppBarStyle.RootNavigationHeader -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        BasicText(
                            text = style.overline,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.color.shadeSecondary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TopBarTitle(style.title)
                    }
                    TopBarPillButton(
                        label = style.actionLabel,
                        containerColor = PulseTheme.color.primary,
                        contentColor = PulseTheme.color.onPrimary,
                        onClick = style.onActionClick,
                    )
                }

                is PulseTopAppBarStyle.HomeGreetingHeader -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        BasicText(
                            text = style.greeting,
                            style = PulseTheme.typography.supportingContent.copy(
                                color = PulseTheme.color.shadeSecondary,
                            ),
                            maxLines = 1,
                        )
                        TopBarTitle(style.userName)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                    ) {
                        PulseBluetoothChip(
                            status = style.bluetoothStatus,
                            onClick = style.onBluetoothClick,
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.primary)
                                .clickable(onClick = style.onAvatarClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = style.avatarInitial,
                                style = PulseTheme.typography.ctaCompact.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTheme.color.onPrimary,
                                ),
                            )
                        }
                    }
                }

                is PulseTopAppBarStyle.LiveWorkoutHeader -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                    ) {
                        TopBarIconButton(
                            icon = Back,
                            contentDescription = "Back",
                            onClick = style.onBackClick,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            BasicText(
                                text = style.exerciseTitle,
                                style = PulseTheme.typography.subtitle.copy(
                                    color = PulseTheme.color.shadePrimary,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            BasicText(
                                text = style.sessionSubtitle,
                                style = PulseTheme.typography.supportingContent.copy(
                                    color = PulseTheme.color.shadeSecondary,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    PulseBluetoothChip(
                        status = style.bluetoothStatus,
                        onClick = style.onBluetoothClick,
                    )
                }

                is PulseTopAppBarStyle.WorkoutSummaryHeader -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                    ) {
                        TopBarIconButton(
                            icon = Close,
                            contentDescription = "Dismiss",
                            onClick = style.onDismissClick,
                        )
                        TopBarTitle(style.title)
                    }
                    TopBarIconButton(
                        icon = ShareIcon,
                        contentDescription = "Share summary",
                        onClick = style.onShareClick,
                    )
                }

                is PulseTopAppBarStyle.BackWithTitleAndDelete -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.small),
                    ) {
                        TopBarIconButton(
                            icon = Back,
                            contentDescription = "Back",
                            onClick = style.onBackClick,
                        )
                        TopBarTitle(style.title)
                    }
                    TopBarIconButton(
                        icon = Trash,
                        contentDescription = "Delete",
                        tint = PulseTheme.color.error,
                        onClick = style.onDeleteClick,
                    )
                }

                is PulseTopAppBarStyle.TitleOnly -> {
                    TopBarTitle(style.title, modifier = Modifier.weight(1f))
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PulseTheme.color.divider),
        )
    }
}

@Composable
private fun TopBarTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = title,
        modifier = modifier,
        style = PulseTheme.typography.subtitle.copy(
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = PulseTheme.color.shadePrimary,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = PulseTheme.color.shadePrimary,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(PulseTheme.corners.fullRounded)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(PulseTheme.size.small),
        )
    }
}

@Composable
private fun TopBarPillButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .clip(PulseTheme.corners.fullRounded)
            .background(containerColor)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(color = contentColor),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseTopAppBarAllVariantsPreview() {
    PulseTheme {
        Column(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseTopAppBar(PulseTopAppBarStyle.BackWithTitle("Screen Title", onBackClick = {}))
            PulseTopAppBar(
                PulseTopAppBarStyle.BackWithTitleAndAction(
                    title = "Screen Title",
                    actionLabel = "Action",
                    onBackClick = {},
                    onActionClick = {},
                ),
            )
            PulseTopAppBar(
                PulseTopAppBarStyle.TitleWithBadge(
                    title = "Workout History",
                    badgeLabel = "Local Storage",
                ),
            )
            PulseTopAppBar(
                PulseTopAppBarStyle.RootNavigationHeader(
                    overline = "CHOOSE YOUR NEXT SESSION",
                    title = "My Workouts",
                    actionLabel = "+ New Plan",
                    onActionClick = {},
                ),
            )
            PulseTopAppBar(
                PulseTopAppBarStyle.HomeGreetingHeader(
                    greeting = "Welcome back",
                    userName = "Good morning, Alex",
                    bluetoothStatus = PulseBluetoothChipStatus.DISCONNECTED,
                    avatarInitial = "A",
                    onBluetoothClick = {},
                    onAvatarClick = {},
                ),
            )
            PulseTopAppBar(
                PulseTopAppBarStyle.LiveWorkoutHeader(
                    exerciseTitle = "Barbell Bench Press",
                    sessionSubtitle = "Set 2 of 4 • 18:42",
                    bluetoothStatus = PulseBluetoothChipStatus.CONNECTED,
                    onBackClick = {},
                    onBluetoothClick = {},
                ),
            )
            PulseTopAppBar(
                PulseTopAppBarStyle.WorkoutSummaryHeader(
                    title = "Workout Summary",
                    onDismissClick = {},
                    onShareClick = {},
                ),
            )
            PulseTopAppBar(
                PulseTopAppBarStyle.BackWithTitleAndDelete(
                    title = "Edit Workout Plan",
                    onBackClick = {},
                    onDeleteClick = {},
                ),
            )
            PulseTopAppBar(PulseTopAppBarStyle.TitleOnly("Profile & Settings"))
        }
    }
}
