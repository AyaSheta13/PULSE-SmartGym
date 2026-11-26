package com.ayasheta.pulsesmartgym.feature.device.permissions

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class BluetoothReadinessState {
    READY,
    PERMISSION_NOT_REQUESTED,
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    BLUETOOTH_DISABLED,
    HARDWARE_UNAVAILABLE,
}

object BluetoothPermissionPolicy {

    fun runtimePermissionsForApi(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        if (sdkInt >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun resolveReadiness(
        hasBleHardware: Boolean,
        isBluetoothEnabled: Boolean,
        allRuntimePermissionsGranted: Boolean,
        hasRequestedBefore: Boolean,
        shouldShowRationale: Boolean,
    ): BluetoothReadinessState = when {
        !hasBleHardware -> BluetoothReadinessState.HARDWARE_UNAVAILABLE
        !allRuntimePermissionsGranted && !hasRequestedBefore -> BluetoothReadinessState.PERMISSION_NOT_REQUESTED
        !allRuntimePermissionsGranted && shouldShowRationale -> BluetoothReadinessState.PERMISSION_DENIED
        !allRuntimePermissionsGranted -> BluetoothReadinessState.PERMISSION_PERMANENTLY_DENIED
        !isBluetoothEnabled -> BluetoothReadinessState.BLUETOOTH_DISABLED
        else -> BluetoothReadinessState.READY
    }

    fun inspectDeviceReadiness(
        context: Context,
        hasRequestedBefore: Boolean,
        shouldShowRationale: Boolean,
    ): BluetoothReadinessState {
        val pm = context.packageManager
        val hasBle = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val btManager = context.getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = btManager?.adapter
        val enabled = adapter?.isEnabled == true
        val perms = runtimePermissionsForApi()
        val granted = perms.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        return resolveReadiness(
            hasBleHardware = hasBle && adapter != null,
            isBluetoothEnabled = enabled,
            allRuntimePermissionsGranted = granted,
            hasRequestedBefore = hasRequestedBefore,
            shouldShowRationale = shouldShowRationale,
        )
    }
}
