package com.palavraviva.app.data.local

import androidx.room.Entity

/** Versículo único, identificado por livro + capítulo + número do versículo. */
@Entity(tableName = "Verse", primaryKeys = ["bookId", "chapter", "verse"])
data class Verse(
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
)
