package com.palavraviva.app.data.ai

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cliente HTTP leve para a API REST do Gemini (`generativelanguage.googleapis.com`),
 * usando [HttpURLConnection] e `org.json` sem dependências externas de rede.
 */
class GeminiStudyClient {

    /**
     * Lista os modelos disponíveis para a chave informada que suportam `generateContent`,
     * ordenados priorizando a família Flash/Hydrogen mais recente.
     */
    suspend fun listAvailableModels(apiKey: String): List<GeminiModelInfo> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) throw AiError.MissingApiKey()

        val encodedKey = URLEncoder.encode(key, "UTF-8")
        val url = "$BASE_URL/models?key=$encodedKey"
        val responseBody = executeGet(url)
        val root = runCatching { JSONObject(responseBody) }.getOrNull() ?: throw AiError.ParseError()
        val array = root.optJSONArray("models") ?: JSONArray()

        val models = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val fullName = item.optString("name", "")
                val id = fullName.removePrefix("models/").trim()
                if (id.isEmpty() || !id.contains("gemini", ignoreCase = true)) continue
                // Filtra modelos especializados de áudio/imagem/embedding que não servem para estudo em texto
                if (id.contains("embedding", ignoreCase = true) ||
                    id.contains("imagen", ignoreCase = true) ||
                    id.contains("tts", ignoreCase = true) ||
                    id.contains("image", ignoreCase = true)
                ) {
                    continue
                }
                val methods = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                var supportsGenerate = false
                for (j in 0 until methods.length()) {
                    if (methods.optString(j) == "generateContent") {
                        supportsGenerate = true
                        break
                    }
                }
                if (!supportsGenerate) continue

                val display = item.optString("displayName", id).ifBlank { id }
                add(GeminiModelInfo(id = id, displayName = display))
            }
        }

        if (models.isEmpty()) {
            throw AiError.ApiError("Nenhum modelo Gemini compatível encontrado para esta chave.")
        }

        models.sortedWith(compareBy<GeminiModelInfo> { modelPriority(it.id) }.thenBy { it.id })
    }

    /**
     * Gera conteúdo estruturado em JSON usando `responseMimeType = "application/json"`.
     */
    suspend fun generateStructuredJson(
        apiKey: String,
        model: String,
        systemInstruction: String,
        userPrompt: String,
    ): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction)),
                ),
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", userPrompt))),
                ),
            )
            put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("temperature", 0.3),
            )
        }
        callGenerateContent(apiKey, model, payload)
    }

    /**
     * Gera resposta textual conversacional enviando histórico da passagem e a nova pergunta.
     */
    suspend fun generateChatReply(
        apiKey: String,
        model: String,
        systemInstruction: String,
        contextPrefix: String,
        history: List<Pair<String, String>>,
        userMessage: String,
    ): String = withContext(Dispatchers.IO) {
        val contents = JSONArray()

        val firstUserIdx = history.indexOfFirst { it.first != "model" }
        val sanitizedHistory = if (firstUserIdx >= 0) history.drop(firstUserIdx) else emptyList()
        sanitizedHistory.forEach { (role, text) ->
            contents.put(
                JSONObject()
                    .put("role", if (role == "model") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", text))),
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userMessage))),
        )

        val payload = JSONObject().apply {
            put(
                "system_instruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put("text", "$systemInstruction\n\n$contextPrefix"),
                    ),
                ),
            )
            put("contents", contents)
            put(
                "generationConfig",
                JSONObject().put("temperature", 0.4),
            )
        }
        callGenerateContent(apiKey, model, payload)
    }

    private fun callGenerateContent(
        apiKey: String,
        model: String,
        body: JSONObject,
    ): String {
        val key = apiKey.trim()
        if (key.isEmpty()) throw AiError.MissingApiKey()
        val cleanModel = model.trim().removePrefix("models/").ifEmpty { "gemini-4-hydrogen" }
        val encodedModel = URLEncoder.encode(cleanModel, "UTF-8")
        val encodedKey = URLEncoder.encode(key, "UTF-8")
        val url = "$BASE_URL/models/$encodedModel:generateContent?key=$encodedKey"

        val responseBody = executePost(url, body.toString())
        return extractCandidateText(responseBody)
    }

    private fun extractCandidateText(responseBody: String): String {
        val root = runCatching { JSONObject(responseBody) }.getOrNull() ?: throw AiError.ParseError()
        val candidates = root.optJSONArray("candidates") ?: throw AiError.ParseError()
        if (candidates.length() == 0) {
            val blockReason = root.optJSONObject("promptFeedback")?.optString("blockReason", "")
            if (!blockReason.isNullOrBlank()) {
                throw AiError.ApiError("A resposta foi bloqueada pelos filtros da API ($blockReason).")
            }
            throw AiError.ParseError()
        }
        val first = candidates.optJSONObject(0) ?: throw AiError.ParseError()
        val content = first.optJSONObject("content") ?: throw AiError.ParseError()
        val parts = content.optJSONArray("parts") ?: throw AiError.ParseError()
        val builder = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val text = part.optString("text", "")
            if (text.isNotEmpty()) builder.append(text)
        }
        val result = builder.toString().trim()
        if (result.isEmpty()) throw AiError.ParseError()
        return result
    }

    private fun executeGet(urlString: String): String = executeRequest("GET", urlString, null)

    private fun executePost(urlString: String, jsonBody: String): String =
        executeRequest("POST", urlString, jsonBody)

    private fun executeRequest(method: String, urlString: String, jsonBody: String?): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
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
                throw mapHttpError(code, body)
            }
        } catch (e: AiError) {
            throw e
        } catch (_: IOException) {
            throw AiError.NetworkError()
        } finally {
            conn.disconnect()
        }
    }

    private fun mapHttpError(code: Int, errorBody: String): AiError {
        val apiMessage = runCatching {
            JSONObject(errorBody).optJSONObject("error")?.optString("message", "")
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return when (code) {
            400 -> {
                if (apiMessage?.contains("API key", ignoreCase = true) == true ||
                    apiMessage?.contains("API_KEY", ignoreCase = true) == true
                ) {
                    AiError.InvalidApiKey(apiMessage)
                } else {
                    AiError.ApiError(apiMessage ?: "Requisição inválida (HTTP 400).")
                }
            }
            401, 403 -> AiError.InvalidApiKey(apiMessage)
            429 -> AiError.RateLimited()
            else -> AiError.ApiError(apiMessage ?: "Erro na API do Gemini (HTTP $code).")
        }
    }

    private fun modelPriority(id: String): Int = when {
        id == "gemini-4-hydrogen" -> 0
        id.contains("4-hydrogen") -> 1
        id.contains("3.8-flash") -> 2
        id.contains("3.5-flash") && !id.contains("lite") -> 3
        id.contains("4-neon") -> 4
        id.contains("flash-lite") -> 5
        id.contains("flash") -> 6
        id.contains("4-argon") -> 7
        id.contains("4-carbon") -> 8
        else -> 9
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 45_000
    }
}
