package com.ayasheta.pulsesmartgym.navigation.route

import androidx.navigation3.runtime.NavKey
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import kotlinx.serialization.Serializable

sealed interface PulseRootRoute : NavKey {
    val tab: PulseTab
}

@Serializable
data object WorkoutsTab : PulseRootRoute {
    override val tab: PulseTab get() = PulseTab.WORKOUTS
}

@Serializable
data object ProgressTab : PulseRootRoute {
    override val tab: PulseTab get() = PulseTab.PROGRESS
}

@Serializable
data object HomeTab : PulseRootRoute {
    override val tab: PulseTab get() = PulseTab.HOME
}

@Serializable
data object HistoryTab : PulseRootRoute {
    override val tab: PulseTab get() = PulseTab.HISTORY
}

@Serializable
data object ProfileTab : PulseRootRoute {
    override val tab: PulseTab get() = PulseTab.PROFILE
}

fun PulseTab.toRootRoute(): PulseRootRoute = when (this) {
    PulseTab.WORKOUTS -> WorkoutsTab
    PulseTab.PROGRESS -> ProgressTab
    PulseTab.HOME -> HomeTab
    PulseTab.HISTORY -> HistoryTab
    PulseTab.PROFILE -> ProfileTab
}
