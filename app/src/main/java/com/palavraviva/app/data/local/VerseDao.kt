package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<Verse>)

    @Query("SELECT * FROM Verse WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
    fun verses(bookId: String, chapter: Int): Flow<List<Verse>>

    @Query("SELECT * FROM Verse WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
    suspend fun versesOnce(bookId: String, chapter: Int): List<Verse>

    @Query("SELECT COUNT(*) FROM Verse WHERE bookId = :bookId")
    suspend fun countForBook(bookId: String): Int

    /** Número de capítulos distintos com versículos no livro. */
    @Query("SELECT COUNT(DISTINCT chapter) FROM Verse WHERE bookId = :bookId")
    suspend fun chapterCount(bookId: String): Int

    /**
     * Total de versículos na ordenação canônica (por ordem dos livros).
     * Casa com [verseAtPosition] para o versículo do dia.
     */
    @Query("SELECT COUNT(*) FROM Verse JOIN Book ON Book.bookId = Verse.bookId")
    suspend fun canonicalCount(): Int

    @Query(
        "SELECT Verse.* FROM Verse JOIN Book ON Book.bookId = Verse.bookId " +
            "ORDER BY Book.bookOrder, Verse.chapter, Verse.verse LIMIT 1 OFFSET :position",
    )
    suspend fun verseAtPosition(position: Int): Verse?
}
