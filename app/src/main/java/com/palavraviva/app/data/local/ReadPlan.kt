package com.palavraviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Plano de leitura (semeado no primeiro início ou criado pelo usuário). */
@Entity(tableName = "ReadPlan")
data class ReadPlan(
    @PrimaryKey val planId: String,
    val name: String,
    val description: String,
    val totalDays: Int,
)
