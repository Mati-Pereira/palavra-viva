package com.palavraviva.app.data.repositories

import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.Bookmark
import com.palavraviva.app.data.local.Highlight
import com.palavraviva.app.data.local.Note
import kotlinx.coroutines.flow.Flow

/** Destaques, marcadores e notas do leitor. */
class HighlightRepository(private val database: AppDatabase) {

    private val highlightDao = database.highlightDao()
    private val bookmarkDao = database.bookmarkDao()
    private val noteDao = database.noteDao()

    // --- Destaques ---

    fun highlights(bookId: String, chapter: Int): Flow<List<Highlight>> =
        highlightDao.highlights(bookId, chapter)

    suspend fun addHighlight(highlight: Highlight) =
        highlightDao.insert(highlight)

    suspend fun removeHighlight(bookId: String, chapter: Int, verse: Int) =
        highlightDao.delete(bookId, chapter, verse)

    // --- Marcadores ---

    fun bookmarks(): Flow<List<Bookmark>> = bookmarkDao.bookmarks()

    fun bookmarksFor(bookId: String, chapter: Int): Flow<List<Bookmark>> =
        bookmarkDao.bookmarksFor(bookId, chapter)

    suspend fun addBookmark(bookmark: Bookmark) =
        bookmarkDao.insert(bookmark)

    suspend fun removeBookmark(bookId: String, chapter: Int, verse: Int) =
        bookmarkDao.delete(bookId, chapter, verse)

    // --- Notas ---

    fun notesFor(bookId: String, chapter: Int): Flow<List<Note>> =
        noteDao.notesFor(bookId, chapter)

    /**
     * Salva (insere quando `id == 0`, atualiza quando já tem id).
     * @return o id da linha persistida.
     */
    suspend fun saveNote(note: Note): Long = noteDao.insert(note)

    suspend fun deleteNote(id: Long) = noteDao.delete(id)
}
