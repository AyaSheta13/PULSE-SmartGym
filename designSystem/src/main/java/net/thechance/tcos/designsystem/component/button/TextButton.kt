package net.thechance.tcos.designsystem.component.button


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import net.thechance.tcos.designsystem.R
import net.thechance.tcos.designsystem.theme.TCOSTheme
import net.thechance.tcos.designsystem.theme.Theme

@Composable
fun TextButton(
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
        containerColor = Color.Transparent,
        disableContainerColor = Color.Transparent,
        contentColor = Theme.color.primary,
        disableContentColor = Theme.color.disable
    )
}

@Composable
fun TextIconButton(
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
        containerColor = Color.Transparent,
        disableContainerColor = Color.Transparent,
        contentColor = Theme.color.primary,
        disableContentColor = Theme.color.disable
    )
}



@Preview(name = "Normal")
@Composable
private fun PreviewNormal(){
    TCOSTheme {
        TextButton(
            caption = "Add",
            onClick = {}
        )
    }
}

@Preview(name = "NormalWith Icon")
@Composable
private fun PreviewNormalWithIcon(){
    TCOSTheme {
        TextButton(
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
        TextIconButton(
            icon = painterResource(R.drawable.ic_cart),
            onClick = {}
        )
    }
}

@Preview(name = "Normal Disable")
@Composable
private fun PreviewNormalDisable(){
    TCOSTheme {
        TextButton(
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
        TextButton(
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
        TextIconButton(
            icon = painterResource(R.drawable.ic_cart),
            onClick = {},
            isEnabled = false
        )
    }
}