package com.palavraviva.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.GoldLight
import com.palavraviva.app.ui.viewmodels.ProgressViewModel
import com.palavraviva.app.ui.viewmodels.percentOf

@Composable
fun ProgressScreen() {
    val vm: ProgressViewModel = viewModel {
        ProgressViewModel(PalavraVivaApp.progressRepository, PalavraVivaApp.planRepository)
    }
    val state by vm.uiState.collectAsState()

    // Enquanto o estado carrega, mantém a geometria dos gráficos estável
    val heatmap = state.heatmap.ifEmpty { List(28) { 0.12f } }
    val weeklyBars = state.weeklyBars.ifEmpty { List(8) { 0.04f } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Meu progresso",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )

        // Linha de estatísticas
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard("🔥 ${state.streak}", "dias seguidos", Modifier.weight(1f), highlight = true)
            StatCard("${state.chaptersRead}", "capítulos", Modifier.weight(1f))
            StatCard("${state.activeDays}", "dias lidos", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        // Mapa de calor (últimas 4 semanas, dias com leitura marcados)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("ÚLTIMAS 4 SEMANAS", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    heatmap.forEach { intensity ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    Gold.copy(alpha = intensity.coerceIn(0.12f, 1f)),
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Gráfico de barras: capítulos lidos por semana (últimas 8 semanas)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("CAPÍTULOS POR SEMANA", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    weeklyBars.forEach { h ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(h)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Progresso por plano
        if (state.planProgress.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("SEUS PLANOS", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(12.dp))
                    state.planProgress.forEach { p ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${p.completedDays}/${p.totalDays} dias · ${percentOf(p.completedDays, p.totalDays)}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { percentOf(p.completedDays, p.totalDays) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp)),
                                color = Gold,
                                trackColor = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Conquista por sequência
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(state.achievementEmoji, fontSize = 24.sp)
                Column {
                    Text(state.achievementTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.achievementSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) GoldLight else LocalContentColor.current,
            )
            Text(
                label.uppercase(),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
