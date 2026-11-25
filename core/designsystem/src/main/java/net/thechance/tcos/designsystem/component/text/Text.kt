package net.thechance.tcos.designsystem.component.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import net.thechance.tcos.designsystem.theme.Theme

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Theme.typography.contentPrimary,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: Color = Theme.color.shadeSecondary,
    autoSize: TextAutoSize? = null
){
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = color
        ),
        onTextLayout = onTextLayout,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        minLines = minLines,
        autoSize = autoSize
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadePrimary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.sectionTitle,
        color = color
    )
}

@Composable
fun CardTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadePrimary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.cardTitle,
        color = color
    )
}

@Composable
fun SubTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadeSecondary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.subtitle,
        color = color
    )
}

@Composable
fun TextContentPrimary(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadeSecondary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.contentPrimary,
        color = color
    )
}

@Composable
fun TextContentSecondary(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadeSecondary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.contentSecondary,
        color = color
    )
}

@Composable
fun TextSupportContent(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadeSecondary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.supportingContent,
        color = color
    )
}

@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadePrimary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.caption,
        color = color
    )
}

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadePrimary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.label,
        color = color
    )
}

@Composable
fun Hint(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Theme.color.shadeTertiary
){
    Text(
        text = text,
        modifier = modifier,
        style = Theme.typography.hint,
        color = color
    )
}