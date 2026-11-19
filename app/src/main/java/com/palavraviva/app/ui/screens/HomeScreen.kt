package com.palavraviva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.ui.Routes
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.GoldLight
import com.palavraviva.app.ui.theme.Indigo
import com.palavraviva.app.ui.theme.IndigoSoft
import com.palavraviva.app.ui.viewmodels.HomeViewModel
import com.palavraviva.app.ui.viewmodels.percentOf

@Composable
fun HomeScreen(nav: NavController) {
    val vm: HomeViewModel = viewModel {
        HomeViewModel(
            PalavraVivaApp.bibleRepository,
            PalavraVivaApp.planRepository,
            PalavraVivaApp.progressRepository,
        )
    }
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Palavra Viva",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(14.dp))
        Text(state.greeting, style = MaterialTheme.typography.titleLarge)
        Text(
            if (state.streak > 0) {
                "${state.streak} ${if (state.streak == 1) "dia" else "dias"} de sequência — continue assim!"
            } else {
                "Comece sua sequência hoje — leia um capítulo"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        // Card "Continuar leitura" (último capítulo lido)
        val cont = state.continueReading
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nav.navigate(
                        if (cont != null) Routes.reader(cont.bookId, cont.chapter) else Routes.reader("jo", 3),
                    )
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                Modifier
                    .background(Brush.linearGradient(listOf(Indigo, IndigoSoft)))
                    .padding(18.dp),
            ) {
                Column {
                    Text(
                        if (cont != null) "CONTINUAR LEITURA" else "COMEÇAR LEITURA",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (cont != null) "${cont.bookName} ${cont.chapter}" else "João 3",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    if (cont != null) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { cont.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = Gold,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Você parou no cap. ${cont.chapter}", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                            Text("Continuar →", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Comece pelo Evangelho de João", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                            Text("Abrir →", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Card "Plano atual" (plano em andamento ou sugestão do primeiro plano)
        val plan = state.planCard
        if (plan != null) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(Routes.planDetail(plan.planId)) },
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(
                        progress = { percentOf(plan.completedDays, plan.totalDays) / 100f },
                        color = Gold,
                        trackColor = MaterialTheme.colorScheme.outline,
                    )
                    Column {
                        Text(plan.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (plan.started) {
                                "Dia ${plan.currentDay} de ${plan.totalDays} · ${percentOf(plan.completedDays, plan.totalDays)}% concluído"
                            } else {
                                "Comece hoje · ${plan.totalDays} dias"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(Routes.PLANS) },
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("📖", fontSize = 24.sp)
                    Column {
                        Text("Escolha um plano de leitura", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Planos diários para cultivar o hábito da leitura",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Versículo do dia
        val verse = state.verseOfDay
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    verse?.text?.let { "\u201c$it\u201d" } ?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    verse?.reference?.uppercase() ?: "VERSÍCULO DO DIA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        TextButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
            Text("Ajustes de leitura", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
    }
}
