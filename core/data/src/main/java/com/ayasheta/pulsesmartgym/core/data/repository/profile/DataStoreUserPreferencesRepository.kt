package com.ayasheta.pulsesmartgym.core.data.repository.profile

import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class DataStoreUserPreferencesRepository(
    private val dataSource: PulsePreferencesDataSource,
) : UserPreferencesRepository {

    override fun observePreferences(): Flow<UserPreferences> = dataSource.preferencesFlow

    override suspend fun updatePreferredWeightUnit(unit: WeightUnit) {
        dataSource.updatePreferredWeightUnit(unit)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataSource.setOnboardingCompleted(completed)
    }

    override suspend fun updateDefaultRestSeconds(seconds: Int) {
        dataSource.updateDefaultRestSeconds(seconds)
    }
}
