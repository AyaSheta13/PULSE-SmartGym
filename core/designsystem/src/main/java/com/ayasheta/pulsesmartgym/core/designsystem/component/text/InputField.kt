package com.ayasheta.pulsesmartgym.core.designsystem.component.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Profile
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.VisibilityOff
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.VisibilityOn

@Composable
fun TextField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = PulseTheme.typography.contentPrimary,
    textColor: Color = PulseTheme.color.shadePrimary,
    focusedBorderColor: Color = PulseTheme.color.primary,
    borderColor: Color = PulseTheme.color.border,
    title: String? = null,
    titleColor: Color = PulseTheme.color.shadeSecondary,
    titleTextStyle: TextStyle = PulseTheme.typography.label,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
    leadingIconColor: Color = PulseTheme.color.shadeSecondary,
    trailingIconColor: Color = PulseTheme.color.shadeSecondary,
    onClickLeadingIcon: (() -> Unit)? = null,
    onClickTrailingIcon: (() -> Unit)? = null,
    hint: String? = null,
    hintColor: Color = PulseTheme.color.shadeSecondary,
    hintTextStyle: TextStyle = PulseTheme.typography.contentPrimary,
    errorMessage: String? = null,
    isError: Boolean = errorMessage != null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val resolvedBorderColor = when {
        !enabled -> PulseTheme.color.disable
        isError -> PulseTheme.color.error
        isFocused -> focusedBorderColor
        else -> borderColor
    }
    val resolvedContainerColor = if (enabled) PulseTheme.color.background else PulseTheme.color.surfaceMuted
    val resolvedTextColor = if (enabled) textColor else PulseTheme.color.onDisable

    Column(
        modifier = modifier.onFocusChanged { isFocused = it.hasFocus },
    ) {
        title?.let {
            BasicText(
                text = it,
                style = titleTextStyle.copy(color = if (isError) PulseTheme.color.error else titleColor),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(PulseTheme.size.large)
                .clip(PulseTheme.corners.large)
                .background(resolvedContainerColor)
                .border(
                    width = 1.dp,
                    shape = PulseTheme.corners.large,
                    color = resolvedBorderColor,
                )
                .padding(horizontal = PulseTheme.space.large),
        ) {
            leadingIcon?.let {
                Image(
                    painter = it,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(leadingIconColor),
                    modifier = Modifier
                        .padding(end = PulseTheme.space.medium)
                        .size(PulseTheme.size.tiny)
                        .clickable(enabled = enabled && onClickLeadingIcon != null) {
                            onClickLeadingIcon?.invoke()
                        },
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty() && hint != null) {
                    BasicText(
                        text = hint,
                        style = hintTextStyle.copy(color = hintColor),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = textStyle.copy(color = resolvedTextColor),
                    cursorBrush = SolidColor(PulseTheme.color.primary),
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                    visualTransformation = visualTransformation,
                    onTextLayout = onTextLayout,
                    interactionSource = interactionSource,
                )
            }

            trailingIcon?.let {
                Image(
                    painter = it,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(trailingIconColor),
                    modifier = Modifier
                        .padding(start = PulseTheme.space.small)
                        .size(PulseTheme.size.tiny)
                        .clickable(enabled = enabled && onClickTrailingIcon != null) {
                            onClickTrailingIcon?.invoke()
                        },
                )
            }
        }

        if (errorMessage != null) {
            BasicText(
                text = errorMessage,
                style = PulseTheme.typography.supportingContent.copy(color = PulseTheme.color.error),
                modifier = Modifier.padding(top = PulseTheme.space.tiny),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextFieldPreview() {
    var email by remember { mutableStateOf("alex.miller@example.com") }
    var password by remember { mutableStateOf("secret123") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    PulseTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PulseTheme.color.container)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextField(
                text = email,
                onTextChange = { email = it },
                title = "EMAIL ADDRESS",
                hint = "example@pulse.fit",
                leadingIcon = rememberVectorPainter(Profile),
            )
            TextField(
                text = password,
                onTextChange = { password = it },
                title = "PASSWORD",
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = rememberVectorPainter(if (isPasswordVisible) VisibilityOff else VisibilityOn),
                onClickTrailingIcon = { isPasswordVisible = !isPasswordVisible },
            )
            TextField(
                text = "",
                onTextChange = {},
                title = "WORKOUT NAME",
                hint = "e.g., Upper Body Power",
                errorMessage = "Workout name cannot be empty",
            )
            TextField(
                text = "Disabled value",
                onTextChange = {},
                title = "READ ONLY / DISABLED",
                enabled = false,
            )
        }
    }
}
