package com.palavraviva.app.ui.viewmodels

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilitários de data/streak compartilhados pelos ViewModels de Início e Progresso.
 */

/**
 * Dias consecutivos de leitura, terminando hoje ou ontem.
 * Se hoje ainda não foi marcado, ontem mantém a sequência "viva";
 * se ontem também não está marcado, o streak é 0.
 */
internal fun computeStreak(days: Set<String>): Int {
    var cursor = LocalDate.now()
    if (cursor.toString() !in days) cursor = cursor.minusDays(1)
    var streak = 0
    while (cursor.toString() in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/** Data por extenso em pt-BR, ex.: "Terça, 17 de novembro". */
internal fun formatFullDate(date: LocalDate): String {
    val raw = date.format(
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR")),
    )
    return raw.replaceFirstChar { it.uppercase() }
}

/** Percentual inteiro seguro contra divisão por zero. */
internal fun percentOf(part: Int, total: Int): Int =
    if (total > 0) (part * 100) / total else 0
