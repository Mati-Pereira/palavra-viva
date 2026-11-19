package com.palavraviva.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco local do app ("palavra_viva.db", versão 1) com todas as entidades
 * de texto bíblico, planos, progresso, destaque, marcadores e notas.
 */
@Database(
    entities = [
        Book::class,
        Verse::class,
        ReadPlan::class,
        PlanDay::class,
        ChapterRead::class,
        DayCompleted::class,
        StreakDay::class,
        Highlight::class,
        Bookmark::class,
        Note::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun verseDao(): VerseDao
    abstract fun planDao(): PlanDao
    abstract fun progressDao(): ProgressDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Instância única do banco para o contexto da aplicação. */
        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "palavra_viva.db",
                ).build().also { instance = it }
            }
    }
}
