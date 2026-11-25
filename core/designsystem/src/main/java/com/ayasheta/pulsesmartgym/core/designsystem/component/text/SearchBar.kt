package com.ayasheta.pulsesmartgym.core.designsystem.component.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Close
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Search

@Composable
fun PulseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search exercises...",
    onClear: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) PulseTheme.color.primary else PulseTheme.color.border

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PulseTheme.size.large)
            .clip(PulseTheme.corners.large)
            .background(PulseTheme.color.background)
            .border(width = 1.dp, color = borderColor, shape = PulseTheme.corners.large)
            .onFocusChanged { isFocused = it.hasFocus }
            .padding(horizontal = PulseTheme.space.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = Search,
            contentDescription = "Search",
            colorFilter = ColorFilter.tint(PulseTheme.color.placeholder),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(PulseTheme.space.medium))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                BasicText(
                    text = placeholder,
                    style = PulseTheme.typography.contentSecondary.copy(
                        color = PulseTheme.color.placeholder,
                    ),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                textStyle = PulseTheme.typography.contentSecondary.copy(
                    color = PulseTheme.color.shadePrimary,
                ),
                cursorBrush = SolidColor(PulseTheme.color.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(PulseTheme.space.small))
            Image(
                imageVector = Close,
                contentDescription = "Clear search",
                colorFilter = ColorFilter.tint(PulseTheme.color.shadeSecondary),
                modifier = Modifier
                    .size(18.dp)
                    .clip(PulseTheme.corners.fullRounded)
                    .clickable(enabled = enabled) {
                        if (onClear != null) onClear() else onQueryChange("")
                    },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulseSearchBarPreview() {
    var query by remember { mutableStateOf("") }
    PulseTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PulseTheme.color.container)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PulseSearchBar(query = query, onQueryChange = { query = it })
            PulseSearchBar(query = "Bench Press", onQueryChange = {})
        }
    }
}
