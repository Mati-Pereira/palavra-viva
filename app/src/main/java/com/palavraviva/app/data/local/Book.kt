package com.palavraviva.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Livro da Bíblia.
 *
 * @param testament "AT" (Antigo Testamento) ou "NT" (Novo Testamento).
 */
@Entity(tableName = "Book")
data class Book(
    @PrimaryKey val bookId: String,
    val name: String,
    val testament: String,
    val bookOrder: Int,
)
