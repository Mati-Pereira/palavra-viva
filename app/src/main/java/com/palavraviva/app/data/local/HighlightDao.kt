package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: Highlight)

    @Query("DELETE FROM Highlight WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun delete(bookId: String, chapter: Int, verse: Int)

    @Query("SELECT * FROM Highlight WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
    fun highlights(bookId: String, chapter: Int): Flow<List<Highlight>>
}
