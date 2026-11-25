package net.thechance.tcos.designsystem.component.card

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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.baseToken.ColorPalette
import net.thechance.tcos.designsystem.component.chips.Chip
import net.thechance.tcos.designsystem.component.text.CardTitleText
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme

/**
 * @param tag1Text if it null then chip won't appear
 */
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
){
    Column(
        modifier = modifier
    ) {
        LabeledImage(
            image,
            title,
            mainLabel
        )
        title?.let {
            CardTitleText(
                text = it,
                modifier = Modifier.padding(bottom = Theme.space.tiny),
            )
        }
        TagsRow(
            tag1Text,
            tag2Text,
            tag1Icon,
            tag2Icon
        )
    }

}
@Composable
private fun LabeledImage(
    image: Painter,
    title: String? = null,
    mainLabel: String? = null,
){
    Card(
        modifier = Modifier.padding(bottom = Theme.space.small)
            .fillMaxWidth()
            .height(122.dp)

    ) {
        Box(modifier = Modifier.fillMaxSize()){
            Image(
                painter = image,
                contentDescription = "$title image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            mainLabel?.let {
                Chip(
                    caption = it,
                    containerColor = ColorPalette.black.copy(alpha = 0.44f),
                    contentColor = Theme.color.onPrimary,
                    modifier = Modifier.padding(Theme.space.small)
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
){
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.space.small)) {
        tag1Text?.let {
            Chip(
                caption = it,
                icon = tag1Icon
            )
        }
        tag2Text?.let {
            Chip(
                caption = it,
                icon = tag2Icon
            )
        }
    }
}


@Preview
@Composable
private fun CardItemPreview(){
    TCOSTheme() {
        Column(Modifier
            .background(Theme.color.background)
            .padding(80.dp)
            .width(180.dp)
        ) {
            CardItem(
                image = painterResource(R.drawable.rabbit),
                title = "Rabbit",
                mainLabel = "label",
                tag1Text = "Tag1",
                tag1Icon = painterResource(R.drawable.ic_cart),
                tag2Text = "Tag1",
                tag2Icon = painterResource(R.drawable.ic_cart)
            )
        }

    }
}
