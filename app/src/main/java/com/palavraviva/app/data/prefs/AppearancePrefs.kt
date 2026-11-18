package com.palavraviva.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceStore by preferencesDataStore(name = "appearance")

/**
 * Preferência global de aparência do app.
 *
 * [AUTO] segue o modo escuro do sistema (claro de dia, escuro à noite);
 * os demais valores fixam o tema manualmente.
 */
enum class AppearanceMode(val label: String) {
    AUTO("Automático"),
    LIGHT("Claro"),
    SEPIA("Sépia"),
    DARK("Escuro");

    companion object {
        /** Tolerante a valores desconhecidos/ausentes: cai em [AUTO]. */
        fun fromName(name: String?): AppearanceMode =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}

/** Leitura/escrita da preferência de aparência via DataStore. */
object AppearancePrefs {
    private val KEY_APPEARANCE = stringPreferencesKey("appearance_mode")

    fun mode(context: Context): Flow<AppearanceMode> =
        context.appearanceStore.data.map { prefs ->
            AppearanceMode.fromName(prefs[KEY_APPEARANCE])
        }

    suspend fun setMode(context: Context, mode: AppearanceMode) {
        context.appearanceStore.edit { prefs ->
            prefs[KEY_APPEARANCE] = mode.name
        }
    }
}
