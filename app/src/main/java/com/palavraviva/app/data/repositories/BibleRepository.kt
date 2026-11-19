package com.palavraviva.app.data.repositories

import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.Book
import com.palavraviva.app.data.local.Verse
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Acesso de leitura ao texto bíblico. */
class BibleRepository(private val database: AppDatabase) {

    private val bookDao = database.bookDao()
    private val verseDao = database.verseDao()

    /** Livros em ordem canônica, reativo a mudanças do banco. */
    fun books(): Flow<List<Book>> = bookDao.books()

    suspend fun book(bookId: String): Book? = bookDao.book(bookId)

    fun verses(bookId: String, chapter: Int): Flow<List<Verse>> =
        verseDao.verses(bookId, chapter)

    suspend fun versesOnce(bookId: String, chapter: Int): List<Verse> =
        verseDao.versesOnce(bookId, chapter)

    suspend fun chapterCount(bookId: String): Int = verseDao.chapterCount(bookId)

    /**
     * Versículo do dia — determinístico pela data: um hash simples do dia do
     * ano (com o ano, para variar entre anos) é usado como índice no total de
     * versículos, na ordenação canônica. Retorna null se o banco estiver vazio.
     */
    suspend fun verseOfTheDay(): Verse? {
        val total = verseDao.canonicalCount()
        if (total == 0) return null
        val today = LocalDate.now()
        val position = (today.year * 366 + today.dayOfYear) % total
        // Versículos vazios por design (ex.: At 28:29) caem no seguinte.
        return verseDao.verseAtPosition(position)?.takeIf { it.text.isNotBlank() }
            ?: verseDao.verseAtPosition((position + 1) % total)
    }
}
