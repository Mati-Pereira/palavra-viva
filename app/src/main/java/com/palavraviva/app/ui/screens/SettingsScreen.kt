package com.palavraviva.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.Sepia
import com.palavraviva.app.ui.theme.NightCard

enum class ReadTheme(val label: String, val bg: Color, val fg: Color) {
    LIGHT("Claro", Color(0xFFFAF7F0), Color(0xFF1B1B33)),
    SEPIA("Sépia", Sepia, Color(0xFF4A3B28)),
    DARK("Escuro", Color(0xFF12122A), Color(0xFFF0EDF7)),
}

@Composable
fun SettingsScreen(nav: NavController) {
    var theme by remember { mutableStateOf(ReadTheme.LIGHT) }
    var fontSize by remember { mutableStateOf(17f) }
    var versesPerLine by remember { mutableStateOf(true) }
    var autoNight by remember { mutableStateOf(false) }

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
            Text("Aparência & leitura", style = MaterialTheme.typography.titleMedium)
        }

        // Tema
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("TEMA", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadTheme.entries.forEach { t ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = if (theme == t)
                                    androidx.compose.foundation.BorderStroke(2.dp, Gold)
                                else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(t.bg)
                                        .padding(12.dp),
                                ) {
                                    repeat(3) { i ->
                                        Box(
                                            Modifier
                                                .fillMaxWidth(fractions[i])
                                                .height(6.dp)
                                                .background(t.fg.copy(alpha = if (i == 0) 1f else 0.3f), RoundedCornerShape(4.dp)),
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            FilterChip(
                                selected = theme == t,
                                onClick = { theme = t },
                                label = { Text(t.label, fontSize = 11.sp) },
                            )
                        }
                    }
                }
            }
        }

        // Tamanho da fonte
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("TAMANHO DA FONTE", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 14f..24f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
                    )
                    Text("A", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                }
                Text(
                    "Prévia: Porque Deus amou o mundo…",
                    fontSize = fontSize.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Opções
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                SettingRow("Versículos em linhas", "Um versículo por linha", versesPerLine) { versesPerLine = it }
                HorizontalDivider(
                    Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
                SettingRow("Modo noite automático", "Após 20h, troca para escuro", autoNight) { autoNight = it }
            }
        }

        // Versão da Bíblia
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Versão da Bíblia", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Almeida Revista e Atualizada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "ARA ▾",
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private val fractions = listOf(0.7f, 0.55f, 0.6f)

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
