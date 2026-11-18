package com.palavraviva.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private data class Verse(val n: Int, val text: String)

private val joao3 = listOf(
    Verse(14, "E como Moisés levantou a serpente no deserto, assim importa que o Filho do homem seja levantado;"),
    Verse(15, "para que todo o que nele crê tenha a vida eterna."),
    Verse(16, "Porque Deus amou o mundo de tal maneira que deu o seu Filho unigênito, para que todo o que nele crê não pereça, mas tenha a vida eterna."),
    Verse(17, "Porque Deus enviou o seu Filho ao mundo não para condenar o mundo, mas para que o mundo seja salvo por ele."),
    Verse(18, "Quem crê nele não é condenado; mas quem não crê já está condenado, porquanto não crê no nome do unigênito Filho de Deus."),
)

@Composable
fun ReaderScreen(book: String, chapter: Int, nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text("$book $chapter", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { nav.navigate(com.palavraviva.app.ui.Routes.SETTINGS) }) {
                Icon(Icons.Default.Settings, "Ajustes de leitura")
            }
        }

        // Passagem
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 4.dp),
        ) {
            joao3.forEach { v ->
                Row {
                    Text(
                        "${v.n}",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp, end = 5.dp),
                    )
                    Text(
                        v.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (v.n == 16) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        // Barra inferior de navegação de capítulo
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = { nav.navigate(com.palavraviva.app.ui.Routes.reader(book, (chapter - 1).coerceAtLeast(1))) },
                label = { Text("Anterior") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) },
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.FavoriteBorder, "Marcar versículo")
            }
            AssistChip(
                onClick = { nav.navigate(com.palavraviva.app.ui.Routes.reader(book, chapter + 1)) },
                label = { Text("Próximo") },
                trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            )
        }
    }
}
