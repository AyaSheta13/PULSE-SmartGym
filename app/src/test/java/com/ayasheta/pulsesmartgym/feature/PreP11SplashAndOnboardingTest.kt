package com.ayasheta.pulsesmartgym.feature

import androidx.lifecycle.SavedStateHandle
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.EditProfileDraft
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.profile.UserProfile
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginViewModel
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpViewModel
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingViewModel
import com.ayasheta.pulsesmartgym.feature.splash.SplashDestination
import com.ayasheta.pulsesmartgym.feature.splash.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreP11SplashAndOnboardingTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun splashViewModel_holdsSplashForMinimum2200ms_andDoesNotRestartOnRepeatedCalls() = runTest(testDispatcher) {
        assertEquals(2_200L, SplashViewModel.DEFAULT_MIN_DISPLAY_DURATION_MS)
        val vm = SplashViewModel(
            preferencesRepository = fakePrefs(onboardingCompleted = true),
            authRepository = fakeAuth(),
            userProfileRepository = fakeProfiles(),
        )

        vm.resolveStartupDestination()
        runCurrent()
        assertTrue(vm.uiState.value.isResolving)
        assertNull(vm.uiState.value.destination)

        advanceTimeBy(1_800L)
        runCurrent()
        assertTrue(vm.uiState.value.isResolving)
        assertNull(vm.uiState.value.destination)

        advanceTimeBy(450L)
        runCurrent()
        assertFalse(vm.uiState.value.isResolving)
        assertEquals(SplashDestination.AuthEntry, vm.uiState.value.destination)

        // Calling resolveStartupDestination again must be a no-op
        vm.resolveStartupDestination()
        runCurrent()
        assertEquals(SplashDestination.AuthEntry, vm.uiState.value.destination)
    }

    @Test
    fun onboardingViewModel_selectPageAndDoubleTapSkipAreGuarded() = runTest(testDispatcher) {
        var completionCalls = 0
        val prefs = object : UserPreferencesRepository {
            private val flow = MutableStateFlow(UserPreferences())
            override fun observePreferences(): Flow<UserPreferences> = flow
            override suspend fun updatePreferredWeightUnit(unit: WeightUnit) {}
            override suspend fun setOnboardingCompleted(completed: Boolean) {
                completionCalls += 1
                flow.value = flow.value.copy(onboardingCompleted = completed)
            }
            override suspend fun updateDefaultRestSeconds(seconds: Int) {}
        }

        val vm = OnboardingViewModel(SavedStateHandle(), prefs)
        vm.selectPage(2)
        assertEquals(2, vm.uiState.value.currentPage)
        assertTrue(vm.previousPage())
        assertEquals(1, vm.uiState.value.currentPage)

        vm.skipOnboarding()
        vm.skipOnboarding()
        runCurrent()
        assertEquals(1, completionCalls)
        assertTrue(vm.uiState.value.completed)
    }

    @Test
    fun loginAndSignUpViewModels_preserveInputsOnInlineValidation_andClearOnEdit() = runTest(testDispatcher) {
        val loginVm = LoginViewModel(SavedStateHandle(), fakeAuth(), fakeProfiles())
        loginVm.onEmailChange("not-an-email")
        loginVm.onPasswordChange("short")
        loginVm.submitLogin()
        assertNotNull(loginVm.uiState.value.emailError)
        assertNotNull(loginVm.uiState.value.passwordError)
        assertEquals("not-an-email", loginVm.uiState.value.email)
        assertEquals("short", loginVm.uiState.value.password)

        loginVm.onEmailChange("alex@example.com")
        assertNull(loginVm.uiState.value.emailError)

        val signUpVm = SignUpViewModel(SavedStateHandle(), fakeAuth())
        signUpVm.onFullNameChange("")
        signUpVm.onEmailChange("bad")
        signUpVm.onPasswordChange("123")
        signUpVm.onConfirmPasswordChange("456")
        signUpVm.submitSignUp()
        assertNotNull(signUpVm.uiState.value.fullNameError)
        assertNotNull(signUpVm.uiState.value.emailError)
        assertNotNull(signUpVm.uiState.value.passwordError)
        assertNotNull(signUpVm.uiState.value.confirmPasswordError)
        assertEquals("bad", signUpVm.uiState.value.email)
    }

    private fun fakePrefs(onboardingCompleted: Boolean = false) = object : UserPreferencesRepository {
        private val flow = MutableStateFlow(UserPreferences(onboardingCompleted = onboardingCompleted))
        override fun observePreferences(): Flow<UserPreferences> = flow
        override suspend fun updatePreferredWeightUnit(unit: WeightUnit) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun updateDefaultRestSeconds(seconds: Int) {}
    }

    private fun fakeAuth() = object : AuthRepository {
        private val flow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        override fun observeAuthState(): Flow<AuthState> = flow
        override suspend fun restoreSession(): AuthState = AuthState.Unauthenticated
        override suspend fun continueAsGuest(): DomainResult<AuthUser> =
            DomainResult.Failure(DataError.ValidationError("not used"))
        override suspend fun registerLocalAccount(fullName: String, email: String, password: String): DomainResult<AuthUser> =
            DomainResult.Failure(DataError.ValidationError("not used"))
        override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean): DomainResult<AuthUser> =
            DomainResult.Failure(DataError.ValidationError("not used"))
        override suspend fun resetPassword(newPassword: String): DomainResult<Unit> = DomainResult.Success(Unit)
        override suspend fun signInDemo(displayName: String, email: String): DomainResult<AuthUser> =
            DomainResult.Failure(DataError.ValidationError("not used"))
        override suspend fun signOut() {}
    }

    private fun fakeProfiles() = object : UserProfileRepository {
        override fun observeProfile(ownerId: String): Flow<UserProfile?> = MutableStateFlow(null)
        override suspend fun getProfile(ownerId: String): UserProfile? = null
        override suspend fun saveCompletedProfile(
            ownerId: String,
            personalInfo: PersonalInfoDraft,
            goals: FitnessGoalsDraft,
        ): DomainResult<UserProfile> = DomainResult.Failure(DataError.ValidationError("not used"))
        override suspend fun updateExistingProfile(ownerId: String, draft: EditProfileDraft): DomainResult<UserProfile> =
            DomainResult.Failure(DataError.ValidationError("not used"))
        override suspend fun isProfileSetupCompleted(ownerId: String): Boolean = false
    }
}
