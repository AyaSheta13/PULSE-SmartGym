package com.ayasheta.pulsesmartgym.core.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.pulsePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pulse_user_preferences",
)

class PulsePreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val unit = prefs[KEY_WEIGHT_UNIT]?.let { raw ->
                runCatching { WeightUnit.valueOf(raw) }.getOrNull()
            } ?: WeightUnit.KG
            UserPreferences(
                preferredWeightUnit = unit,
                onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
                defaultRestSeconds = prefs[KEY_DEFAULT_REST_SECONDS] ?: 90,
            )
        }

    suspend fun updatePreferredWeightUnit(unit: WeightUnit) {
        dataStore.edit { prefs ->
            prefs[KEY_WEIGHT_UNIT] = unit.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateDefaultRestSeconds(seconds: Int) {
        require(seconds >= 0) { "Rest seconds cannot be negative: $seconds" }
        dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_REST_SECONDS] = seconds
        }
    }

    private companion object {
        val KEY_WEIGHT_UNIT = stringPreferencesKey("preferred_weight_unit")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
    }
}
