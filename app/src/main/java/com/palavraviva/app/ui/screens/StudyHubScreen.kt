package com.palavraviva.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.data.local.GlossaryTerm
import com.palavraviva.app.data.local.Highlight
import com.palavraviva.app.data.local.Note
import com.palavraviva.app.data.local.StudyCache
import com.palavraviva.app.data.local.VerseSearchResult
import com.palavraviva.app.data.repositories.StudyRepository
import com.palavraviva.app.ui.Routes
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.Green
import com.palavraviva.app.ui.viewmodels.StudyHubUiState
import com.palavraviva.app.ui.viewmodels.StudyHubViewModel

/**
 * Central de Estudos Offline + IA:
 * 1. Buscar na Bíblia offline
 * 2. Glossário histórico-cultural e linguístico
 * 3. Caderno pessoal (Notas, Destaques e Estudos salvos pela IA)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyHubScreen(nav: NavController) {
    val vm: StudyHubViewModel = viewModel {
        StudyHubViewModel(
            PalavraVivaApp.bibleRepository,
            PalavraVivaApp.highlightRepository,
            PalavraVivaApp.studyRepository,
        )
    }
    val state by vm.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp),
        ) {
            Text("Estudo", style = MaterialTheme.typography.titleLarge)
            Text(
                "Busca bíblica, glossário de época e seu caderno offline",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Buscar") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Glossário") },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Caderno") },
            )
        }

        when (selectedTab) {
            0 -> SearchSection(
                query = state.searchQuery,
                searching = state.searching,
                results = state.searchResults,
                onQueryChange = vm::onSearchQueryChange,
                onOpenPassage = { bookId, chapter -> nav.navigate(Routes.reader(bookId, chapter)) },
            )

            1 -> GlossarySection(
                query = state.glossaryQuery,
                terms = state.glossaryTerms,
                onQueryChange = vm::onGlossaryQueryChange,
            )

            else -> NotebookSection(
                state = state,
                onOpenPassage = { bookId, chapter -> nav.navigate(Routes.reader(bookId, chapter)) },
            )
        }
    }
}

@Composable
private fun SearchSection(
    query: String,
    searching: Boolean,
    results: List<VerseSearchResult>,
    onQueryChange: (String) -> Unit,
    onOpenPassage: (bookId: String, chapter: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar palavra ou expressão (ex.: pastor, fé, odre)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        when {
            searching -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            query.trim().length < 2 -> EmptyStudyHint(
                title = "Busca 100% offline",
                subtitle = "Digite ao menos 2 letras para localizar versículos em toda a Bíblia.",
            )

            results.isEmpty() -> EmptyStudyHint(
                title = "Nenhum versículo encontrado",
                subtitle = "Tente outro termo ou uma variação sem flexão (ex.: \"amor\" ou \"semear\").",
            )

            else -> {
                Text(
                    "${results.size}${if (results.size >= 80) "+" else ""} versículos encontrados",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    items(
                        items = results,
                        key = { "${it.bookId}:${it.chapter}:${it.verse}" },
                    ) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPassage(item.bookId, item.chapter) },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${item.bookName} ${item.chapter}:${item.verse}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        item.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Abrir capítulo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlossarySection(
    query: String,
    terms: List<GlossaryTerm>,
    onQueryChange: (String) -> Unit,
) {
    var selectedCategory by rememberSaveable { mutableStateOf("Todos") }
    val categories = remember(terms) {
        listOf("Todos") + terms.map { it.category }.distinct().sorted()
    }
    val visibleTerms = remember(terms, selectedCategory) {
        if (selectedCategory == "Todos") terms else terms.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Filtrar termos da época (ex.: denário, fariseu)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Limpar filtro")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (visibleTerms.isEmpty()) {
            EmptyStudyHint(
                title = "Nenhum termo encontrado",
                subtitle = "Limpe o filtro para ver todo o glossário histórico e cultural.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(visibleTerms, key = { it.termKey }) { term ->
                    GlossaryTermCard(term)
                }
            }
        }
    }
}

@Composable
private fun GlossaryTermCard(term: GlossaryTerm) {
    var expanded by rememberSaveable(term.termKey) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(term.term, style = MaterialTheme.typography.titleMedium)
                    if (!term.originalWord.isNullOrBlank()) {
                        Text(
                            term.originalWord,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = Gold,
                            fontSize = 12.sp,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        term.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                term.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (expanded && term.historicalContext.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    term.historicalContext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (term.historicalContext.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Toque para ver o contexto da época",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun NotebookSection(
    state: StudyHubUiState,
    onOpenPassage: (bookId: String, chapter: Int) -> Unit,
) {
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0=Notas, 1=Destaques, 2=Estudos IA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter == 0,
                onClick = { filter = 0 },
                label = { Text("Notas (${state.notes.size})") },
            )
            FilterChip(
                selected = filter == 1,
                onClick = { filter = 1 },
                label = { Text("Destaques (${state.highlights.size})") },
            )
            FilterChip(
                selected = filter == 2,
                onClick = { filter = 2 },
                label = { Text("Estudos IA (${state.cachedStudies.size})") },
            )
        }
        Spacer(Modifier.height(10.dp))

        when (filter) {
            0 -> {
                if (state.notes.isEmpty()) {
                    EmptyStudyHint(
                        title = "Sem notas ainda",
                        subtitle = "Toque em qualquer versículo durante a leitura para escrever anotações ou salvar estudos da IA.",
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                    ) {
                        items(state.notes, key = { it.id }) { note ->
                            NoteNotebookCard(
                                bookName = state.bookName(note.bookId),
                                note = note,
                                onClick = { onOpenPassage(note.bookId, note.chapter) },
                            )
                        }
                    }
                }
            }

            1 -> {
                if (state.highlights.isEmpty()) {
                    EmptyStudyHint(
                        title = "Sem destaques ainda",
                        subtitle = "Toque em um versículo e escolha uma cor para destacá-lo no leitor.",
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                    ) {
                        items(
                            items = state.highlights,
                            key = { "${it.bookId}:${it.chapter}:${it.verse}" },
                        ) { hl ->
                            HighlightNotebookCard(
                                bookName = state.bookName(hl.bookId),
                                highlight = hl,
                                onClick = { onOpenPassage(hl.bookId, hl.chapter) },
                            )
                        }
                    }
                }
            }

            else -> {
                if (state.cachedStudies.isEmpty()) {
                    EmptyStudyHint(
                        title = "Nenhum estudo salvo offline",
                        subtitle = "Quando você consultar a IA sobre um versículo ou capítulo, a explicação ficará disponível aqui offline.",
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                    ) {
                        items(state.cachedStudies, key = { it.cacheKey }) { cached ->
                            CachedStudyNotebookCard(
                                item = cached,
                                onClick = { onOpenPassage(cached.bookId, cached.chapter) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteNotebookCard(
    bookName: String,
    note: Note,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "$bookName ${note.chapter}:${note.verse}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                note.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HighlightNotebookCard(
    bookName: String,
    highlight: Highlight,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(highlight.color.toInt())),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "$bookName ${highlight.chapter}:${highlight.verse}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Abrir passagem",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CachedStudyNotebookCard(
    item: StudyCache,
    onClick: () -> Unit,
) {
    val kindLabel = if (item.kind == StudyRepository.KIND_CHAPTER) "Guia do capítulo" else "Estudo do versículo"
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$kindLabel · Salvo offline (${item.model})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = Green,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Abrir passagem",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun EmptyStudyHint(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
