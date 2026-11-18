package com.palavraviva.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private data class Day(val day: Int, val reading: String, val done: Boolean)

private val days = listOf(
    Day(142, "Gênesis 44–46 · Lucas 19", done = true),
    Day(143, "Gênesis 47–50 · Salmo 22", done = false),
    Day(144, "Êxodo 1–3 · Lucas 20", done = false),
    Day(145, "Êxodo 4–6 · Lucas 21", done = false),
)

@Composable
fun PlanDetailScreen(planId: String, nav: NavController) {
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
            Text("Plano Anual", style = MaterialTheme.typography.titleMedium)
        }

        // Cabeçalho do dia
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                progress = { 0.39f },
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(58.dp),
            )
            Column {
                Text("Dia 142", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Terça, 17 de novembro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "39% concluído",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                days.take(1).forEach { d ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Check, "Lido", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Text(d.reading, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // checkbox do dia
                    Checkbox(checked = false, onCheckedChange = {})
                    Spacer(Modifier.width(10.dp))
                    Text("Lucas 19", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "ler agora →",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(99.dp),
        ) {
            Text("Marcar dia como lido", modifier = Modifier.padding(vertical = 4.dp))
        }

        // Próximos dias
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("PRÓXIMOS DIAS", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                days.drop(1).forEach { d ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = d.done, onCheckedChange = {})
                        Spacer(Modifier.width(10.dp))
                        Text("Dia ${d.day} · ${d.reading}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
