package com.palavraviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Termo do glossário histórico-cultural e linguístico embutido para consulta offline.
 */
@Entity(tableName = "GlossaryTerm")
data class GlossaryTerm(
    @PrimaryKey val termKey: String,
    val term: String,
    val category: String,
    val originalWord: String?,
    val summary: String,
    val historicalContext: String,
    val aliasesCsv: String,
)
