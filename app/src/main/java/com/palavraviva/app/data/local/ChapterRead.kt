package com.palavraviva.app.data.local

import androidx.room.Entity

/** Capítulo marcado como lido. */
@Entity(tableName = "ChapterRead", primaryKeys = ["bookId", "chapter"])
data class ChapterRead(
    val bookId: String,
    val chapter: Int,
    val readAt: Long,
)
