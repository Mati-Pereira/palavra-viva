package com.palavraviva.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palavraviva.app.data.local.ChapterRead
import com.palavraviva.app.data.local.StreakDay
import com.palavraviva.app.data.repositories.PlanRepository
import com.palavraviva.app.data.repositories.ProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class PlanProgressUi(
    val planId: String,
    val name: String,
    val completedDays: Int,
    val totalDays: Int,
)

data class ProgressUiState(
    val streak: Int = 0,
    /** Total de dias com leitura registrada (independente de serem consecutivos). */
    val activeDays: Int = 0,
    val chaptersRead: Int = 0,
    /** Intensidade (0–1) das últimas 28 dias para o mapa de calor. */
    val heatmap: List<Float> = emptyList(),
    /** Altura normalizada (0–1) dos capítulos lidos nas últimas 8 semanas. */
    val weeklyBars: List<Float> = emptyList(),
    val planProgress: List<PlanProgressUi> = emptyList(),
    val achievementEmoji: String = "🎯",
    val achievementTitle: String = "Sua jornada começa hoje",
    val achievementSubtitle: String = "Conclua o dia de um plano para iniciar a sequência",
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val progress: ProgressRepository,
    private val plans: PlanRepository,
) : ViewModel() {

    private data class PlanEntry(val planId: String, val name: String, val totalDays: Int, val completed: Int)

    /** Progresso (dias concluídos/total) de cada plano. */
    private val planProgressFlow: Flow<List<PlanEntry>> =
        plans.plans().flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(list.map { plan ->
                    progress.completedDays(plan.planId).map { days ->
                        PlanEntry(plan.planId, plan.name, plan.totalDays, days.size)
                    }
                }) { it.toList() }
            }
        }

    val uiState: StateFlow<ProgressUiState> = combine(
        progress.streakDays(),
        progress.readChapters(),
        planProgressFlow,
    ) { streakDays, reads, planEntries ->
        buildState(streakDays, reads, planEntries)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState(),
    )

    private fun buildState(
        streakDays: List<StreakDay>,
        reads: List<ChapterRead>,
        planEntries: List<PlanEntry>,
    ): ProgressUiState {
        val daySet = streakDays.map { it.day }.toSet()
        val streak = computeStreak(daySet)

        // Mapa de calor: últimas 28 dias
        val today = LocalDate.now()
        val heatmap = (27 downTo 0).map { offset ->
            if (today.minusDays(offset.toLong()).toString() in daySet) 1f else 0.12f
        }

        // Capítulos por semana: últimas 8 semanas
        val now = System.currentTimeMillis()
        val weekMillis = 7L * 24L * 60L * 60L * 1000L
        val counts = (7 downTo 0).map { weeksAgo ->
            val start = now - weekMillis * (weeksAgo + 1)
            val end = now - weekMillis * weeksAgo
            reads.count { it.readAt in start until end }
        }
        val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
        val weeklyBars = counts.map { (it.toFloat() / maxCount).coerceIn(0.04f, 1f) }

        // Conquistas por sequência
        val (emoji, title, subtitle) = when {
            streak >= 25 ->
                Triple("🏆", "Conquista desbloqueada!", "25 dias de sequência — “Constância”")
            streak >= 7 ->
                Triple("🏆", "Conquista desbloqueada!", "$streak dias de sequência — continue firme!")
            streak > 0 ->
                Triple("🎯", "Próxima conquista", "Faltam ${7 - streak} ${if (7 - streak == 1) "dia" else "dias"} para a medalha “Constância”")
            else ->
                Triple("🎯", "Sua jornada começa hoje", "Conclua o dia de um plano para iniciar a sequência")
        }

        return ProgressUiState(
            streak = streak,
            activeDays = daySet.size,
            chaptersRead = reads.size,
            heatmap = heatmap,
            weeklyBars = weeklyBars,
            planProgress = planEntries.map {
                PlanProgressUi(it.planId, it.name, it.completed, it.totalDays)
            },
            achievementEmoji = emoji,
            achievementTitle = title,
            achievementSubtitle = subtitle,
        )
    }
}
