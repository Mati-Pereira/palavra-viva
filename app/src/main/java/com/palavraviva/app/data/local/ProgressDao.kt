package com.palavraviva.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markChapterRead(chapterRead: ChapterRead)

    @Query("SELECT * FROM ChapterRead WHERE bookId = :bookId AND chapter = :chapter")
    suspend fun chapterRead(bookId: String, chapter: Int): ChapterRead?

    @Query("SELECT * FROM ChapterRead")
    fun readChapters(): Flow<List<ChapterRead>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDayCompleted(dayCompleted: DayCompleted)

    @Query("SELECT * FROM DayCompleted WHERE planId = :planId ORDER BY dayNumber")
    fun completedDays(planId: String): Flow<List<DayCompleted>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markStreakDay(streakDay: StreakDay)

    @Query("SELECT * FROM StreakDay ORDER BY day DESC")
    fun streakDays(): Flow<List<StreakDay>>
}
