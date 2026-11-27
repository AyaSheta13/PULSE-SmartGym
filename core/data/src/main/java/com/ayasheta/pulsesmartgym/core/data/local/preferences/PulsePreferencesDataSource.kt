package com.ayasheta.pulsesmartgym.core.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

val Context.pulsePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pulse_user_preferences",
)

enum class PersistedSessionKind {
    NONE,
    GUEST,
    LOCAL_ACCOUNT,
}

data class PersistedSessionSnapshot(
    val kind: PersistedSessionKind = PersistedSessionKind.NONE,
    val ownerId: String? = null,
    val rememberMe: Boolean = false,
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

    suspend fun getOrCreateGuestOwnerId(): String {
        val current = dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()[KEY_GUEST_OWNER_ID]
        if (!current.isNullOrBlank()) return current

        val generated = "guest_${UUID.randomUUID()}"
        var resolved = generated
        dataStore.edit { prefs ->
            val existing = prefs[KEY_GUEST_OWNER_ID]
            if (existing.isNullOrBlank()) {
                prefs[KEY_GUEST_OWNER_ID] = generated
                resolved = generated
            } else {
                resolved = existing
            }
        }
        return resolved
    }

    suspend fun readPersistedSession(): PersistedSessionSnapshot {
        val prefs = dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        val kind = prefs[KEY_SESSION_KIND]?.let { raw ->
            runCatching { PersistedSessionKind.valueOf(raw) }.getOrNull()
        } ?: PersistedSessionKind.NONE
        return PersistedSessionSnapshot(
            kind = kind,
            ownerId = prefs[KEY_SESSION_OWNER_ID],
            rememberMe = prefs[KEY_REMEMBER_ME] ?: false,
        )
    }

    suspend fun setActiveSession(
        kind: PersistedSessionKind,
        ownerId: String?,
        rememberMe: Boolean,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_SESSION_KIND] = kind.name
            if (ownerId != null) {
                prefs[KEY_SESSION_OWNER_ID] = ownerId
            } else {
                prefs.remove(KEY_SESSION_OWNER_ID)
            }
            prefs[KEY_REMEMBER_ME] = rememberMe
        }
    }

    suspend fun clearActiveSession() {
        dataStore.edit { prefs ->
            prefs[KEY_SESSION_KIND] = PersistedSessionKind.NONE.name
            prefs.remove(KEY_SESSION_OWNER_ID)
            prefs[KEY_REMEMBER_ME] = false
        }
    }

    /**
     * Reads whether default starter plans were already seeded for [ownerId].
     * Returns `null` on preference read failure so callers never mistake an I/O error
     * for "not seeded" and re-inject deleted starter plans.
     */
    suspend fun readDefaultPlansSeededState(ownerId: String): Boolean? {
        return runCatching {
            val prefs = dataStore.data.first()
            ownerId in (prefs[KEY_SEEDED_DEFAULT_PLAN_OWNERS] ?: emptySet())
        }.getOrNull()
    }

    suspend fun markDefaultPlansSeeded(ownerId: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_SEEDED_DEFAULT_PLAN_OWNERS] ?: emptySet()
            prefs[KEY_SEEDED_DEFAULT_PLAN_OWNERS] = current + ownerId
        }
    }

    private companion object {
        val KEY_WEIGHT_UNIT = stringPreferencesKey("preferred_weight_unit")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val KEY_GUEST_OWNER_ID = stringPreferencesKey("guest_owner_id")
        val KEY_SESSION_KIND = stringPreferencesKey("active_session_kind")
        val KEY_SESSION_OWNER_ID = stringPreferencesKey("active_session_owner_id")
        val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        val KEY_SEEDED_DEFAULT_PLAN_OWNERS = stringSetPreferencesKey("seeded_default_plan_owners")
    }
}
