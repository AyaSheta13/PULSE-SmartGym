package com.ayasheta.pulsesmartgym.feature.profile

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import java.util.Locale

private val ProfileBackground = Color(0xFFFCF9F8)
private val ProfileCardBorder = Color(0xFFF1EEEA)
private val ProfileTextPrimary = Color(0xFF1C1B1B)
private val ProfileTextSecondary = Color(0xFF7A7570)
private val ProfileOrange = Color(0xFFFF623E)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditProfileClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    deviceStatusText: String = "Not paired • Tap to connect",
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreenContent(
        uiState = uiState,
        onEditProfileClick = onEditProfileClick,
        onDeviceClick = onDeviceClick,
        onOpenSheet = viewModel::openSheet,
        onDismissSheet = viewModel::dismissSheet,
        onConfirmMetricUnits = viewModel::confirmMetricUnits,
        onLogoutClick = { viewModel.signOut(onLoggedOut) },
        modifier = modifier,
        deviceStatusText = deviceStatusText,
    )
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onOpenSheet: (ProfileModalSheet) -> Unit,
    onDismissSheet: () -> Unit,
    onConfirmMetricUnits: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    deviceStatusText: String = "Not paired • Tap to connect",
) {
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProfileBackground),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.TitleOnly("Profile & Settings"),
        )

        when (uiState) {
            ProfileUiState.Loading, ProfileUiState.SignedOut -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseLoadingIndicator()
                }
            }

            is ProfileUiState.Error -> {
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

            is ProfileUiState.Loaded -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("profile_scroll_list"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(key = "avatar_card") {
                        ProfileHeaderCard(
                            fullName = uiState.fullName,
                            initials = uiState.initials,
                            subtitleIdentity = uiState.subtitleIdentity,
                            membershipLabel = uiState.membershipLabel,
                            onEditClick = onEditProfileClick,
                        )
                    }

                    item(key = "physical_stats") {
                        PhysicalStatsCard(
                            ageYears = uiState.ageYears,
                            heightCm = uiState.heightCm,
                            weightKg = uiState.weightKg,
                            fitnessGoal = uiState.fitnessGoal,
                            weightUnit = uiState.preferredWeightUnit,
                        )
                    }

                    item(key = "pulse_device") {
                        MyPulseDeviceCard(
                            deviceStatusText = deviceStatusText,
                            onClick = onDeviceClick,
                        )
                    }

                    item(key = "settings_menu") {
                        SettingsMenuCard(
                            isGuest = uiState.isGuest,
                            preferredWeightUnit = uiState.preferredWeightUnit,
                            onAccountPreferencesClick = { onOpenSheet(ProfileModalSheet.ACCOUNT_PREFERENCES) },
                            onUnitsClick = { onOpenSheet(ProfileModalSheet.UNITS_AND_MEASUREMENTS) },
                            onRemindersClick = { onOpenSheet(ProfileModalSheet.WORKOUT_REMINDERS_COMING_SOON) },
                            onPrivacyClick = { onOpenSheet(ProfileModalSheet.PRIVACY_AND_LOCAL_DATA) },
                        )
                    }

                    item(key = "logout_button") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF2D9D9)), RoundedCornerShape(14.dp))
                                .clickable(onClick = onLogoutClick)
                                .testTag("profile_logout_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "Log Out",
                                style = PulseTheme.typography.ctaPrimary.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53333),
                                ),
                            )
                        }
                    }
                }

                ProfileModalHost(
                    activeSheet = uiState.activeSheet,
                    isGuest = uiState.isGuest,
                    preferredUnit = uiState.preferredWeightUnit,
                    onDismiss = onDismissSheet,
                    onConfirmUnits = onConfirmMetricUnits,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    fullName: String,
    initials: String,
    subtitleIdentity: String,
    membershipLabel: String,
    onEditClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, ProfileCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ProfileOrange),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = initials,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                BasicText(
                    text = fullName,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileTextPrimary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = subtitleIdentity,
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = ProfileTextSecondary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = membershipLabel,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ProfileOrange,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF6F3F2))
                .border(1.dp, ProfileCardBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onEditClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("profile_edit_button"),
        ) {
            BasicText(
                text = "Edit",
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileTextPrimary,
                ),
            )
        }
    }
}

