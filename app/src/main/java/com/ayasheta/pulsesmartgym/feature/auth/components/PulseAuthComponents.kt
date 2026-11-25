package com.ayasheta.pulsesmartgym.feature.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.VisibilityOff
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.VisibilityOn

internal object PulseAuthColors {
    val ScreenSurface = Color(0xFFFCF9F8)
    val EntrySurface = Color(0xFFFCFCFC)
    val ErrorBorder = Color(0xFFFF4D4D)
    val ErrorBannerBg = Color(0xFFFFF0EB)
    val ErrorText = Color(0xFFD92626)
    val ErrorBodyText = Color(0xFF8C3333)
    val Placeholder = Color(0xFFA6A19C)
    val DebugButtonText = Color(0xFFD9331A)
    val CardBorderLight = Color(0xFFF1EEEA)
    val ProgressTrack = Color(0xFFEBE5E0)
    val PillBg = Color(0xFFF2F2F2)
    val PillText = Color(0xFF404040)
    val BadgeBg = Color(0xFFF0F0F0)
    val BadgeText = Color(0xFF4D4D4D)
    val EntryPrimaryCta = Color(0xFFFF5733)
    val EntrySecondaryBorder = Color(0xFFE0E0E0)
    val EntrySecondaryText = Color(0xFF1F1F1F)
    val EntryMutedText = Color(0xFF737373)
    val SelectedTintBg = Color(0x1FFF623E)
    val SelectedIconBg = Color(0x26FF623E)
}

@Composable
fun PulseAuthFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isError = errorMessage != null
    val shape = RoundedCornerShape(if (isError) 10.dp else 12.dp)
    val containerColor = if (isError) PulseTheme.color.container else PulseTheme.color.background
    val borderColor = when {
        isError -> PulseAuthColors.ErrorBorder
        isFocused -> PulseTheme.color.primary
        else -> PulseTheme.color.border
    }
    val borderWidth = if (isError || isFocused) 1.5.dp else 1.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.hasFocus },
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = PulseTheme.typography.label.fontFamily,
                fontSize = 11.sp,
                fontWeight = if (isError) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = if (isError) 0.sp else 0.6.sp,
                color = PulseTheme.color.shadeSecondary,
            ),
            modifier = Modifier.padding(bottom = 6.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(containerColor)
                .border(width = borderWidth, color = borderColor, shape = shape)
                .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = PulseTheme.typography.contentPrimary.copy(
                            color = PulseAuthColors.Placeholder,
                        ),
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = PulseTheme.typography.contentPrimary.copy(
                        color = PulseTheme.color.shadePrimary,
                    ),
                    cursorBrush = SolidColor(PulseTheme.color.primary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = if (isPassword && !isPasswordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_$label"),
                )
            }

            if (isPassword && onTogglePasswordVisibility != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Button,
                            onClick = onTogglePasswordVisibility,
                        )
                        .semantics {
                            contentDescription = if (isPasswordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = if (isPasswordVisible) VisibilityOn else VisibilityOff,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(PulseTheme.color.shadeSecondary),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (errorMessage != null) {
            BasicText(
                text = errorMessage,
                style = TextStyle(
                    fontFamily = PulseTheme.typography.supportingContent.fontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    color = PulseAuthColors.ErrorText,
                ),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun PulseValidationBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PulseAuthColors.ErrorBannerBg)
            .border(width = 1.dp, color = PulseAuthColors.ErrorBorder, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                fontFamily = PulseTheme.typography.cardTitle.fontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PulseAuthColors.ErrorText,
            ),
        )
        BasicText(
            text = message,
            style = TextStyle(
                fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                color = PulseAuthColors.ErrorBodyText,
            ),
        )
    }
}

@Composable
fun PulsePrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    fontSizeSp: Int = 15,
    containerColor: Color = PulseTheme.color.primary,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    withElevationShadow: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val bgColor = if (enabled) containerColor else PulseTheme.color.disable
    val contentColor = if (enabled) PulseTheme.color.onPrimary else PulseTheme.color.onDisable
    val shadowModifier = if (withElevationShadow && enabled) {
        Modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = containerColor.copy(alpha = 0.25f),
            spotColor = containerColor.copy(alpha = 0.25f),
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(shadowModifier)
            .clip(shape)
            .background(bgColor)
            .clickable(
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            PulseLoadingIndicator(
                size = 22.dp,
                strokeWidth = 2.5.dp,
                color = contentColor,
            )
        } else {
            BasicText(
                text = text,
                style = TextStyle(
                    fontFamily = PulseTheme.typography.ctaPrimary.fontFamily,
                    fontSize = fontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                ),
            )
        }
    }
}
