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
fun Button(
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
        hasBorder = false,
        containerColor = Theme.color.primary,
        disableContainerColor = Theme.color.disable,
        contentColor = Theme.color.onPrimary,
        disableContentColor = Theme.color.onDisable
    )
}

@Composable
fun IconButton(
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
        hasBorder = false,
        containerColor = Theme.color.primary,
        disableContainerColor = Theme.color.disable,
        contentColor = Theme.color.onPrimary,
        disableContentColor = Theme.color.onDisable
    )
}



@Preview(name = "Normal")
@Composable
private fun PreviewNormal(){
    TCOSTheme {
        Button(
            caption = "Add",
            onClick = {}
        )
    }
}

@Preview(name = "NormalWith Icon")
@Composable
private fun PreviewNormalWithIcon(){
    TCOSTheme {
        Button(
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
        IconButton(
            icon = painterResource(R.drawable.ic_cart),
            onClick = {}
        )
    }
}

@Preview(name = "Normal Disable")
@Composable
private fun PreviewNormalDisable(){
    TCOSTheme {
        Button(
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
        Button(
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
        IconButton(
            icon = painterResource(R.drawable.ic_cart),
            onClick = {},
            isEnabled = false
        )
    }
}