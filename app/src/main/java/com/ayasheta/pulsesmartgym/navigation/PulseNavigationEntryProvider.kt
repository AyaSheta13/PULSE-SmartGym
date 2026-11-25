package com.ayasheta.pulsesmartgym.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.feature.navigationdemo.NavigationDemoAction
import com.ayasheta.pulsesmartgym.feature.navigationdemo.NavigationPlaceholderScreen
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

@Composable
fun rememberPulseEntryProvider(
    navigator: PulseNavigator,
    isDebugEntryEnabled: Boolean,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    // --- Auth & Onboarding ---
    entry<Splash> {
        NavigationPlaceholderScreen(
            title = "Splash",
            routeLabel = "Splash",
            actions = listOf(
                NavigationDemoAction("Continue to Onboarding", isPrimary = true) {
                    navigator.navigate(Onboarding)
                },
                NavigationDemoAction("Go to Auth Entry") {
                    navigator.navigate(AuthEntry)
                },
            ),
            onDebugSkipToMain = if (isDebugEntryEnabled) {
                { navigator.completeAuthAndEnterMain() }
            } else {
                null
            },
        )
    }

    entry<Onboarding> {
        NavigationPlaceholderScreen(
            title = "Onboarding",
            routeLabel = "Onboarding",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Welcome", onBackClick = { navigator.navigateBack() }),
            actions = listOf(
                NavigationDemoAction("Finish Onboarding -> Auth Entry", isPrimary = true) {
                    navigator.navigate(AuthEntry)
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<AuthEntry> {
        NavigationPlaceholderScreen(
            title = "Welcome / Auth Entry",
            routeLabel = "AuthEntry",
            actions = listOf(
                NavigationDemoAction("Log In", isPrimary = true) {
                    navigator.navigate(Login)
                },
                NavigationDemoAction("Create Account") {
                    navigator.navigate(SignUp)
                },
            ),
            onBackClick = { navigator.navigateBack() },
            onDebugSkipToMain = if (isDebugEntryEnabled) {
                { navigator.completeAuthAndEnterMain() }
            } else {
                null
            },
        )
    }

    entry<Login> {
        NavigationPlaceholderScreen(
            title = "Log In",
            routeLabel = "Login",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Log In", onBackClick = { navigator.navigateBack() }),
            actions = listOf(
                NavigationDemoAction("Complete Login -> Home", isPrimary = true) {
                    navigator.completeAuthAndEnterMain()
                },
                NavigationDemoAction("Forgot Password") {
                    navigator.navigate(ForgotPassword)
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<SignUp> {
        NavigationPlaceholderScreen(
            title = "Sign Up",
            routeLabel = "SignUp",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Create Account", onBackClick = { navigator.navigateBack() }),
            actions = listOf(
                NavigationDemoAction("Continue -> Personal Info", isPrimary = true) {
                    navigator.navigate(ProfileSetupPersonalInfo)
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<ForgotPassword> {
        NavigationPlaceholderScreen(
            title = "Forgot Password",
            routeLabel = "ForgotPassword",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Reset Password", onBackClick = { navigator.navigateBack() }),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<ProfileSetupPersonalInfo> {
        NavigationPlaceholderScreen(
            title = "Personal Info Setup",
            routeLabel = "ProfileSetupPersonalInfo",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Personal Info", onBackClick = { navigator.navigateBack() }),
            actions = listOf(
                NavigationDemoAction("Next -> Fitness Goals", isPrimary = true) {
                    navigator.navigate(ProfileSetupFitnessGoals)
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    entry<ProfileSetupFitnessGoals> {
        NavigationPlaceholderScreen(
            title = "Fitness Goals Setup",
            routeLabel = "ProfileSetupFitnessGoals",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Fitness Goals", onBackClick = { navigator.navigateBack() }),
            actions = listOf(
                NavigationDemoAction("Finish Setup -> Home", isPrimary = true) {
                    navigator.completeAuthAndEnterMain()
                },
            ),
            onBackClick = { navigator.navigateBack() },
        )
    }

    // --- Five Main Tab Roots ---
    entry<HomeTab> {
        NavigationPlaceholderScreen(
            title = "Home",
            routeLabel = "HomeTab",
            topBarStyle = PulseTopAppBarStyle.HomeGreetingHeader(
                greeting = "Welcome back",
                userName = "PULSE Athlete",
                bluetoothStatus = PulseBluetoothChipStatus.DISCONNECTED,
                avatarInitial = "P",
                onBluetoothClick = { navigator.navigate(MyDevice) },
                onAvatarClick = { navigator.selectTab(PulseTab.PROFILE) },
            ),
            actions = listOf(
                NavigationDemoAction("Open Workout Details", isPrimary = true) {
                    navigator.openWorkoutDetails(planId = "plan_sample_1")
                },
                NavigationDemoAction("Open My Device") {
                    navigator.navigate(MyDevice)
                },
            ),
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
        NavigationPlaceholderScreen(
            title = "Profile & Settings",
            routeLabel = "ProfileTab",
            topBarStyle = PulseTopAppBarStyle.TitleOnly("Profile & Settings"),
            actions = listOf(
                NavigationDemoAction("Edit Profile", isPrimary = true) {
                    navigator.navigate(EditProfile)
                },
                NavigationDemoAction("My Device") {
                    navigator.navigate(MyDevice)
                },
                NavigationDemoAction("Return to Auth Entry") {
                    navigator.resetToAuth(AuthEntry)
                },
            ),
            supportsSheetDemo = true,
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
        NavigationPlaceholderScreen(
            title = "Edit Profile",
            routeLabel = "EditProfile",
            topBarStyle = PulseTopAppBarStyle.BackWithTitle("Edit Profile", onBackClick = { navigator.navigateBack() }),
            onBackClick = { navigator.navigateBack() },
        )
    }
}
