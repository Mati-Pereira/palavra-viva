package com.palavraviva.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.ui.Routes
import com.palavraviva.app.ui.viewmodels.PlanDayItem
import com.palavraviva.app.ui.viewmodels.PlanDetailUiState
import com.palavraviva.app.ui.viewmodels.PlanDetailViewModel
import com.palavraviva.app.ui.viewmodels.percentOf

/** Rótulo da leitura do dia, ex.: "Gênesis 1" ou "Lucas 19–21". */
private fun readingLabel(day: PlanDayItem): String =
    if (day.startChapter == day.endChapter) {
        "${day.bookName} ${day.startChapter}"
    } else {
        "${day.bookName} ${day.startChapter}–${day.endChapter}"
    }

@Composable
fun PlanDetailScreen(planId: String, nav: NavController) {
    val vm: PlanDetailViewModel = viewModel {
        PlanDetailViewModel(
            planId,
            PalavraVivaApp.planRepository,
            PalavraVivaApp.progressRepository,
            PalavraVivaApp.bibleRepository,
        )
    }
    val state by vm.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text(
                state.planName.ifEmpty { "Plano de leitura" },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        when {
            state.loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.notFound -> Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Plano não encontrado", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Este plano não está disponível no momento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> PlanDetailContent(state, nav, onComplete = vm::completeCurrentDay)
        }
    }
}

@Composable
private fun PlanDetailContent(
    state: PlanDetailUiState,
    nav: NavController,
    onComplete: () -> Unit,
) {
    val percent = percentOf(state.completedCount, state.totalDays)
    val currentDay = state.currentDay

    // Cabeçalho do dia
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            progress = { percent / 100f },
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(58.dp),
        )
        Column {
            Text(
                if (currentDay != null) "Dia ${currentDay.dayNumber}" else "Plano concluído 🎉",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                state.todayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$percent% concluído",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    // Leituras de hoje
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("LEITURAS DE HOJE", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(12.dp))
            if (currentDay != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate(Routes.reader(currentDay.bookId, currentDay.startChapter)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, "Ler", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(10.dp))
                    Text(readingLabel(currentDay), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "ler agora →",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Text(
                    "Você concluiu todos os dias deste plano. Parabéns! 🎉",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))

    Button(
        onClick = onComplete,
        enabled = currentDay != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(99.dp),
    ) {
        Text(
            if (currentDay != null) "Marcar dia como lido" else "Dia concluído ✓",
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }

    // Próximos dias
    if (state.upcoming.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("PRÓXIMOS DIAS", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                state.upcoming.forEach { d ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(Routes.reader(d.bookId, d.startChapter)) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = d.done, onCheckedChange = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Dia ${d.dayNumber} · ${readingLabel(d)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
