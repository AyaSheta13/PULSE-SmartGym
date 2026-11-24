package net.thechance.tcos.designsystem.component.generic

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
import net.thechance.tcos.designsystem.component.button.TextButton
import net.thechance.tcos.designsystem.component.text.SectionTitle
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme

@Composable
fun SectionHeader(
    sectionTitle: String,
    buttonCaption: String,
    onClickButton: () -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SectionTitle(sectionTitle)
        TextButton(
            caption = buttonCaption,
            onClick = onClickButton
        )
    }
}


@Preview
@Composable
private fun SectionHeaderPreview() {
    TCOSTheme {
        Box(Modifier.background(Theme.color.background)){
            SectionHeader(
                sectionTitle = "Deals",
                buttonCaption = "View All",
                onClickButton = {}
            )
        }
    }
}