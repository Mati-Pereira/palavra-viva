package com.palavraviva.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.palavraviva.app.data.local.Verse
import com.palavraviva.app.ui.Routes
import com.palavraviva.app.ui.reader.ChapterStudySheet
import com.palavraviva.app.ui.reader.ReaderBottomBar
import com.palavraviva.app.ui.reader.ReaderViewModel
import com.palavraviva.app.ui.reader.VerseActionsSheet
import com.palavraviva.app.ui.reader.VerseRow
import com.palavraviva.app.ui.reader.VerseStudySheet
import com.palavraviva.app.ui.reader.highlightColorOf

/**
 * Tela de leitura (Tópico 6): capítulo real do banco, destaque, marcadores,
 * notas e progresso. `book` pode ser um bookId ("gn") ou um nome ("João"),
 * conforme as rotas existentes.
 */
@Composable
fun ReaderScreen(book: String, chapter: Int, nav: NavController) {
    val vm: ReaderViewModel = viewModel(factory = ReaderViewModel.factory(book, chapter))
    val state by vm.uiState.collectAsState()
    val verseStudyState by vm.verseStudyState.collectAsState()
    val chapterGuideState by vm.chapterGuideState.collectAsState()
    val hasApiKey by vm.hasApiKey.collectAsState()
    var sheetVerse by remember { mutableStateOf<Verse?>(null) }
    val scroll = rememberScrollState()

    // Volta ao topo quando o capítulo muda.
    LaunchedEffect(state.book?.bookId, state.chapter) { scroll.scrollTo(0) }

    Column(Modifier.fillMaxSize()) {
        // Barra superior — título com nome real do livro
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
            Text(
                "${state.book?.name ?: book} ${state.chapter}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.isReady && state.verses.isNotEmpty()) {
                IconButton(onClick = { vm.openChapterGuide() }) {
                    Icon(Icons.Filled.AutoAwesome, "Guia do capítulo")
                }
            }
            IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                Icon(Icons.Default.Settings, "Ajustes de leitura")
            }
        }

        when {
            // Aguardando a primeira resposta do banco
            state.resolving -> Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            // Banco vazio (importação da Bíblia pendente) — fallback elegante
            !state.isReady -> BibleNotAvailableNotice(
                modifier = Modifier.weight(1f),
                onRetry = vm::retry,
            )

            else -> {
                // Passagem
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(horizontal = 22.dp, vertical = 4.dp),
                ) {
                    when {
                        !state.versesLoaded -> Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }

                        state.verses.isEmpty() -> EmptyChapterNotice()

                        else -> state.verses.forEach { v ->
                            VerseRow(
                                number = v.verse,
                                text = v.text,
                                highlightColor = highlightColorOf(state.highlightFor(v.verse)),
                                isBookmarked = state.isBookmarked(v.verse),
                                hasNote = state.noteFor(v.verse) != null,
                                onClick = { sheetVerse = v },
                                onLongClick = { sheetVerse = v },
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Navegação de capítulo + leitura concluída
                ReaderBottomBar(
                    chapter = state.chapter,
                    chapterCount = state.chapterCount,
                    isRead = state.isChapterRead,
                    onPrevious = vm::previousChapter,
                    onNext = vm::nextChapter,
                    onMarkRead = vm::markChapterAsRead,
                )
            }
        }
    }

    // Ações do versículo selecionado (destaque, marcador, nota, estudo)
    sheetVerse?.let { target ->
        VerseActionsSheet(
            verse = target,
            highlight = state.highlightFor(target.verse),
            bookmark = state.bookmarks.firstOrNull { it.verse == target.verse },
            note = state.noteFor(target.verse),
            hasCachedStudy = state.hasCachedStudy(target.verse),
            onOpenStudy = {
                sheetVerse = null
                vm.openVerseStudy(target)
            },
            onToggleHighlight = { argb -> vm.toggleHighlight(target.verse, argb) },
            onToggleBookmark = { vm.toggleBookmark(target.verse) },
            onSaveBookmarkLabel = { vm.saveBookmarkLabel(target.verse, it) },
            onSaveNote = { vm.saveNote(target.verse, it) },
            onDeleteNote = { vm.deleteNote(target.verse) },
            onDismiss = { sheetVerse = null },
        )
    }

    // Estudo contextual do versículo (glossário offline + IA + chat)
    if (verseStudyState.verse != null) {
        VerseStudySheet(
            bookName = state.book?.name ?: book,
            chapter = state.chapter,
            state = verseStudyState,
            hasApiKey = hasApiKey,
            onRefresh = { verseStudyState.verse?.let { vm.openVerseStudy(it, forceRefresh = true) } },
            onSaveToNote = vm::saveVerseStudyToNote,
            onOpenReference = { refBookId, refChapter ->
                vm.closeVerseStudy()
                nav.navigate(Routes.reader(refBookId, refChapter))
            },
            onSendChat = vm::sendVerseChat,
            onClearChat = vm::clearVerseChat,
            onOpenSettings = {
                vm.closeVerseStudy()
                nav.navigate(Routes.SETTINGS)
            },
            onDismiss = vm::closeVerseStudy,
        )
    }

    // Guia de contexto do capítulo inteiro + chat
    if (chapterGuideState.visible) {
        ChapterStudySheet(
            bookName = state.book?.name ?: book,
            chapter = state.chapter,
            state = chapterGuideState,
            hasApiKey = hasApiKey,
            onRefresh = { vm.openChapterGuide(forceRefresh = true) },
            onSendChat = vm::sendChapterChat,
            onClearChat = vm::clearChapterChat,
            onOpenSettings = {
                vm.closeChapterGuide()
                nav.navigate(Routes.SETTINGS)
            },
            onDismiss = vm::closeChapterGuide,
        )
    }
}

/** Fallback quando a Bíblia ainda não foi importada (primeiro start). */
@Composable
private fun BibleNotAvailableNotice(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "A Bíblia ainda não foi carregada",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "O texto bíblico é importado automaticamente no primeiro início do aplicativo. " +
                "Esta tela é atualizada sozinha assim que a importação terminar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = onRetry) { Text("Tentar novamente") }
    }
}

/** Capítulo sem versículos registrados (dados incompletos). */
@Composable
private fun EmptyChapterNotice(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nada por aqui.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Este capítulo não possui versículos registrados.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
