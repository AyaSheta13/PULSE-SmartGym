package net.thechance.tcos.designsystem.component.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme

@Composable
fun OutlineButton(
    caption: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
    isLoading: Boolean = false,
    loading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
){
    BaseButton(
        modifier = modifier,
        onClick = onClick,
        icon = icon,
        loading = loading,
        caption = caption,
        isLoading = isLoading,
        isEnabled = isEnabled,
        hasBorder = true,
        borderColor = Theme.color.shadeQuaternary,
        containerColor = Theme.color.background,
        disableContainerColor = Theme.color.background,
        contentColor = Theme.color.shadeSecondary,
        disableContentColor = Theme.color.disable
    )
}

@Composable
fun OutlineIconButton(
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    loading: (@Composable () -> Unit)? = null,
    isEnabled: Boolean= true,
){
    BaseButton(
        modifier = modifier,
        onClick = onClick,
        icon = icon,
        loading = loading,
        isLoading = isLoading,
        isEnabled = isEnabled,
        hasBorder = true,
        borderColor = Theme.color.shadeQuaternary,
        containerColor = Theme.color.background,
        disableContainerColor = Theme.color.background,
        contentColor = Theme.color.shadeSecondary,
        disableContentColor = Theme.color.disable
    )
}

@Preview(name = "Normal")
@Composable
private fun PreviewNormal(){
    TCOSTheme {
        OutlineButton(
            caption = "Add",
            onClick = {}
        )
    }
}

@Preview(name = "NormalWith Icon")
@Composable
private fun PreviewNormalWithIcon(){
    TCOSTheme {
        OutlineButton(
            caption = "Add",
            icon = painterResource(R.drawable.ic_cart),
            onClick = {}
        )
    }
}

@Preview(name = "Icon")
@Composable
private fun PreviewIcon(){
    TCOSTheme {
        OutlineIconButton(
            icon = painterResource(R.drawable.ic_cart),
            onClick = {}
        )
    }
}

@Preview(name = "Normal Disable")
@Composable
private fun PreviewNormalDisable(){
    TCOSTheme {
        OutlineButton(
            caption = "Add",
            onClick = {},
            isEnabled = false
        )
    }
}

@Preview(name = "NormalWith Icon Disable")
@Composable
private fun PreviewNormalWithIconDisable(){
    TCOSTheme {
        OutlineButton(
            caption = "Add",
            icon = painterResource(R.drawable.ic_cart),
            onClick = {},
            isEnabled = false
        )
    }
}

@Preview(name = "Icon Disable")
@Composable
private fun PreviewIconDisable(){
    TCOSTheme {
        OutlineIconButton(
            icon = painterResource(R.drawable.ic_cart),
            onClick = {},
            isEnabled = false
        )
    }
}