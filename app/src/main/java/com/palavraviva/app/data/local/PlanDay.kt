package com.palavraviva.app.data.local

import androidx.room.Entity

/** Leitura de um dia de um plano: faixa contígua de capítulos de um único livro. */
@Entity(tableName = "PlanDay", primaryKeys = ["planId", "dayNumber"])
data class PlanDay(
    val planId: String,
    val dayNumber: Int,
    val bookId: String,
    val startChapter: Int,
    val endChapter: Int,
)
