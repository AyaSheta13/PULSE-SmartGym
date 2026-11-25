package com.ayasheta.pulsesmartgym.feature.auth.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthEntryNavigationTarget {
    data object ProfileSetup : AuthEntryNavigationTarget
    data object MainHome : AuthEntryNavigationTarget
}

data class AuthEntryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigationTarget: AuthEntryNavigationTarget? = null,
)

class AuthEntryViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthEntryUiState())
    val uiState: StateFlow<AuthEntryUiState> = _uiState.asStateFlow()

    fun continueAsGuest() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.continueAsGuest()) {
                is DomainResult.Success -> {
                    val setupDone = userProfileRepository.isProfileSetupCompleted(result.data.id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationTarget = if (setupDone) {
                                AuthEntryNavigationTarget.MainHome
                            } else {
                                AuthEntryNavigationTarget.ProfileSetup
                            },
                        )
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.userMessage(),
                        )
                    }
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigationTarget = null) }
    }
}
