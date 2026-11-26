package com.ayasheta.pulsesmartgym.core.data.device

import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreP11DeviceScanDurationTest {

    @Test
    fun fakeDeviceDataSource_defaultScanDuration_isAtLeast3500ms_andCanBeCanceledImmediately() = runTest {
        assertEquals(3_500L, FakeDeviceDataSource.DEFAULT_SCAN_DELAY_MS)

        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val dataSource = FakeDeviceDataSource(scope = scope)

        val scanDeferred = scope.async { dataSource.startScan() }
        scope.runCurrent()
        assertEquals(DeviceConnectionState.SCANNING, dataSource.connectionState.value.phase)

        // Still scanning at 3000ms
        scope.advanceTimeBy(3_000L)
        scope.runCurrent()
        assertEquals(DeviceConnectionState.SCANNING, dataSource.connectionState.value.phase)

        // Completes by 3500ms
        scope.advanceTimeBy(600L)
        scope.runCurrent()
        val result = scanDeferred.await()
        assertTrue(result is DeviceOperationResult.Success)
        assertEquals(DeviceConnectionState.DISCONNECTED, dataSource.connectionState.value.phase)
        assertTrue(dataSource.connectionState.value.hasSearched)
        assertTrue(dataSource.discoveredDevices.value.isNotEmpty())

        // Rescan and cancel immediately
        val secondScan = scope.async { dataSource.startScan() }
        scope.runCurrent()
        assertEquals(DeviceConnectionState.SCANNING, dataSource.connectionState.value.phase)

        dataSource.stopScan()
        scope.runCurrent()
        val cancelResult = secondScan.await()
        assertTrue(cancelResult is DeviceOperationResult.Failure)
        assertEquals(
            DeviceOperationError.CANCELED,
            (cancelResult as DeviceOperationResult.Failure).error,
        )
        assertEquals(DeviceConnectionState.DISCONNECTED, dataSource.connectionState.value.phase)
    }
}
