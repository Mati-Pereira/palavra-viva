package com.palavraviva.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiStudyStore by preferencesDataStore(name = "ai_study")

/**
 * Preferências do assistente de estudo bíblico (chave da API Gemini, modelo
 * selecionado e catálogo de modelos obtido dinamicamente da conta).
 */
object AiPrefs {
    const val DEFAULT_MODEL = "gemini-4-hydrogen"

    val FALLBACK_MODELS = listOf(
        "gemini-4-hydrogen",
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-4-neon",
    )

    private val KEY_API_KEY = stringPreferencesKey("gemini_api_key")
    private val KEY_MODEL = stringPreferencesKey("gemini_model")
    private val KEY_AVAILABLE_MODELS = stringPreferencesKey("gemini_available_models")

    fun apiKey(context: Context): Flow<String> =
        context.aiStudyStore.data.map { prefs -> prefs[KEY_API_KEY].orEmpty() }

    suspend fun setApiKey(context: Context, key: String) {
        context.aiStudyStore.edit { prefs ->
            val trimmed = key.trim()
            if (trimmed.isEmpty()) prefs.remove(KEY_API_KEY)
            else prefs[KEY_API_KEY] = trimmed
        }
    }

    fun model(context: Context): Flow<String> =
        context.aiStudyStore.data.map { prefs ->
            prefs[KEY_MODEL]?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        }

    suspend fun setModel(context: Context, model: String) {
        val cleaned = model.trim()
        if (cleaned.isEmpty()) return
        context.aiStudyStore.edit { prefs ->
            prefs[KEY_MODEL] = cleaned
        }
    }

    fun availableModels(context: Context): Flow<List<String>> =
        context.aiStudyStore.data.map { prefs ->
            val saved = prefs[KEY_AVAILABLE_MODELS]
                ?.split('\n')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            saved.ifEmpty { FALLBACK_MODELS }
        }

    suspend fun setAvailableModels(context: Context, models: List<String>) {
        val cleaned = models.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        context.aiStudyStore.edit { prefs ->
            if (cleaned.isEmpty()) prefs.remove(KEY_AVAILABLE_MODELS)
            else prefs[KEY_AVAILABLE_MODELS] = cleaned.joinToString("\n")
        }
    }
}
