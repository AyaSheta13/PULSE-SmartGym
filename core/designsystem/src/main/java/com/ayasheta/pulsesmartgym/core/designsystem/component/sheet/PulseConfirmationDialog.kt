package com.ayasheta.pulsesmartgym.core.designsystem.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.CompactButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.NegativeButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.OutlineButton
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun PulseConfirmationDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = dismissOnClickOutside,
                    onClick = onDismiss,
                )
                .padding(PulseTheme.space.huge)
                .semantics { dialog() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PulseTheme.corners.xLarge)
                    .background(PulseTheme.color.container)
                    .border(1.dp, PulseTheme.color.border, PulseTheme.corners.xLarge)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(PulseTheme.space.huge),
            ) {
                BasicText(
                    text = title,
                    style = PulseTheme.typography.sectionTitle.copy(
                        fontSize = 18.sp,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                Spacer(Modifier.height(PulseTheme.space.small))
                BasicText(
                    text = message,
                    style = PulseTheme.typography.contentSecondary.copy(
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
                Spacer(Modifier.height(PulseTheme.space.huge))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.medium),
                ) {
                    OutlineButton(
                        caption = dismissLabel,
                        isCompact = true,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDestructive) {
                        NegativeButton(
                            caption = confirmLabel,
                            isFilled = true,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        CompactButton(
                            caption = confirmLabel,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseConfirmationDialogPreview() {
    PulseTheme {
        PulseConfirmationDialog(
            visible = true,
            title = "Delete Workout Plan?",
            message = "This will permanently remove Upper Body Power from your saved plans.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {},
            onDismiss = {},
            isDestructive = true,
        )
    }
}
