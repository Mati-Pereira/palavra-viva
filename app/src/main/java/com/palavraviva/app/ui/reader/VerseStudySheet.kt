package com.palavraviva.app.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palavraviva.app.data.ai.VerseStudy
import com.palavraviva.app.data.local.GlossaryTerm
import com.palavraviva.app.data.local.Verse
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.Green

/**
 * Painel de estudo contextual de um versículo:
 * - Glossário histórico-cultural offline detectado automaticamente no texto
 * - Explicação em linguagem clara, contexto da época, termos originais e referências cruzadas clicáveis
 * - Aba "Perguntar" para conversar com a IA sobre a passagem
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseStudySheet(
    bookName: String,
    chapter: Int,
    state: VerseStudyUiState,
    hasApiKey: Boolean,
    onRefresh: () -> Unit,
    onSaveToNote: (Verse, VerseStudy) -> Unit,
    onOpenReference: (bookId: String, chapter: Int) -> Unit,
    onSendChat: (String) -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val verse = state.verse ?: return
    var selectedTab by rememberSaveable(verse.verse) { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            // Cabeçalho da passagem
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$bookName $chapter:${verse.verse}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (state.fromCache && state.study != null) {
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
                            contentDescription = "Gerar nova explicação",
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "\u201c${verse.text}\u201d",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Estudo") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Perguntar") },
                )
            }
            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                VerseStudyContent(
                    verse = verse,
                    state = state,
                    hasApiKey = hasApiKey,
                    onRefresh = onRefresh,
                    onSaveToNote = onSaveToNote,
                    onOpenReference = onOpenReference,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                StudyChatPanel(
                    messages = state.chatMessages,
                    sending = state.chatSending,
                    error = state.chatError,
                    missingApiKey = !hasApiKey,
                    suggestedQuestions = listOf(
                        "Qual era o contexto histórico aqui?",
                        "Há algum costume ou termo da época neste versículo?",
                        "Como este versículo se conecta ao capítulo?",
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
private fun VerseStudyContent(
    verse: Verse,
    state: VerseStudyUiState,
    hasApiKey: Boolean,
    onRefresh: () -> Unit,
    onSaveToNote: (Verse, VerseStudy) -> Unit,
    onOpenReference: (bookId: String, chapter: Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    // 1. Termos do glossário offline detectados imediatamente no texto
    if (state.offlineTerms.isNotEmpty()) {
        StudySectionHeader("NO GLOSSÁRIO OFFLINE")
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.offlineTerms.forEach { term ->
                OfflineGlossaryMiniCard(term)
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
    }

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
                    "Analisando contexto histórico e termos da época…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.study != null -> {
            val study = state.study

            StudySectionHeader("EM LINGUAGEM CLARA")
            Spacer(Modifier.height(6.dp))
            Text(
                study.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))

            StudySectionHeader("CONTEXTO HISTÓRICO E CULTURAL")
            Spacer(Modifier.height(6.dp))
            Text(
                study.historicalContext,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (study.periodTerms.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                StudySectionHeader("TERMOS DA ÉPOCA E ORIGINAIS")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    study.periodTerms.forEach { periodTerm ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        periodTerm.term,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (!periodTerm.originalWord.isNullOrBlank()) {
                                        Text(
                                            periodTerm.originalWord,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontStyle = FontStyle.Italic,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = Gold,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    periodTerm.meaning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (study.crossReferences.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                StudySectionHeader("REFERÊNCIAS CRUZADAS")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    study.crossReferences.forEach { ref ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenReference(ref.bookId, ref.chapter) },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ref.referenceLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (ref.reason.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            ref.reason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Abrir passagem",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onSaveToNote(verse, study) },
                    enabled = !state.savedToNote,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (state.savedToNote) Icons.Filled.Check else Icons.Filled.BookmarkAdded,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.savedToNote) "Salvo nas notas" else "Salvar nas notas")
                }
            }
        }

        state.missingApiKey || !hasApiKey -> {
            MissingApiKeyCard(
                description = "Configure sua chave da API do Gemini nos Ajustes para gerar explicações detalhadas, termos originais e referências cruzadas com cache offline.",
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

@Composable
private fun OfflineGlossaryMiniCard(term: GlossaryTerm) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    term.term,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    term.originalWord ?: term.category,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = Gold,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                term.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (term.historicalContext.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    term.historicalContext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun MissingApiKeyCard(
    description: String,
    onOpenSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Ativar assistente de estudo", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings) {
                Text("Configurar chave nos Ajustes")
            }
        }
    }
}

@Composable
internal fun StudyErrorCard(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Não foi possível concluir o estudo",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRetry) { Text("Tentar novamente") }
                OutlinedButton(onClick = onOpenSettings) { Text("Ajustes") }
            }
        }
    }
}

@Composable
internal fun StudySectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
