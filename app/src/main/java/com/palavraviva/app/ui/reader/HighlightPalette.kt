package com.palavraviva.app.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.palavraviva.app.data.local.Highlight

/** Opção da paleta de destaque do leitor. */
data class HighlightOption(val label: String, val color: Color) {
    /** Valor ARGB persistado em [Highlight.color] (como Long). */
    val argb: Int get() = color.toArgb()
}

/** Paleta do leitor (Tópico 6): dourado, índigo claro, verde e rosa. */
val HighlightPalette = listOf(
    HighlightOption("Dourado", Color(0xFFC9A227)),
    HighlightOption("Índigo", Color(0xFF8C9EFF)),
    HighlightOption("Verde", Color(0xFF2E7D5B)),
    HighlightOption("Rosa", Color(0xFFF06292)),
)

/**
 * Cor de UI correspondente a um destaque salvo (null se não houver).
 * Cores fora da paleta (futuras) continuam sendo exibidas corretamente.
 */
fun highlightColorOf(highlight: Highlight?): Color? {
    if (highlight == null) return null
    return HighlightPalette.firstOrNull { it.argb == highlight.color.toInt() }?.color
        ?: Color(highlight.color.toInt())
}
