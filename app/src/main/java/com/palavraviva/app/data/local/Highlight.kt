package com.palavraviva.app.data.local

import androidx.room.Entity

/** Versículo destacado (marcado com cor). */
@Entity(tableName = "Highlight", primaryKeys = ["bookId", "chapter", "verse"])
data class Highlight(
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    /** Cor ARGB persistida como Long. */
    val color: Long,
    val createdAt: Long,
)
