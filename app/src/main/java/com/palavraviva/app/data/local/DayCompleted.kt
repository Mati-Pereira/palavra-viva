package com.palavraviva.app.data.local

import androidx.room.Entity

/** Dia de plano concluído. */
@Entity(tableName = "DayCompleted", primaryKeys = ["planId", "dayNumber"])
data class DayCompleted(
    val planId: String,
    val dayNumber: Int,
    val completedAt: Long,
)
