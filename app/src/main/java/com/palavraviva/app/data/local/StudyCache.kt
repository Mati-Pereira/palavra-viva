package com.palavraviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache local permanente de explicações geradas pela IA (por versículo ou capítulo),
 * permitindo reabertura instantânea e consulta 100% offline.
 */
@Entity(tableName = "StudyCache")
data class StudyCache(
    @PrimaryKey val cacheKey: String,
    val kind: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int?,
    val title: String,
    val payloadJson: String,
    val model: String,
    val createdAt: Long,
)
