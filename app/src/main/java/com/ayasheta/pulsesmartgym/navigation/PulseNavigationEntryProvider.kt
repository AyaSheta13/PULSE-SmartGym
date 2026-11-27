package com.ayasheta.pulsesmartgym.navigation

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.feature.device.DeviceViewModel
import com.ayasheta.pulsesmartgym.feature.device.center.MyDeviceScreen
import com.ayasheta.pulsesmartgym.feature.device.diagnostics.HardwareDiagnosticsScreen
import com.ayasheta.pulsesmartgym.feature.device.discovery.DeviceDiscoveryScreen
import com.ayasheta.pulsesmartgym.feature.device.env.DemoEnvironmentController
import com.ayasheta.pulsesmartgym.feature.device.env.ExecutionEnvironmentState
import com.ayasheta.pulsesmartgym.feature.device.permissions.BluetoothPermissionPolicy
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftStore
import com.ayasheta.pulsesmartgym.feature.session.devicecounter.DeviceAssistedRepCounterScreen
import com.ayasheta.pulsesmartgym.feature.session.devicecounter.DeviceAssistedRepCounterViewModel
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsScreen
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsViewModel
import com.ayasheta.pulsesmartgym.feature.workout.exercise.SessionExerciseDetailsViewModel
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
import com.ayasheta.pulsesmartgym.navigation.route.ExerciseDetails
import com.ayasheta.pulsesmartgym.navigation.route.ExercisePicker
import com.ayasheta.pulsesmartgym.navigation.route.ForgotPassword
import com.ayasheta.pulsesmartgym.navigation.route.HardwareDiagnostics
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
import com.ayasheta.pulsesmartgym.navigation.route.SessionExerciseDetails
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val deviceViewModel: DeviceViewModel = viewModel(
        viewModelStoreOwner = authFlowViewModelStoreOwner,
        key = "global_device_view_model",
    ) {
        DeviceViewModel(
            authRepository = dataContainer.authRepository,
            deviceRepository = dataContainer.deviceRepository,
            fakeDeviceDataSource = dataContainer.fakeDeviceDataSource,
        )
    }
    val deviceUiState by deviceViewModel.uiState.collectAsStateWithLifecycle()

    val demoController: DemoEnvironmentController = viewModel(
        viewModelStoreOwner = authFlowViewModelStoreOwner,
        key = "global_demo_environment_controller",
    ) {
        DemoEnvironmentController(
            appContext = context.applicationContext,
            authRepository = dataContainer.authRepository,
            deviceRepository = dataContainer.deviceRepository,
        )
    }
    val setDraftStore = demoController.setDraftStore
    val envState by demoController.state.collectAsStateWithLifecycle()

    val globalBluetoothChipStatus = when (deviceUiState.linkState.phase) {
        DeviceConnectionState.CONNECTED -> PulseBluetoothChipStatus.CONNECTED
        DeviceConnectionState.SCANNING,
        DeviceConnectionState.CONNECTING,
        -> PulseBluetoothChipStatus.CONNECTING
        DeviceConnectionState.CONNECTION_LOST,
        DeviceConnectionState.FAILED,
        -> PulseBluetoothChipStatus.CONNECTION_LOST
        DeviceConnectionState.DISCONNECTED -> PulseBluetoothChipStatus.DISCONNECTED
    }

    val sessionLaunchCoordinator = remember(navigator, dataContainer, coroutineScope) {
        WorkoutSessionLaunchCoordinator(
            scope = coroutineScope,
            authRepository = dataContainer.authRepository,
            workoutSessionRepository = dataContainer.workoutSessionRepository,
            onNavigateToSession = { planId, sessionId ->
                navigator.startWorkoutSession(
                    planId = planId,
                    sessionId = sessionId,
                    environment = ExecutionEnvironmentKind.GENUINE,
                )
            },
        )
    }
    WorkoutSessionLaunchOverlays(coordinator = sessionLaunchCoordinator)

    val demoLaunchCoordinator = remember(navigator, envState, coroutineScope) {
        val ready = envState as? ExecutionEnvironmentState.DemoReady
        ready?.let {
            WorkoutSessionLaunchCoordinator(
                scope = coroutineScope,
                authRepository = it.isolatedContainer.container.authRepository,
                workoutSessionRepository = it.isolatedContainer.container.workoutSessionRepository,
                onNavigateToSession = { planId, sessionId ->
                    navigator.startWorkoutSession(
                        planId = planId,
                        sessionId = sessionId,
                        environment = ExecutionEnvironmentKind.DEMO,
                    )
                },
            )
        }
    }
    if (demoLaunchCoordinator != null) {
        WorkoutSessionLaunchOverlays(coordinator = demoLaunchCoordinator)
    }

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
            bluetoothStatus = globalBluetoothChipStatus,
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
        val profileDeviceText = when (deviceUiState.linkState.phase) {
            DeviceConnectionState.CONNECTED -> {
                val name = deviceUiState.linkState.selectedDevice?.name ?: "PULSE Pod"
                if (deviceUiState.linkState.isSimulated) "$name • Connected (Simulated)" else "$name • Connected"
            }
            DeviceConnectionState.CONNECTION_LOST -> "Connection lost • Tap to reconnect"
            DeviceConnectionState.CONNECTING -> "Connecting..."
            DeviceConnectionState.SCANNING -> "Scanning..."
            DeviceConnectionState.FAILED -> "Connection failed • Tap to retry"
            DeviceConnectionState.DISCONNECTED -> "Not paired • Tap to connect"
        }
        ProfileScreen(
            viewModel = viewModel,
            onEditProfileClick = { navigator.navigate(EditProfile) },
            onDeviceClick = { navigator.navigate(MyDevice) },
            onLoggedOut = {
                setDraftStore.clearAll()
                deviceViewModel.exitDemoMode()
                navigator.resetToAuth(AuthEntry)
            },
            deviceStatusText = profileDeviceText,
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
            onExerciseDetailsClick = { exerciseId ->
                navigator.navigate(ExerciseDetails(exerciseId = exerciseId))
            },
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
            onExerciseDetailsClick = { exerciseId ->
                navigator.navigate(ExerciseDetails(exerciseId = exerciseId))
            },
        )
    }

    entry<ExerciseDetails> { key ->
        val viewModel: com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsViewModel = viewModel {
            com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsViewModel(
                exerciseId = key.exerciseId,
                exerciseRepository = dataContainer.exerciseRepository,
            )
        }
        com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsScreen(
            viewModel = viewModel,
            onBackClick = { navigator.navigateBack() },
        )
    }

    // --- Workout Execution & Summaries (P7) ---
    var pendingSessionError by rememberSaveable { mutableStateOf<String?>(null) }

    entry<WorkoutSession> { key ->
        DemoEnvironmentGate(
            environment = key.environment,
            envState = envState,
            genuineContainer = dataContainer,
            onExitDemoToHome = {
                deviceViewModel.exitDemoMode()
                navigator.exitWorkoutSummaryToHome()
            },
        ) { scopedContainer ->
            val viewModel: WorkoutSessionViewModel = viewModel(
                key = "ws_${key.environment.name}_${key.planId}_${key.sessionId}",
            ) {
                WorkoutSessionViewModel(
                    sessionId = key.sessionId,
                    authRepository = scopedContainer.authRepository,
                    workoutSessionRepository = scopedContainer.workoutSessionRepository,
                    userPreferencesRepository = scopedContainer.userPreferencesRepository,
                    deviceRepository = dataContainer.deviceRepository,
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
                            environment = key.environment,
                        ),
                    )
                },
                onNavigateToNextSet = { sessionId, exerciseId, setIndex ->
                    navigator.navigate(
                        ReadyStartSet(
                            sessionId = sessionId,
                            exerciseId = exerciseId,
                            setIndex = setIndex,
                            environment = key.environment,
                        ),
                    )
                },
                onHowToPerformClick = { sessionId, sessionExerciseId ->
                    navigator.navigate(
                        SessionExerciseDetails(
                            sessionId = sessionId,
                            sessionExerciseId = sessionExerciseId,
                            environment = key.environment,
                        ),
                    )
                },
                pendingActionError = pendingSessionError,
                onConsumePendingActionError = { pendingSessionError = null },
                onSessionCompleted = { completedSessionId ->
                    navigator.completeWorkoutFlow(
                        WorkoutComplete(sessionId = completedSessionId, environment = key.environment),
                    )
                },
                onSessionEndedEarly = { endedSessionId ->
                    navigator.completeWorkoutFlow(
                        PartialWorkoutSummary(sessionId = endedSessionId, environment = key.environment),
                    )
                },
            )
        }
    }

    entry<SessionExerciseDetails> { key ->
        DemoEnvironmentGate(
            environment = key.environment,
            envState = envState,
            genuineContainer = dataContainer,
            onExitDemoToHome = {
                deviceViewModel.exitDemoMode()
                navigator.exitWorkoutSummaryToHome()
            },
        ) { scopedContainer ->
            val viewModel: SessionExerciseDetailsViewModel = viewModel(
                key = "sed_${key.environment.name}_${key.sessionId}_${key.sessionExerciseId}",
            ) {
                SessionExerciseDetailsViewModel(
                    sessionId = key.sessionId,
                    sessionExerciseId = key.sessionExerciseId,
                    authRepository = scopedContainer.authRepository,
                    workoutSessionRepository = scopedContainer.workoutSessionRepository,
                    exerciseRepository = scopedContainer.exerciseRepository,
                )
            }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ExerciseDetailsScreenContent(
                uiState = uiState,
                onBackClick = { navigator.navigateBack() },
            )
        }
    }

    entry<ReadyStartSet> { key ->
        DemoEnvironmentGate(
            environment = key.environment,
            envState = envState,
            genuineContainer = dataContainer,
            onExitDemoToHome = {
                deviceViewModel.exitDemoMode()
                navigator.exitWorkoutSummaryToHome()
            },
        ) { scopedContainer ->
            val viewModel: com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionViewModel = viewModel(
                key = "set_exec_${key.environment.name}_${key.sessionId}_${key.exerciseId}_${key.setIndex}",
            ) {
                com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionViewModel(
                    sessionId = key.sessionId,
                    exerciseId = key.exerciseId,
                    setIndex = key.setIndex,
                    authRepository = scopedContainer.authRepository,
                    workoutSessionRepository = scopedContainer.workoutSessionRepository,
                    userPreferencesRepository = scopedContainer.userPreferencesRepository,
                    deviceRepository = dataContainer.deviceRepository,
                    fakeDeviceDataSource = dataContainer.fakeDeviceDataSource,
                    setDraftStore = setDraftStore,
                    savedStateHandle = createSavedStateHandle(),
                    executionEnvironment = key.environment,
                )
            }
            com.ayasheta.pulsesmartgym.feature.session.execution.SetExecutionScreen(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onSetFinished = { sessionId, _, nextTarget, restErrorMessage ->
                    if (restErrorMessage != null) {
                        pendingSessionError = restErrorMessage
                    }
                    navigator.finishSetAndReturnToSession(
                        sessionId = sessionId,
                        nextExerciseId = nextTarget?.exerciseId,
                        nextSetIndex = nextTarget?.setIndex,
                        openNextSetImmediately = false,
                        environment = key.environment,
                    )
                },
                onSessionCompleted = { completedSessionId ->
                    navigator.completeWorkoutFlow(
                        WorkoutComplete(sessionId = completedSessionId, environment = key.environment),
                    )
                },
                onSessionEndedEarly = { endedSessionId ->
                    navigator.completeWorkoutFlow(
                        PartialWorkoutSummary(sessionId = endedSessionId, environment = key.environment),
                    )
                },
            )
        }
    }

    entry<ManualRepCounter> { key ->
        DemoEnvironmentGate(
            environment = key.environment,
            envState = envState,
            genuineContainer = dataContainer,
            onExitDemoToHome = {
                deviceViewModel.exitDemoMode()
                navigator.exitWorkoutSummaryToHome()
            },
        ) { scopedContainer ->
            val viewModel: com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterViewModel = viewModel(
                key = "mrc_${key.environment.name}_${key.sessionId}_${key.exerciseId}_${key.setIndex}",
            ) {
                com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterViewModel(
                    sessionId = key.sessionId,
                    exerciseId = key.exerciseId,
                    setIndex = key.setIndex,
                    authRepository = scopedContainer.authRepository,
                    workoutSessionRepository = scopedContainer.workoutSessionRepository,
                    userPreferencesRepository = scopedContainer.userPreferencesRepository,
                    savedStateHandle = createSavedStateHandle(),
                    setDraftStore = setDraftStore,
                    executionEnvironment = key.environment,
                )
            }
            com.ayasheta.pulsesmartgym.feature.session.counter.ManualRepCounterScreen(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onSetFinished = { sessionId, _, nextTarget, restErrorMessage ->
                    if (restErrorMessage != null) {
                        pendingSessionError = restErrorMessage
                    }
                    navigator.finishSetAndReturnToSession(
                        sessionId = sessionId,
                        nextExerciseId = nextTarget?.exerciseId,
                        nextSetIndex = nextTarget?.setIndex,
                        openNextSetImmediately = false,
                        environment = key.environment,
                    )
                },
                onSessionCompleted = { completedSessionId ->
                    navigator.completeWorkoutFlow(
                        WorkoutComplete(sessionId = completedSessionId, environment = key.environment),
                    )
                },
                onSessionEndedEarly = { endedSessionId ->
                    navigator.completeWorkoutFlow(
                        PartialWorkoutSummary(sessionId = endedSessionId, environment = key.environment),
                    )
                },
            )
        }
    }

    entry<DeviceAssistedRepCounter> { key ->
        DemoEnvironmentGate(
            environment = key.environment,
            envState = envState,
            genuineContainer = dataContainer,
            onExitDemoToHome = {
                deviceViewModel.exitDemoMode()
                navigator.exitWorkoutSummaryToHome()
            },
        ) { scopedContainer ->
            val viewModel: DeviceAssistedRepCounterViewModel = viewModel(
                key = "darc_${key.environment.name}_${key.sessionId}_${key.exerciseId}_${key.setIndex}",
            ) {
                DeviceAssistedRepCounterViewModel(
                    sessionId = key.sessionId,
                    exerciseId = key.exerciseId,
                    setIndex = key.setIndex,
                    authRepository = scopedContainer.authRepository,
                    workoutSessionRepository = scopedContainer.workoutSessionRepository,
                    userPreferencesRepository = scopedContainer.userPreferencesRepository,
                    deviceRepository = dataContainer.deviceRepository,
                    fakeDeviceDataSource = dataContainer.fakeDeviceDataSource,
                    setDraftStore = setDraftStore,
                    savedStateHandle = createSavedStateHandle(),
                    executionEnvironment = key.environment,
                )
            }
            DeviceAssistedRepCounterScreen(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onSetFinished = { sessionId, _, nextTarget: com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget?, restErrorMessage ->
                    if (restErrorMessage != null) {
                        pendingSessionError = restErrorMessage
                    }
                    navigator.finishSetAndReturnToSession(
                        sessionId = sessionId,
                        nextExerciseId = nextTarget?.exerciseId,
                        nextSetIndex = nextTarget?.setIndex,
                        openNextSetImmediately = false,
                        environment = key.environment,
                    )
                },
                onFallbackToManual = { sessionId, exerciseId, setIndex ->
                    navigator.switchDeviceCounterToManual(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        setIndex = setIndex,
                        environment = key.environment,
                    )
                },
                onSessionEnded = { endedSessionId, status ->
                    val route = if (status == com.ayasheta.pulsesmartgym.core.model.session.SessionStatus.COMPLETED) {
                        WorkoutComplete(sessionId = endedSessionId, environment = key.environment)
                    } else {
                        PartialWorkoutSummary(sessionId = endedSessionId, environment = key.environment)
                    }
                    navigator.completeWorkoutFlow(route)
                },
            )
        }
    }

    entry<WorkoutComplete> { key ->
        DemoEnvironmentGate(
            environment = key.environment,
            envState = envState,
            genuineContainer = dataContainer,
            onExitDemoToHome = {
                deviceViewModel.exitDemoMode()
                navigator.exitWorkoutSummaryToHome()
            },
        ) { scopedContainer ->
            val viewModel: WorkoutResultViewModel = viewModel(
                key = "wc_${key.environment.name}_${key.sessionId}",
            ) {
                WorkoutResultViewModel(
                    sessionId = key.sessionId,
                    authRepository = scopedContainer.authRepository,
                    workoutSessionRepository = scopedContainer.workoutSessionRepository,
                    userPreferencesRepository = scopedContainer.userPreferencesRepository,
                    isDemoEnvironment = key.environment == com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
                )
            }
            WorkoutResultScreen(
                viewModel = viewModel,
                onBackToHome = {
                    if (key.environment == ExecutionEnvironmentKind.DEMO) {
                        deviceViewModel.exitDemoMode()
                    }
                    navigator.exitWorkoutSummaryToHome()
                },
                onViewHistory = if (key.environment == ExecutionEnvironmentKind.DEMO) {
                    null
                } else {
                    { navigator.exitWorkoutSummaryToHistory() }
                },
            )
        }
    }

    entry<PartialWorkoutSummary> { key ->
        DemoEnvironmentGate(
            environment = key.environment,
            envState = envState,
            genuineContainer = dataContainer,
            onExitDemoToHome = {
                deviceViewModel.exitDemoMode()
                navigator.exitWorkoutSummaryToHome()
            },
        ) { scopedContainer ->
            val viewModel: WorkoutResultViewModel = viewModel(
                key = "pws_${key.environment.name}_${key.sessionId}",
            ) {
                WorkoutResultViewModel(
                    sessionId = key.sessionId,
                    authRepository = scopedContainer.authRepository,
                    workoutSessionRepository = scopedContainer.workoutSessionRepository,
                    userPreferencesRepository = scopedContainer.userPreferencesRepository,
                    isDemoEnvironment = key.environment == com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind.DEMO,
                )
            }
            WorkoutResultScreen(
                viewModel = viewModel,
                onBackToHome = {
                    if (key.environment == ExecutionEnvironmentKind.DEMO) {
                        deviceViewModel.exitDemoMode()
                    }
                    navigator.exitWorkoutSummaryToHome()
                },
                onViewHistory = if (key.environment == ExecutionEnvironmentKind.DEMO) {
                    null
                } else {
                    { navigator.exitWorkoutSummaryToHistory() }
                },
            )
        }
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
        MyDeviceScreen(
            viewModel = deviceViewModel,
            onBackClick = { navigator.navigateBack() },
            onSearchAndConnectClick = { navigator.navigate(DeviceDiscovery) },
            onStartWorkoutWithAutoTracking = {
                val ready = envState as? ExecutionEnvironmentState.DemoReady
                val coordinator = demoLaunchCoordinator
                if (ready != null && coordinator != null && ready.demoPlanId != null) {
                    coordinator.startOrResumePlan(ready.demoPlanId)
                } else if (!deviceUiState.isDemoSessionActive) {
                    navigator.selectTab(PulseTab.WORKOUTS)
                }
            },
            showDeveloperDiagnostics = dataContainer.isDeveloperModeAvailable,
            onOpenDeveloperDiagnostics = { navigator.navigate(HardwareDiagnostics) },
        )
    }

    entry<HardwareDiagnostics> {
        HardwareDiagnosticsScreen(
            deviceRepository = dataContainer.deviceRepository,
            realBleDeviceDataSource = dataContainer.realBleDeviceDataSource,
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<DeviceDiscovery> {
        var hasRequestedBtPermissions by rememberSaveable { mutableStateOf(false) }
        val refreshBtReadiness = {
            val activity = context as? Activity
            val rationale = if (activity != null) {
                BluetoothPermissionPolicy.runtimePermissionsForApi().any { perm ->
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                }
            } else {
                false
            }
            deviceViewModel.updateBluetoothReadiness(
                BluetoothPermissionPolicy.inspectDeviceReadiness(
                    context = context,
                    hasRequestedBefore = hasRequestedBtPermissions,
                    shouldShowRationale = rationale,
                ),
            )
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            refreshBtReadiness()
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            hasRequestedBtPermissions = true
            refreshBtReadiness()
        }
        val enableBtLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            refreshBtReadiness()
        }
        DeviceDiscoveryScreen(
            viewModel = deviceViewModel,
            onBackClick = { navigator.navigateBack() },
            onContinueToWorkout = {
                val ready = envState as? ExecutionEnvironmentState.DemoReady
                val coordinator = demoLaunchCoordinator
                if (ready != null && coordinator != null && ready.demoPlanId != null) {
                    coordinator.startOrResumePlan(ready.demoPlanId)
                } else if (!deviceUiState.isDemoSessionActive) {
                    navigator.selectTab(PulseTab.WORKOUTS)
                }
            },
            onViewInDeviceCenter = { navigator.navigateBack() },
            onContinueWithoutDevice = {
                deviceViewModel.exitDemoMode()
                navigator.selectTab(PulseTab.WORKOUTS)
            },
            onRequestPermissionsClick = {
                permissionLauncher.launch(BluetoothPermissionPolicy.runtimePermissionsForApi().toTypedArray())
            },
            onEnableBluetoothClick = {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
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

@Composable
private fun DemoEnvironmentGate(
    environment: ExecutionEnvironmentKind,
    envState: ExecutionEnvironmentState,
    genuineContainer: PulseDataContainer,
    onExitDemoToHome: () -> Unit,
    content: @Composable (PulseDataContainer) -> Unit,
) {
    if (environment == ExecutionEnvironmentKind.GENUINE) {
        content(genuineContainer)
        return
    }
    when (envState) {
        is ExecutionEnvironmentState.DemoReady -> content(envState.isolatedContainer.container)
        is ExecutionEnvironmentState.DemoInitializing,
        is ExecutionEnvironmentState.DemoClosing,
        -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF8F7))
                    .testTag("demo_environment_initializing"),
                contentAlignment = Alignment.Center,
            ) {
                PulseLoadingIndicator()
            }
        }
        is ExecutionEnvironmentState.DemoFailed,
        ExecutionEnvironmentState.Genuine,
        -> {
            val message = (envState as? ExecutionEnvironmentState.DemoFailed)?.message
                ?: "The isolated demo environment is no longer active."
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF8F7))
                    .padding(24.dp)
                    .testTag("demo_environment_unavailable"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = "Demo Environment Unavailable",
                    style = PulseTheme.typography.sectionTitle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = message,
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        color = Color(0xFF6B6560),
                    ),
                )
                Spacer(modifier = Modifier.height(20.dp))
                PulsePrimaryButton(
                    caption = "Return to Home",
                    onClick = onExitDemoToHome,
                    modifier = Modifier.testTag("button_demo_unavailable_home"),
                )
            }
        }
    }
}
