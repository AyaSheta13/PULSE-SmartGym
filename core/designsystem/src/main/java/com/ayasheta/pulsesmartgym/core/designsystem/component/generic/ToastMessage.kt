package com.ayasheta.pulsesmartgym.core.designsystem.component.generic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.SubTitleText
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.TextSupportContent
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Close
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.ErrorCircle
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.SuccessCircle

enum class ToastStatus { SUCCESS, ERROR }

@Composable
fun ToastMessage(
    status: ToastStatus,
    isVisible: Boolean,
    title: String,
    description: String,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        val statusColor = if (status == ToastStatus.SUCCESS) PulseTheme.color.success else PulseTheme.color.error
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(PulseTheme.size.giant)
                .dropShadow(
                    shape = PulseTheme.corners.xLarge,
                    shadow = Shadow(
                        radius = 16.dp,
                        offset = DpOffset(x = 0.dp, y = 4.dp),
                        color = statusColor.copy(alpha = 0.08f),
                    ),
                )
                .clip(PulseTheme.corners.xLarge)
                .background(PulseTheme.color.container)
                .border(1.dp, PulseTheme.color.border, PulseTheme.corners.xLarge),
        ) {
            Image(
                imageVector = Close,
                contentDescription = "Close Icon",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(PulseTheme.space.small)
                    .size(PulseTheme.size.tiny)
                    .clip(PulseTheme.corners.fullRounded)
                    .clickable(onClick = onClickClose),
                colorFilter = ColorFilter.tint(PulseTheme.color.shadeSecondary),
            )
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = PulseTheme.space.medium,
                        vertical = PulseTheme.space.small,
                    )
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(status)
                Column {
                    SubTitleText(
                        text = title,
                        color = PulseTheme.color.shadePrimary,
                    )
                    TextSupportContent(description)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ToastStatus) {
    Image(
        imageVector = if (status == ToastStatus.SUCCESS) SuccessCircle else ErrorCircle,
        contentDescription = "${status.name} icon",
        modifier = Modifier
            .padding(end = PulseTheme.space.medium)
            .size(PulseTheme.size.medium),
        colorFilter = ColorFilter.tint(
            color = if (status == ToastStatus.SUCCESS) {
                PulseTheme.color.success
            } else {
                PulseTheme.color.error
            },
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ToastMessagePreview() {
    PulseTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PulseTheme.color.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ToastMessage(
                status = ToastStatus.SUCCESS,
                isVisible = true,
                title = "Success",
                description = "Workout saved to device",
                onClickClose = {},
            )
            ToastMessage(
                status = ToastStatus.ERROR,
                isVisible = true,
                title = "Error",
                description = "Unable to connect to sensor",
                onClickClose = {},
            )
        }
    }
}
