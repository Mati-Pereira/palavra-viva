package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /** Insere (id = 0) ou atualiza (id existente); retorna o id da linha. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Query("DELETE FROM Note WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM Note WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
    fun notesFor(bookId: String, chapter: Int): Flow<List<Note>>
}
