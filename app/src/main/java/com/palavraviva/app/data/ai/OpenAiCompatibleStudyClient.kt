package com.palavraviva.app.data.ai

import com.palavraviva.app.data.prefs.AiProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cliente HTTP leve para APIs compatíveis com OpenAI (Groq e OpenRouter),
 * usando [HttpURLConnection] e `org.json` sem dependências externas.
 */
class OpenAiCompatibleStudyClient {

    suspend fun listAvailableModels(
        provider: AiProvider,
        apiKey: String,
    ): List<GeminiModelInfo> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) throw AiError.MissingApiKey()

        val responseBody = executeRequest(
            method = "GET",
            urlString = "${baseUrl(provider)}/models",
            provider = provider,
            apiKey = key,
            jsonBody = null,
        )
        val root = runCatching { JSONObject(responseBody) }.getOrNull() ?: throw AiError.ParseError()
        val array = root.optJSONArray("data") ?: JSONArray()

        val filtered = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id", "").trim()
                if (id.isEmpty() || !isChatStudyModel(provider, id)) continue
                val display = item.optString("name", id).ifBlank { id }
                add(GeminiModelInfo(id = id, displayName = display))
            }
        }

        if (filtered.isEmpty()) {
            throw AiError.ApiError("Nenhum modelo de texto compatível encontrado para ${provider.label}.")
        }

        val sorted = filtered.sortedWith(
            compareBy<GeminiModelInfo> { modelPriority(provider, it.id) }.thenBy { it.id },
        )
        if (provider == AiProvider.OPENROUTER) sorted.take(30) else sorted
    }

    suspend fun generateStructuredJson(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPrompt: String,
    ): String = withContext(Dispatchers.IO) {
        val cleanModel = model.trim().ifEmpty { provider.defaultModel }
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemInstruction))
            .put(JSONObject().put("role", "user").put("content", userPrompt))

        val withJsonFormat = JSONObject().apply {
            put("model", cleanModel)
            put("messages", messages)
            put("temperature", 0.3)
            put("response_format", JSONObject().put("type", "json_object"))
        }

        try {
            val raw = callChatCompletions(provider, apiKey, withJsonFormat)
            stripThinkingBlocks(raw)
        } catch (err: AiError.ApiError) {
            val msg = err.message.lowercase()
            if (msg.contains("response_format") || msg.contains("json_object")) {
                val fallbackBody = JSONObject().apply {
                    put("model", cleanModel)
                    put("messages", messages)
                    put("temperature", 0.3)
                }
                stripThinkingBlocks(callChatCompletions(provider, apiKey, fallbackBody))
            } else {
                throw err
            }
        }
    }

    suspend fun generateChatReply(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemInstruction: String,
        contextPrefix: String,
        history: List<Pair<String, String>>,
        userMessage: String,
    ): String = withContext(Dispatchers.IO) {
        val cleanModel = model.trim().ifEmpty { provider.defaultModel }
        val messages = JSONArray().put(
            JSONObject()
                .put("role", "system")
                .put("content", "$systemInstruction\n\n$contextPrefix"),
        )
        history.forEach { (role, text) ->
            messages.put(
                JSONObject()
                    .put("role", if (role == "model") "assistant" else "user")
                    .put("content", text),
            )
        }
        messages.put(
            JSONObject()
                .put("role", "user")
                .put("content", userMessage),
        )

        val payload = JSONObject().apply {
            put("model", cleanModel)
            put("messages", messages)
            put("temperature", 0.4)
        }
        stripThinkingBlocks(callChatCompletions(provider, apiKey, payload))
    }

    private fun callChatCompletions(
        provider: AiProvider,
        apiKey: String,
        body: JSONObject,
    ): String {
        val key = apiKey.trim()
        if (key.isEmpty()) throw AiError.MissingApiKey()
        val responseBody = executeRequest(
            method = "POST",
            urlString = "${baseUrl(provider)}/chat/completions",
            provider = provider,
            apiKey = key,
            jsonBody = body.toString(),
        )
        return extractAssistantText(responseBody)
    }

    private fun extractAssistantText(responseBody: String): String {
        val root = runCatching { JSONObject(responseBody) }.getOrNull() ?: throw AiError.ParseError()
        val choices = root.optJSONArray("choices") ?: throw AiError.ParseError()
        if (choices.length() == 0) throw AiError.ParseError()
        val first = choices.optJSONObject(0) ?: throw AiError.ParseError()
        val message = first.optJSONObject("message") ?: throw AiError.ParseError()
        val content = message.optString("content", "").trim()
        if (content.isEmpty()) throw AiError.ParseError()
        return content
    }

    private fun executeRequest(
        method: String,
        urlString: String,
        provider: AiProvider,
        apiKey: String,
        jsonBody: String?,
    ): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            if (provider == AiProvider.OPENROUTER) {
                setRequestProperty("HTTP-Referer", "https://github.com/Mati-Pereira/palavra-viva")
                setRequestProperty("X-Title", "Palavra Viva")
            }
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }
        return try {
            if (jsonBody != null) {
                conn.outputStream.use { out ->
                    out.write(jsonBody.toByteArray(Charsets.UTF_8))
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code in 200..299) {
                body
            } else {
                throw mapHttpError(provider, code, body)
            }
        } catch (e: AiError) {
            throw e
        } catch (_: IOException) {
            throw AiError.NetworkError()
        } finally {
            conn.disconnect()
        }
    }

    private fun mapHttpError(provider: AiProvider, code: Int, errorBody: String): AiError {
        val apiMessage = runCatching {
            JSONObject(errorBody).optJSONObject("error")?.optString("message", "")
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return when (code) {
            401, 403 -> AiError.InvalidApiKey(
                apiMessage ?: "Chave da API (${provider.label}) inválida ou sem permissão.",
            )
            429 -> AiError.RateLimited()
            else -> AiError.ApiError(
                apiMessage ?: "Erro na API (${provider.label}, HTTP $code).",
            )
        }
    }

    private fun isChatStudyModel(provider: AiProvider, id: String): Boolean {
        val lower = id.lowercase()
        if (lower.contains("whisper") ||
            lower.contains("tts") ||
            lower.contains("guard") ||
            lower.contains("safeguard") ||
            lower.contains("playai") ||
            lower.contains("orpheus") ||
            lower.contains("allam") ||
            lower.contains("embedding") ||
            lower.contains("image") ||
            lower.contains("audio") ||
            lower.contains("moderation")
        ) {
            return false
        }
        return when (provider) {
            AiProvider.GROQ -> !lower.contains("compound")
            AiProvider.OPENROUTER -> true
            AiProvider.GEMINI -> false
        }
    }

    private fun modelPriority(provider: AiProvider, id: String): Int {
        val lower = id.lowercase()
        return when (provider) {
            AiProvider.GROQ -> when {
                lower == "llama-3.3-70b-versatile" -> 0
                lower.contains("70b") -> 1
                lower.contains("qwen3-32b") -> 2
                lower.contains("gpt-oss") -> 3
                lower.contains("llama-3.1-8b") -> 4
                else -> 5
            }
            AiProvider.OPENROUTER -> when {
                lower == "meta-llama/llama-3.3-70b-instruct:free" -> 0
                lower.endsWith(":free") && lower.contains("llama") -> 1
                lower.endsWith(":free") && lower.contains("qwen") -> 2
                lower.endsWith(":free") -> 3
                lower.contains("gemini") -> 4
                else -> 5
            }
            AiProvider.GEMINI -> 9
        }
    }

    private fun baseUrl(provider: AiProvider): String = when (provider) {
        AiProvider.GROQ -> "https://api.groq.com/openai/v1"
        AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1"
        AiProvider.GEMINI -> error("Gemini usa GeminiStudyClient")
    }

    internal fun stripThinkingBlocks(raw: String): String =
        raw.replace(THINK_REGEX, "").trim()

    companion object {
        private val THINK_REGEX = Regex("(?is)<think>.*?</think>")
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 45_000
    }
}
