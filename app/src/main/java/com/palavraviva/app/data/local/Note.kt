package com.palavraviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Nota pessoal anexada a um versículo. */
@Entity(tableName = "Note")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val editedAt: Long,
)
