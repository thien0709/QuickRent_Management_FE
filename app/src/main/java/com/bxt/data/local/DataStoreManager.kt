package com.bxt.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val DATASTORE_NAME = "app_preferences"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Authentication
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        val USER_ID = longPreferencesKey("user_id")

        // App settings
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val IS_FIRST_TIME_KEY = booleanPreferencesKey("is_first_time")

        // Location
        val CURRENT_LAT_KEY = doublePreferencesKey("current_latitude")
        val CURRENT_LNG_KEY = doublePreferencesKey("current_longitude")
        val CURRENT_ADDRESS_KEY = stringPreferencesKey("current_address")
        val PENDING_LAT_KEY = doublePreferencesKey("pending_latitude")
        val PENDING_LNG_KEY = doublePreferencesKey("pending_longitude")
    }

    // Authentication
    val accessToken: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { !it[TOKEN_KEY].isNullOrEmpty() }
    val userId: Flow<Long?> = context.dataStore.data.map { it[USER_ID] }

    suspend fun saveAuthData(token: String, refreshToken: String, userId: Long) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            it[REFRESH_TOKEN_KEY] = refreshToken
            it[USER_ID] = userId
            it[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit {
            it.remove(TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
            it.remove(USER_ID)
            it[IS_LOGGED_IN_KEY] = false
        }
    }

    // App settings
    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE_KEY] ?: false }
    val isFirstTime: Flow<Boolean> = context.dataStore.data.map { it[IS_FIRST_TIME_KEY] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    suspend fun setFirstTimeCompleted() {
        context.dataStore.edit { it[IS_FIRST_TIME_KEY] = false }
    }

    // Location
    val currentLocation: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        prefs[CURRENT_LAT_KEY]?.let { lat ->
            prefs[CURRENT_LNG_KEY]?.let { lng -> Pair(lat, lng) }
        }
    }

    val currentAddress: Flow<String?> = context.dataStore.data.map { it[CURRENT_ADDRESS_KEY] }

    val pendingLocation: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        prefs[PENDING_LAT_KEY]?.let { lat ->
            prefs[PENDING_LNG_KEY]?.let { lng -> Pair(lat, lng) }
        }
    }

    val hasPendingLocation: Flow<Boolean> = pendingLocation.map { it != null }

    suspend fun saveCurrentLocation(lat: Double, lng: Double, address: String? = null) {
        context.dataStore.edit {
            it[CURRENT_LAT_KEY] = lat
            it[CURRENT_LNG_KEY] = lng
            address?.let { addr -> it[CURRENT_ADDRESS_KEY] = addr }
        }
    }

    suspend fun savePendingLocation(lat: Double, lng: Double) {
        context.dataStore.edit {
            it[PENDING_LAT_KEY] = lat
            it[PENDING_LNG_KEY] = lng
        }
    }

    suspend fun clearPendingLocation() {
        context.dataStore.edit {
            it.remove(PENDING_LAT_KEY)
            it.remove(PENDING_LNG_KEY)
        }
    }

    suspend fun getPendingLocation(): Pair<Double, Double>? {
        val prefs = context.dataStore.data.first()
        return prefs[PENDING_LAT_KEY]?.let { lat ->
            prefs[PENDING_LNG_KEY]?.let { lng -> Pair(lat, lng) }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}