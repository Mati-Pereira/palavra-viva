package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark)

    @Query("DELETE FROM Bookmark WHERE bookId = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun delete(bookId: String, chapter: Int, verse: Int)

    @Query("SELECT * FROM Bookmark ORDER BY createdAt DESC")
    fun bookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM Bookmark WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse")
    fun bookmarksFor(bookId: String, chapter: Int): Flow<List<Bookmark>>
}
