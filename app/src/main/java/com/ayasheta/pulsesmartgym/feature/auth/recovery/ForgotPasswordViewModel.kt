package com.ayasheta.pulsesmartgym.feature.auth.recovery

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

data class ForgotPasswordUiState(
    val newPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val passwordError: String? = null,
    val feedbackMessage: String? = null,
    val isFeedbackError: Boolean = false,
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(newPassword = value, passwordError = null, feedbackMessage = null)
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submitReset() {
        val current = _uiState.value
        if (current.isLoading) return

        if (!ProfileAndAuthValidators.isStrongPassword(current.newPassword)) {
            _uiState.update {
                it.copy(
                    passwordError = "✕ Weak password: Minimum 8 characters required with symbol & number.",
                    feedbackMessage = null,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, passwordError = null, feedbackMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.resetPassword(current.newPassword)) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            newPassword = "",
                            feedbackMessage = "Password updated for your active local account.",
                            isFeedbackError = false,
                        )
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feedbackMessage = result.error.userMessage(),
                            isFeedbackError = true,
                        )
                    }
                }
            }
        }
    }
}
