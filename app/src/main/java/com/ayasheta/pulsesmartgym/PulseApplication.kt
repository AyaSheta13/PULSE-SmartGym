package com.ayasheta.pulsesmartgym

import android.app.Application
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer

class PulseApplication : Application() {
    val dataContainer: PulseDataContainer by lazy {
        PulseDataContainer(this)
    }
}
