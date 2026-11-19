package com.palavraviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Dia com atividade de leitura, para cálculo da sequência (streak). */
@Entity(tableName = "StreakDay")
data class StreakDay(
    /** Data no formato "yyyy-MM-dd". */
    @PrimaryKey val day: String,
)
