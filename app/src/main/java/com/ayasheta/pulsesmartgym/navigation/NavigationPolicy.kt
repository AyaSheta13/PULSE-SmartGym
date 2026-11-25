package com.ayasheta.pulsesmartgym.navigation

import androidx.navigation3.runtime.NavKey
import com.ayasheta.pulsesmartgym.navigation.route.DeviceAssistedRepCounter
import com.ayasheta.pulsesmartgym.navigation.route.ManualRepCounter
import com.ayasheta.pulsesmartgym.navigation.route.PartialWorkoutSummary
import com.ayasheta.pulsesmartgym.navigation.route.PulseRootRoute
import com.ayasheta.pulsesmartgym.navigation.route.ReadyStartSet
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutComplete
import com.ayasheta.pulsesmartgym.navigation.route.WorkoutSession

object NavigationPolicy {

    fun shouldShowBottomBar(route: NavKey?): Boolean = route is PulseRootRoute

    fun isFullScreenWorkoutRoute(route: NavKey?): Boolean = when (route) {
        is WorkoutSession,
        is ReadyStartSet,
        is ManualRepCounter,
        is DeviceAssistedRepCounter,
        is WorkoutComplete,
        is PartialWorkoutSummary,
        -> true
        else -> false
    }
}
