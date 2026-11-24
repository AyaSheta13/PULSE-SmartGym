package net.thechance.tcos.designsystem.component.generic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import net.thechance.tcos.designsystem.component.text.SubTitleText
import net.thechance.tcos.designsystem.component.text.TextSupportContent
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme
import net.thechance.tcos.designsystem.vectors.Close
import net.thechance.tcos.designsystem.vectors.ErrorCircle
import net.thechance.tcos.designsystem.vectors.SuccessCircle

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
        val shadowColor = if (status == ToastStatus.SUCCESS) Theme.color.success else Theme.color.error
        Box(
            modifier = modifier.fillMaxWidth()
                .height(Theme.size.giant)
                .dropShadow(
                    shape = Theme.corners.xLarge,
                    shadow = Shadow(
                        radius = 16.dp,
                        offset = DpOffset(x = 0.dp, y = 4.dp),
                        color = shadowColor.copy(alpha = 0.08f)
                    )
                )
                .clip(Theme.corners.xLarge)
                .background(Theme.color.container)


        ){
            Image(
                imageVector = Close,
                contentDescription = "Close Icon",
                modifier = modifier.align(Alignment.TopEnd)
                    .padding(Theme.space.medium)
                    .size(Theme.size.tiny)
                    .clip(Theme.corners.fullRounded)
                    .clickable(onClick = onClickClose),
                colorFilter = ColorFilter.tint(Theme.color.shadeSecondary)
            )
            Row(
                modifier = Modifier.padding(
                    horizontal = Theme.space.large,
                    vertical = Theme.space.medium,
                ).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status)
                Column() {
                    SubTitleText(
                        text = title,
                        color = Theme.color.shadePrimary
                    )
                    TextSupportContent(description)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ToastStatus){
    Image(
        imageVector = if (status == ToastStatus.SUCCESS) {
            SuccessCircle
        } else {
            ErrorCircle
        },
        contentDescription = "${status.name} icon",
        modifier = Modifier.padding(end = Theme.space.large)
            .size(Theme.size.medium),
        colorFilter = ColorFilter.tint(
            color = if (status == ToastStatus.SUCCESS) {
                Theme.color.success
            } else {
                Theme.color.error
            }
        )
    )
}

@Preview
@Composable
private fun ToastMessagePreview() {
    TCOSTheme() {
        Column(
            Modifier.fillMaxWidth().background(Theme.color.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ToastMessage(
                status = ToastStatus.SUCCESS,
                isVisible = true,
                title = "Success",
                description = "Book Added to favourites",
                onClickClose = {}
            )
            ToastMessage(
                status = ToastStatus.ERROR,
                isVisible = true,
                title = "Error",
                description = "Invalid Password",
                onClickClose = {}
            )
        }
    }
}