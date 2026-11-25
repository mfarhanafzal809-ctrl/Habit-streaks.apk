package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Thread-safe preferences manager for secure key-value app settings.
 * Manages user identity, reactive state flows, and persistence.
 */
class PreferencesManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "habit_streaks_secure_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val DEFAULT_USER_NAME = "Champion"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _userNameFlow = MutableStateFlow(
        prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_NAME
    )
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    /**
     * Checks if a custom username was explicitly set and stored by the user.
     */
    fun hasStoredUserName(): Boolean {
        val stored = prefs.getString(KEY_USER_NAME, null)
        return !stored.isNullOrBlank()
    }

    fun getUserName(): String {
        return _userNameFlow.value
    }

    /**
     * Updates and persists the username securely and notifies reactive observers.
     */
    fun setUserName(name: String) {
        val cleanName = name.trim().ifBlank { DEFAULT_USER_NAME }
        prefs.edit().putString(KEY_USER_NAME, cleanName).apply()
        _userNameFlow.value = cleanName
    }

    suspend fun setUserNameAsync(name: String) {
        setUserName(name)
    }
}
