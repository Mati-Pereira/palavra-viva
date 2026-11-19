package com.palavraviva.app.data.local

import androidx.room.Entity

/** Marcador (favorito) de um versículo. */
@Entity(tableName = "Bookmark", primaryKeys = ["bookId", "chapter", "verse"])
data class Bookmark(
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val label: String?,
    val createdAt: Long,
)
