package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.feature.auth.entry.AuthEntryScreen
import com.ayasheta.pulsesmartgym.feature.auth.entry.AuthEntryViewModel
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginScreen
import com.ayasheta.pulsesmartgym.feature.auth.login.LoginViewModel
import com.ayasheta.pulsesmartgym.feature.auth.recovery.ForgotPasswordScreen
import com.ayasheta.pulsesmartgym.feature.auth.recovery.ForgotPasswordViewModel
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpScreen
import com.ayasheta.pulsesmartgym.feature.auth.signup.SignUpViewModel
import com.ayasheta.pulsesmartgym.feature.home.HomeScreen
import com.ayasheta.pulsesmartgym.feature.home.HomeViewModel
import com.ayasheta.pulsesmartgym.feature.navigationdemo.NavigationDemoAction
import com.ayasheta.pulsesmartgym.feature.navigationdemo.NavigationPlaceholderScreen
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingScreen
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingViewModel
import com.ayasheta.pulsesmartgym.feature.profile.ProfileScreen
import com.ayasheta.pulsesmartgym.feature.profile.ProfileViewModel
import com.ayasheta.pulsesmartgym.feature.profile.edit.EditProfileScreen
import com.ayasheta.pulsesmartgym.feature.profile.edit.EditProfileViewModel
import com.ayasheta.pulsesmartgym.feature.profilesetup.ProfileSetupDraftHolder
import com.ayasheta.pulsesmartgym.feature.profilesetup.goals.FitnessGoalsScreen
import com.ayasheta.pulsesmartgym.feature.profilesetup.goals.FitnessGoalsViewModel
import com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo.PersonalInfoScreen
import com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo.PersonalInfoViewModel
import com.ayasheta.pulsesmartgym.feature.splash.SplashDestination
import com.ayasheta.pulsesmartgym.feature.splash.SplashScreen
import com.ayasheta.pulsesmartgym.feature.splash.SplashViewModel
import kotlinx.coroutines.launch
import com.ayasheta.pulsesmartgym.navigation.route.AuthEntry
import com.ayasheta.pulsesmartgym.navigation.route.CreateWorkoutPlan
import com.ayasheta.pulsesmartgym.navigation.route.DeviceAssistedRepCounter
import com.ayasheta.pulsesmartgym.navigation.route.DeviceDiscovery
import com.ayasheta.pulsesmartgym.navigation.route.EditProfile
import com.ayasheta.pulsesmartgym.navigation.route.EditWorkoutPlan
import com.ayasheta.pulsesmartgym.navigation.route.ExercisePicker
import com.ayasheta.pulsesmartgym.navigation.route.ForgotPassword
import com.ayasheta.pulsesmartgym.navigation.route.HistoryTab
import com.ayasheta.pulsesmartgym.navigation.route.HomeTab
import com.ayasheta.pulsesmartgym.navigation.route.Login
import com.ayasheta.pulsesmartgym.navigation.route.ManualRepCounter
import com.ayasheta.pulsesmartgym.navigation.route.MyDevice
import com.ayasheta.pulsesmartgym.navigation.route.Onboarding
import com.ayasheta.pulsesmartgym.navigation.route.PartialWorkoutSummary
import com.ayasheta.pulsesmartgym.navigation.route.ProfileSetupFitnessGoals
import com.ayasheta.pulsesmartgym.navigation.route.ProfileSetupPersonalInfo
import com.ayasheta.pulsesmartgym.navigation.route.ProfileTab
import com.ayasheta.pulsesmartgym.navigation.route.ProgressTab
import com.ayasheta.pulsesmartgym.navigation.route.ReadyStartSet
import com.ayasheta.pulsesmartgym.navigation.route.SessionDetails
import com.ayasheta.pulsesmartgym.navigation.route.SignUp
import com.ayasheta.pulsesmartgym.navigation.route.Splash
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutComplete
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutDetails
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutSession
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutsTab

class ProfileSetupDraftFlowViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val draftHolder = ProfileSetupDraftHolder(savedStateHandle)
}

