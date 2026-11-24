package com.palavraviva.app.data.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converte respostas JSON do Gemini em objetos de domínio e serializa/desserializa
 * o payload salvo no cache local do Room.
 */
object StudyJsonParser {

    fun parseVerseStudy(raw: String): VerseStudy {
        val obj = parseObject(raw)
        val summary = optNonBlank(obj, "summary").orEmpty()
        val historicalContext = optNonBlank(obj, "historicalContext").orEmpty()
        if (summary.isBlank() && historicalContext.isBlank()) {
            throw AiError.ParseError()
        }

        val termsArray = obj.optJSONArray("periodTerms") ?: JSONArray()
        val terms = buildList {
            for (i in 0 until termsArray.length()) {
                val item = termsArray.optJSONObject(i) ?: continue
                val term = optNonBlank(item, "term") ?: continue
                val meaning = optNonBlank(item, "meaning") ?: continue
                val originalWord = optNonBlank(item, "originalWord")
                add(PeriodTerm(term = term, originalWord = originalWord, meaning = meaning))
            }
        }

        val refsArray = obj.optJSONArray("crossReferences") ?: JSONArray()
        val refs = buildList {
            for (i in 0 until refsArray.length()) {
                val item = refsArray.optJSONObject(i) ?: continue
                val rawBookId = optNonBlank(item, "bookId")?.lowercase() ?: continue
                if (rawBookId !in StudyPrompts.VALID_BOOK_IDS) continue
                val chapter = item.optInt("chapter", 0)
                if (chapter < 1) continue
                val verse = if (!item.isNull("verse")) item.optInt("verse", 0).takeIf { it > 0 } else null
                val label = optNonBlank(item, "referenceLabel") ?: continue
                val reason = optNonBlank(item, "reason").orEmpty()
                add(
                    CrossReference(
                        bookId = rawBookId,
                        chapter = chapter,
                        verse = verse,
                        referenceLabel = label,
                        reason = reason,
                    ),
                )
            }
        }

        return VerseStudy(
            summary = summary,
            historicalContext = historicalContext,
            periodTerms = terms,
            crossReferences = refs,
        )
    }

    fun serializeVerseStudy(study: VerseStudy): String = JSONObject().apply {
        put("summary", study.summary)
        put("historicalContext", study.historicalContext)
        put(
            "periodTerms",
            JSONArray().apply {
                study.periodTerms.forEach { t ->
                    put(
                        JSONObject().apply {
                            put("term", t.term)
                            if (t.originalWord != null) put("originalWord", t.originalWord) else put("originalWord", JSONObject.NULL)
                            put("meaning", t.meaning)
                        },
                    )
                }
            },
        )
        put(
            "crossReferences",
            JSONArray().apply {
                study.crossReferences.forEach { r ->
                    put(
                        JSONObject().apply {
                            put("bookId", r.bookId)
                            put("chapter", r.chapter)
                            if (r.verse != null) put("verse", r.verse) else put("verse", JSONObject.NULL)
                            put("referenceLabel", r.referenceLabel)
                            put("reason", r.reason)
                        },
                    )
                }
            },
        )
    }.toString()

    fun parseChapterGuide(raw: String): ChapterGuide {
        val obj = parseObject(raw)
        val overview = optNonBlank(obj, "overview").orEmpty()
        val bookPlacement = optNonBlank(obj, "bookPlacement").orEmpty()
        if (overview.isBlank() && bookPlacement.isBlank()) {
            throw AiError.ParseError()
        }

        val figures = parseStringList(obj.optJSONArray("keyFiguresAndPlaces"))
        val questions = parseStringList(obj.optJSONArray("reflectionQuestions"))

        return ChapterGuide(
            overview = overview,
            bookPlacement = bookPlacement,
            keyFiguresAndPlaces = figures,
            reflectionQuestions = questions,
        )
    }

    fun serializeChapterGuide(guide: ChapterGuide): String = JSONObject().apply {
        put("overview", guide.overview)
        put("bookPlacement", guide.bookPlacement)
        put("keyFiguresAndPlaces", JSONArray(guide.keyFiguresAndPlaces))
        put("reflectionQuestions", JSONArray(guide.reflectionQuestions))
    }.toString()

    internal fun stripMarkdownFences(raw: String): String {
        val withoutThink = raw.replace(Regex("(?is)<think>.*?</think>"), "").trim()
        if (!withoutThink.startsWith("```")) return withoutThink
        val firstNewLine = withoutThink.indexOf('\n')
        val withoutStart = if (firstNewLine >= 0) withoutThink.substring(firstNewLine + 1) else withoutThink.removePrefix("```")
        val endFence = withoutStart.lastIndexOf("```")
        return (if (endFence >= 0) withoutStart.substring(0, endFence) else withoutStart).trim()
    }

    private fun parseObject(raw: String): JSONObject {
        val cleaned = stripMarkdownFences(raw)
        return try {
            JSONObject(cleaned)
        } catch (_: Exception) {
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start >= 0 && end > start) {
                try {
                    JSONObject(cleaned.substring(start, end + 1))
                } catch (_: Exception) {
                    throw AiError.ParseError()
                }
            } else {
                throw AiError.ParseError()
            }
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                if (!array.isNull(i)) {
                    array.optString(i, "").trim().takeIf { it.isNotEmpty() }?.let(::add)
                }
            }
        }
    }

    private fun optNonBlank(obj: JSONObject, key: String): String? {
        if (obj.isNull(key)) return null
        return obj.optString(key, "").trim().takeIf { it.isNotEmpty() && it != "null" }
    }
}
