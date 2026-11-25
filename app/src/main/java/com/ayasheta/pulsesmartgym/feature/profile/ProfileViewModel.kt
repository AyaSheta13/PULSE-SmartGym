package com.ayasheta.pulsesmartgym.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    private val activeSheet = MutableStateFlow<ProfileModalSheet?>(null)
    private var lastOwnerId: String? = null

    val uiState: StateFlow<ProfileUiState> = authRepository.observeAuthState()
        .onEach { authState ->
            val currentOwner = (authState as? AuthState.Authenticated)?.user?.id
            if (currentOwner != lastOwnerId) {
                activeSheet.value = null
                lastOwnerId = currentOwner
            }
        }
        .flatMapLatest { authState ->
            when (authState) {
                AuthState.Initializing -> flowOf(ProfileUiState.Loading)
                AuthState.Unauthenticated -> flowOf(ProfileUiState.SignedOut)
                is AuthState.Authenticated -> observeOwnerProfile(authState.user)
            }
        }
        .catch { throwable ->
            emit(ProfileUiState.Error(throwable.message ?: "Unable to load profile."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState.Loading,
        )

    private fun observeOwnerProfile(user: AuthUser): Flow<ProfileUiState> = combine(
        userProfileRepository.observeProfile(user.id),
        userPreferencesRepository.observePreferences(),
        activeSheet,
    ) { profile, preferences, sheet ->
        val resolvedName = profile?.fullName?.trim()?.takeIf { it.isNotEmpty() }
            ?: user.displayName.trim().takeIf { it.isNotEmpty() }
            ?: if (user.isGuest) "Guest" else "Athlete"

        val subtitleIdentity = when {
            user.isGuest -> "Offline Guest Profile"
            user.email.isNotBlank() -> user.email
            else -> "Local Offline Profile"
        }

        val createdAt = user.createdAtEpochMs
        val membershipLabel = when {
            user.isGuest -> "Guest Session • On-Device Only"
            createdAt != null -> {
                val localDate = Instant.ofEpochMilli(createdAt)
                    .atZone(zoneIdProvider())
                    .toLocalDate()
                "Member since ${MEMBER_DATE_FORMATTER.format(localDate)}"
            }
            else -> "Local Offline Account"
        }

        ProfileUiState.Loaded(
            ownerId = user.id,
            fullName = resolvedName,
            initials = computeInitials(resolvedName),
            subtitleIdentity = subtitleIdentity,
            membershipLabel = membershipLabel,
            isGuest = user.isGuest,
            ageYears = profile?.age,
            heightCm = profile?.heightCm,
            weightKg = profile?.weightKg,
            fitnessGoal = profile?.fitnessGoal,
            preferredWeightUnit = preferences.preferredWeightUnit,
            activeSheet = sheet,
        )
    }

    fun openSheet(sheet: ProfileModalSheet) {
        activeSheet.value = sheet
    }

    fun dismissSheet() {
        activeSheet.value = null
    }

    fun confirmMetricUnits() {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferredWeightUnit(WeightUnit.KG)
            activeSheet.value = null
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            activeSheet.value = null
            authRepository.signOut()
            onSignedOut()
        }
    }

    private companion object {
        val MEMBER_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

        fun computeInitials(name: String): String {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "P"
                parts.size == 1 -> parts[0].first().uppercaseChar().toString()
                else -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
            }
        }
    }
}
