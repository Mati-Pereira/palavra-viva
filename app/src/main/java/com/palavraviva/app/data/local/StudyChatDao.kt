package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyChatDao {

    @Query("SELECT * FROM StudyChatMessage WHERE scopeKey = :scopeKey ORDER BY createdAt ASC, id ASC")
    fun observeMessages(scopeKey: String): Flow<List<StudyChatMessage>>

    @Query("SELECT * FROM StudyChatMessage WHERE scopeKey = :scopeKey ORDER BY createdAt ASC, id ASC")
    suspend fun messagesOnce(scopeKey: String): List<StudyChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: StudyChatMessage): Long

    @Query("DELETE FROM StudyChatMessage WHERE scopeKey = :scopeKey")
    suspend fun clearScope(scopeKey: String)
}
