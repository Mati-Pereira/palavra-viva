package com.palavraviva.app.data.bible

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.Book
import com.palavraviva.app.data.local.Verse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** DataStore privado da semente (independente de data/prefs). */
private val Context.seedStore by preferencesDataStore(name = "bible_seed")

/**
 * Importador do primeiro início: lê TODOS os arquivos JSON de `assets/bible/` disponíveis
 * e popula [Book]/[Verse] no Room, semeando em seguida os planos de leitura.
 *
 * - A lista de assets é dinâmica: se nenhum arquivo existir ainda, a semente
 *   simplesmente não roda e será tentada novamente no próximo início.
 * - Livros já importados são pulados (verificação por contagem de versículos),
 *   então a execução é idempotente mesmo sem a flag.
 * - A flag DataStore `seed_done` evita retrabalho nos inícios seguintes.
 */
object BibleImporter {

    private val KEY_SEED_DONE = booleanPreferencesKey("seed_done")

    /**
     * Ordem canônica dos livros por abreviação (minúsculas), usada como
     * fallback quando o JSON não traz `bookOrder`.
     */
    private val CANONICAL_ORDER = listOf(
        // Antigo Testamento
        "gn", "ex", "lv", "nm", "dt", "js", "jz", "rt", "1sm", "2sm",
        "1rs", "2rs", "1cr", "2cr", "ed", "ne", "et", "job", "sl", "pv",
        "ec", "ct", "is", "jr", "lm", "ez", "dn", "os", "jl", "am",
        "ob", "jn", "mq", "na", "hc", "sf", "ag", "zc", "ml",
        // Novo Testamento
        "mt", "mc", "lc", "jo", "at", "rm", "1co", "2co", "gl", "ef",
        "fp", "cl", "1ts", "2ts", "1tm", "2tm", "tt", "fm", "hb", "tg",
        "1pe", "2pe", "1jo", "2jo", "3jo", "jd", "ap",
    )

    suspend fun seed(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val seedDone = context.seedStore.data.first()[KEY_SEED_DONE] == true
            if (seedDone && database.bookDao().count() > 0) return@withContext

            importAssets(context, database)

            // Só semeia planos e marca a flag quando há livros no banco;
            // sem assets, o próximo início tentará de novo.
            if (database.bookDao().count() > 0) {
                PlanSeeder.seedPlans(database)
                context.seedStore.edit { it[KEY_SEED_DONE] = true }
            }
        }
    }

    /** Percorre os JSONs em `bible/` dinamicamente; arquivos ausentes/inválidos são ignorados. */
    private suspend fun importAssets(context: Context, database: AppDatabase) {
        val files = runCatching { context.assets.list("bible") }
            .getOrNull()
            ?.filter { it.endsWith(".json") }
            .orEmpty()
            .sorted()
        if (files.isEmpty()) return

        files.forEachIndexed { index, fileName ->
            runCatching { importBookFile(context, database, fileName, index) }
        }
    }

    /**
     * Importa um arquivo de livro.
     *
     * @param fileIndex posição do arquivo na lista ordenada — usada como
     *   `bookOrder` de fallback para abreviações fora de [CANONICAL_ORDER].
     */
    private suspend fun importBookFile(
        context: Context,
        database: AppDatabase,
        fileName: String,
        fileIndex: Int,
    ): Boolean {
        val json = context.assets.open("bible/$fileName").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val bookId = root.getString("bookId")
        if (bookId.isBlank()) return false

        // Idempotente por livro: já importado em execução anterior.
        if (database.verseDao().countForBook(bookId) > 0) return true

        val name = root.getString("name")
        val testament = root.optString("testament", "AT").uppercase()
        val bookOrder = resolveBookOrder(bookId, root.optInt("bookOrder", -1), fileIndex)
        val book = Book(bookId = bookId, name = name, testament = testament, bookOrder = bookOrder)

        val chapters = root.getJSONArray("chapters")
        val verses = ArrayList<Verse>(chapters.length() * 25)
        for (i in 0 until chapters.length()) {
            val chapterJson = chapters.getJSONObject(i)
            val chapter = chapterJson.getInt("number")
            val verseArray = chapterJson.getJSONArray("verses")
            for (j in 0 until verseArray.length()) {
                val verseJson = verseArray.getJSONObject(j)
                verses += Verse(
                    bookId = bookId,
                    chapter = chapter,
                    verse = verseJson.getInt("number"),
                    text = verseJson.getString("text"),
                )
            }
        }

        database.withTransaction {
            database.bookDao().insertAll(listOf(book))
            verses.chunked(500).forEach { database.verseDao().insertAll(it) }
        }
        return true
    }

    private fun resolveBookOrder(bookId: String, jsonOrder: Int, fileIndex: Int): Int =
        when {
            jsonOrder >= 0 -> jsonOrder
            else -> CANONICAL_ORDER.indexOf(bookId)
                .takeIf { it >= 0 }
                ?: (CANONICAL_ORDER.size + fileIndex)
        }
}
