package com.palavraviva.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderStore by preferencesDataStore(name = "reminder")

/** Preferência do lembrete diário (ligado por padrão). */
object ReminderPrefs {
    private val KEY_ENABLED = booleanPreferencesKey("daily_reminder_enabled")

    fun enabled(context: Context): Flow<Boolean> =
        context.reminderStore.data.map { prefs -> prefs[KEY_ENABLED] ?: true }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.reminderStore.edit { prefs -> prefs[KEY_ENABLED] = enabled }
    }
}
