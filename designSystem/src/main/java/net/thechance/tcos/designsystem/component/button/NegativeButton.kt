package net.thechance.tcos.designsystem.component.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.baseToken.ColorPalette
import net.thechance.tcos.designsystem.semanticToken.defaultDarkColors
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme


@Composable
fun NegativeButton(
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
        containerColor = Theme.color.errorVariant,
        disableContainerColor = Theme.color.disable,
        contentColor = Theme.color.error,
        disableContentColor = Theme.color.onDisable
    )
}

@Composable
fun NegativeIconButton(
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
        containerColor = Theme.color.errorVariant,
        disableContainerColor = Theme.color.disable,
        contentColor = Theme.color.error,
        disableContentColor = Theme.color.onDisable
    )
}



@Preview(name = "Normal")
@Composable
private fun PreviewNormal(){
    TCOSTheme {
        NegativeButton(
            caption = "Delete",
            onClick = {}
        )
    }
}

@Preview(name = "NormalWith Icon")
@Composable
private fun PreviewNormalWithIcon(){
    TCOSTheme {
        NegativeButton(
            caption = "Delete",
            icon = painterResource(R.drawable.ic_delete),
            onClick = {}
        )
    }
}

@Preview(name = "Icon")
@Composable
private fun PreviewIcon(){
    TCOSTheme() {
        NegativeIconButton(
            icon = painterResource(R.drawable.ic_delete),
            onClick = {}
        )
    }
}

@Preview(name = "Normal Disable")
@Composable
private fun PreviewNormalDisable(){
    TCOSTheme {
        NegativeButton(
            caption = "Delete",
            onClick = {},
            isEnabled = false
        )
    }
}

@Preview(name = "NormalWith Icon Disable")
@Composable
private fun PreviewNormalWithIconDisable(){
    TCOSTheme {
        NegativeButton(
            caption = "Delete",
            icon = painterResource(R.drawable.ic_delete),
            onClick = {},
            isEnabled = false
        )
    }
}

@Preview(name = "Icon Disable")
@Composable
private fun PreviewIconDisable(){
    TCOSTheme {
        NegativeIconButton(
            icon = painterResource(R.drawable.ic_delete),
            onClick = {},
            isEnabled = false
        )
    }
}