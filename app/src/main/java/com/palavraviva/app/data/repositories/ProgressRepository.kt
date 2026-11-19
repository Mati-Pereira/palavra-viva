package com.palavraviva.app.data.repositories

import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.ChapterRead
import com.palavraviva.app.data.local.DayCompleted
import com.palavraviva.app.data.local.StreakDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Persistência de progresso: capítulos lidos, dias de plano concluídos e streak. */
class ProgressRepository(private val database: AppDatabase) {

    private val progressDao = database.progressDao()

    suspend fun markChapterRead(bookId: String, chapter: Int) {
        progressDao.markChapterRead(ChapterRead(bookId, chapter, System.currentTimeMillis()))
    }

    suspend fun isChapterRead(bookId: String, chapter: Int): Boolean =
        progressDao.chapterRead(bookId, chapter) != null

    fun readChapters(): Flow<List<ChapterRead>> = progressDao.readChapters()

    suspend fun markDayCompleted(planId: String, dayNumber: Int) {
        progressDao.markDayCompleted(DayCompleted(planId, dayNumber, System.currentTimeMillis()))
    }

    fun completedDays(planId: String): Flow<List<DayCompleted>> =
        progressDao.completedDays(planId)

    /** Registra atividade hoje na sequência ("yyyy-MM-dd"); idempotente. */
    suspend fun markTodayStreak() {
        progressDao.markStreakDay(StreakDay(LocalDate.now().toString()))
    }

    /** Dias com atividade, do mais recente para o mais antigo. */
    fun streakDays(): Flow<List<StreakDay>> = progressDao.streakDays()
}
