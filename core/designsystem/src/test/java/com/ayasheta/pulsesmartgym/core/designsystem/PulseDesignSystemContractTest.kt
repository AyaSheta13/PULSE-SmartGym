package com.ayasheta.pulsesmartgym.core.designsystem

import androidx.compose.ui.graphics.Color
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.SetRowStatus
import com.ayasheta.pulsesmartgym.core.designsystem.semanticToken.defaultLightColors
import org.junit.Assert.assertEquals
import org.junit.Test

class PulseDesignSystemContractTest {

    @Test
    fun pulseLightColors_matchApprovedFigmaTokens() {
        assertEquals(Color(0xFFFF623E), defaultLightColors.primary)
        assertEquals(Color(0xFFFFF1EC), defaultLightColors.completedSetBackground)
        assertEquals(Color(0xFF10B870), defaultLightColors.success)
        assertEquals(Color(0xFF059669), defaultLightColors.successDark)
        assertEquals(Color(0xFFF6F3F2), defaultLightColors.background)
        assertEquals(Color(0xFFFFFFFF), defaultLightColors.container)
        assertEquals(Color(0xFFE5E0DC), defaultLightColors.border)
        assertEquals(Color(0xFF1C1B1B), defaultLightColors.shadePrimary)
        assertEquals(Color(0xFF7A7570), defaultLightColors.shadeSecondary)
        assertEquals(Color(0xFFDC2626), defaultLightColors.error)
        assertEquals(Color(0xFF0284C7), defaultLightColors.telemetry)
    }

    @Test
    fun pulseBottomNavTabs_matchFiveApprovedDestinations() {
        assertEquals(
            listOf("Workouts", "Progress", "Home", "History", "Profile"),
            PulseTab.entries.map { it.label },
        )
    }

    @Test
    fun pulseBluetoothChipStatuses_coverAllFiveTelemetryStates() {
        assertEquals(5, PulseBluetoothChipStatus.entries.size)
    }

    @Test
    fun setRowStatuses_coverUnstartedInProgressAndCompleted() {
        assertEquals(3, SetRowStatus.entries.size)
    }
}
