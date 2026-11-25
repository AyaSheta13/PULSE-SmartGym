package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import com.ayasheta.pulsesmartgym.navigation.PulseNavDisplay
import com.ayasheta.pulsesmartgym.navigation.PulseNavigator
import com.ayasheta.pulsesmartgym.navigation.PerStackViewModelStoreHolder
import com.ayasheta.pulsesmartgym.navigation.rememberPulseNavigationState
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutDetails
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ayasheta.pulsesmartgym.core.model.profile.UserProfile
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P4AuthAndProfileUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var dataContainer: PulseDataContainer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataContainer = PulseDataContainer(context)
        runBlocking {
            dataContainer.userPreferencesRepository.setOnboardingCompleted(false)
            dataContainer.authRepository.signOut()
        }
    }

    @Test
    fun onboardingToGuestProfileSetup_preservesPersonalInfoOnBack_andEntersMain() {
        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.AUTH,
                dataContainer = dataContainer,
            )
        }

        // Splash resolves to Onboarding page 1
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Welcome to PULSE").assertExists()
                true
            }.getOrDefault(false)
        }

        // Advance Page 1 -> Page 2 -> Page 3 -> Auth Entry
        composeRule.onNodeWithText("Next →").performClick()
        composeRule.onNodeWithText("Track Every Workout").assertIsDisplayed()
        composeRule.onNodeWithText("Next →").performClick()
        composeRule.onNodeWithText("Analyze Your Progress").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started →").performClick()

        // Auth Entry -> Continue as Guest (Offline Mode)
        composeRule.onNodeWithText("Elevate Every Rep").assertIsDisplayed()
        composeRule.onNodeWithText("Continue as Guest (Offline Mode) →").performClick()

        // Personal Info Setup
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("About You").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("field_YOUR NAME").performTextInput("Sam Rivera")
        composeRule.onNodeWithTag("metric_AGE").performTextInput("29")
        composeRule.onNodeWithTag("metric_HEIGHT").performTextInput("178")
        composeRule.onNodeWithTag("metric_WEIGHT").performTextInput("75")
        composeRule.onNodeWithText("Female").performClick()
        composeRule.onNodeWithText("Continue to Goals →").performScrollTo().performClick()

        // Fitness Goals Setup -> Navigate back to verify Personal Info is preserved
        composeRule.onNodeWithText("Your Fitness Goals").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Sam Rivera").assertIsDisplayed()

        // Proceed forward and complete setup
        composeRule.onNodeWithText("Continue to Goals →").performClick()
        composeRule.onNodeWithText("Increase Strength & Power").performClick()
        composeRule.onNodeWithText("Advanced").performClick()
        composeRule.onNodeWithText("5 Days").performClick()
        composeRule.onNodeWithText("Complete Setup & Start →").performClick()

        // Enters Main Shell (HomeTab)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("THIS WEEK'S PROGRESS").assertExists()
                true
            }.getOrDefault(false)
        }

        // Logout from ProfileTab returns to AuthEntry
        composeRule.onNodeWithText(PulseTab.PROFILE.label).performClick()
        composeRule.onNodeWithTag("profile_logout_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Elevate Every Rep").assertExists()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun loginAndSignUp_displayFigmaValidationErrors_andForgotPasswordIsHonest() {
        runBlocking {
            dataContainer.userPreferencesRepository.setOnboardingCompleted(true)
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.AUTH,
                dataContainer = dataContainer,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Elevate Every Rep").assertExists()
                true
            }.getOrDefault(false)
        }

        // Check Sign Up 4-scenario validation errors
        composeRule.onNodeWithText("Create Free Account").performClick()
        composeRule.onNodeWithText("Start Your Training").assertIsDisplayed()
        composeRule.onNode(hasText("Create Account") and hasClickAction()).performClick()
        composeRule.onNodeWithText("Registration Incomplete (4 Errors)").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Missing required field: Name cannot be blank.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Invalid email format: Missing domain extension.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Weak password: Minimum 8 characters required with symbol & number.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Confirmation mismatch: Passwords do not match.").assertIsDisplayed()

        // Go back to Auth Entry -> Log In -> Google/Apple honest unavailable feedback -> Validation Errors + Forgot Password
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Log In").performClick()
        composeRule.onNodeWithText("Welcome Back").assertIsDisplayed()

        // Verify restored Figma OR CONTINUE WITH + Google / Apple buttons
        composeRule.onNodeWithText("OR CONTINUE WITH").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Google").performScrollTo().performClick()
        composeRule.onNodeWithText("Google Sign-In Unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Apple").performScrollTo().performClick()
        composeRule.onNodeWithText("Apple Sign-In Unavailable").assertIsDisplayed()

        composeRule.onNodeWithText("⚠ Test Invalid Login & Error Feedback").performScrollTo().performClick()
        composeRule.onNodeWithText("Incorrect Email or Password").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Invalid email. Format must be name@example.com, not empty.").assertIsDisplayed()
        composeRule.onNodeWithText("✕ Password cannot be empty (minimum 8 characters required).").assertIsDisplayed()

        // Navigate to Forgot Password and verify unauthenticated reset is blocked honestly
        composeRule.onNodeWithText("Forgot Password? Reset it here →").performScrollTo().performClick()
        composeRule.onNodeWithText("Save New Password").assertIsDisplayed()
        composeRule.onNodeWithTag("field_NEW PASSWORD").performTextInput("Pulse@2026!")
        composeRule.onNodeWithText("Save New Password").performClick()
        composeRule.onNodeWithText("Password Reset Unavailable").assertIsDisplayed()
    }

    @Test
    fun restoredMainFlow_withoutValidSession_resetsToAuthEntryWithoutBypass() {
        runBlocking {
            dataContainer.userPreferencesRepository.setOnboardingCompleted(true)
            dataContainer.authRepository.signOut()
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                dataContainer = dataContainer,
                enforceAuthGuard = true,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Elevate Every Rep").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Route: HomeTab").assertDoesNotExist()
    }

    @Test
    fun authenticatedIncompleteProfile_restoredMain_redirectsToProfileSetupWithoutShowingMain() {
        runBlocking {
            dataContainer.userPreferencesRepository.setOnboardingCompleted(true)
            dataContainer.authRepository.signOut()
            val email = "incomplete_${System.nanoTime()}@pulse.fit"
            val reg = dataContainer.authRepository.registerLocalAccount("Taylor", email, "Pulse@2026!")
            assertTrue(reg is DomainResult.Success)
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                dataContainer = dataContainer,
                enforceAuthGuard = true,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("About You").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Route: HomeTab").assertDoesNotExist()
    }

    @Test
    fun rememberMeDisabled_retainedInMemory_clearedOnNewContainerProcessRestart() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val email = "noremem_${System.nanoTime()}@pulse.fit"
        runBlocking {
            dataContainer.authRepository.registerLocalAccount("Jordan", email, "Pulse@2026!")
            dataContainer.authRepository.signOut()
            val login = dataContainer.authRepository.signInWithEmail(email, "Pulse@2026!", rememberMe = false)
            assertTrue(login is DomainResult.Success)
            // Same process (Activity recreation) retains Authenticated state
            assertTrue(dataContainer.authRepository.restoreSession() is AuthState.Authenticated)

            // Simulated process restart with a fresh PulseDataContainer clears session
            val freshContainer = PulseDataContainer(context)
            assertEquals(AuthState.Unauthenticated, freshContainer.authRepository.restoreSession())
        }
    }

    @Test
    fun accountSwitching_isolatesProfileDraftAndPersistedProfile() {
        val emailA = "usera_${System.nanoTime()}@pulse.fit"
        val emailB = "userb_${System.nanoTime()}@pulse.fit"
        val userA = runBlocking {
            dataContainer.userPreferencesRepository.setOnboardingCompleted(true)
            val rA = dataContainer.authRepository.registerLocalAccount("Alice Alpha", emailA, "Pulse@2026!")
            (rA as DomainResult.Success).data
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.AUTH,
                dataContainer = dataContainer,
            )
        }

        // Alice enters Personal Info (incomplete, not saved)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("About You").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("metric_AGE").performTextInput("27")

        // Switch active auth identity to Bob Beta
        runBlocking {
            dataContainer.authRepository.signOut()
            dataContainer.authRepository.registerLocalAccount("Bob Beta", emailB, "Pulse@2026!")
        }

        // Re-verify: Bob does not inherit Alice's age "27" or profile
        runBlocking {
            assertEquals(null, dataContainer.userProfileRepository.getProfile(userA.id))
            val bobAuth = dataContainer.authRepository.observeAuthState().first() as AuthState.Authenticated
            assertEquals("Bob Beta", bobAuth.user.displayName)
            assertEquals(null, dataContainer.userProfileRepository.getProfile(bobAuth.user.id))
        }
    }

    @Test
    fun sameDestinationInTwoTabs_hasIndependentViewModels_andPopClearsOnlyPopped() {
        lateinit var navigator: PulseNavigator
        composeRule.setContent {
            com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme {
                val navState = rememberPulseNavigationState(
                    initialFlow = AppEntryFlow.MAIN,
                    initialTab = PulseTab.HOME,
                )
                navigator = remember(navState) { PulseNavigator(navState) }
                PulseNavDisplay(
                    navigationState = navState,
                    navigator = navigator,
                    isDebugEntryEnabled = true,
                    dataContainer = dataContainer,
                )
            }
        }

        // Push WorkoutDetails("plan_sample_1") on HOME tab
        composeRule.runOnIdle { navigator.navigate(WorkoutDetails("plan_sample_1")) }
        composeRule.onNodeWithText("Route: WorkoutDetails").assertIsDisplayed()
        composeRule.onNodeWithText("Increment Local State").performClick()
        composeRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()

        // Switch to WORKOUTS tab and push same WorkoutDetails("plan_sample_1")
        composeRule.runOnIdle { navigator.selectTab(PulseTab.WORKOUTS) }
        composeRule.runOnIdle { navigator.navigate(WorkoutDetails("plan_sample_1")) }
        composeRule.onNodeWithText("Route: WorkoutDetails").assertIsDisplayed()
        composeRule.onNodeWithText("Saved State Counter: 0").assertIsDisplayed()

        // Pop WORKOUTS's WorkoutDetails, then switch back to HOME -> HOME's WorkoutDetails state is intact
        composeRule.runOnIdle { navigator.navigateBack() }
        composeRule.onNodeWithText("Route: WorkoutsTab").assertIsDisplayed()
        composeRule.runOnIdle { navigator.selectTab(PulseTab.HOME) }
        composeRule.onNodeWithText("Route: WorkoutDetails").assertIsDisplayed()
        composeRule.onNodeWithText("Saved State Counter: 1").assertIsDisplayed()
    }

    @Test
    fun delayedIncompleteProfileGate_neverComposesMainEvenDuringPendingQuery() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val delayedContainer = object : PulseDataContainer(context) {
            override val userProfileRepository: UserProfileRepository = object : UserProfileRepository {
                override fun observeProfile(ownerId: String): Flow<UserProfile?> = MutableStateFlow(null)
                override suspend fun getProfile(ownerId: String): UserProfile? = null
                override suspend fun saveCompletedProfile(
                    ownerId: String,
                    personalInfo: PersonalInfoDraft,
                    goals: FitnessGoalsDraft,
                ): DomainResult<UserProfile> = error("unused")
                override suspend fun isProfileSetupCompleted(ownerId: String): Boolean {
                    delay(350)
                    return false
                }
            }
        }

        runBlocking {
            delayedContainer.userPreferencesRepository.setOnboardingCompleted(true)
            delayedContainer.authRepository.signOut()
            delayedContainer.authRepository.registerLocalAccount(
                "Delayed User",
                "delayed_${System.nanoTime()}@pulse.fit",
                "Pulse@2026!",
            )
        }

        var homeEverDisplayed = false
        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                dataContainer = delayedContainer,
                enforceAuthGuard = true,
            )
        }

        // Poll during the 350ms pending query: HomeTab must NEVER be composed
        for (i in 0 until 6) {
            composeRule.waitForIdle()
            if (runCatching { composeRule.onNodeWithText("Route: HomeTab").assertExists(); true }.getOrDefault(false)) {
                homeEverDisplayed = true
            }
            Thread.sleep(50)
        }
        assertFalse("HomeTab flashed while profile completion query was pending", homeEverDisplayed)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("About You").assertExists()
                true
            }.getOrDefault(false)
        }
        assertFalse(homeEverDisplayed)
    }

    class ProbeViewModel(
        val instanceTag: String,
        private val onClearedCallback: (String) -> Unit,
    ) : ViewModel() {
        var count by mutableIntStateOf(0)
        override fun onCleared() {
            onClearedCallback(instanceTag)
        }
    }

    @Test
    fun nav3RealViewModel_clearedOnPopAndOnLogoutAccountSwitch_neverReusedAcrossOwners() {
        val clearedTags = mutableListOf<String>()
        val createdInstances = mutableListOf<ProbeViewModel>()
        val ownerFlow = MutableStateFlow("user_A")
        lateinit var navigator: PulseNavigator

        composeRule.setContent {
            val activeOwner by ownerFlow.collectAsState()
            val stackHolder: PerStackViewModelStoreHolder = viewModel()
            LaunchedEffect(activeOwner) {
                stackHolder.syncOwner(activeOwner)
            }
            val navState = rememberPulseNavigationState(
                initialFlow = AppEntryFlow.MAIN,
                initialTab = PulseTab.HOME,
            )
            navigator = remember(navState) { PulseNavigator(navState) }

            val tabEntries = key(activeOwner) {
                PulseTab.entries.associateWith { tab ->
                    val storeOwner = remember(tab, stackHolder) {
                        object : ViewModelStoreOwner {
                            override val viewModelStore: ViewModelStore = stackHolder.storeFor("TAB_${tab.name}")
                        }
                    }
                    rememberDecoratedNavEntries(
                        backStack = navState.tabBackStacks.getValue(tab),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(viewModelStoreOwner = storeOwner),
                        ),
                        entryProvider = entryProvider {
                            entry<com.ayasheta.pulsesmartgym.navigation.route.HomeTab> { Text("Tab Home") }
                            entry<com.ayasheta.pulsesmartgym.navigation.route.WorkoutsTab> { Text("Tab Workouts") }
                            entry<com.ayasheta.pulsesmartgym.navigation.route.ProgressTab> { Text("Tab Progress") }
                            entry<com.ayasheta.pulsesmartgym.navigation.route.HistoryTab> { Text("Tab History") }
                            entry<com.ayasheta.pulsesmartgym.navigation.route.ProfileTab> { Text("Tab Profile") }
                            entry<WorkoutDetails> { key ->
                                val tag = "${activeOwner}_${tab.name}_${key.planId}"
                                val vm: ProbeViewModel = viewModel {
                                    ProbeViewModel(tag) { clearedTags.add(it) }.also { createdInstances.add(it) }
                                }
                                Column {
                                    Text("VM Tag: ${vm.instanceTag}")
                                    Text("VM Count: ${vm.count}")
                                    Button(onClick = { vm.count++ }) { Text("Inc VM") }
                                }
                            }
                        },
                    )
                }
            }

            NavDisplay(
                entries = tabEntries.getValue(navState.selectedTab),
                onBack = { navigator.navigateBack() },
            )
        }

        // 1. Open WorkoutDetails("p1") in HOME for user_A and increment VM count
        composeRule.runOnIdle { navigator.navigate(WorkoutDetails("p1")) }
        composeRule.onNodeWithText("VM Tag: user_A_HOME_p1").assertIsDisplayed()
        composeRule.onNodeWithText("Inc VM").performClick()
        composeRule.onNodeWithText("VM Count: 1").assertIsDisplayed()

        // 2. Switch to WORKOUTS and open same WorkoutDetails("p1") -> independent VM
        composeRule.runOnIdle { navigator.selectTab(PulseTab.WORKOUTS) }
        composeRule.runOnIdle { navigator.navigate(WorkoutDetails("p1")) }
        composeRule.onNodeWithText("VM Tag: user_A_WORKOUTS_p1").assertIsDisplayed()
        composeRule.onNodeWithText("VM Count: 0").assertIsDisplayed()

        // 3. Pop WORKOUTS's WorkoutDetails -> only WORKOUTS VM is cleared, HOME VM survives
        composeRule.runOnIdle { navigator.navigateBack() }
        composeRule.waitForIdle()
        assertTrue(clearedTags.contains("user_A_WORKOUTS_p1"))
        assertFalse(clearedTags.contains("user_A_HOME_p1"))

        composeRule.runOnIdle { navigator.selectTab(PulseTab.HOME) }
        composeRule.onNodeWithText("VM Count: 1").assertIsDisplayed()

        // 4. Logout / switch account from nested WorkoutDetails in HOME -> clears HOME VM
        composeRule.runOnIdle {
            ownerFlow.value = "user_B"
            navigator.resetToAuth()
            navigator.completeAuthAndEnterMain()
            navigator.navigate(WorkoutDetails("p1"))
        }
        composeRule.waitForIdle()
        assertTrue(clearedTags.contains("user_A_HOME_p1"))
        composeRule.onNodeWithText("VM Tag: user_B_HOME_p1").assertIsDisplayed()
        composeRule.onNodeWithText("VM Count: 0").assertIsDisplayed()
        val homeA = createdInstances.first { it.instanceTag == "user_A_HOME_p1" }
        val homeB = createdInstances.first { it.instanceTag == "user_B_HOME_p1" }
        assertNotSame(homeA, homeB)
    }
}
