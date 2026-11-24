package com.palavraviva.app.data.study

import com.palavraviva.app.data.local.GlossaryTerm
import java.text.Normalizer
import java.util.Locale

/**
 * Detecta termos do glossário bíblico offline dentro do texto de um versículo,
 * ignorando acentos e caixa alta/baixa e respeitando fronteiras de palavra.
 */
object GlossaryMatcher {

    fun findMatches(text: String, glossary: List<GlossaryTerm>): List<GlossaryTerm> {
        if (text.isBlank() || glossary.isEmpty()) return emptyList()
        val normalizedText = normalize(text)

        return glossary.filter { entry ->
            val candidates = buildList {
                add(entry.term)
                entry.aliasesCsv
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach(::add)
            }
            candidates.any { candidate ->
                val normCandidate = normalize(candidate)
                if (normCandidate.length < 2) return@any false
                val pattern = Regex("(?<![a-z0-9])${Regex.escape(normCandidate)}(?![a-z0-9])")
                pattern.containsMatchIn(normalizedText)
            }
        }
    }

    internal fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "")
            .lowercase(Locale.ROOT)
            .trim()
}
