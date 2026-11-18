package com.palavraviva.app.ui.screens

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
import androidx.navigation.NavController
import com.palavraviva.app.ui.Routes

private data class Plan(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val action: String,
)

private val plans = listOf(
    Plan("fe", "🔥", "30 dias de Fé", "Hebreus · Tiago · meditações curtas", "Iniciar"),
    Plan("salmos", "🎼", "Salmos em 15 dias", "Um salmo por dia, com devocional", "Iniciar"),
    Plan("cristo", "✝️", "Vida de Cristo", "90 dias pelos 4 Evangelhos", "Em breve"),
    Plan("comuns", "🌅", "Novos começos", "Gênesis · 21 dias", "Iniciar"),
)

@Composable
fun PlansScreen(nav: NavController) {
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

        // Card destaque: Plano Anual em andamento
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(progress = { 0.39f }, color = MaterialTheme.colorScheme.secondary)
                Column(Modifier.weight(1f)) {
                    Text("Plano Anual", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Dia 142 · 223 dias restantes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        plans.forEach { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                onClick = { if (p.action == "Iniciar") nav.navigate(Routes.planDetail(p.id)) },
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(p.emoji, fontSize = 22.sp)
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            p.desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (p.action == "Iniciar") {
                        Text(
                            p.action,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
