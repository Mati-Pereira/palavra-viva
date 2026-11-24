package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyCacheDao {

    @Query("SELECT * FROM StudyCache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): StudyCache?

    @Query("SELECT * FROM StudyCache WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeByKey(cacheKey: String): Flow<StudyCache?>

    @Query("SELECT cacheKey FROM StudyCache WHERE bookId = :bookId AND chapter = :chapter")
    fun observeKeysForChapter(bookId: String, chapter: Int): Flow<List<String>>

    @Query("SELECT * FROM StudyCache ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StudyCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: StudyCache)

    @Query("DELETE FROM StudyCache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)
}
