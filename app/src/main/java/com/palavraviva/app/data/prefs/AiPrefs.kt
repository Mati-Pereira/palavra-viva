package com.palavraviva.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiStudyStore by preferencesDataStore(name = "ai_study")

/** Provedores de IA suportados pelo assistente de estudo. */
enum class AiProvider(
    val label: String,
    val defaultModel: String,
    val fallbackModels: List<String>,
    val keyUrl: String,
    val keyUrlLabel: String,
    val keyHint: String,
) {
    GEMINI(
        label = "Gemini",
        defaultModel = "gemini-4-hydrogen",
        fallbackModels = listOf(
            "gemini-4-hydrogen",
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite",
            "gemini-4-neon",
        ),
        keyUrl = "https://aistudio.google.com/apikey",
        keyUrlLabel = "Gerar chave no Google AI Studio (aistudio.google.com/apikey)",
        keyHint = "Cole sua chave do Google AI Studio",
    ),
    GROQ(
        label = "Groq",
        defaultModel = "llama-3.3-70b-versatile",
        fallbackModels = listOf(
            "llama-3.3-70b-versatile",
            "qwen/qwen3-32b",
            "llama-3.1-8b-instant",
        ),
        keyUrl = "https://console.groq.com/keys",
        keyUrlLabel = "Gerar chave gratuita no Groq Console (console.groq.com/keys)",
        keyHint = "Cole sua chave do Groq (gsk_...)",
    ),
    OPENROUTER(
        label = "OpenRouter",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free",
        fallbackModels = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "qwen/qwen3-32b:free",
            "google/gemini-2.5-flash",
        ),
        keyUrl = "https://openrouter.ai/keys",
        keyUrlLabel = "Gerar chave no OpenRouter (openrouter.ai/keys)",
        keyHint = "Cole sua chave do OpenRouter (sk-or-...)",
    );

    companion object {
        fun fromName(name: String?): AiProvider =
            entries.firstOrNull { it.name == name } ?: GEMINI
    }
}

/**
 * Preferências do assistente de estudo bíblico: provedor ativo, chave de API,
 * modelo escolhido e lista de modelos obtidos dinamicamente por provedor.
 */
object AiPrefs {
    const val DEFAULT_MODEL = "gemini-4-hydrogen"
    val FALLBACK_MODELS = AiProvider.GEMINI.fallbackModels

    private val KEY_PROVIDER = stringPreferencesKey("ai_provider")

    private fun apiKeyPref(provider: AiProvider): Preferences.Key<String> = when (provider) {
        AiProvider.GEMINI -> stringPreferencesKey("gemini_api_key")
        else -> stringPreferencesKey("${provider.name.lowercase()}_api_key")
    }

    private fun modelPref(provider: AiProvider): Preferences.Key<String> = when (provider) {
        AiProvider.GEMINI -> stringPreferencesKey("gemini_model")
        else -> stringPreferencesKey("${provider.name.lowercase()}_model")
    }

    private fun availableModelsPref(provider: AiProvider): Preferences.Key<String> = when (provider) {
        AiProvider.GEMINI -> stringPreferencesKey("gemini_available_models")
        else -> stringPreferencesKey("${provider.name.lowercase()}_available_models")
    }

    fun provider(context: Context): Flow<AiProvider> =
        context.aiStudyStore.data.map { prefs -> AiProvider.fromName(prefs[KEY_PROVIDER]) }

    suspend fun setProvider(context: Context, provider: AiProvider) {
        context.aiStudyStore.edit { prefs ->
            prefs[KEY_PROVIDER] = provider.name
        }
    }

    fun apiKey(context: Context, provider: AiProvider = AiProvider.GEMINI): Flow<String> =
        context.aiStudyStore.data.map { prefs -> prefs[apiKeyPref(provider)].orEmpty() }

    suspend fun setApiKey(context: Context, provider: AiProvider, key: String) {
        context.aiStudyStore.edit { prefs ->
            val trimmed = key.trim()
            val prefKey = apiKeyPref(provider)
            if (trimmed.isEmpty()) prefs.remove(prefKey)
            else prefs[prefKey] = trimmed
        }
    }

    suspend fun setApiKey(context: Context, key: String) =
        setApiKey(context, AiProvider.GEMINI, key)

    fun model(context: Context, provider: AiProvider = AiProvider.GEMINI): Flow<String> =
        context.aiStudyStore.data.map { prefs ->
            prefs[modelPref(provider)]?.takeIf { it.isNotBlank() } ?: provider.defaultModel
        }

    suspend fun setModel(context: Context, provider: AiProvider, model: String) {
        val cleaned = model.trim()
        if (cleaned.isEmpty()) return
        context.aiStudyStore.edit { prefs ->
            prefs[modelPref(provider)] = cleaned
        }
    }

    suspend fun setModel(context: Context, model: String) =
        setModel(context, AiProvider.GEMINI, model)

    fun availableModels(context: Context, provider: AiProvider = AiProvider.GEMINI): Flow<List<String>> =
        context.aiStudyStore.data.map { prefs ->
            val saved = prefs[availableModelsPref(provider)]
                ?.split('\n')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            saved.ifEmpty { provider.fallbackModels }
        }

    suspend fun setAvailableModels(context: Context, provider: AiProvider, models: List<String>) {
        val cleaned = models.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        context.aiStudyStore.edit { prefs ->
            val prefKey = availableModelsPref(provider)
            if (cleaned.isEmpty()) prefs.remove(prefKey)
            else prefs[prefKey] = cleaned.joinToString("\n")
        }
    }

    suspend fun setAvailableModels(context: Context, models: List<String>) =
        setAvailableModels(context, AiProvider.GEMINI, models)
}
