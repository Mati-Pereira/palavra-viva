package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>)

    @Query("SELECT * FROM Book ORDER BY bookOrder")
    fun books(): Flow<List<Book>>

    /** Instantâneo ordenado (usado na semente de planos e importador). */
    @Query("SELECT * FROM Book ORDER BY bookOrder")
    suspend fun allBooks(): List<Book>

    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    suspend fun book(bookId: String): Book?

    @Query("SELECT COUNT(*) FROM Book")
    suspend fun count(): Int
}
