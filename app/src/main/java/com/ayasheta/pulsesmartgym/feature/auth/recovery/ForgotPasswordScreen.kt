package com.ayasheta.pulsesmartgym.feature.auth.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthColors
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthFormField
import com.ayasheta.pulsesmartgym.feature.auth.components.PulsePrimaryCta
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseValidationBanner

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    ForgotPasswordContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onSaveClick = viewModel::submitReset,
        modifier = modifier,
    )
}

@Composable
fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    onBackClick: () -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PulseAuthColors.ScreenSurface),
    ) {
        PulseTopAppBar(
            style = PulseTopAppBarStyle.BackWithTitle(
                title = "Reset Password",
                onBackClick = onBackClick,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    text = "Reset Password",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.color.shadePrimary,
                    ),
                )
                BasicText(
                    text = "Set a new password for this device. Remote email recovery is not supported in this offline prototype.",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
            }

            if (uiState.feedbackMessage != null) {
                if (uiState.isFeedbackError) {
                    PulseValidationBanner(
                        title = "Password Reset Unavailable",
                        message = uiState.feedbackMessage,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PulseTheme.color.successVariant)
                            .border(
                                width = 1.dp,
                                color = PulseTheme.color.successBorder,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        BasicText(
                            text = uiState.feedbackMessage,
                            style = PulseTheme.typography.supportingContent.copy(
                                color = PulseTheme.color.successContent,
                            ),
                        )
                    }
                }
            }

            PulseAuthFormField(
                label = "NEW PASSWORD",
                value = uiState.newPassword,
                onValueChange = onNewPasswordChange,
                placeholder = "Min. 8 chars with number & symbol",
                errorMessage = uiState.passwordError,
                isPassword = true,
                isPasswordVisible = uiState.isPasswordVisible,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSaveClick()
                    },
                ),
            )

            PulsePrimaryCta(
                text = "Save New Password",
                onClick = onSaveClick,
                isLoading = uiState.isLoading,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "← Back to Log In",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.caption.fontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PulseTheme.color.primary,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun ForgotPasswordPreview() {
    PulseTheme {
        ForgotPasswordContent(
            uiState = ForgotPasswordUiState(),
            onBackClick = {},
            onNewPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSaveClick = {},
        )
    }
}
