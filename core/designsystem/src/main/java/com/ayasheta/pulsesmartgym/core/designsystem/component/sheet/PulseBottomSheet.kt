package com.ayasheta.pulsesmartgym.core.designsystem.component.sheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.CompactButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.NegativeButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.OutlineButton
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun PulseBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    summaryContent: (@Composable () -> Unit)? = null,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    isDestructivePrimary: Boolean = false,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(visible) }
    val slideFraction = remember { Animatable(1f) }
    val scrimAlpha = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(visible) {
        if (visible) {
            showDialog = true
            coroutineScope {
                launch {
                    scrimAlpha.animateTo(
                        targetValue = 0.5f,
                        animationSpec = tween(durationMillis = SHEET_ENTER_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                }
                launch {
                    slideFraction.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = SHEET_ENTER_DURATION_MS, easing = FastOutSlowInEasing),
                    )
                }
            }
        } else if (showDialog) {
            coroutineScope {
                launch {
                    scrimAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = SHEET_EXIT_DURATION_MS, easing = FastOutLinearInEasing),
                    )
                }
                launch {
                    slideFraction.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = SHEET_EXIT_DURATION_MS, easing = FastOutLinearInEasing),
                    )
                }
            }
            showDialog = false
        }
    }

    if (!showDialog) return

    Dialog(
        onDismissRequest = onDismissRequest,
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
                .background(Color.Black.copy(alpha = scrimAlpha.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = dismissOnClickOutside,
                    onClick = onDismissRequest,
                )
                .semantics { dialog() },
        ) {
            val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { sheetHeightPx = it.height }
                    .graphicsLayer {
                        val height = sheetHeightPx
                        translationY = if (height > 0) height * slideFraction.value else OFFSCREEN_FALLBACK_PX
                    }
                    .fillMaxWidth()
                    .clip(sheetShape)
                    .background(PulseTheme.color.container)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(horizontal = PulseTheme.space.huge, vertical = PulseTheme.space.medium),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(PulseTheme.corners.fullRounded)
                        .background(PulseTheme.color.dragHandle),
                )
                Spacer(Modifier.height(PulseTheme.space.xLarge))
                BasicText(
                    text = title,
                    style = PulseTheme.typography.sectionTitle.copy(
                        fontSize = 18.sp,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                if (description != null) {
                    Spacer(Modifier.height(PulseTheme.space.small))
                    BasicText(
                        text = description,
                        style = PulseTheme.typography.contentSecondary.copy(
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                }
                if (summaryContent != null) {
                    Spacer(Modifier.height(PulseTheme.space.large))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PulseTheme.corners.medium)
                            .background(PulseTheme.color.background)
                            .padding(PulseTheme.space.large),
                    ) {
                        summaryContent()
                    }
                }
                Spacer(Modifier.height(PulseTheme.space.huge))
                if (isDestructivePrimary) {
                    NegativeButton(
                        caption = primaryActionLabel,
                        isFilled = true,
                        onClick = onPrimaryAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    CompactButton(
                        caption = primaryActionLabel,
                        onClick = onPrimaryAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(Modifier.height(PulseTheme.space.medium))
                    OutlineButton(
                        caption = secondaryActionLabel,
                        isCompact = true,
                        onClick = onSecondaryAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(PulseTheme.space.small))
            }
        }
    }
}

/**
 * Reusable bottom-sheet overlay that animates its scrim alpha and slides the sheet content from
 * its full measured height outside the bottom edge (300ms enter / 240ms exit).
 */
@Composable
fun PulseAnimatedBottomSheetOverlay(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black,
    scrimAlphaMax: Float = 0.55f,
    dismissEnabled: Boolean = true,
    scrimTestTag: String? = null,
    sheetContent: @Composable BoxScope.(Modifier) -> Unit,
) {
    val slideFraction = remember { Animatable(1f) }
    val scrimAlpha = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                scrimAlpha.animateTo(
                    targetValue = scrimAlphaMax,
                    animationSpec = tween(durationMillis = SHEET_ENTER_DURATION_MS, easing = FastOutSlowInEasing),
                )
            }
            launch {
                slideFraction.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = SHEET_ENTER_DURATION_MS, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scrimColor.copy(alpha = scrimAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = dismissEnabled,
                onClick = onDismissRequest,
            )
            .let { if (scrimTestTag != null) it.semantics { dialog() } else it },
        contentAlignment = Alignment.BottomCenter,
    ) {
        val sheetModifier = Modifier
            .align(Alignment.BottomCenter)
            .onSizeChanged { sheetHeightPx = it.height }
            .graphicsLayer {
                val height = sheetHeightPx
                translationY = if (height > 0) height * slideFraction.value else OFFSCREEN_FALLBACK_PX
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
        sheetContent(sheetModifier)
    }
}

private const val SHEET_ENTER_DURATION_MS = 300
private const val SHEET_EXIT_DURATION_MS = 240
private const val OFFSCREEN_FALLBACK_PX = 2000f

@Preview(showBackground = true)
@Composable
private fun PulseBottomSheetPreview() {
    PulseTheme {
        PulseBottomSheet(
            visible = true,
            onDismissRequest = {},
            title = "End Workout Early?",
            description = "You have completed 5 of 16 sets. Ending now will record your completed volume to history and discard unfinished exercises.",
            summaryContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        text = "Logged Volume: 4,250 kg • Duration: 18m 45s",
                        style = PulseTheme.typography.caption.copy(
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                    BasicText(
                        text = "Data will be saved locally to device storage",
                        style = PulseTheme.typography.supportingContent.copy(
                            color = PulseTheme.color.successDark,
                        ),
                    )
                }
            },
            primaryActionLabel = "Save & End Workout",
            onPrimaryAction = {},
            secondaryActionLabel = "Resume Workout",
            onSecondaryAction = {},
            isDestructivePrimary = true,
        )
    }
}
