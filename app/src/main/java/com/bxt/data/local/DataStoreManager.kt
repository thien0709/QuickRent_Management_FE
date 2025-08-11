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
        val USER_ID = longPreferencesKey("user_id")
    }

    // Sử dụng Flow cho userId
    val userId: Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[USER_ID] }

    // Lấy userId hiện tại (suspend function)
    suspend fun getUserId(): Long? {
        return userId.first()
    }

    suspend fun saveUserId(id: Long) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = id
        }
    }

    suspend fun clearUserId() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID)
        }
    }

    // Các hàm tương tự cho token, dark mode, isFirstTime, ...
    val accessToken: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[TOKEN_KEY] }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[IS_LOGGED_IN_KEY] = true
        }
    }

    val refreshToken: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[REFRESH_TOKEN_KEY] }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[REFRESH_TOKEN_KEY] = token
        }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> !prefs[TOKEN_KEY].isNullOrEmpty() }

    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }

    val isFirstTimeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_FIRST_TIME_KEY] ?: true }

    suspend fun isFirstTime(): Boolean = isFirstTimeFlow.first()

    suspend fun setFirstTimeCompleted() {
        context.dataStore.edit { prefs ->
            prefs[IS_FIRST_TIME_KEY] = false
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.clear()
            it[IS_LOGGED_IN_KEY] = false
            it[IS_FIRST_TIME_KEY] = false
        }
    }
}
