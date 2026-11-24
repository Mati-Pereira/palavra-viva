package com.palavraviva.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco local do app ("palavra_viva.db", versão 2) com entidades de texto
 * bíblico, planos, progresso, destaque, marcadores, notas e estudo offline/IA.
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
        StudyCache::class,
        StudyChatMessage::class,
        GlossaryTerm::class,
    ],
    version = 2,
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
    abstract fun studyCacheDao(): StudyCacheDao
    abstract fun studyChatDao(): StudyChatDao
    abstract fun glossaryDao(): GlossaryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `StudyCache` (
                        `cacheKey` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `verse` INTEGER,
                        `title` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `model` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`cacheKey`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `StudyChatMessage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `scopeKey` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_StudyChatMessage_scopeKey` ON `StudyChatMessage` (`scopeKey`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `GlossaryTerm` (
                        `termKey` TEXT NOT NULL,
                        `term` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `originalWord` TEXT,
                        `summary` TEXT NOT NULL,
                        `historicalContext` TEXT NOT NULL,
                        `aliasesCsv` TEXT NOT NULL,
                        PRIMARY KEY(`termKey`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Instância única do banco para o contexto da aplicação. */
        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "palavra_viva.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
