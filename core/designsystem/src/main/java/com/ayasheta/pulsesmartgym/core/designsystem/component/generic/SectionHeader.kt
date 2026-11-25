package com.ayasheta.pulsesmartgym.core.designsystem.component.generic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.TextButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.SectionHeaderText
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme

@Composable
fun SectionHeader(
    sectionTitle: String,
    buttonCaption: String,
    onClickButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionHeaderText(sectionTitle)
        TextButton(
            caption = buttonCaption,
            onClick = onClickButton,
        )
    }
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    PulseTheme {
        Box(Modifier.background(PulseTheme.color.background)) {
            SectionHeader(
                sectionTitle = "Exercises (4)",
                buttonCaption = "View All",
                onClickButton = {},
            )
        }
    }
}
