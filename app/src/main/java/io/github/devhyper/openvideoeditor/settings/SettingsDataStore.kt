package io.github.devhyper.openvideoeditor.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class SettingsDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
        val THEME = stringPreferencesKey("theme")
    }

    fun getThemeBlocking(): String {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            preferences[THEME] ?: "System"
        }
    }

    fun getThemeAsync(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[THEME] ?: "System"
            }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME] = value
        }
    }
}
