package com.ayasheta.pulsesmartgym.navigation.route

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MyDevice : NavKey

@Serializable
data object DeviceDiscovery : NavKey

@Serializable
data object HardwareDiagnostics : NavKey

@Serializable
data object EditProfile : NavKey
