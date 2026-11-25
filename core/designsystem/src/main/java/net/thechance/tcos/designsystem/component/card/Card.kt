package net.thechance.tcos.designsystem.component.card

import androidx.compose.foundation.background
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
import net.thechance.tcos.designsystem.component.text.Text
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = Theme.corners.xxLarge,
    cardColor: Color = Theme.color.container,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
){
    Column(
        modifier = modifier.clip(shape).background(cardColor).padding(contentPadding)
    ) {
        content()
    }
}

@Preview
@Composable
private fun CardPreview(){
    TCOSTheme {
        Card(
            contentPadding = PaddingValues(Theme.space.large)
        ) {
            Text("hello")
        }
    }
}