package com.palavraviva.app.data.repositories

import android.content.Context
import com.palavraviva.app.data.ai.AiError
import com.palavraviva.app.data.ai.ChapterGuide
import com.palavraviva.app.data.ai.GeminiModelInfo
import com.palavraviva.app.data.ai.GeminiStudyClient
import com.palavraviva.app.data.ai.StudyJsonParser
import com.palavraviva.app.data.ai.StudyPrompts
import com.palavraviva.app.data.ai.VerseStudy
import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.Book
import com.palavraviva.app.data.local.GlossaryTerm
import com.palavraviva.app.data.local.StudyCache
import com.palavraviva.app.data.local.StudyChatMessage
import com.palavraviva.app.data.local.Verse
import com.palavraviva.app.data.prefs.AiPrefs
import com.palavraviva.app.data.study.GlossaryMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Resultado de estudo com indicação de origem (cache offline vs. geração online). */
data class StudyResult<T>(
    val data: T,
    val fromCache: Boolean,
    val model: String,
)

/**
 * Orquestra o estudo bíblico: cache offline no Room, glossário embutido,
 * chat contextual persistido e chamadas à API do Gemini quando necessário.
 */
class StudyRepository(
    private val context: Context,
    database: AppDatabase,
    private val client: GeminiStudyClient = GeminiStudyClient(),
) {

    private val cacheDao = database.studyCacheDao()
    private val chatDao = database.studyChatDao()
    private val glossaryDao = database.glossaryDao()

    @Volatile
    private var glossarySnapshot: List<GlossaryTerm>? = null

    fun verseCacheKey(bookId: String, chapter: Int, verse: Int): String =
        "verse:$bookId:$chapter:$verse"

    fun chapterCacheKey(bookId: String, chapter: Int): String =
        "chapter:$bookId:$chapter"

    fun observeCachedKeysForChapter(bookId: String, chapter: Int): Flow<Set<String>> =
        cacheDao.observeKeysForChapter(bookId, chapter).map { it.toSet() }

    fun observeAllCachedStudies(): Flow<List<StudyCache>> = cacheDao.observeAll()

    suspend fun getVerseStudy(
        book: Book,
        chapter: Int,
        verse: Verse,
        chapterVerses: List<Verse>,
        forceRefresh: Boolean = false,
    ): Result<StudyResult<VerseStudy>> {
        val key = verseCacheKey(book.bookId, chapter, verse.verse)
        if (!forceRefresh) {
            val cached = cacheDao.getByKey(key)
            if (cached != null) {
                val parsed = runCatching { StudyJsonParser.parseVerseStudy(cached.payloadJson) }.getOrNull()
                if (parsed != null) {
                    return Result.success(StudyResult(parsed, fromCache = true, model = cached.model))
                }
            }
        }

        val apiKey = AiPrefs.apiKey(context).first()
        if (apiKey.isBlank()) {
            return Result.failure(AiError.MissingApiKey())
        }
        val model = AiPrefs.model(context).first()

        val surrounding = buildSurroundingExcerpt(chapterVerses, verse.verse)
        val prompt = StudyPrompts.buildVerseStudyPrompt(
            bookName = book.name,
            bookId = book.bookId,
            chapter = chapter,
            verseNumber = verse.verse,
            verseText = verse.text,
            surroundingVerses = surrounding,
        )

        return runCatching {
            val rawJson = client.generateStructuredJson(
                apiKey = apiKey,
                model = model,
                systemInstruction = StudyPrompts.SYSTEM_INSTRUCTION,
                userPrompt = prompt,
            )
            val parsed = StudyJsonParser.parseVerseStudy(rawJson)
            val canonicalJson = StudyJsonParser.serializeVerseStudy(parsed)
            cacheDao.upsert(
                StudyCache(
                    cacheKey = key,
                    kind = KIND_VERSE,
                    bookId = book.bookId,
                    chapter = chapter,
                    verse = verse.verse,
                    title = "${book.name} $chapter:${verse.verse}",
                    payloadJson = canonicalJson,
                    model = model,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            StudyResult(parsed, fromCache = false, model = model)
        }
    }

    suspend fun getChapterGuide(
        book: Book,
        chapter: Int,
        chapterVerses: List<Verse>,
        forceRefresh: Boolean = false,
    ): Result<StudyResult<ChapterGuide>> {
        val key = chapterCacheKey(book.bookId, chapter)
        if (!forceRefresh) {
            val cached = cacheDao.getByKey(key)
            if (cached != null) {
                val parsed = runCatching { StudyJsonParser.parseChapterGuide(cached.payloadJson) }.getOrNull()
                if (parsed != null) {
                    return Result.success(StudyResult(parsed, fromCache = true, model = cached.model))
                }
            }
        }

        val apiKey = AiPrefs.apiKey(context).first()
        if (apiKey.isBlank()) {
            return Result.failure(AiError.MissingApiKey())
        }
        val model = AiPrefs.model(context).first()

        val chapterText = buildChapterExcerpt(chapterVerses, maxChars = 14_000)
        val prompt = StudyPrompts.buildChapterGuidePrompt(
            bookName = book.name,
            bookId = book.bookId,
            chapter = chapter,
            chapterText = chapterText,
        )

        return runCatching {
            val rawJson = client.generateStructuredJson(
                apiKey = apiKey,
                model = model,
                systemInstruction = StudyPrompts.SYSTEM_INSTRUCTION,
                userPrompt = prompt,
            )
            val parsed = StudyJsonParser.parseChapterGuide(rawJson)
            val canonicalJson = StudyJsonParser.serializeChapterGuide(parsed)
            cacheDao.upsert(
                StudyCache(
                    cacheKey = key,
                    kind = KIND_CHAPTER,
                    bookId = book.bookId,
                    chapter = chapter,
                    verse = null,
                    title = "${book.name} $chapter",
                    payloadJson = canonicalJson,
                    model = model,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            StudyResult(parsed, fromCache = false, model = model)
        }
    }

    fun observeChat(scopeKey: String): Flow<List<StudyChatMessage>> =
        chatDao.observeMessages(scopeKey)

    suspend fun sendChatMessage(
        scopeKey: String,
        contextPrefix: String,
        userMessage: String,
    ): Result<Unit> {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty()) return Result.success(Unit)

        val apiKey = AiPrefs.apiKey(context).first()
        if (apiKey.isBlank()) {
            return Result.failure(AiError.MissingApiKey())
        }
        val model = AiPrefs.model(context).first()

        val priorHistory = chatDao.messagesOnce(scopeKey).takeLast(12).map { it.role to it.text }
        val userTime = System.currentTimeMillis()
        val replyResult = runCatching {
            client.generateChatReply(
                apiKey = apiKey,
                model = model,
                systemInstruction = StudyPrompts.SYSTEM_INSTRUCTION,
                contextPrefix = contextPrefix,
                history = priorHistory,
                userMessage = trimmed,
            )
        }

        return replyResult.map { replyText ->
            chatDao.insert(
                StudyChatMessage(
                    scopeKey = scopeKey,
                    role = "user",
                    text = trimmed,
                    createdAt = userTime,
                ),
            )
            chatDao.insert(
                StudyChatMessage(
                    scopeKey = scopeKey,
                    role = "model",
                    text = replyText,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun clearChat(scopeKey: String) {
        chatDao.clearScope(scopeKey)
    }

    suspend fun findGlossaryTermsInText(text: String): List<GlossaryTerm> {
        val terms = glossarySnapshot?.takeIf { it.isNotEmpty() }
            ?: glossaryDao.allOnce().also { if (it.isNotEmpty()) glossarySnapshot = it }
        return GlossaryMatcher.findMatches(text, terms)
    }

    fun observeGlossary(query: String): Flow<List<GlossaryTerm>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            glossaryDao.observeAll()
        } else {
            glossaryDao.search("%$trimmed%")
        }
    }

    /**
     * Valida a chave informada consultando os modelos disponíveis na conta e
     * atualiza o catálogo salvo em [AiPrefs].
     */
    suspend fun testAndFetchModels(apiKey: String): Result<List<GeminiModelInfo>> = runCatching {
        val models = client.listAvailableModels(apiKey)
        val ids = models.map { it.id }
        AiPrefs.setApiKey(context, apiKey)
        AiPrefs.setAvailableModels(context, ids)

        val currentModel = AiPrefs.model(context).first()
        if (currentModel !in ids && ids.isNotEmpty()) {
            AiPrefs.setModel(context, ids.first())
        }
        models
    }

    suspend fun hasApiKey(): Boolean = AiPrefs.apiKey(context).first().isNotBlank()

    fun observeHasApiKey(): Flow<Boolean> = AiPrefs.apiKey(context).map { it.isNotBlank() }

    private fun buildSurroundingExcerpt(verses: List<Verse>, targetVerse: Int): String {
        val window = verses.filter { it.verse in (targetVerse - 3)..(targetVerse + 3) }
        return window.joinToString("\n") { "${it.verse}. ${it.text}" }
    }

    private fun buildChapterExcerpt(verses: List<Verse>, maxChars: Int): String {
        val sb = StringBuilder()
        for (v in verses) {
            val line = "${v.verse}. ${v.text}\n"
            if (sb.length + line.length > maxChars) {
                sb.append("... (capítulo resumido por tamanho)")
                break
            }
            sb.append(line)
        }
        return sb.toString().trim()
    }

    companion object {
        const val KIND_VERSE = "VERSE"
        const val KIND_CHAPTER = "CHAPTER"
    }
}
