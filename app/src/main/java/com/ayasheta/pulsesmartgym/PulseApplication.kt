package com.ayasheta.pulsesmartgym

import android.app.Application
import androidx.annotation.VisibleForTesting
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer

class PulseApplication : Application() {
    private val defaultDataContainer: PulseDataContainer by lazy {
        PulseDataContainer(this)
    }

    @Volatile
    private var testDataContainerOverride: PulseDataContainer? = null

    val dataContainer: PulseDataContainer
        get() = testDataContainerOverride ?: defaultDataContainer

    @VisibleForTesting
    fun setTestDataContainerOverride(override: PulseDataContainer?) {
        testDataContainerOverride = override
    }
}
