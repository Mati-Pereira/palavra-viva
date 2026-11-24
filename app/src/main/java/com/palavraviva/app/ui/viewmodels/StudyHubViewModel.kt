package com.palavraviva.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palavraviva.app.data.local.GlossaryTerm
import com.palavraviva.app.data.local.Highlight
import com.palavraviva.app.data.local.Note
import com.palavraviva.app.data.local.StudyCache
import com.palavraviva.app.data.local.VerseSearchResult
import com.palavraviva.app.data.repositories.BibleRepository
import com.palavraviva.app.data.repositories.HighlightRepository
import com.palavraviva.app.data.repositories.StudyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Estado unificado da aba Estudo (Buscar · Glossário · Caderno). */
data class StudyHubUiState(
    val searchQuery: String = "",
    val searching: Boolean = false,
    val searchResults: List<VerseSearchResult> = emptyList(),
    val glossaryQuery: String = "",
    val glossaryTerms: List<GlossaryTerm> = emptyList(),
    val notes: List<Note> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val cachedStudies: List<StudyCache> = emptyList(),
    val bookNames: Map<String, String> = emptyMap(),
) {
    fun bookName(bookId: String): String = bookNames[bookId] ?: bookId.uppercase()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class StudyHubViewModel(
    private val bible: BibleRepository,
    highlights: HighlightRepository,
    private val study: StudyRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _searching = MutableStateFlow(false)
    private val _glossaryQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val glossaryQuery: StateFlow<String> = _glossaryQuery.asStateFlow()

    private val searchResultsFlow: Flow<List<VerseSearchResult>> = _searchQuery
        .debounce(260)
        .mapLatest { query ->
            val trimmed = query.trim()
            if (trimmed.length < 2) {
                _searching.value = false
                emptyList()
            } else {
                _searching.value = true
                try {
                    bible.searchVerses(trimmed)
                } finally {
                    _searching.value = false
                }
            }
        }.onStart { emit(emptyList()) }

    private val glossaryFlow: Flow<List<GlossaryTerm>> = _glossaryQuery
        .debounce(150)
        .distinctUntilChanged()
        .flatMapLatest { q -> study.observeGlossary(q) }

    private val bookNamesFlow: Flow<Map<String, String>> = bible.books()
        .mapLatest { list -> list.associate { it.bookId to it.name } }

    private val notebookFlow = combine(
        highlights.allNotes(),
        highlights.allHighlights(),
        study.observeAllCachedStudies(),
        bookNamesFlow,
    ) { notes, allHighlights, cached, names ->
        NotebookBundle(notes, allHighlights, cached, names)
    }

    val uiState: StateFlow<StudyHubUiState> = combine(
        _searchQuery,
        _searching,
        searchResultsFlow,
        _glossaryQuery,
        glossaryFlow,
        notebookFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val notebook = values[5] as NotebookBundle
        StudyHubUiState(
            searchQuery = values[0] as String,
            searching = values[1] as Boolean,
            searchResults = values[2] as List<VerseSearchResult>,
            glossaryQuery = values[3] as String,
            glossaryTerms = values[4] as List<GlossaryTerm>,
            notes = notebook.notes,
            highlights = notebook.highlights,
            cachedStudies = notebook.cachedStudies,
            bookNames = notebook.bookNames,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudyHubUiState(),
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onGlossaryQueryChange(query: String) {
        _glossaryQuery.value = query
    }

    private data class NotebookBundle(
        val notes: List<Note>,
        val highlights: List<Highlight>,
        val cachedStudies: List<StudyCache>,
        val bookNames: Map<String, String>,
    )
}
