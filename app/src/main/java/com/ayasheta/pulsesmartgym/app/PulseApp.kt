package com.ayasheta.pulsesmartgym.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import com.ayasheta.pulsesmartgym.navigation.PulseNavigator
import com.ayasheta.pulsesmartgym.navigation.rememberPulseNavigationState

@Composable
fun PulseApp(
    isDebugEntryEnabled: Boolean = false,
    initialFlow: AppEntryFlow = AppEntryFlow.AUTH,
    initialTab: PulseTab = PulseTab.HOME,
) {
    PulseTheme {
        PulseMaterialThemeBridge {
            val navigationState = rememberPulseNavigationState(
                initialFlow = initialFlow,
                initialTab = initialTab,
            )
            val navigator = remember(navigationState) {
                PulseNavigator(navigationState)
            }
            PulseAppShell(
                navigationState = navigationState,
                navigator = navigator,
                isDebugEntryEnabled = isDebugEntryEnabled,
            )
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
