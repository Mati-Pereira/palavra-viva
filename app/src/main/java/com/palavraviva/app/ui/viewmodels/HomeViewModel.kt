package com.palavraviva.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palavraviva.app.data.local.ChapterRead
import com.palavraviva.app.data.repositories.BibleRepository
import com.palavraviva.app.data.repositories.PlanRepository
import com.palavraviva.app.data.repositories.ProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime

data class VerseOfDayUi(val text: String, val reference: String)

data class ContinueReadingUi(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val progress: Float,
)

data class HomePlanCardUi(
    val planId: String,
    val name: String,
    val currentDay: Int,
    val totalDays: Int,
    val completedDays: Int,
    val started: Boolean,
)

data class HomeUiState(
    val greeting: String = "",
    val streak: Int = 0,
    val continueReading: ContinueReadingUi? = null,
    val planCard: HomePlanCardUi? = null,
    val verseOfDay: VerseOfDayUi? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val bible: BibleRepository,
    private val plans: PlanRepository,
    private val progress: ProgressRepository,
) : ViewModel() {

    private val fallbackVerse = VerseOfDayUi(
        text = "Lâmpada para os meus pés é a tua palavra, e luz para o meu caminho.",
        reference = "Salmos 119:105",
    )

    /** Streak atual (dias consecutivos terminando hoje ou ontem). */
    private val streakFlow: Flow<Int> =
        progress.streakDays().map { days -> computeStreak(days.map { it.day }.toSet()) }

    /** Último capítulo lido → "continuar leitura", com progresso dentro do livro. */
    private val continueFlow: Flow<ContinueReadingUi?> =
        progress.readChapters().map { reads -> latestRead(reads) }

    /** Plano em andamento (mais recente com dias concluídos) ou o 1º plano como sugestão. */
    private val planCardFlow: Flow<HomePlanCardUi?> =
        plans.plans().flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(null)
            } else {
                combine(list.map { plan ->
                    progress.completedDays(plan.planId).map { days ->
                        Triple(plan, days.size, days.maxOfOrNull { it.completedAt } ?: 0L)
                    }
                }) { entries ->
                    val active = entries.toList()
                        .filter { it.second > 0 && it.second < it.first.totalDays }
                        .maxByOrNull { it.third }
                    if (active != null) {
                        val (plan, done, _) = active
                        HomePlanCardUi(
                            planId = plan.planId,
                            name = plan.name,
                            currentDay = minOf(done + 1, plan.totalDays),
                            totalDays = plan.totalDays,
                            completedDays = done,
                            started = true,
                        )
                    } else {
                        val plan = entries.first().first
                        HomePlanCardUi(
                            planId = plan.planId,
                            name = plan.name,
                            currentDay = 1,
                            totalDays = plan.totalDays,
                            completedDays = 0,
                            started = false,
                        )
                    }
                }
            }
        }

    /**
     * Versículo do dia (determinístico) com fallback estático se o banco estiver
     * vazio. Reativo ao AVANÇO da importação inicial: recalcula a cada livro
     * que entra (no máx. 66 vezes, apenas na primeira importação), de modo que
     * o valor final é idêntico ao do widget, atualizado ao fim da semente.
     */
    private val verseFlow: Flow<VerseOfDayUi> = bible.books()
        .map { it.size }
        .distinctUntilChanged()
        .mapLatest { count ->
            if (count == 0) return@mapLatest fallbackVerse
            val verse = bible.verseOfTheDay() ?: return@mapLatest fallbackVerse
            val bookName = bible.book(verse.bookId)?.name ?: verse.bookId
            VerseOfDayUi(
                text = verse.text,
                reference = "$bookName ${verse.chapter}:${verse.verse}",
            )
        }

    val uiState: StateFlow<HomeUiState> = combine(
        streakFlow,
        continueFlow,
        planCardFlow,
        verseFlow,
    ) { streak, continueReading, planCard, verseOfDay ->
        HomeUiState(
            greeting = greetingForHour(),
            streak = streak,
            continueReading = continueReading,
            planCard = planCard,
            verseOfDay = verseOfDay,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(greeting = greetingForHour()),
    )

    private suspend fun latestRead(reads: List<ChapterRead>): ContinueReadingUi? {
        val last = reads.maxByOrNull { it.readAt } ?: return null
        val book = bible.book(last.bookId)
        val total = bible.chapterCount(last.bookId).coerceAtLeast(1)
        return ContinueReadingUi(
            bookId = last.bookId,
            bookName = book?.name ?: last.bookId,
            chapter = last.chapter,
            progress = (last.chapter.toFloat() / total).coerceIn(0.02f, 1f),
        )
    }

    private fun greetingForHour(): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 12 -> "Bom dia, Matheus ☀️"
            hour < 18 -> "Boa tarde, Matheus"
            else -> "Boa noite, Matheus 🌙"
        }
    }
}
