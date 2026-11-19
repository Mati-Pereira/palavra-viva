package com.palavraviva.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palavraviva.app.data.local.DayCompleted
import com.palavraviva.app.data.local.PlanDay
import com.palavraviva.app.data.local.ReadPlan
import com.palavraviva.app.data.repositories.BibleRepository
import com.palavraviva.app.data.repositories.PlanRepository
import com.palavraviva.app.data.repositories.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanDayItem(
    val dayNumber: Int,
    val bookId: String,
    val bookName: String,
    val startChapter: Int,
    val endChapter: Int,
    val done: Boolean,
)

data class PlanDetailUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val planName: String = "",
    val totalDays: Int = 0,
    val completedCount: Int = 0,
    val todayLabel: String = "",
    /** Próximo dia não concluído (null quando o plano acabou). */
    val currentDay: PlanDayItem? = null,
    /** Até 4 dias após o atual. */
    val upcoming: List<PlanDayItem> = emptyList(),
)

class PlanDetailViewModel(
    private val planId: String,
    private val plans: PlanRepository,
    private val progress: ProgressRepository,
    private val bible: BibleRepository,
) : ViewModel() {

    private val base = combine(
        plans.plans(),
        plans.days(planId),
        progress.completedDays(planId),
    ) { planList, days, completed -> Triple(planList, days, completed) }

    val uiState: StateFlow<PlanDetailUiState> = base
        .map { (planList, days, completed) ->
            buildState(planList, days, completed)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanDetailUiState(todayLabel = formatFullDate(LocalDate.now())),
        )

    /** "Concluir dia": marca o dia atual e registra o streak de hoje. */
    fun completeCurrentDay() {
        val day = uiState.value.currentDay ?: return
        viewModelScope.launch {
            progress.markDayCompleted(planId, day.dayNumber)
            progress.markTodayStreak()
        }
    }

    private suspend fun buildState(
        planList: List<ReadPlan>,
        days: List<PlanDay>,
        completed: List<DayCompleted>,
    ): PlanDetailUiState {
        val todayLabel = formatFullDate(LocalDate.now())
        val plan = planList.firstOrNull { it.planId == planId }
        if (plan == null) {
            return PlanDetailUiState(
                loading = false,
                notFound = true,
                todayLabel = todayLabel,
            )
        }

        val doneNumbers = completed.map { it.dayNumber }.toSet()
        val sortedDays = days.sortedBy { it.dayNumber }
        val currentIndex = sortedDays.indexOfFirst { it.dayNumber !in doneNumbers }

        val currentDay: PlanDayItem? =
            if (currentIndex >= 0) sortedDays[currentIndex].toItem(done = false) else null

        val upcoming: List<PlanDayItem> =
            if (currentIndex >= 0) {
                val from = currentIndex + 1
                val to = minOf(from + 4, sortedDays.size)
                if (from < to) {
                    sortedDays.subList(from, to).map {
                        it.toItem(done = it.dayNumber in doneNumbers)
                    }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }

        return PlanDetailUiState(
            loading = false,
            notFound = false,
            planName = plan.name,
            totalDays = plan.totalDays,
            completedCount = doneNumbers.size,
            todayLabel = todayLabel,
            currentDay = currentDay,
            upcoming = upcoming,
        )
    }

    private suspend fun PlanDay.toItem(done: Boolean) = PlanDayItem(
        dayNumber = dayNumber,
        bookId = bookId,
        bookName = bible.book(bookId)?.name ?: bookId,
        startChapter = startChapter,
        endChapter = endChapter,
        done = done,
    )
}
