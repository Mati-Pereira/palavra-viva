package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terms: List<GlossaryTerm>)

    @Query("SELECT COUNT(*) FROM GlossaryTerm")
    suspend fun count(): Int

    @Query("SELECT * FROM GlossaryTerm ORDER BY term ASC")
    fun observeAll(): Flow<List<GlossaryTerm>>

    @Query("SELECT * FROM GlossaryTerm ORDER BY term ASC")
    suspend fun allOnce(): List<GlossaryTerm>

    @Query(
        "SELECT * FROM GlossaryTerm " +
            "WHERE term LIKE :pattern OR summary LIKE :pattern OR historicalContext LIKE :pattern OR aliasesCsv LIKE :pattern " +
            "ORDER BY term ASC",
    )
    fun search(pattern: String): Flow<List<GlossaryTerm>>
}
