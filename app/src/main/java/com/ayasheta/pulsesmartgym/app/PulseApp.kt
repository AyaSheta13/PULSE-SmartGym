package com.ayasheta.pulsesmartgym.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ayasheta.pulsesmartgym.PulseApplication
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.feature.splash.SplashContent
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import com.ayasheta.pulsesmartgym.navigation.PulseNavigator
import com.ayasheta.pulsesmartgym.navigation.rememberPulseNavigationState
import com.ayasheta.pulsesmartgym.navigation.route.AuthEntry
import com.ayasheta.pulsesmartgym.navigation.route.ProfileSetupPersonalInfo

@Composable
fun PulseApp(
    isDebugEntryEnabled: Boolean = false,
    initialFlow: AppEntryFlow = AppEntryFlow.AUTH,
    initialTab: PulseTab = PulseTab.HOME,
    dataContainer: PulseDataContainer? = null,
    enforceAuthGuard: Boolean = initialFlow == AppEntryFlow.AUTH,
) {
    val context = LocalContext.current
    val resolvedDataContainer = remember(context, dataContainer) {
        dataContainer
            ?: (context.applicationContext as? PulseApplication)?.dataContainer
            ?: PulseDataContainer(context)
    }

    PulseTheme {
        PulseMaterialThemeBridge {
            val navigationState = rememberPulseNavigationState(
                initialFlow = initialFlow,
                initialTab = initialTab,
            )
            val navigator = remember(navigationState) {
                PulseNavigator(navigationState)
            }
            val authState by resolvedDataContainer.authRepository.observeAuthState()
                .collectAsState(initial = AuthState.Initializing)

            var authorizedOwnerId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(enforceAuthGuard, resolvedDataContainer) {
                if (enforceAuthGuard) {
                    resolvedDataContainer.authRepository.restoreSession()
                }
            }

            LaunchedEffect(enforceAuthGuard, navigationState.activeFlow, authState) {
                if (!enforceAuthGuard) return@LaunchedEffect
                when (val currentAuth = authState) {
                    AuthState.Initializing -> {
                        authorizedOwnerId = null
                    }
                    AuthState.Unauthenticated -> {
                        authorizedOwnerId = null
                        if (navigationState.activeFlow == AppEntryFlow.MAIN) {
                            navigator.resetToAuth(AuthEntry)
                        }
                    }
                    is AuthState.Authenticated -> {
                        if (navigationState.activeFlow == AppEntryFlow.MAIN) {
                            val setupDone = resolvedDataContainer.userProfileRepository
                                .isProfileSetupCompleted(currentAuth.user.id)
                            if (setupDone) {
                                authorizedOwnerId = currentAuth.user.id
                            } else {
                                authorizedOwnerId = null
                                navigator.resetToAuth(ProfileSetupPersonalInfo)
                            }
                        } else {
                            authorizedOwnerId = null
                        }
                    }
                }
            }

            val currentOwnerId = (authState as? AuthState.Authenticated)?.user?.id
            val hideUnauthorizedMainDuringGate = enforceAuthGuard &&
                navigationState.activeFlow == AppEntryFlow.MAIN &&
                (currentOwnerId == null || authorizedOwnerId != currentOwnerId)

            if (hideUnauthorizedMainDuringGate) {
                SplashContent()
            } else {
                PulseAppShell(
                    navigationState = navigationState,
                    navigator = navigator,
                    isDebugEntryEnabled = isDebugEntryEnabled,
                    dataContainer = resolvedDataContainer,
                )
            }
        }
    }
}

@Composable
private fun PulseMaterialThemeBridge(
    content: @Composable () -> Unit,
) {
    val pulseColors = PulseTheme.color
    val pulseTypography = PulseTheme.typography
    val colorScheme = lightColorScheme(
        primary = pulseColors.primary,
        onPrimary = pulseColors.onPrimary,
        primaryContainer = pulseColors.primaryContainer,
        onPrimaryContainer = pulseColors.primaryDark,
        secondary = pulseColors.secondary,
        onSecondary = pulseColors.onPrimary,
        secondaryContainer = pulseColors.secondaryVariant,
        onSecondaryContainer = pulseColors.shadePrimary,
        background = pulseColors.background,
        onBackground = pulseColors.shadePrimary,
        surface = pulseColors.container,
        onSurface = pulseColors.shadePrimary,
        surfaceVariant = pulseColors.surfaceMuted,
        onSurfaceVariant = pulseColors.shadeSecondary,
        outline = pulseColors.border,
        error = pulseColors.error,
        onError = pulseColors.onPrimary,
        errorContainer = pulseColors.errorVariant,
    )
    val materialTypography = androidx.compose.material3.Typography(
        displayLarge = pulseTypography.displayHero,
        headlineMedium = pulseTypography.sectionTitle,
        titleLarge = pulseTypography.sectionHeader,
        titleMedium = pulseTypography.cardTitle,
        bodyLarge = pulseTypography.contentPrimary,
        bodyMedium = pulseTypography.contentSecondary,
        bodySmall = pulseTypography.supportingContent,
        labelLarge = pulseTypography.ctaPrimary,
        labelMedium = pulseTypography.label,
        labelSmall = pulseTypography.hint,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = materialTypography,
        content = content,
    )
}
