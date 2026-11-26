package com.ayasheta.pulsesmartgym.feature.auth.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.ProfileAndAuthValidators
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoginNavigationTarget {
    data object ProfileSetup : LoginNavigationTarget
    data object MainHome : LoginNavigationTarget
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val bannerTitle: String? = null,
    val bannerMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val navigationTarget: LoginNavigationTarget? = null,
) {
    val hasErrors: Boolean
        get() = bannerTitle != null || emailError != null || passwordError != null
}

class LoginViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            email = savedStateHandle.get<String>(KEY_EMAIL).orEmpty(),
            rememberMe = savedStateHandle.get<Boolean>(KEY_REMEMBER_ME) ?: true,
        ),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                bannerTitle = null,
                bannerMessage = null,
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                bannerTitle = null,
                bannerMessage = null,
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRememberMe() {
        val next = !_uiState.value.rememberMe
        savedStateHandle[KEY_REMEMBER_ME] = next
        _uiState.update { it.copy(rememberMe = next) }
    }

    fun submitLogin() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailError = if (!ProfileAndAuthValidators.isValidEmail(current.email)) {
            "✕ Invalid email. Format must be name@example.com, not empty."
        } else {
            null
        }
        val passwordError = if (current.password.length < 8) {
            "✕ Password cannot be empty (minimum 8 characters required)."
        } else {
            null
        }

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    bannerTitle = "Validation Failed",
                    bannerMessage = "Please check the highlighted email and password fields.",
                    emailError = emailError,
                    passwordError = passwordError,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                bannerTitle = null,
                bannerMessage = null,
                emailError = null,
                passwordError = null,
            )
        }

        viewModelScope.launch {
            when (
                val result = authRepository.signInWithEmail(
                    email = current.email,
                    password = current.password,
                    rememberMe = current.rememberMe,
                )
            ) {
                is DomainResult.Success -> {
                    val setupDone = userProfileRepository.isProfileSetupCompleted(result.data.id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationTarget = if (setupDone) {
                                LoginNavigationTarget.MainHome
                            } else {
                                LoginNavigationTarget.ProfileSetup
                            },
                        )
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            bannerTitle = "Incorrect Email or Password",
                            bannerMessage = result.error.userMessage(),
                        )
                    }
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigationTarget = null) }
    }

    private companion object {
        const val KEY_EMAIL = "login_email"
        const val KEY_REMEMBER_ME = "login_remember_me"
    }
}
