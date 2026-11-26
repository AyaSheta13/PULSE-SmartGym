package com.ayasheta.pulsesmartgym.feature.auth.signup

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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onLogInClick: () -> Unit,
    onNavigateToPersonalInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToPersonalInfo) {
        if (uiState.navigateToPersonalInfo) {
            viewModel.consumeNavigation()
            onNavigateToPersonalInfo()
        }
    }

    SignUpContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onFullNameChange = viewModel::onFullNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
        onSubmitClick = viewModel::submitSignUp,
        onLogInClick = onLogInClick,
        modifier = modifier,
    )
}

@Composable
fun SignUpContent(
    uiState: SignUpUiState,
    onBackClick: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmitClick: () -> Unit,
    onLogInClick: () -> Unit,
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
                title = "Create Account",
                onBackClick = onBackClick,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (uiState.hasErrors) 12.dp else 16.dp),
        ) {
            if (uiState.bannerTitle != null && uiState.bannerMessage != null) {
                PulseValidationBanner(
                    title = uiState.bannerTitle,
                    message = uiState.bannerMessage,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(
                        text = "Start Your Training",
                        style = TextStyle(
                            fontFamily = PulseTheme.typography.numericMetric.fontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseTheme.color.shadePrimary,
                        ),
                    )
                    BasicText(
                        text = "Set up your account to track workouts & volume.",
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
                label = "FULL NAME",
                value = uiState.fullName,
                onValueChange = onFullNameChange,
                placeholder = "Alex Miller",
                errorMessage = uiState.fullNameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

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
                placeholder = "Min. 8 chars with number & symbol",
                errorMessage = uiState.passwordError,
                isPassword = true,
                isPasswordVisible = uiState.isPasswordVisible,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

            PulseAuthFormField(
                label = "CONFIRM PASSWORD",
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                placeholder = "Re-enter password",
                errorMessage = uiState.confirmPasswordError,
                isPassword = true,
                isPasswordVisible = uiState.isConfirmPasswordVisible,
                onTogglePasswordVisibility = onToggleConfirmPasswordVisibility,
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

            PulsePrimaryCta(
                text = "Create Account",
                onClick = onSubmitClick,
                isLoading = uiState.isLoading,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onLogInClick),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "Already have an account?",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.contentPrimary.fontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = PulseTheme.color.shadeSecondary,
                    ),
                )
                BasicText(
                    text = "Log In",
                    style = TextStyle(
                        fontFamily = PulseTheme.typography.cardTitle.fontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PulseTheme.color.primary,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun SignUpPreview() {
    PulseTheme {
        SignUpContent(
            uiState = SignUpUiState(),
            onBackClick = {},
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSubmitClick = {},
            onLogInClick = {},
        )
    }
}
