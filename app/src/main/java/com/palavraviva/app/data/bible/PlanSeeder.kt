package com.palavraviva.app.data.bible

import com.palavraviva.app.data.local.AppDatabase
import com.palavraviva.app.data.local.Book
import com.palavraviva.app.data.local.PlanDay
import com.palavraviva.app.data.local.ReadPlan

/**
 * Semeia os planos de leitura padrão com base nos livros realmente presentes
 * no banco (livros ausentes são ignorados; planos que não puderem ser
 * montados são pulados sem erro). Idempotente: repositórios usam REPLACE.
 */
object PlanSeeder {

    private const val PLAN_SALMOS_PROVERBIOS = "salmos-proverbios-31"
    private const val PLAN_NOVO_TESTAMENTO = "novo-testamento-90"
    private const val PLAN_EVANGELHOS = "evangelhos-30"

    suspend fun seedPlans(database: AppDatabase) {
        val books = database.bookDao().allBooks()
            .filter { database.verseDao().chapterCount(it.bookId) > 0 }
        if (books.isEmpty()) return

        seedSalmosProverbios(database, books)
        seedNovoTestamento(database, books)
        seedEvangelhos(database, books)
    }

    /**
     * "Salmos e Provérbios em 31 dias" (exige Sl e Pv): dias ímpares lêem uma
     * faixa dos Salmos (16 blocos), dias pares uma faixa de Provérbios (15 blocos).
     */
    private suspend fun seedSalmosProverbios(database: AppDatabase, books: List<Book>) {
        val salmos = books.firstOrNull { it.bookId == "sl" } ?: return
        val proverbios = books.firstOrNull { it.bookId == "pv" } ?: return

        val slChapters = database.verseDao().chapterCount(salmos.bookId)
        val pvChapters = database.verseDao().chapterCount(proverbios.bookId)
        val totalDays = 31
        val days = ArrayList<PlanDay>(totalDays)

        var slSegment = 0
        var pvSegment = 0
        for (day in 1..totalDays) {
            if (day % 2 == 1) {
                val (start, end) = slice(slChapters, slSegment++, 16)
                days += PlanDay(PLAN_SALMOS_PROVERBIOS, day, salmos.bookId, start, end)
            } else {
                val (start, end) = slice(pvChapters, pvSegment++, 15)
                days += PlanDay(PLAN_SALMOS_PROVERBIOS, day, proverbios.bookId, start, end)
            }
        }

        database.planDao().insertPlans(
            listOf(
                ReadPlan(
                    planId = PLAN_SALMOS_PROVERBIOS,
                    name = "Salmos e Provérbios em 31 dias",
                    description =
                        "Um mês de meditação diária: nos dias ímpares você lê os Salmos, " +
                            "nos dias pares um trecho de Provérbios.",
                    totalDays = totalDays,
                ),
            ),
        )
        database.planDao().insertDays(days)
    }

    /** "Novo Testamento em 90 dias": distribui proporcionalmente os livros do NT presentes. */
    private suspend fun seedNovoTestamento(database: AppDatabase, books: List<Book>) {
        val ntBooks = books.filter { it.testament == "NT" }
        if (ntBooks.isEmpty()) return

        val counts = ntBooks.map { database.verseDao().chapterCount(it.bookId) }
        val days = buildDistributedDays(PLAN_NOVO_TESTAMENTO, ntBooks, counts, 90)

        database.planDao().insertPlans(
            listOf(
                ReadPlan(
                    planId = PLAN_NOVO_TESTAMENTO,
                    name = "Novo Testamento em 90 dias",
                    description =
                        "Percorra todo o Novo Testamento em cerca de 90 dias, " +
                            "com trechos equilibrados a cada dia.",
                    totalDays = days.size,
                ),
            ),
        )
        database.planDao().insertDays(days)
    }

    /** "Evangelhos em 30 dias" (exige Mt, Mc, Lc e Jo). */
    private suspend fun seedEvangelhos(database: AppDatabase, books: List<Book>) {
        val gospelIds = listOf("mt", "mc", "lc", "jo")
        val gospels = books.filter { it.bookId in gospelIds }
        if (gospels.size != gospelIds.size) return

        val counts = gospels.map { database.verseDao().chapterCount(it.bookId) }
        val days = buildDistributedDays(PLAN_EVANGELHOS, gospels, counts, 30)

        database.planDao().insertPlans(
            listOf(
                ReadPlan(
                    planId = PLAN_EVANGELHOS,
                    name = "Evangelhos em 30 dias",
                    description =
                        "A vida e os ensinamentos de Jesus segundo Mateus, Marcos, " +
                            "Lucas e João, em um mês.",
                    totalDays = days.size,
                ),
            ),
        )
        database.planDao().insertDays(days)
    }

    /**
     * Gera [PlanDay]s sequenciais: cada dia cobre uma faixa de um único livro,
     * com o número de dias de cada livro proporcional ao seu tamanho.
     */
    private fun buildDistributedDays(
        planId: String,
        books: List<Book>,
        chapterCounts: List<Int>,
        targetDays: Int,
    ): List<PlanDay> {
        val allocation = allocateDays(chapterCounts, targetDays)
        val days = ArrayList<PlanDay>()
        var dayNumber = 1
        books.forEachIndexed { index, book ->
            val bookDays = allocation[index]
            val chapters = chapterCounts[index]
            for (segment in 0 until bookDays) {
                val (start, end) = slice(chapters, segment, bookDays)
                days += PlanDay(planId, dayNumber++, book.bookId, start, end)
            }
        }
        return days
    }

    /**
     * Distribui [targetDays] dias entre os livros proporcionalmente aos
     * capítulos, com no mínimo 1 dia e no máximo 1 dia por capítulo de cada
     * livro; o total fecha exatamente em `min(targetDays, capítulos totais)`
     * sempre que possível.
     */
    private fun allocateDays(chapterCounts: List<Int>, targetDays: Int): List<Int> {
        if (chapterCounts.isEmpty()) return emptyList()
        val totalChapters = chapterCounts.sum()
        if (totalChapters == 0) return chapterCounts
        val maxDays = minOf(targetDays, totalChapters)

        val allocation = chapterCounts.map { chapters ->
            if (chapters <= 0) {
                0
            } else {
                (chapters * maxDays / totalChapters).coerceIn(1, chapters)
            }
        }.toMutableList()
        var allocated = allocation.sum()

        while (allocated > maxDays) {
            val i = allocation.indices
                .filter { allocation[it] > 1 }
                .maxByOrNull { allocation[it] } ?: break
            allocation[i]--
            allocated--
        }
        while (allocated < maxDays) {
            val i = allocation.indices
                .filter { allocation[it] < chapterCounts[it] }
                .maxByOrNull { chapterCounts[it] - allocation[it] } ?: break
            allocation[i]++
            allocated++
        }
        return allocation
    }

    /** Faixa contígua [segment] (0-based) de [chapters] dividido em [segments] partes. */
    private fun slice(chapters: Int, segment: Int, segments: Int): Pair<Int, Int> {
        val start = chapters * segment / segments + 1
        val end = chapters * (segment + 1) / segments
        return start to maxOf(start, end)
    }
}
