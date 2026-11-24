package com.palavraviva.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mensagem salva do chat contextual associado a um versículo ou capítulo.
 */
@Entity(
    tableName = "StudyChatMessage",
    indices = [Index(value = ["scopeKey"])],
)
data class StudyChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scopeKey: String,
    val role: String,
    val text: String,
    val createdAt: Long,
)
