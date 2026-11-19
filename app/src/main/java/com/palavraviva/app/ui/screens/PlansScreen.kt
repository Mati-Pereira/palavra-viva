package com.palavraviva.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.palavraviva.app.ui.viewmodels.PlanListItemUi
import com.palavraviva.app.ui.viewmodels.PlanListViewModel
import com.palavraviva.app.ui.viewmodels.percentOf

/** Emoji por plano (identidade visual dos cards); fallback neutro. */
private fun emojiFor(planId: String): String = when (planId) {
    // planIds semeados pelo PlanSeeder (data/bible)
    "salmos-proverbios-31" -> "🎼"
    "novo-testamento-90" -> "✝️"
    "evangelhos-30" -> "🕊️"
    else -> "📖"
}

@Composable
fun PlansScreen(nav: NavController) {
    val vm: PlanListViewModel = viewModel {
        PlanListViewModel(PalavraVivaApp.planRepository, PalavraVivaApp.progressRepository)
    }
    val state by vm.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Planos de leitura",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )

        when {
            state.loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.plans.isEmpty() && state.inProgress == null -> Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Nenhum plano disponível ainda", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Os planos de leitura aparecerão aqui assim que forem carregados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                // Card destaque: plano em andamento
                state.inProgress?.let { p ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { nav.navigate(Routes.planDetail(p.planId)) },
                    ) {
                        Row(
                            Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(
                                progress = { percentOf(p.completedDays, p.totalDays) / 100f },
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Dia ${p.completedDays + 1} · ${p.totalDays - p.completedDays} dias restantes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                state.plans.forEach { p ->
                    PlanCard(p) { nav.navigate(Routes.planDetail(p.planId)) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PlanCard(p: PlanListItemUi, onClick: () -> Unit) {
    val action = when {
        p.completedDays >= p.totalDays -> "✓"
        p.completedDays > 0 -> "Continuar"
        else -> "Iniciar"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(emojiFor(p.planId), fontSize = 22.sp)
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (p.description.isBlank()) "${p.totalDays} dias" else p.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (p.completedDays > 0) {
                    Text(
                        "${p.completedDays} de ${p.totalDays} dias concluídos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text(
                action,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
