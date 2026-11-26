package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.ayasheta.pulsesmartgym.feature.session.WorkoutSessionLaunchCoordinator
import com.ayasheta.pulsesmartgym.feature.session.WorkoutSessionLaunchOverlays
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionScreen
import com.ayasheta.pulsesmartgym.feature.session.dashboard.WorkoutSessionViewModel
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultScreen
import com.ayasheta.pulsesmartgym.feature.session.summary.WorkoutResultViewModel
import com.ayasheta.pulsesmartgym.feature.profilesetup.ProfileSetupDraftHolder
import com.ayasheta.pulsesmartgym.feature.profilesetup.goals.FitnessGoalsScreen
import com.ayasheta.pulsesmartgym.feature.profilesetup.goals.FitnessGoalsViewModel
import com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo.PersonalInfoScreen
import com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo.PersonalInfoViewModel
import com.ayasheta.pulsesmartgym.feature.splash.SplashDestination
import com.ayasheta.pulsesmartgym.feature.splash.SplashScreen
import com.ayasheta.pulsesmartgym.feature.splash.SplashViewModel
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsScreen
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsViewModel
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanEditorScreen
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanEditorViewModel
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanSelectionBridge
import com.ayasheta.pulsesmartgym.feature.workout.list.MyWorkoutsScreen
import com.ayasheta.pulsesmartgym.feature.workout.list.MyWorkoutsViewModel
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerScreen
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerViewModel
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
    val workoutSelectionBridge: WorkoutPlanSelectionBridge = viewModel(
        viewModelStoreOwner = authFlowViewModelStoreOwner,
        key = "workout_plan_selection_bridge",
    ) {
        WorkoutPlanSelectionBridge()
    }
    val coroutineScope = rememberCoroutineScope()
    val sessionLaunchCoordinator = remember(navigator, dataContainer, coroutineScope) {
        WorkoutSessionLaunchCoordinator(
            scope = coroutineScope,
            authRepository = dataContainer.authRepository,
            workoutSessionRepository = dataContainer.workoutSessionRepository,
            onNavigateToSession = { planId, sessionId ->
                navigator.startWorkoutSession(planId = planId, sessionId = sessionId)
            },
        )
    }
    WorkoutSessionLaunchOverlays(coordinator = sessionLaunchCoordinator)

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
            onStartPlanClick = { planId -> sessionLaunchCoordinator.startOrResumePlan(planId) },
            onResumeSessionClick = { planId, sessionId ->
                navigator.startWorkoutSession(planId = planId, sessionId = sessionId)
            },
            onProgressClick = { navigator.selectTab(PulseTab.PROGRESS) },
            onLastSessionClick = { sessionId -> navigator.navigate(SessionDetails(sessionId = sessionId)) },
        )
    }

    entry<WorkoutsTab> {
        val viewModel: MyWorkoutsViewModel = viewModel {
            MyWorkoutsViewModel(
                authRepository = dataContainer.authRepository,
                workoutPlanRepository = dataContainer.workoutPlanRepository,
                exerciseRepository = dataContainer.exerciseRepository,
            )
        }
        MyWorkoutsScreen(
            viewModel = viewModel,
            onNewPlanClick = { navigator.navigate(CreateWorkoutPlan) },
            onPlanClick = { planId -> navigator.openWorkoutDetails(planId = planId) },
            onStartPlanClick = { planId -> sessionLaunchCoordinator.startOrResumePlan(planId) },
        )
    }

    entry<ProgressTab> {
        val viewModel: com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressViewModel = viewModel {
            com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressViewModel(
                authRepository = dataContainer.authRepository,
                workoutHistoryRepository = dataContainer.workoutHistoryRepository,
                userProfileRepository = dataContainer.userProfileRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        com.ayasheta.pulsesmartgym.feature.progress.TrainingProgressScreen(
            viewModel = viewModel,
            onStartWorkoutClick = { navigator.selectTab(PulseTab.WORKOUTS) },
        )
    }

    entry<HistoryTab> {
        val viewModel: com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryViewModel = viewModel {
            com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryViewModel(
                authRepository = dataContainer.authRepository,
                workoutHistoryRepository = dataContainer.workoutHistoryRepository,
            )
        }
        com.ayasheta.pulsesmartgym.feature.history.WorkoutHistoryScreen(
            viewModel = viewModel,
            onSessionClick = { sessionId ->
                navigator.navigate(SessionDetails(sessionId = sessionId))
            },
            onGoToWorkoutsClick = { navigator.selectTab(PulseTab.WORKOUTS) },
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

    // --- Workout Planning (P6) ---
    entry<WorkoutDetails> { key ->
        val viewModel: WorkoutDetailsViewModel = viewModel {
            WorkoutDetailsViewModel(
                savedStateHandle = createSavedStateHandle(),
                planId = key.planId,
                authRepository = dataContainer.authRepository,
                workoutPlanRepository = dataContainer.workoutPlanRepository,
                exerciseRepository = dataContainer.exerciseRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
            )
        }
        WorkoutDetailsScreen(
            viewModel = viewModel,
            onBackClick = { navigator.navigateBack() },
            onEditPlanClick = { planId -> navigator.navigate(EditWorkoutPlan(planId)) },
            onStartWorkoutClick = { planId -> sessionLaunchCoordinator.startOrResumePlan(planId) },
        )
    }

    entry<CreateWorkoutPlan> {
        val viewModel: WorkoutPlanEditorViewModel = viewModel {
            WorkoutPlanEditorViewModel(
                savedStateHandle = createSavedStateHandle(),
                planId = null,
                authRepository = dataContainer.authRepository,
                workoutPlanRepository = dataContainer.workoutPlanRepository,
                exerciseRepository = dataContainer.exerciseRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
                selectionBridge = workoutSelectionBridge,
            )
        }
        WorkoutPlanEditorScreen(
            viewModel = viewModel,
            onOpenExercisePicker = { planId, editorSessionId ->
                navigator.navigate(ExercisePicker(planId = planId, editorSessionId = editorSessionId))
            },
            onPlanCreated = { createdPlanId -> navigator.replaceTopWithWorkoutDetails(createdPlanId) },
            onPlanUpdated = { navigator.navigateBack() },
            onPlanDeleted = { navigator.navigateBack() },
            onNavigateBack = { navigator.navigateBack() },
        )
    }

    entry<EditWorkoutPlan> { key ->
        val viewModel: WorkoutPlanEditorViewModel = viewModel {
            WorkoutPlanEditorViewModel(
                savedStateHandle = createSavedStateHandle(),
                planId = key.planId,
                authRepository = dataContainer.authRepository,
                workoutPlanRepository = dataContainer.workoutPlanRepository,
                exerciseRepository = dataContainer.exerciseRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
                selectionBridge = workoutSelectionBridge,
            )
        }
        WorkoutPlanEditorScreen(
            viewModel = viewModel,
            onOpenExercisePicker = { planId, editorSessionId ->
                navigator.navigate(ExercisePicker(planId = planId, editorSessionId = editorSessionId))
            },
            onPlanCreated = { navigator.navigateBack() },
            onPlanUpdated = { navigator.navigateBack() },
            onPlanDeleted = { navigator.popWorkoutPlanFlowAfterDelete(key.planId) },
            onNavigateBack = { navigator.navigateBack() },
        )
    }

    entry<ExercisePicker> { key ->
        val viewModel: ExercisePickerViewModel = viewModel {
            ExercisePickerViewModel(
                savedStateHandle = createSavedStateHandle(),
                routePlanId = key.planId,
                routeEditorSessionId = key.editorSessionId,
                authRepository = dataContainer.authRepository,
                exerciseRepository = dataContainer.exerciseRepository,
                selectionBridge = workoutSelectionBridge,
            )
        }
        ExercisePickerScreen(
            viewModel = viewModel,
            onBackClick = { navigator.navigateBack() },
            onSelectionConfirmed = { navigator.navigateBack() },
        )
    }

    // --- Workout Execution & Summaries (P7) ---
    var pendingSessionError by rememberSaveable { mutableStateOf<String?>(null) }

    entry<WorkoutSession> { key ->
        val viewModel: WorkoutSessionViewModel = viewModel {
            WorkoutSessionViewModel(
                sessionId = key.sessionId,
                authRepository = dataContainer.authRepository,
                workoutSessionRepository = dataContainer.workoutSessionRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
            )
        }
        WorkoutSessionScreen(
            viewModel = viewModel,
            onBackClick = { navigator.navigateBack() },
            onStartSet = { sessionId, exerciseId, setIndex ->
                navigator.navigate(
                    ReadyStartSet(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        setIndex = setIndex,
                    ),
                )
            },
            onNavigateToNextSet = { sessionId, exerciseId, setIndex ->
                navigator.navigate(
                    ReadyStartSet(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        setIndex = setIndex,
                    ),
                )
            },
            pendingActionError = pendingSessionError,
            onConsumePendingActionError = { pendingSessionError = null },
            onSessionCompleted = { completedSessionId ->
                navigator.completeWorkoutFlow(WorkoutComplete(sessionId = completedSessionId))
            },
            onSessionEndedEarly = { endedSessionId ->
                navigator.completeWorkoutFlow(PartialWorkoutSummary(sessionId = endedSessionId))
            },
        )
    }

    entry<ReadyStartSet> { key ->
        val viewModel: com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetViewModel = viewModel {
            com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetViewModel(
                sessionId = key.sessionId,
                exerciseId = key.exerciseId,
                setIndex = key.setIndex,
                authRepository = dataContainer.authRepository,
                workoutSessionRepository = dataContainer.workoutSessionRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
            )
        }
        com.ayasheta.pulsesmartgym.feature.session.ready.ReadyStartSetScreen(
            viewModel = viewModel,
            onBackClick = { navigator.navigateBack() },
            onStartSetClick = { sessionId, exerciseId, setIndex ->
                navigator.navigate(
                    ManualRepCounter(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        setIndex = setIndex,
                    ),
                )
            },
            onSessionCompleted = { completedSessionId ->
                navigator.completeWorkoutFlow(WorkoutComplete(sessionId = completedSessionId))
            },
            onSessionEndedEarly = { endedSessionId ->
                navigator.completeWorkoutFlow(PartialWorkoutSummary(sessionId = endedSessionId))
            },
        )
    }

    entry<ManualRepCounter> { key ->
        val viewModel: com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterViewModel = viewModel {
            com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterViewModel(
                sessionId = key.sessionId,
                exerciseId = key.exerciseId,
                setIndex = key.setIndex,
                authRepository = dataContainer.authRepository,
                workoutSessionRepository = dataContainer.workoutSessionRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterScreen(
            viewModel = viewModel,
            onBackClick = { navigator.navigateBack() },
            onSetFinished = { sessionId, hasRestStarted, nextTarget, restErrorMessage ->
                if (restErrorMessage != null) {
                    pendingSessionError = restErrorMessage
                }
                navigator.finishSetAndReturnToSession(
                    sessionId = sessionId,
                    nextExerciseId = nextTarget?.exerciseId,
                    nextSetIndex = nextTarget?.setIndex,
                    openNextSetImmediately = false,
                )
            },
            onSessionCompleted = { completedSessionId ->
                navigator.completeWorkoutFlow(WorkoutComplete(sessionId = completedSessionId))
            },
            onSessionEndedEarly = { endedSessionId ->
                navigator.completeWorkoutFlow(PartialWorkoutSummary(sessionId = endedSessionId))
            },
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
        val viewModel: WorkoutResultViewModel = viewModel {
            WorkoutResultViewModel(
                sessionId = key.sessionId,
                authRepository = dataContainer.authRepository,
                workoutSessionRepository = dataContainer.workoutSessionRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
            )
        }
        WorkoutResultScreen(
            viewModel = viewModel,
            onBackToHome = { navigator.exitWorkoutSummaryToHome() },
            onViewHistory = { navigator.exitWorkoutSummaryToHistory() },
        )
    }

    entry<PartialWorkoutSummary> { key ->
        val viewModel: WorkoutResultViewModel = viewModel {
            WorkoutResultViewModel(
                sessionId = key.sessionId,
                authRepository = dataContainer.authRepository,
                workoutSessionRepository = dataContainer.workoutSessionRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
            )
        }
        WorkoutResultScreen(
            viewModel = viewModel,
            onBackToHome = { navigator.exitWorkoutSummaryToHome() },
            onViewHistory = { navigator.exitWorkoutSummaryToHistory() },
        )
    }

    entry<SessionDetails> { key ->
        val viewModel: com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsViewModel = viewModel {
            com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsViewModel(
                sessionId = key.sessionId,
                authRepository = dataContainer.authRepository,
                workoutHistoryRepository = dataContainer.workoutHistoryRepository,
                userPreferencesRepository = dataContainer.userPreferencesRepository,
            )
        }
        com.ayasheta.pulsesmartgym.feature.history.details.SessionDetailsScreen(
            viewModel = viewModel,
            onBackClick = { navigator.navigateBack() },
            onRepeatWorkoutClick = { historicalSessionId ->
                sessionLaunchCoordinator.repeatHistoricalSession(historicalSessionId)
            },
            isRepeating = sessionLaunchCoordinator.isStarting,
            repeatErrorMessage = sessionLaunchCoordinator.errorMessage,
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
