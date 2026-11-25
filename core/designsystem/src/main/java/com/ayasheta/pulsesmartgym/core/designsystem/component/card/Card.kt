package com.ayasheta.pulsesmartgym.core.designsystem.component.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.Text
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = PulseTheme.corners.xLarge,
    cardColor: Color = PulseTheme.color.container,
    borderColor: Color? = PulseTheme.color.border,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderModifier = if (borderColor != null) {
        Modifier.border(width = 1.dp, color = borderColor, shape = shape)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(cardColor)
            .then(borderModifier)
            .padding(contentPadding),
    ) {
        content()
    }
}

@Preview
@Composable
private fun CardPreview() {
    PulseTheme {
        Card(
            contentPadding = PaddingValues(PulseTheme.space.large),
        ) {
            Text("PULSE Card Container")
        }
    }
}
