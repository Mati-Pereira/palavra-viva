package com.palavraviva.app

import com.palavraviva.app.data.local.GlossaryTerm
import com.palavraviva.app.data.study.GlossaryMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryMatcherTest {

    private val sampleGlossary = listOf(
        GlossaryTerm(
            termKey = "publicano",
            term = "Publicano",
            category = "Sociedade",
            originalWord = "telōnēs (grego)",
            summary = "Cobrador de impostos.",
            historicalContext = "Contratado por Roma.",
            aliasesCsv = "publicanos,cobrador de impostos",
        ),
        GlossaryTerm(
            termKey = "denario",
            term = "Denário",
            category = "Medidas e Moedas",
            originalWord = "dēnarion (grego)",
            summary = "Moeda de prata romana.",
            historicalContext = "Equivalia a um dia de trabalho.",
            aliasesCsv = "denarios",
        ),
        GlossaryTerm(
            termKey = "fe",
            term = "Fé / Fidelidade",
            category = "Idiomas Originais",
            originalWord = "pistis (grego)",
            summary = "Confiança firme.",
            historicalContext = "Une crer e ser fiel.",
            aliasesCsv = "",
        ),
        GlossaryTerm(
            termKey = "fermento",
            term = "Fermento",
            category = "Metáforas",
            originalWord = "zymē (grego)",
            summary = "Massa levedada.",
            historicalContext = "Símbolo de influência.",
            aliasesCsv = "levedo",
        ),
    )

    @Test
    fun matchesAccentedAndPluralAliasesRegardlessOfCase() {
        val text = "Dois homens subiram ao templo: um fariseu e um cobrador de impostos; pagaram cem denários."
        val matches = GlossaryMatcher.findMatches(text, sampleGlossary)
        val keys = matches.map { it.termKey }
        assertEquals(listOf("publicano", "denario"), keys)
    }

    @Test
    fun respectsWordBoundariesForShortTerms() {
        val text = "Cuidado com o fermento dos fariseus na estrada de Jerusalém."
        val matches = GlossaryMatcher.findMatches(text, sampleGlossary)
        val keys = matches.map { it.termKey }
        assertEquals(listOf("fermento"), keys)
        assertTrue("fe" !in keys)
    }
}
