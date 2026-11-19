package com.palavraviva.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palavraviva.app.data.local.ReadPlan
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

data class PlanListItemUi(
    val planId: String,
    val name: String,
    val description: String,
    val totalDays: Int,
    val completedDays: Int,
)

data class PlansUiState(
    val loading: Boolean = true,
    /** Plano em andamento exibido no card destaque (não repete na lista). */
    val inProgress: PlanListItemUi? = null,
    val plans: List<PlanListItemUi> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanListViewModel(
    private val planRepository: PlanRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private data class Entry(val plan: ReadPlan, val completed: Int, val lastCompletedAt: Long)

    /** Planos + contagem de dias concluídos de cada um, reativo a qualquer marcação. */
    private val entriesFlow: Flow<List<Entry>> =
        planRepository.plans().flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(list.map { plan ->
                    progressRepository.completedDays(plan.planId).map { days ->
                        Entry(plan, days.size, days.maxOfOrNull { it.completedAt } ?: 0L)
                    }
                }) { it.toList() }
            }
        }

    val uiState: StateFlow<PlansUiState> = entriesFlow
        .map { entries ->
            val active = entries
                .filter { it.completed > 0 && it.completed < it.plan.totalDays }
                .maxByOrNull { it.lastCompletedAt }
            PlansUiState(
                loading = false,
                inProgress = active?.let { it.toUi() },
                plans = entries
                    .filterNot { it === active }
                    .map { it.toUi() },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlansUiState(),
        )

    private fun Entry.toUi() = PlanListItemUi(
        planId = plan.planId,
        name = plan.name,
        description = plan.description,
        totalDays = plan.totalDays,
        completedDays = completed,
    )
}
