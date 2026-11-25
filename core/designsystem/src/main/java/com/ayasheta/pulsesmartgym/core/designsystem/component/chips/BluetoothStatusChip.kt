package com.ayasheta.pulsesmartgym.core.designsystem.component.chips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Bluetooth

enum class PulseBluetoothChipStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CONNECTION_LOST,
    FAILED,
}

@Composable
fun PulseBluetoothChip(
    status: PulseBluetoothChipStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val palette = when (status) {
        PulseBluetoothChipStatus.CONNECTED -> BluetoothChipPalette(
            background = PulseTheme.color.successVariant,
            border = PulseTheme.color.successBorder,
            content = PulseTheme.color.successContent,
            dot = PulseTheme.color.success,
            label = "Connected",
        )
        PulseBluetoothChipStatus.CONNECTING -> BluetoothChipPalette(
            background = PulseTheme.color.primaryContainer,
            border = PulseTheme.color.completedSetBorder,
            content = PulseTheme.color.primary,
            dot = PulseTheme.color.primary,
            label = "Connecting...",
        )
        PulseBluetoothChipStatus.CONNECTION_LOST -> BluetoothChipPalette(
            background = PulseTheme.color.warningVariant,
            border = PulseTheme.color.warning.copy(alpha = 0.4f),
            content = PulseTheme.color.warning,
            dot = PulseTheme.color.warning,
            label = "Connection Lost",
        )
        PulseBluetoothChipStatus.FAILED -> BluetoothChipPalette(
            background = PulseTheme.color.errorVariant,
            border = PulseTheme.color.error.copy(alpha = 0.4f),
            content = PulseTheme.color.error,
            dot = PulseTheme.color.error,
            label = "Failed",
        )
        PulseBluetoothChipStatus.DISCONNECTED -> BluetoothChipPalette(
            background = PulseTheme.color.telemetryDisconnectedBg,
            border = PulseTheme.color.telemetryDisconnectedBorder,
            content = PulseTheme.color.telemetryDisconnectedText,
            dot = PulseTheme.color.telemetryDisconnectedText,
            label = "Disconnected",
        )
    }

    Row(
        modifier = modifier
            .height(PulseTheme.size.small)
            .clip(PulseTheme.corners.large)
            .background(palette.background)
            .border(width = 1.dp, color = palette.border, shape = PulseTheme.corners.large)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = PulseTheme.space.small, vertical = PulseTheme.space.tiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Image(
            imageVector = Bluetooth,
            contentDescription = null,
            colorFilter = ColorFilter.tint(palette.content),
            modifier = Modifier.size(12.dp),
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(PulseTheme.corners.fullRounded)
                .background(palette.dot),
        )
        BasicText(
            text = palette.label,
            style = PulseTheme.typography.label.copy(color = palette.content),
        )
    }
}

private data class BluetoothChipPalette(
    val background: Color,
    val border: Color,
    val content: Color,
    val dot: Color,
    val label: String,
)

@Preview(showBackground = true)
@Composable
private fun PulseBluetoothChipPreview() {
    PulseTheme {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseBluetoothChip(status = PulseBluetoothChipStatus.DISCONNECTED, onClick = {})
            PulseBluetoothChip(status = PulseBluetoothChipStatus.CONNECTING)
            PulseBluetoothChip(status = PulseBluetoothChipStatus.CONNECTED, onClick = {})
            PulseBluetoothChip(status = PulseBluetoothChipStatus.CONNECTION_LOST)
            PulseBluetoothChip(status = PulseBluetoothChipStatus.FAILED)
        }
    }
}