@Composable
private fun PhysicalStatsCard(
    ageYears: Int?,
    heightCm: Int?,
    weightKg: Double?,
    fitnessGoal: FitnessGoal?,
    weightUnit: WeightUnit,
) {
    val ageValue = ageYears?.let { "$it yrs" } ?: "—"
    val heightValue = heightCm?.let { "$it cm" } ?: "—"
    val weightValue = weightKg?.let {
        val unitLabel = if (weightUnit == WeightUnit.KG) "kg" else "lbs"
        String.format(Locale.US, "%.1f %s", it, unitLabel)
    } ?: "—"
    val targetValue = when (fitnessGoal) {
        FitnessGoal.HYPERTROPHY -> "Hypertrophy"
        FitnessGoal.STRENGTH -> "Strength"
        FitnessGoal.FITNESS -> "Fitness"
        null -> "—"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, ProfileCardBorder, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatColumn(label = "AGE", value = ageValue, modifier = Modifier.weight(0.9f))
        StatDivider()
        StatColumn(label = "HEIGHT", value = heightValue, modifier = Modifier.weight(1f))
        StatDivider()
        StatColumn(label = "WEIGHT", value = weightValue, modifier = Modifier.weight(1f))
        StatDivider()
        StatColumn(label = "TARGET", value = targetValue, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProfileTextSecondary,
            ),
            maxLines = 1,
        )
        BasicText(
            text = value,
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ProfileTextPrimary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(ProfileCardBorder),
    )
}

@Composable
private fun MyPulseDeviceCard(
    deviceStatusText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("profile_device_card"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_device_bluetooth_badge),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                BasicText(
                    text = "My PULSE Device",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    ),
                )
                BasicText(
                    text = deviceStatusText,
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                BasicText(
                    text = "Connect",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                    ),
                )
            }
            BasicText(
                text = "›",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 18.sp,
                    color = Color(0xFF64748B),
                ),
            )
        }
    }
}

@Composable
private fun SettingsMenuCard(
    isGuest: Boolean,
    preferredWeightUnit: WeightUnit,
    onAccountPreferencesClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    val unitsSummary = if (preferredWeightUnit == WeightUnit.KG) "Metric (kg, cm)" else "Imperial (lbs, in)"
    val accountSummary = if (isGuest) "(Guest Profile)" else "(Offline Profile)"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, ProfileCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SettingsMenuRow(
            iconRes = R.drawable.ic_settings_gear,
            title = "Account Preferences",
            trailingValue = accountSummary,
            showChevron = true,
            onClick = onAccountPreferencesClick,
            testTag = "settings_row_account",
        )
        MenuDivider()
        SettingsMenuRow(
            iconRes = R.drawable.ic_units_sliders,
            title = "Units & Measurements",
            trailingValue = unitsSummary,
            showChevron = true,
            onClick = onUnitsClick,
            testTag = "settings_row_units",
        )
        MenuDivider()
        SettingsMenuRow(
            iconRes = R.drawable.ic_reminders_bell,
            title = "Workout Reminders",
            trailingValue = "Coming Soon",
            showChevron = false,
            onClick = onRemindersClick,
            testTag = "settings_row_reminders",
        )
        MenuDivider()
        SettingsMenuRow(
            iconRes = R.drawable.ic_privacy_shield_row,
            title = "Privacy & Local Data",
            trailingValue = "Local Storage Active",
            showChevron = true,
            onClick = onPrivacyClick,
            testTag = "settings_row_privacy",
        )
    }
}

@Composable
private fun SettingsMenuRow(
    iconRes: Int,
    title: String,
    trailingValue: String,
    showChevron: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            BasicText(
                text = title,
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileTextPrimary,
                ),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = trailingValue,
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = ProfileTextSecondary,
                ),
            )
            if (showChevron) {
                BasicText(
                    text = "›",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA6A19C),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ProfileCardBorder),
    )
}
