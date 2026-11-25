package com.ayasheta.pulsesmartgym.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentPage: Int = 0,
    val isCompleting: Boolean = false,
    val completed: Boolean = false,
)

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextPage() {
        val state = _uiState.value
        if (state.isCompleting || state.completed) return
        if (state.currentPage < LAST_PAGE_INDEX) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        } else {
            completeOnboarding()
        }
    }

    fun previousPage(): Boolean {
        val state = _uiState.value
        if (state.currentPage > 0) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1) }
            return true
        }
        return false
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    private fun completeOnboarding() {
        val state = _uiState.value
        if (state.isCompleting || state.completed) return
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            _uiState.update { it.copy(isCompleting = false, completed = true) }
        }
    }

    companion object {
        const val LAST_PAGE_INDEX = 2
    }
}
