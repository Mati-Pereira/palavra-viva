package com.palavraviva.app

import com.palavraviva.app.data.ai.AiError
import com.palavraviva.app.data.ai.ChapterGuide
import com.palavraviva.app.data.ai.CrossReference
import com.palavraviva.app.data.ai.PeriodTerm
import com.palavraviva.app.data.ai.StudyJsonParser
import com.palavraviva.app.data.ai.VerseStudy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyJsonParserTest {

    @Test
    fun parsesVerseStudyWithMarkdownFencesAndFiltersInvalidBookIds() {
        val raw = """
            ```json
            {
              "summary": "Jesus explica a Nicodemos o novo nascimento.",
              "historicalContext": "Nicodemos era fariseu e membro do Sinédrio em Jerusalém.",
              "periodTerms": [
                {
                  "term": "Nascer de novo",
                  "originalWord": "anōthen (grego)",
                  "meaning": "Pode significar tanto 'de novo' quanto 'do alto'."
                },
                {
                  "term": "Rabi",
                  "originalWord": null,
                  "meaning": "Tratamento respeitoso dado a mestres judeus."
                }
              ],
              "crossReferences": [
                {
                  "bookId": "ez",
                  "chapter": 36,
                  "verse": 25,
                  "referenceLabel": "Ezequiel 36:25",
                  "reason": "Promessa de purificação com água e espírito novo."
                },
                {
                  "bookId": "livro_invalido",
                  "chapter": 1,
                  "verse": 1,
                  "referenceLabel": "Inválido 1:1",
                  "reason": "Deve ser filtrado."
                },
                {
                  "bookId": "rm",
                  "chapter": 0,
                  "verse": null,
                  "referenceLabel": "Romanos 0",
                  "reason": "Capítulo inválido também é filtrado."
                }
              ]
            }
            ```
        """.trimIndent()

        val parsed = StudyJsonParser.parseVerseStudy(raw)
        assertEquals("Jesus explica a Nicodemos o novo nascimento.", parsed.summary)
        assertEquals(2, parsed.periodTerms.size)
        assertEquals("anōthen (grego)", parsed.periodTerms[0].originalWord)
        assertNull(parsed.periodTerms[1].originalWord)
        assertEquals(1, parsed.crossReferences.size)
        assertEquals("ez", parsed.crossReferences[0].bookId)
        assertEquals(36, parsed.crossReferences[0].chapter)
    }

    @Test
    fun roundTripsVerseStudyAndChapterGuide() {
        val study = VerseStudy(
            summary = "Resumo claro.",
            historicalContext = "Contexto histórico.",
            periodTerms = listOf(
                PeriodTerm("Odres", "askos", "Recipiente de couro."),
            ),
            crossReferences = listOf(
                CrossReference("mc", 2, 22, "Marcos 2:22", "Relato paralelo."),
            ),
        )
        val serializedStudy = StudyJsonParser.serializeVerseStudy(study)
        val restoredStudy = StudyJsonParser.parseVerseStudy(serializedStudy)
        assertEquals(study, restoredStudy)

        val guide = ChapterGuide(
            overview = "Panorama do capítulo.",
            bookPlacement = "Início da missão na Galileia.",
            keyFiguresAndPlaces = listOf("Cafarnaum — vila pesqueira"),
            reflectionQuestions = listOf("O que chama atenção na reação da multidão?"),
        )
        val serializedGuide = StudyJsonParser.serializeChapterGuide(guide)
        val restoredGuide = StudyJsonParser.parseChapterGuide(serializedGuide)
        assertEquals(guide, restoredGuide)
    }

    @Test
    fun throwsParseErrorOnInvalidPayload() {
        var caught: Throwable? = null
        try {
            StudyJsonParser.parseVerseStudy("{}")
        } catch (e: Throwable) {
            caught = e
        }
        assertTrue(caught is AiError.ParseError)
    }

    @Test
    fun stripsReasoningThinkTagsBeforeParsingVerseStudy() {
        val raw = """
            <think>
            Analisando o contexto de Lucas 18:13 e o termo publicano.
            </think>
            ```json
            {
              "summary": "O cobrador de impostos pede misericórdia com humildade.",
              "historicalContext": "Publicanos trabalhavam para o sistema tributário romano.",
              "periodTerms": [
                {
                  "term": "Publicano",
                  "originalWord": "telōnēs (grego)",
                  "meaning": "Arrecadador de tributos locais."
                }
              ],
              "crossReferences": []
            }
            ```
        """.trimIndent()

        val parsed = StudyJsonParser.parseVerseStudy(raw)
        assertEquals("O cobrador de impostos pede misericórdia com humildade.", parsed.summary)
        assertEquals(1, parsed.periodTerms.size)
        assertEquals("Publicano", parsed.periodTerms[0].term)
    }
}
