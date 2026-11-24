package com.palavraviva.app.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.Green

/**
 * Painel de Guia de Contexto do Capítulo inteiro:
 * panorama, posição no livro, personagens/lugares e perguntas de reflexão,
 * além de chat contextual sobre o capítulo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterStudySheet(
    bookName: String,
    chapter: Int,
    state: ChapterGuideUiState,
    hasApiKey: Boolean,
    onRefresh: () -> Unit,
    onSendChat: (String) -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by rememberSaveable(bookName, chapter) { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Guia de $bookName $chapter",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (state.fromCache && state.guide != null) {
                        Text(
                            "Salvo offline · ${state.modelUsed ?: "Cache local"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = Green,
                        )
                    }
                }
                if (selectedTab == 0 && !state.loading && hasApiKey) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Atualizar guia",
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Guia do capítulo") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Perguntar") },
                )
            }
            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                ChapterGuideContent(
                    state = state,
                    hasApiKey = hasApiKey,
                    onRefresh = onRefresh,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                StudyChatPanel(
                    messages = state.chatMessages,
                    sending = state.chatSending,
                    error = state.chatError,
                    missingApiKey = !hasApiKey,
                    suggestedQuestions = listOf(
                        "Qual é o tema principal deste capítulo?",
                        "Quem são os grupos ou personagens citados?",
                        "Qual é o cenário histórico deste trecho?",
                    ),
                    onSend = onSendChat,
                    onClear = onClearChat,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun ChapterGuideContent(
    state: ChapterGuideUiState,
    hasApiKey: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when {
        state.loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(
                    "Montando o guia histórico do capítulo…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.guide != null -> {
            val guide = state.guide

            StudySectionHeader("PANORAMA DO CAPÍTULO")
            Spacer(Modifier.height(6.dp))
            Text(
                guide.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))

            StudySectionHeader("ONDE SE ENCAIXA NO LIVRO")
            Spacer(Modifier.height(6.dp))
            Text(
                guide.bookPlacement,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (guide.keyFiguresAndPlaces.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                StudySectionHeader("PERSONAGENS E LUGARES")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    guide.keyFiguresAndPlaces.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }

            if (guide.reflectionQuestions.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                StudySectionHeader("PERGUNTAS PARA REFLEXÃO")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    guide.reflectionQuestions.forEachIndexed { idx, question ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "${idx + 1}.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Gold,
                                )
                                Text(
                                    question,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }
        }

        state.missingApiKey || !hasApiKey -> {
            MissingApiKeyCard(
                description = "Configure sua chave da API do Gemini nos Ajustes para gerar o guia histórico deste capítulo (com salvamento offline automático).",
                onOpenSettings = onOpenSettings,
            )
        }

        !state.error.isNullOrBlank() -> {
            StudyErrorCard(
                message = state.error,
                onRetry = onRefresh,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}
