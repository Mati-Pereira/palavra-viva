package com.palavraviva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.GoldLight

private val weekBars = listOf(0.45f, 0.62f, 0.38f, 0.74f, 0.55f, 0.88f, 0.66f, 0.95f)

@Composable
fun ProgressScreen() {
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
            StatCard("🔥 27", "dias seguidos", Modifier.weight(1f), highlight = true)
            StatCard("843", "capítulos", Modifier.weight(1f))
            StatCard("210", "dias lidos", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        // Mapa de calor simplificado (últimas 4 semanas)
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
                    repeat(28) { i ->
                        val intensity = when (i % 4) {
                            0 -> 0.25f
                            1 -> 0.45f
                            2 -> 0.7f
                            else -> 1f
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    Gold.copy(alpha = intensity),
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Gráfico de barras semanais
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
                    weekBars.forEach { h ->
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

        // Conquista
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
                Text("🏆", fontSize = 24.sp)
                Column {
                    Text("Conquista desbloqueada!", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "25 dias de sequência — \u201cConstância\u201d",
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
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
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
