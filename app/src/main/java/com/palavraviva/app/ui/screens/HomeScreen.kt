package com.palavraviva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.palavraviva.app.ui.Routes
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.GoldLight
import com.palavraviva.app.ui.theme.Indigo
import com.palavraviva.app.ui.theme.IndigoSoft
import com.palavraviva.app.ui.theme.NightInk

@Composable
fun HomeScreen(nav: NavController) {
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
        Text("Bom dia, Matheus ☀️", style = MaterialTheme.typography.titleLarge)
        Text(
            "27 dias de sequência — continue assim!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        // Card "Continuar leitura"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { nav.navigate(Routes.reader("João", 3)) },
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
                        "CONTINUAR LEITURA",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("João 3", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { 0.62f },
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
                        Text("Você parou no v. 16", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        Text("Continuar →", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Card "Plano atual"
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(progress = { 0.39f }, color = Gold, trackColor = MaterialTheme.colorScheme.outline)
                Column {
                    Text("Plano Anual", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Dia 142 de 365 · 39% concluído",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Versículo do dia
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "\u201cLâmpada para os meus pés é a tua palavra, e luz para o meu caminho.\u201d",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                )
                Spacer(Modifier.height(10.dp))
                Text("— SALMOS 119:105", style = MaterialTheme.typography.labelSmall, color = Gold)
            }
        }
        Spacer(Modifier.height(24.dp))

        TextButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
            Text("Ajustes de leitura", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
    }
}
