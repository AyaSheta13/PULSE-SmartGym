package com.ayasheta.pulsesmartgym.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseAnimatedBottomSheetHost
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseAnimatedBottomSheetOverlay
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit

private val SheetTextPrimary = Color(0xFF1C1B1B)
private val SheetTextSecondary = Color(0xFF7A7570)
private val SheetHandleColor = Color(0xFFD9D4CF)
private val SheetPrimaryOrange = Color(0xFFFF623E)

@Composable
fun ProfileModalHost(
    activeSheet: ProfileModalSheet?,
    isGuest: Boolean,
    preferredUnit: WeightUnit,
    onDismiss: () -> Unit,
    onConfirmUnits: () -> Unit,
    inWindow: Boolean = false,
) {
    PulseAnimatedBottomSheetHost(targetState = activeSheet) { sheet, visible, onExitComplete ->
        when (sheet) {
            ProfileModalSheet.ACCOUNT_PREFERENCES -> AccountPreferencesSheet(
                isGuest = isGuest,
                onDismiss = onDismiss,
                visible = visible,
                onExitComplete = onExitComplete,
                inWindow = inWindow,
            )
            ProfileModalSheet.UNITS_AND_MEASUREMENTS -> UnitsMeasurementsSheet(
                preferredUnit = preferredUnit,
                onConfirm = onConfirmUnits,
                onDismiss = onDismiss,
                visible = visible,
                onExitComplete = onExitComplete,
                inWindow = inWindow,
            )
            ProfileModalSheet.PRIVACY_AND_LOCAL_DATA -> PrivacyLocalDataSheet(
                onDismiss = onDismiss,
                visible = visible,
                onExitComplete = onExitComplete,
                inWindow = inWindow,
            )
            ProfileModalSheet.WORKOUT_REMINDERS_COMING_SOON -> WorkoutRemindersComingSoonSheet(
                onDismiss = onDismiss,
                visible = visible,
                onExitComplete = onExitComplete,
                inWindow = inWindow,
            )
        }
    }
}

@Composable
private fun ProfileModalFrame(
    onDismiss: () -> Unit,
    testTag: String,
    visible: Boolean = true,
    onExitComplete: () -> Unit = {},
    minHeight: Dp = 364.dp,
    inWindow: Boolean = false,
    content: @Composable () -> Unit,
) {
    val frameBody: @Composable () -> Unit = {
        PulseAnimatedBottomSheetOverlay(
            visible = visible,
            onDismissRequest = onDismiss,
            onExitComplete = onExitComplete,
            scrimAlphaMax = 0.55f,
            scrimTestTag = "${testTag}_scrim",
            modifier = Modifier.testTag("${testTag}_scrim"),
        ) { sheetModifier ->
            val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            Column(
                modifier = sheetModifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
                    .testTag(testTag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SheetHandleColor),
                )
                content()
            }
        }
    }

    if (inWindow) {
        frameBody()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        ) {
            frameBody()
        }
    }
}

@Composable
private fun SheetPrimaryButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SheetPrimaryOrange)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.ctaPrimary.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
        )
    }
}

@Composable
private fun AccountPreferencesSheet(
    isGuest: Boolean,
    onDismiss: () -> Unit,
    visible: Boolean = true,
    onExitComplete: () -> Unit = {},
    inWindow: Boolean = false,
) {
    ProfileModalFrame(
        onDismiss = onDismiss,
        testTag = "sheet_account_preferences",
        visible = visible,
        onExitComplete = onExitComplete,
        minHeight = 404.dp,
        inWindow = inWindow,
    ) {
        BasicText(
            text = "Account Preferences",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary,
            ),
        )

        val accountStatusLine = if (isGuest) {
            "You are currently signed in with an Offline Guest Profile on this device."
        } else {
            "Your local account, profile, and training data are stored locally on this device in Offline Mode."
        }

        BasicText(
            text = "$accountStatusLine\n\nCloud synchronization, multi-device backup, and remote account services are disabled in this offline version. All personal records and custom routines remain safely on your phone.",
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = SheetTextSecondary,
            ),
        )

        SheetPrimaryButton(label = "Got It", onClick = onDismiss)
    }
}

@Composable
private fun UnitsMeasurementsSheet(
    preferredUnit: WeightUnit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    visible: Boolean = true,
    onExitComplete: () -> Unit = {},
    inWindow: Boolean = false,
) {
    val isMetricSelected = preferredUnit == WeightUnit.KG
    ProfileModalFrame(
        onDismiss = onDismiss,
        testTag = "sheet_units",
        visible = visible,
        onExitComplete = onExitComplete,
        minHeight = 364.dp,
        inWindow = inWindow,
    ) {
        BasicText(
            text = "Units & Measurements",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary,
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF1EE))
                .border(
                    BorderStroke(1.5.dp, SheetPrimaryOrange),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "Metric System (kg, cm)",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SheetTextPrimary,
                    ),
                )
                BasicText(
                    text = "Kilograms for load, centimeters for height",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SheetTextSecondary,
                    ),
                )
            }
            if (isMetricSelected) {
                BasicText(
                    text = "✓",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SheetPrimaryOrange,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF7F6F2))
                .semantics { disabled() }
                .padding(horizontal = 16.dp)
                .testTag("unit_option_imperial_disabled"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "Imperial System (lbs, in)",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8C8782),
                    ),
                )
                BasicText(
                    text = "Coming in future release",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA6A19C),
                    ),
                )
            }
        }

        SheetPrimaryButton(label = "Confirm", onClick = onConfirm)
    }
}

@Composable
private fun PrivacyLocalDataSheet(
    onDismiss: () -> Unit,
    visible: Boolean = true,
    onExitComplete: () -> Unit = {},
    inWindow: Boolean = false,
) {
    ProfileModalFrame(
        onDismiss = onDismiss,
        testTag = "sheet_privacy",
        visible = visible,
        onExitComplete = onExitComplete,
        minHeight = 404.dp,
        inWindow = inWindow,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_privacy_shield_badge),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        BasicText(
            text = "Privacy & Local Storage",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary,
                textAlign = TextAlign.Center,
            ),
        )

        BasicText(
            text = "Your workout plans, completed sessions, and fitness preferences are currently stored on this device.\n\nCloud sync and online account services are not available in this version. Your fitness data remains local and under your control.",
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = SheetTextSecondary,
                textAlign = TextAlign.Center,
            ),
        )

        SheetPrimaryButton(label = "Got It", onClick = onDismiss)
    }
}

@Composable
private fun WorkoutRemindersComingSoonSheet(
    onDismiss: () -> Unit,
    visible: Boolean = true,
    onExitComplete: () -> Unit = {},
    inWindow: Boolean = false,
) {
    ProfileModalFrame(
        onDismiss = onDismiss,
        testTag = "sheet_reminders_coming_soon",
        visible = visible,
        onExitComplete = onExitComplete,
        inWindow = inWindow,
    ) {
        BasicText(
            text = "Workout Reminders",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary,
            ),
        )

        BasicText(
            text = "Scheduled workout reminders and notification alerts are coming in a future update. No background alarms or notifications are active in this version.",
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = SheetTextSecondary,
                textAlign = TextAlign.Center,
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        SheetPrimaryButton(label = "Got It", onClick = onDismiss)
    }
}
