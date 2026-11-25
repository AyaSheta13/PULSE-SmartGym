package com.ayasheta.pulsesmartgym.core.designsystem.component.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.Chip
import com.ayasheta.pulsesmartgym.core.designsystem.component.text.CardTitleText
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Workouts

@Composable
fun CardItem(
    image: Painter,
    modifier: Modifier = Modifier,
    title: String? = null,
    mainLabel: String? = null,
    tag1Text: String? = null,
    tag2Text: String? = null,
    tag1Icon: Painter? = null,
    tag2Icon: Painter? = null,
) {
    Column(modifier = modifier) {
        LabeledImage(
            image = image,
            title = title,
            mainLabel = mainLabel,
        )
        title?.let {
            CardTitleText(
                text = it,
                modifier = Modifier.padding(bottom = PulseTheme.space.tiny),
            )
        }
        TagsRow(
            tag1Text = tag1Text,
            tag2Text = tag2Text,
            tag1Icon = tag1Icon,
            tag2Icon = tag2Icon,
        )
    }
}

@Composable
private fun LabeledImage(
    image: Painter,
    title: String? = null,
    mainLabel: String? = null,
) {
    Card(
        modifier = Modifier
            .padding(bottom = PulseTheme.space.small)
            .fillMaxWidth()
            .height(122.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = image,
                contentDescription = title?.let { "$it image" },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            mainLabel?.let {
                Chip(
                    caption = it,
                    containerColor = Color.Black.copy(alpha = 0.44f),
                    contentColor = PulseTheme.color.onPrimary,
                    borderColor = Color.Transparent,
                    modifier = Modifier.padding(PulseTheme.space.small),
                )
            }
        }
    }
}

@Composable
private fun TagsRow(
    tag1Text: String? = null,
    tag2Text: String? = null,
    tag1Icon: Painter? = null,
    tag2Icon: Painter? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.space.tiny)) {
        tag1Text?.let {
            Chip(caption = it, icon = tag1Icon)
        }
        tag2Text?.let {
            Chip(caption = it, icon = tag2Icon)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardItemPreview() {
    PulseTheme {
        Column(
            modifier = Modifier
                .background(PulseTheme.color.background)
                .padding(16.dp)
                .width(180.dp),
        ) {
            CardItem(
                image = rememberVectorPainter(Workouts),
                title = "Upper Body Power",
                mainLabel = "45 min",
                tag1Text = "Chest",
                tag2Text = "16 sets",
            )
        }
    }
}
