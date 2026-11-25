package com.ayasheta.pulsesmartgym.feature.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.ProfileAndAuthValidators
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val bannerTitle: String? = null,
    val bannerMessage: String? = null,
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val navigateToPersonalInfo: Boolean = false,
) {
    val hasErrors: Boolean
        get() = bannerTitle != null ||
            fullNameError != null ||
            emailError != null ||
            passwordError != null ||
            confirmPasswordError != null
}

class SignUpViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update {
            it.copy(fullName = value, fullNameError = null, bannerTitle = null, bannerMessage = null)
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = null, bannerTitle = null, bannerMessage = null)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = null, bannerTitle = null, bannerMessage = null)
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, bannerTitle = null, bannerMessage = null)
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun submitSignUp() {
        val current = _uiState.value
        if (current.isLoading) return

        val fullNameError = if (current.fullName.trim().isEmpty()) {
            "✕ Missing required field: Name cannot be blank."
        } else {
            null
        }
        val emailError = if (!ProfileAndAuthValidators.isValidEmail(current.email)) {
            "✕ Invalid email format: Missing domain extension."
        } else {
            null
        }
        val passwordError = if (!ProfileAndAuthValidators.isStrongPassword(current.password)) {
            "✕ Weak password: Minimum 8 characters required with symbol & number."
        } else {
            null
        }
        val confirmPasswordError = if (current.confirmPassword != current.password || current.confirmPassword.isEmpty()) {
            "✕ Confirmation mismatch: Passwords do not match."
        } else {
            null
        }

        val errorCount = listOfNotNull(fullNameError, emailError, passwordError, confirmPasswordError).size
        if (errorCount > 0) {
            _uiState.update {
                it.copy(
                    bannerTitle = "Registration Incomplete ($errorCount ${if (errorCount == 1) "Error" else "Errors"})",
                    bannerMessage = "Please correct the highlighted fields below to create your account.",
                    fullNameError = fullNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                bannerTitle = null,
                bannerMessage = null,
                fullNameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
            )
        }

        viewModelScope.launch {
            when (
                val result = authRepository.registerLocalAccount(
                    fullName = current.fullName,
                    email = current.email,
                    password = current.password,
                )
            ) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, navigateToPersonalInfo = true) }
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            bannerTitle = "Registration Failed",
                            bannerMessage = result.error.userMessage(),
                        )
                    }
                }
            }
        }
    }

    fun triggerValidationErrorsSample(isDebugEnabled: Boolean) {
        if (!isDebugEnabled) return
        _uiState.update {
            it.copy(
                fullName = "",
                email = "invalid.email@",
                password = "12345",
                confirmPassword = "abcdef",
                bannerTitle = "Registration Incomplete (4 Errors)",
                bannerMessage = "Please correct the highlighted fields below to create your account.",
                fullNameError = "✕ Missing required field: Name cannot be blank.",
                emailError = "✕ Invalid email format: Missing domain extension.",
                passwordError = "✕ Weak password: Minimum 8 characters required with symbol & number.",
                confirmPasswordError = "✕ Confirmation mismatch: Passwords do not match.",
            )
        }
    }

    fun clearErrorsAndFillDemoRegistration(isDebugEnabled: Boolean) {
        if (!isDebugEnabled) return
        _uiState.update {
            it.copy(
                fullName = "Alex Miller",
                email = "alex.miller@example.com",
                password = "Pulse@2026!",
                confirmPassword = "Pulse@2026!",
                bannerTitle = null,
                bannerMessage = null,
                fullNameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
            )
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigateToPersonalInfo = false) }
    }
}
