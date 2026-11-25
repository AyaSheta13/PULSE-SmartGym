package com.ayasheta.pulsesmartgym.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
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
import com.ayasheta.pulsesmartgym.core.designsystem.component.selectable.CheckBox
import com.ayasheta.pulsesmartgym.core.designsystem.component.selectable.CheckBoxStatus
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthColors
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseAuthFormField
import com.ayasheta.pulsesmartgym.feature.auth.components.PulsePrimaryCta
import com.ayasheta.pulsesmartgym.feature.auth.components.PulseValidationBanner

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    isDebugEntryEnabled: Boolean,
    onBackClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigationTarget) {
        when (uiState.navigationTarget) {
            LoginNavigationTarget.ProfileSetup -> {
                viewModel.consumeNavigation()
                onNavigateToProfileSetup()
            }
            LoginNavigationTarget.MainHome -> {
                viewModel.consumeNavigation()
                onNavigateToMain()
            }
            null -> Unit
        }
    }

    LoginContent(
        uiState = uiState,
        isDebugEntryEnabled = isDebugEntryEnabled,
        onBackClick = onBackClick,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onToggleRememberMe = viewModel::toggleRememberMe,
        onForgotPasswordClick = onForgotPasswordClick,
        onSubmitClick = viewModel::submitLogin,
        onExternalProviderClick = viewModel::onExternalProviderClick,
        onTestErrorsClick = { viewModel.triggerValidationErrorSample(isDebugEntryEnabled) },
        onFillValidDemoClick = { viewModel.clearErrorsAndFillDebugCredentials(isDebugEntryEnabled) },
        onSignUpClick = onSignUpClick,
        modifier = modifier,
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    isDebugEntryEnabled: Boolean,
    onBackClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleRememberMe: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onExternalProviderClick: (String) -> Unit,
    onTestErrorsClick: () -> Unit,
    onFillValidDemoClick: () -> Unit,
    onSignUpClick: () -> Unit,
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
                title = "Log In",
                onBackClick = onBackClick,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (uiState.bannerTitle != null && uiState.bannerMessage != null) {
                PulseValidationBanner(
                    title = uiState.bannerTitle,
                    message = uiState.bannerMessage,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(
                        text = "Welcome Back",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                    BasicText(
                        text = "Sign in to access your saved routines and training history on this device.",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                }
            }

            PulseAuthFormField(
                label = "EMAIL ADDRESS",
                value = uiState.email,
                onValueChange = onEmailChange,
                placeholder = "alex.miller@example.com",
                errorMessage = uiState.emailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

            PulseAuthFormField(
                label = "PASSWORD",
                value = uiState.password,
                onValueChange = onPasswordChange,
                placeholder = "Enter your password",
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
                        onSubmitClick()
                    },
                ),
            )

            if (!uiState.isValidationErrorState) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(role = Role.Checkbox, onClick = onToggleRememberMe)
                            .padding(end = 4.dp),
                    ) {
                        CheckBox(
                            status = if (uiState.rememberMe) CheckBoxStatus.CHECKED else CheckBoxStatus.UN_CHECKED,
                            onClick = onToggleRememberMe,
                        )
                        BasicText(
                            text = "Remember me",
                            style = TextStyle(
                                fontFamily = PulseTheme.typography.contentSecondary.fontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = PulseTheme.color.shadePrimary,
                            ),
                        )
                    }

                    BasicText(
                        text = "Forgot password?",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.caption.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PulseTheme.color.primary,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(role = Role.Button, onClick = onForgotPasswordClick)
                            .padding(vertical = 2.dp),
                    )
                }
            }

            if (uiState.isValidationErrorState && isDebugEntryEnabled) {
                PulsePrimaryCta(
                    text = "Try Valid Demo Credentials",
                    onClick = onFillValidDemoClick,
                    height = 50.dp,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clickable(role = Role.Button, onClick = onForgotPasswordClick),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Forgot Password? Reset it here →",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.caption.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PulseTheme.color.primary,
                        ),
                    )
                }
            } else {
                PulsePrimaryCta(
                    text = "Log In",
                    onClick = onSubmitClick,
                    isLoading = uiState.isLoading,
                )

                // OR CONTINUE WITH separator (Figma 281:52)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PulseAuthColors.CardBorderLight),
                    )
                    BasicText(
                        text = "OR CONTINUE WITH",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.label.fontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PulseAuthColors.Placeholder,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PulseAuthColors.CardBorderLight),
                    )
                }

                // Google & Apple buttons (Figma 281:56)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExternalProviderButton(
                        label = "Google",
                        onClick = { onExternalProviderClick("Google") },
                        modifier = Modifier.weight(1f),
                    )
                    ExternalProviderButton(
                        label = "Apple",
                        onClick = { onExternalProviderClick("Apple") },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (isDebugEntryEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PulseAuthColors.ErrorBannerBg)
                            .border(
                                width = 1.dp,
                                color = PulseTheme.color.primary,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable(role = Role.Button, onClick = onTestErrorsClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "⚠ Test Invalid Login & Error Feedback",
                            style = TextStyle(
                                fontFamily = PulseTheme.typography.supportingContent.fontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PulseAuthColors.DebugButtonText,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onSignUpClick),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = "Don't have an account?",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = PulseTheme.color.shadeSecondary,
                        ),
                    )
                    BasicText(
                        text = "Sign Up",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.cardTitle.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.color.primary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExternalProviderButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(PulseTheme.color.container)
            .border(width = 1.dp, color = PulseAuthColors.CardBorderLight, shape = shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontFamily = PulseTheme.typography.ctaPrimary.fontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PulseTheme.color.shadePrimary,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun LoginPreview() {
    PulseTheme {
        LoginContent(
            uiState = LoginUiState(email = "alex.miller@example.com", password = "••••••••••••"),
            isDebugEntryEnabled = true,
            onBackClick = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleRememberMe = {},
            onForgotPasswordClick = {},
            onSubmitClick = {},
            onExternalProviderClick = {},
            onTestErrorsClick = {},
            onFillValidDemoClick = {},
            onSignUpClick = {},
        )
    }
}