@Composable
fun rememberPulseEntryProvider(
    navigator: PulseNavigator,
    isDebugEntryEnabled: Boolean,
    dataContainer: PulseDataContainer,
    authFlowViewModelStoreOwner: ViewModelStoreOwner,
): (NavKey) -> NavEntry<NavKey> {
    val profileDraftViewModel: ProfileSetupDraftFlowViewModel = viewModel(
        viewModelStoreOwner = authFlowViewModelStoreOwner,
        key = "profile_setup_draft_flow",
    ) {
        ProfileSetupDraftFlowViewModel(createSavedStateHandle())
    }
    val profileDraftHolder = profileDraftViewModel.draftHolder
    val coroutineScope = rememberCoroutineScope()

    return entryProvider {
        // --- Auth & Onboarding (P4) ---
        entry<Splash> {
            val viewModel: SplashViewModel = viewModel {
                SplashViewModel(
                    preferencesRepository = dataContainer.userPreferencesRepository,
                    authRepository = dataContainer.authRepository,
                    userProfileRepository = dataContainer.userProfileRepository,
                )
            }
            SplashScreen(
                viewModel = viewModel,
                onResolved = { destination ->
                    when (destination) {
                        SplashDestination.Onboarding -> navigator.replaceAuthRoot(Onboarding)
                        SplashDestination.AuthEntry -> navigator.replaceAuthRoot(AuthEntry)
                        SplashDestination.ProfileSetup -> navigator.replaceAuthRoot(ProfileSetupPersonalInfo)
                        SplashDestination.MainHome -> navigator.completeAuthAndEnterMain()
                    }
                },
            )
        }

        entry<Onboarding> {
            val viewModel: OnboardingViewModel = viewModel {
                OnboardingViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    preferencesRepository = dataContainer.userPreferencesRepository,
                )
            }
            OnboardingScreen(
                viewModel = viewModel,
                onCompleted = { navigator.replaceAuthRoot(AuthEntry) },
                onExitAppBack = { navigator.navigateBack() },
            )
        }

        entry<AuthEntry> {
            val viewModel: AuthEntryViewModel = viewModel {
                AuthEntryViewModel(
                    authRepository = dataContainer.authRepository,
                    userProfileRepository = dataContainer.userProfileRepository,
                )
            }
            AuthEntryScreen(
                viewModel = viewModel,
                onCreateAccountClick = { navigator.navigate(SignUp) },
                onLogInClick = { navigator.navigate(Login) },
                onNavigateToProfileSetup = { navigator.navigate(ProfileSetupPersonalInfo) },
                onNavigateToMain = { navigator.completeAuthAndEnterMain() },
            )
        }

        entry<Login> {
            val viewModel: LoginViewModel = viewModel {
                LoginViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    authRepository = dataContainer.authRepository,
                    userProfileRepository = dataContainer.userProfileRepository,
                )
            }
            LoginScreen(
                viewModel = viewModel,
                isDebugEntryEnabled = isDebugEntryEnabled,
                onBackClick = { navigator.navigateBack() },
                onForgotPasswordClick = { navigator.navigate(ForgotPassword) },
                onSignUpClick = { navigator.navigate(SignUp) },
                onNavigateToProfileSetup = { navigator.navigate(ProfileSetupPersonalInfo) },
                onNavigateToMain = { navigator.completeAuthAndEnterMain() },
            )
        }

        entry<SignUp> {
            val viewModel: SignUpViewModel = viewModel {
                SignUpViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    authRepository = dataContainer.authRepository,
                )
            }
            SignUpScreen(
                viewModel = viewModel,
                isDebugEntryEnabled = isDebugEntryEnabled,
                onBackClick = { navigator.navigateBack() },
                onLogInClick = { navigator.navigate(Login) },
                onNavigateToPersonalInfo = { navigator.navigate(ProfileSetupPersonalInfo) },
            )
        }

        entry<ForgotPassword> {
            val viewModel: ForgotPasswordViewModel = viewModel {
                ForgotPasswordViewModel(
                    authRepository = dataContainer.authRepository,
                )
            }
            ForgotPasswordScreen(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
            )
        }

        entry<ProfileSetupPersonalInfo> {
            val viewModel: PersonalInfoViewModel = viewModel {
                PersonalInfoViewModel(
                    draftHolder = profileDraftHolder,
                    authRepository = dataContainer.authRepository,
                    userProfileRepository = dataContainer.userProfileRepository,
                )
            }
            PersonalInfoScreen(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onContinueToGoals = { navigator.navigate(ProfileSetupFitnessGoals) },
            )
        }

        entry<ProfileSetupFitnessGoals> {
            val viewModel: FitnessGoalsViewModel = viewModel {
                FitnessGoalsViewModel(
                    draftHolder = profileDraftHolder,
                    authRepository = dataContainer.authRepository,
                    userProfileRepository = dataContainer.userProfileRepository,
                )
            }
            FitnessGoalsScreen(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onSetupCompleted = { navigator.completeAuthAndEnterMain() },
            )
        }

    // --- Five Main Tab Roots ---
    entry<HomeTab> {
        val viewModel: HomeViewModel = viewModel {
            HomeViewModel(
                authRepository = dataContainer.authRepository,
                userProfileRepository = dataContainer.userProfileRepository,
                workoutPlanRepository = dataContainer.workoutPlanRepository,
                workoutSessionRepository = dataContainer.workoutSessionRepository,
                workoutHistoryRepository = dataContainer.workoutHistoryRepository,
            )
        }
        HomeScreen(
            viewModel = viewModel,
            onAvatarClick = { navigator.selectTab(PulseTab.PROFILE) },
            onDeviceStatusClick = { navigator.navigate(MyDevice) },
            onSeeAllPlansClick = { navigator.selectTab(PulseTab.WORKOUTS) },
            onPlanClick = { planId -> navigator.openWorkoutDetails(planId = planId) },
            onProgressClick = { navigator.selectTab(PulseTab.PROGRESS) },
            onLastSessionClick = { sessionId -> navigator.navigate(SessionDetails(sessionId = sessionId)) },
        )
    }

    entry<WorkoutsTab> {
        NavigationPlaceholderScreen(
            title = "My Workouts",
            routeLabel = "WorkoutsTab",
            topBarStyle = PulseTopAppBarStyle.RootNavigationHeader(
                overline = "CHOOSE YOUR NEXT SESSION",
                title = "My Workouts",
                actionLabel = "+ New Plan",
                onActionClick = { navigator.navigate(CreateWorkoutPlan) },
            ),
            actions = listOf(
                NavigationDemoAction("Open Workout Details", isPrimary = true) {
                    navigator.openWorkoutDetails(planId = "plan_sample_1")
                },
                NavigationDemoAction("Create Workout Plan") {
                    navigator.navigate(CreateWorkoutPlan)
                },
            ),
        )
    }

    entry<ProgressTab> {
        NavigationPlaceholderScreen(
            title = "Progress",
            routeLabel = "ProgressTab",
            topBarStyle = PulseTopAppBarStyle.TitleOnly("Progress"),
        )
    }

    entry<HistoryTab> {
        NavigationPlaceholderScreen(
            title = "Workout History",
            routeLabel = "HistoryTab",
            topBarStyle = PulseTopAppBarStyle.TitleWithBadge(
                title = "Workout History",
                badgeLabel = "Local Storage",
            ),
            actions = listOf(
                NavigationDemoAction("Open Session Details", isPrimary = true) {
                    navigator.navigate(SessionDetails(sessionId = "session_sample_1"))
                },
            ),
        )
    }

    entry<ProfileTab> {
        val viewModel: ProfileViewModel = viewModel {
            ProfileViewModel(
                authRepository = dataContainer.authRepository,
                userProfileRepository = dataContainer.userProfileRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
            )
        }
        ProfileScreen(
            viewModel = viewModel,
            onEditProfileClick = { navigator.navigate(EditProfile) },
            onDeviceClick = { navigator.navigate(MyDevice) },
            onLoggedOut = { navigator.resetToAuth(AuthEntry) },
        )
    }

    // --- Workout Planning ---
    entry<WorkoutDetails> { key ->
        NavigationPlaceholderScreen(
            title = "Workout Details",
            routeLabel = "WorkoutDetails",
            topBarStyle = PulseTopAppBarStyle.BackWithTitleAndAction(
                title = "Workout Details",
                actionLabel = "Edit",
                onBackClick = { navigator.navigateBack() },
                onActionClick = { navigator.navigate(EditWorkoutPlan(key.planId)) },
            ),
            routeArguments = listOf("planId" to key.planId),
            actions = listOf(
                NavigationDemoAction("Start Workout Session", isPrimary = true) {
                    navigator.startWorkoutSession(planId = key.planId, sessionId = "session_sample_1")
                },
                NavigationDemoAction("Edit Workout Plan") {
                    navigator.navigate(EditWorkoutPlan(key.planId))
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<CreateWorkoutPlan> {
        NavigationPlaceholderScreen(
            title = "Create Workout Plan",
            routeLabel = "CreateWorkoutPlan",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("New Plan", onBackClick = { navigator.navigateBack() }),
            actions = listOf(
                NavigationDemoAction("Add Exercises (Picker)", isPrimary = true) {
                    navigator.navigate(ExercisePicker(planId = null))
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<EditWorkoutPlan> { key ->
        NavigationPlaceholderScreen(
            title = "Edit Workout Plan",
            routeLabel = "EditWorkoutPlan",
            topBarStyle = PulseTopAppBarStyle.BackWithTitleAndDelete(
                title = "Edit Workout Plan",
                onBackClick = { navigator.navigateBack() },
                onDeleteClick = { navigator.navigateBack() },
            ),
            routeArguments = listOf("planId" to key.planId),
            actions = listOf(
                NavigationDemoAction("Open Exercise Picker", isPrimary = true) {
                    navigator.navigate(ExercisePicker(planId = key.planId))
                },
            ),
            onBackClick = { navigator.navigateBack() },
            supportsDialogDemo = true,
        )
    }

    entry<ExercisePicker> { key ->
        NavigationPlaceholderScreen(
            title = "Exercise Picker",
            routeLabel = "ExercisePicker",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Select Exercises", onBackClick = { navigator.navigateBack() }),
            routeArguments = listOf("planId" to (key.planId ?: "new")),
            onBackClick = { navigator.navigateBack() },
        )
    }

    // --- Workout Execution & Summaries ---
    entry<WorkoutSession> { key ->
        val sessionId = key.sessionId ?: "session_sample_1"
        NavigationPlaceholderScreen(
            title = "Workout Session",
            routeLabel = "WorkoutSession",
            topBarStyle = PulseTopAppBarStyle.LiveWorkoutHeader(
                exerciseTitle = "Workout Session",
                sessionSubtitle = "Plan: ${key.planId}",
                bluetoothStatus = PulseBluetoothChipStatus.DISCONNECTED,
                onBackClick = { navigator.navigateBack() },
                onBluetoothClick = { navigator.navigate(MyDevice) },
            ),
            routeArguments = listOf("planId" to key.planId, "sessionId" to sessionId),
            actions = listOf(
                NavigationDemoAction("Ready / Start Set", isPrimary = true) {
                    navigator.navigate(ReadyStartSet(sessionId = sessionId, exerciseId = "ex_1", setIndex = 1))
                },
                NavigationDemoAction("Complete Workout -> Summary") {
                    navigator.completeWorkoutFlow(WorkoutComplete(sessionId = sessionId))
                },
                NavigationDemoAction("End Early -> Partial Summary") {
                    navigator.completeWorkoutFlow(PartialWorkoutSummary(sessionId = sessionId))
                },
            ),
            onBackClick = { navigator.navigateBack() },
            supportsSheetDemo = true,
        )
    }

    entry<ReadyStartSet> { key ->
        NavigationPlaceholderScreen(
            title = "Ready / Start Set",
            routeLabel = "ReadyStartSet",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Prepare Set", onBackClick = { navigator.navigateBack() }),
            routeArguments = listOf(
                "sessionId" to key.sessionId,
                "exerciseId" to key.exerciseId,
                "setIndex" to key.setIndex.toString(),
            ),
            actions = listOf(
                NavigationDemoAction("Manual Rep Counter", isPrimary = true) {
                    navigator.navigate(
                        ManualRepCounter(
                            sessionId = key.sessionId,
                            exerciseId = key.exerciseId,
                            setIndex = key.setIndex,
                        ),
                    )
                },
                NavigationDemoAction("Device-Assisted Rep Counter") {
                    navigator.navigate(
                        DeviceAssistedRepCounter(
                            sessionId = key.sessionId,
                            exerciseId = key.exerciseId,
                            setIndex = key.setIndex,
                        ),
                    )
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<ManualRepCounter> { key ->
        NavigationPlaceholderScreen(
            title = "Manual Rep Counter",
            routeLabel = "ManualRepCounter",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Manual Counter", onBackClick = { navigator.navigateBack() }),
            routeArguments = listOf(
                "sessionId" to key.sessionId,
                "exerciseId" to key.exerciseId,
                "setIndex" to key.setIndex.toString(),
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<DeviceAssistedRepCounter> { key ->
        NavigationPlaceholderScreen(
            title = "Device-Assisted Rep Counter",
            routeLabel = "DeviceAssistedRepCounter",
            topBarStyle = PulseTopAppBarStyle.LiveWorkoutHeader(
                exerciseTitle = "Device Counter",
                sessionSubtitle = "Set ${key.setIndex}",
                bluetoothStatus = PulseBluetoothChipStatus.CONNECTED,
                onBackClick = { navigator.navigateBack() },
            ),
            routeArguments = listOf(
                "sessionId" to key.sessionId,
                "exerciseId" to key.exerciseId,
                "setIndex" to key.setIndex.toString(),
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<WorkoutComplete> { key ->
        NavigationPlaceholderScreen(
            title = "Workout Complete",
            routeLabel = "WorkoutComplete",
            topBarStyle = PulseTopAppBarStyle.WorkoutSummaryHeader(
                title = "Workout Summary",
                onDismissClick = { navigator.navigateBack() },
                onShareClick = {},
            ),
            routeArguments = listOf("sessionId" to key.sessionId),
            actions = listOf(
                NavigationDemoAction("View Session Details", isPrimary = true) {
                    navigator.navigate(SessionDetails(key.sessionId))
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<PartialWorkoutSummary> { key ->
        NavigationPlaceholderScreen(
            title = "Partial Workout Summary",
            routeLabel = "PartialWorkoutSummary",
            topBarStyle = PulseTopAppBarStyle.WorkoutSummaryHeader(
                title = "Partial Summary",
                onDismissClick = { navigator.navigateBack() },
                onShareClick = {},
            ),
            routeArguments = listOf("sessionId" to key.sessionId),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<SessionDetails> { key ->
        NavigationPlaceholderScreen(
            title = "Session Details",
            routeLabel = "SessionDetails",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Session Details", onBackClick = { navigator.navigateBack() }),
            routeArguments = listOf("sessionId" to key.sessionId),
            onBackClick = { navigator.navigateBack() },
        )
    }

    // --- Device & Profile Settings ---
    entry<MyDevice> {
        NavigationPlaceholderScreen(
            title = "My Device",
            routeLabel = "MyDevice",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("My Device", onBackClick = { navigator.navigateBack() }),
            actions = listOf(
                NavigationDemoAction("Scan for Devices", isPrimary = true) {
                    navigator.navigate(DeviceDiscovery)
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<DeviceDiscovery> {
        NavigationPlaceholderScreen(
            title = "Device Discovery",
            routeLabel = "DeviceDiscovery",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Device Discovery", onBackClick = { navigator.navigateBack() }),
            onBackClick = { navigator.navigateBack() },
        )
    }

        entry<EditProfile> {
            val viewModel: EditProfileViewModel = viewModel {
                EditProfileViewModel(
                    authRepository = dataContainer.authRepository,
                    userProfileRepository = dataContainer.userProfileRepository,
                )
            }
            EditProfileScreen(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onSaveSuccess = { navigator.navigateBack() },
            )
        }
    }
}
