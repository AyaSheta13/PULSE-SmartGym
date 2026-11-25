package com.ayasheta.pulsesmartgym.feature.navigationdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.CompactButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.OutlineButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.card.Card
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseBottomSheet
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseConfirmationDialog
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.Label
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.SectionHeaderText
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.TextContentPrimary
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.TextSupportContent
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

data class NavigationDemoAction(
    val label: String,
    val isPrimary: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun NavigationPlaceholderScreen(
    title: String,
    routeLabel: String,
    modifier: Modifier = Modifier,
    topBarStyle: PulseTopAppBarStyle = PulseTopAppBarStyle.TitleOnly(title = title),
    routeArguments: List<Pair<String, String>> = emptyList(),
    actions: List<NavigationDemoAction> = emptyList(),
    onBackClick: (() -> Unit)? = null,
    supportsSheetDemo: Boolean = false,
    supportsDialogDemo: Boolean = false,
    onDebugSkipToMain: (() -> Unit)? = null,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseTheme.color.background),
    ) {
        PulseTopAppBar(style = topBarStyle)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PulseTheme.space.large),
            verticalArrangement = Arrangement.spacedBy(PulseTheme.space.large),
        ) {
            Card(contentPadding = PaddingValues(PulseTheme.space.large)) {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.space.small)) {
                    Label(
                        text = "NAVIGATION 3 PLACEHOLDER",
                        color = PulseTheme.color.primary,
                    )
                    SectionHeaderText(text = title)
                    TextContentPrimary(text = "Route: $routeLabel")
                    routeArguments.forEach { (key, value) ->
                        TextSupportContent(text = "$key = $value")
                    }
                }
            }

            if (actions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.space.medium)) {
                    actions.forEach { action ->
                        if (action.isPrimary) {
                            Button(
                                caption = action.label,
                                onClick = action.onClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            OutlineButton(
                                caption = action.label,
                                isCompact = true,
                                onClick = action.onClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (supportsSheetDemo) {
                OutlineButton(
                    caption = "Open Modal Sheet",
                    isCompact = true,
                    onClick = { showSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (supportsDialogDemo) {
                OutlineButton(
                    caption = "Open Confirmation Dialog",
                    isCompact = true,
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (onBackClick != null) {
                OutlineButton(
                    caption = "Navigate Back",
                    isCompact = true,
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (onDebugSkipToMain != null) {
                Spacer(Modifier.height(4.dp))
                CompactButton(
                    caption = "Debug: Enter Main App Shell",
                    onClick = onDebugSkipToMain,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    PulseBottomSheet(
        visible = showSheet,
        onDismissRequest = { showSheet = false },
        title = "Sample Modal Sheet",
        description = "Blocks background touches and dismisses on System Back or scrim tap without popping the underlying route.",
        primaryActionLabel = "Confirm",
        onPrimaryAction = { showSheet = false },
        secondaryActionLabel = "Cancel",
        onSecondaryAction = { showSheet = false },
    )

    PulseConfirmationDialog(
        visible = showDialog,
        title = "Sample Confirmation",
        message = "Modal dialog overlay confined to Dialog window.",
        confirmLabel = "Confirm",
        dismissLabel = "Cancel",
        onConfirm = { showDialog = false },
        onDismiss = { showDialog = false },
    )
}
