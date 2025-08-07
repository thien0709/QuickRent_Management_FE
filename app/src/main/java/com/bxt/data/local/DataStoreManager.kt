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
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        val IS_FIRST_TIME_KEY = booleanPreferencesKey("is_first_time")
    }

    suspend fun isFirstTime(): Boolean {
        return context.dataStore.data.first()[IS_FIRST_TIME_KEY] ?: true
    }


    suspend fun setFirstTimeCompleted() {
        context.dataStore.edit { prefs ->
            prefs[IS_FIRST_TIME_KEY] = false
        }
    }

    val isFirstTimeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_FIRST_TIME_KEY] ?: true }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[REFRESH_TOKEN_KEY] = token
        }
    }

    val refreshToken: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[REFRESH_TOKEN_KEY] }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[TOKEN_KEY] }

    suspend fun clear() {
        context.dataStore.edit {
            it.clear()
            it[IS_LOGGED_IN_KEY] = false
            it[IS_FIRST_TIME_KEY] = false
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }

    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] ?: false }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            val token = prefs[TOKEN_KEY]
            !token.isNullOrEmpty()
        }
}