package com.palavraviva.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.data.ai.AiError
import com.palavraviva.app.data.ai.ChapterGuide
import com.palavraviva.app.data.ai.StudyPrompts
import com.palavraviva.app.data.ai.VerseStudy
import com.palavraviva.app.data.local.Book
import com.palavraviva.app.data.local.Bookmark
import com.palavraviva.app.data.local.GlossaryTerm
import com.palavraviva.app.data.local.Highlight
import com.palavraviva.app.data.local.Note
import com.palavraviva.app.data.local.StudyChatMessage
import com.palavraviva.app.data.local.Verse
import java.text.Normalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado imutável consumido pela tela de leitura. */
data class ReaderUiState(
    val resolving: Boolean = true,       // aguardando a primeira resposta do banco
    val dbEmpty: Boolean = false,       // banco sem livros (importação pendente)
    val book: Book? = null,
    val chapter: Int = 1,
    val chapterCount: Int = 0,
    val verses: List<Verse> = emptyList(),
    val versesLoaded: Boolean = false,  // capítulo já respondeu (evita "vazio" falso)
    val highlights: List<Highlight> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val notes: List<Note> = emptyList(),
    val cachedStudyKeys: Set<String> = emptySet(),
    val isChapterRead: Boolean = false,
) {
    val isReady: Boolean get() = book != null

    fun highlightFor(verseNumber: Int): Highlight? = highlights.firstOrNull { it.verse == verseNumber }

    fun isBookmarked(verseNumber: Int): Boolean = bookmarks.any { it.verse == verseNumber }

    fun noteFor(verseNumber: Int): Note? = notes.firstOrNull { it.verse == verseNumber }

    fun hasCachedStudy(verseNumber: Int): Boolean {
        val id = book?.bookId ?: return false
        return "verse:$id:$chapter:$verseNumber" in cachedStudyKeys
    }
}

/** Estado do painel de estudo de um versículo. */
data class VerseStudyUiState(
    val verse: Verse? = null,
    val offlineTerms: List<GlossaryTerm> = emptyList(),
    val loading: Boolean = false,
    val study: VerseStudy? = null,
    val fromCache: Boolean = false,
    val modelUsed: String? = null,
    val missingApiKey: Boolean = false,
    val error: String? = null,
    val savedToNote: Boolean = false,
    val chatMessages: List<StudyChatMessage> = emptyList(),
    val chatSending: Boolean = false,
    val chatError: String? = null,
)

/** Estado do painel de guia de contexto do capítulo. */
data class ChapterGuideUiState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val guide: ChapterGuide? = null,
    val fromCache: Boolean = false,
    val modelUsed: String? = null,
    val missingApiKey: Boolean = false,
    val error: String? = null,
    val chatMessages: List<StudyChatMessage> = emptyList(),
    val chatSending: Boolean = false,
    val chatError: String? = null,
)

private data class ChapterKey(val bookId: String, val chapter: Int)

private data class ChapterContent(
    val verses: List<Verse>,
    val highlights: List<Highlight>,
    val bookmarks: List<Bookmark>,
    val notes: List<Note>,
    val cachedKeys: Set<String>,
    val isRead: Boolean,
)

private data class ReaderMeta(val book: Book?, val chapterCount: Int, val chapter: Int)

/**
 * ViewModel da tela de leitura (Tópico 6): versículos reais, destaque,
 * marcadores, notas e progresso de leitura.
 *
 * O parâmetro de navegação pode ser um bookId ("gn") ou um nome ("João"),
 * conforme as rotas atuais — [resolveBook] aceita ambos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
    private val bookInput: String,
    startChapter: Int,
) : ViewModel() {

    private val bibleRepository get() = PalavraVivaApp.bibleRepository
    private val progressRepository get() = PalavraVivaApp.progressRepository
    private val highlightRepository get() = PalavraVivaApp.highlightRepository
    private val studyRepository get() = PalavraVivaApp.studyRepository

    private val _book = MutableStateFlow<Book?>(null)
    private val _chapterCount = MutableStateFlow(0)
    private val _chapter = MutableStateFlow(startChapter.coerceAtLeast(1))
    private val _dbChecked = MutableStateFlow(false)
    private val _versesLoaded = MutableStateFlow(false)
    private var resolveJob: Job? = null
    private var verseStudyJob: Job? = null
    private var verseChatJob: Job? = null
    private var chapterGuideJob: Job? = null
    private var chapterChatJob: Job? = null

    private val _verseStudyState = MutableStateFlow(VerseStudyUiState())
    val verseStudyState: StateFlow<VerseStudyUiState> = _verseStudyState.asStateFlow()

    private val _chapterGuideState = MutableStateFlow(ChapterGuideUiState())
    val chapterGuideState: StateFlow<ChapterGuideUiState> = _chapterGuideState.asStateFlow()

    val hasApiKey: StateFlow<Boolean> = studyRepository.observeHasApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** true quando o livro exato foi encontrado (fallbacks param de ser tentados). */
    private var exactMatch = false

    /** Contador de retomadas da resolução após exceção (limitado a 5). */
    private var resolveAttempts = 0

    /** Capítulo pedido na rota, preservado caso o livro exato chegue depois. */
    private val requestedChapter = startChapter.coerceAtLeast(1)

    /** Chave do capítulo atual (nula até o livro ser resolvido). */
    private val chapterKey: Flow<ChapterKey?> =
        combine(_book, _chapter) { book, chapter ->
            book?.let { ChapterKey(it.bookId, chapter) }
        }.distinctUntilChanged()

    /** Conteúdo reativo do capítulo: versículos, anotações e leitura. */
    private val contentFlow: Flow<ChapterContent?> = chapterKey.flatMapLatest { key ->
        if (key == null) {
            flowOf(null)
        } else {
            _versesLoaded.value = false
            combine(
                bibleRepository.verses(key.bookId, key.chapter),
                highlightRepository.highlights(key.bookId, key.chapter),
                highlightRepository.bookmarksFor(key.bookId, key.chapter),
                highlightRepository.notesFor(key.bookId, key.chapter),
                studyRepository.observeCachedKeysForChapter(key.bookId, key.chapter),
                progressRepository.readChapters().map { reads ->
                    reads.any { it.bookId == key.bookId && it.chapter == key.chapter }
                },
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                ChapterContent(
                    verses = values[0] as List<Verse>,
                    highlights = values[1] as List<Highlight>,
                    bookmarks = values[2] as List<Bookmark>,
                    notes = values[3] as List<Note>,
                    cachedKeys = values[4] as Set<String>,
                    isRead = values[5] as Boolean,
                )
            }.onEach { _versesLoaded.value = true }
        }
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        combine(_book, _chapterCount, _chapter) { book, count, chapter ->
            ReaderMeta(book, count, chapter)
        },
        _dbChecked,
        _versesLoaded,
        contentFlow,
    ) { meta, dbChecked, versesLoaded, content ->
        ReaderUiState(
            resolving = !dbChecked,
            dbEmpty = dbChecked && meta.book == null,
            book = meta.book,
            chapter = meta.chapter,
            chapterCount = meta.chapterCount,
            verses = content?.verses.orEmpty(),
            versesLoaded = versesLoaded,
            highlights = content?.highlights.orEmpty(),
            bookmarks = content?.bookmarks.orEmpty(),
            notes = content?.notes.orEmpty(),
            cachedStudyKeys = content?.cachedKeys.orEmpty(),
            isChapterRead = content?.isRead == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        startResolving()
        // Total de capítulos do livro, para limitar a navegação.
        viewModelScope.launch {
            // collectLatest: se o livro exato chega enquanto o processamento
            // do fallback ainda está suspenso em chapterCount, o antigo é
            // cancelado — ele não pode limitar o capítulo restaurado aos
            // capítulos do livro de fallback.
            _book.filterNotNull().distinctUntilChanged().collectLatest { book ->
                _chapterCount.value = bibleRepository.chapterCount(book.bookId)
                if (_chapterCount.value > 0) {
                    _chapter.value = _chapter.value.coerceIn(1, _chapterCount.value)
                }
            }
        }
    }

    // ---------------------------------------------------------------- ações

    /** Alternar destaque: tocar na cor atual remove; outra cor substitui. */
    fun toggleHighlight(verseNumber: Int, colorArgb: Int) {
        val key = currentKey() ?: return
        viewModelScope.launch {
            val current = uiState.value.highlightFor(verseNumber)
            when {
                current == null -> highlightRepository.addHighlight(
                    Highlight(key.bookId, key.chapter, verseNumber, colorArgb.toLong(), System.currentTimeMillis()),
                )
                current.color == colorArgb.toLong() ->
                    highlightRepository.removeHighlight(key.bookId, key.chapter, verseNumber)
                else -> {
                    highlightRepository.removeHighlight(key.bookId, key.chapter, verseNumber)
                    highlightRepository.addHighlight(
                        Highlight(key.bookId, key.chapter, verseNumber, colorArgb.toLong(), System.currentTimeMillis()),
                    )
                }
            }
        }
    }

    fun toggleBookmark(verseNumber: Int) {
        val key = currentKey() ?: return
        viewModelScope.launch {
            if (uiState.value.isBookmarked(verseNumber)) {
                highlightRepository.removeBookmark(key.bookId, key.chapter, verseNumber)
            } else {
                highlightRepository.addBookmark(
                    Bookmark(key.bookId, key.chapter, verseNumber, null, System.currentTimeMillis()),
                )
            }
        }
    }

    fun saveBookmarkLabel(verseNumber: Int, label: String) {
        val existing = uiState.value.bookmarks.firstOrNull { it.verse == verseNumber } ?: return
        viewModelScope.launch {
            highlightRepository.addBookmark(
                Bookmark(
                    bookId = existing.bookId,
                    chapter = existing.chapter,
                    verse = existing.verse,
                    label = label.trim().ifEmpty { null },
                    createdAt = existing.createdAt,
                ),
            )
        }
    }

    /** Salva a nota; texto vazio em nota existente a exclui. */
    fun saveNote(verseNumber: Int, text: String) {
        val key = currentKey() ?: return
        val trimmed = text.trim()
        viewModelScope.launch {
            val existing = uiState.value.noteFor(verseNumber)
            when {
                trimmed.isEmpty() && existing != null -> highlightRepository.deleteNote(existing.id)
                trimmed.isEmpty() -> Unit
                existing != null -> highlightRepository.saveNote(
                    Note(existing.id, key.bookId, key.chapter, verseNumber, trimmed, System.currentTimeMillis()),
                )
                else -> highlightRepository.saveNote(
                    Note(0L, key.bookId, key.chapter, verseNumber, trimmed, System.currentTimeMillis()),
                )
            }
        }
    }

    fun deleteNote(verseNumber: Int) {
        val existing = uiState.value.noteFor(verseNumber) ?: return
        viewModelScope.launch { highlightRepository.deleteNote(existing.id) }
    }

    /** Marca o capítulo atual como lido e alimenta o streak diário. */
    fun markChapterAsRead() {
        val key = currentKey() ?: return
        viewModelScope.launch {
            progressRepository.markChapterRead(key.bookId, key.chapter)
            progressRepository.markTodayStreak()
        }
    }

    fun previousChapter() = goToChapter(_chapter.value - 1)

    fun nextChapter() = goToChapter(_chapter.value + 1)

    private fun goToChapter(target: Int) {
        closeVerseStudy()
        closeChapterGuide()
        val limit = if (_chapterCount.value > 0) _chapterCount.value else Int.MAX_VALUE
        _chapter.value = target.coerceIn(1, limit)
    }

    // --------------------------------------------------------- estudo e IA

    fun openVerseStudy(verse: Verse, forceRefresh: Boolean = false) {
        val book = _book.value ?: return
        val chapter = _chapter.value
        val scopeKey = studyRepository.verseCacheKey(book.bookId, chapter, verse.verse)
        val sameVerse = _verseStudyState.value.verse?.verse == verse.verse

        verseStudyJob?.cancel()
        if (!sameVerse) {
            verseChatJob?.cancel()
            _verseStudyState.value = VerseStudyUiState(verse = verse, loading = true)
            verseChatJob = viewModelScope.launch {
                studyRepository.observeChat(scopeKey).collect { msgs ->
                    _verseStudyState.update { it.copy(chatMessages = msgs) }
                }
            }
        }

        verseStudyJob = viewModelScope.launch {
            val offlineTerms = studyRepository.findGlossaryTermsInText(verse.text)
            _verseStudyState.update {
                it.copy(
                    verse = verse,
                    offlineTerms = offlineTerms,
                    loading = true,
                    missingApiKey = false,
                    error = null,
                )
            }

            val result = studyRepository.getVerseStudy(
                book = book,
                chapter = chapter,
                verse = verse,
                chapterVerses = uiState.value.verses,
                forceRefresh = forceRefresh,
            )

            result.onSuccess { res ->
                _verseStudyState.update {
                    it.copy(
                        loading = false,
                        study = res.data,
                        fromCache = res.fromCache,
                        modelUsed = res.model,
                        missingApiKey = false,
                        error = null,
                    )
                }
            }.onFailure { err ->
                _verseStudyState.update {
                    it.copy(
                        loading = false,
                        missingApiKey = err is AiError.MissingApiKey,
                        error = if (err is AiError.MissingApiKey) null else (err.message ?: "Erro ao consultar a IA."),
                    )
                }
            }
        }
    }

    fun closeVerseStudy() {
        verseStudyJob?.cancel()
        verseChatJob?.cancel()
        _verseStudyState.value = VerseStudyUiState()
    }

    fun saveVerseStudyToNote(verse: Verse, study: VerseStudy) {
        val bookName = _book.value?.name ?: return
        val chapter = _chapter.value
        val studyBlock = buildString {
            appendLine("[Estudo · $bookName $chapter:${verse.verse}]")
            appendLine(study.summary)
            if (study.historicalContext.isNotBlank()) {
                appendLine()
                appendLine("Contexto: ${study.historicalContext}")
            }
            if (study.periodTerms.isNotEmpty()) {
                appendLine()
                appendLine("Termos da época:")
                study.periodTerms.forEach { term ->
                    val orig = term.originalWord?.let { " ($it)" }.orEmpty()
                    appendLine("- ${term.term}$orig: ${term.meaning}")
                }
            }
        }.trim()

        val existing = uiState.value.noteFor(verse.verse)?.text?.trim()
        val combined = if (existing.isNullOrEmpty()) {
            studyBlock
        } else {
            "$existing\n\n---\n\n$studyBlock"
        }
        saveNote(verse.verse, combined)
        _verseStudyState.update { it.copy(savedToNote = true) }
    }

    fun sendVerseChat(question: String) {
        val book = _book.value ?: return
        val chapter = _chapter.value
        val verse = _verseStudyState.value.verse ?: return
        val scopeKey = studyRepository.verseCacheKey(book.bookId, chapter, verse.verse)
        val surrounding = uiState.value.verses
            .filter { it.verse in (verse.verse - 3)..(verse.verse + 3) }
            .joinToString("\n") { "${it.verse}. ${it.text}" }
        val contextPrefix = StudyPrompts.buildVerseChatContext(
            bookName = book.name,
            chapter = chapter,
            verseNumber = verse.verse,
            verseText = verse.text,
            surroundingVerses = surrounding,
        )

        viewModelScope.launch {
            _verseStudyState.update { it.copy(chatSending = true, chatError = null) }
            val res = studyRepository.sendChatMessage(scopeKey, contextPrefix, question)
            _verseStudyState.update {
                it.copy(
                    chatSending = false,
                    chatError = res.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun clearVerseChat() {
        val book = _book.value ?: return
        val verse = _verseStudyState.value.verse ?: return
        val scopeKey = studyRepository.verseCacheKey(book.bookId, _chapter.value, verse.verse)
        viewModelScope.launch { studyRepository.clearChat(scopeKey) }
    }

    fun openChapterGuide(forceRefresh: Boolean = false) {
        val book = _book.value ?: return
        val chapter = _chapter.value
        val scopeKey = studyRepository.chapterCacheKey(book.bookId, chapter)

        chapterGuideJob?.cancel()
        if (!_chapterGuideState.value.visible) {
            chapterChatJob?.cancel()
            _chapterGuideState.value = ChapterGuideUiState(visible = true, loading = true)
            chapterChatJob = viewModelScope.launch {
                studyRepository.observeChat(scopeKey).collect { msgs ->
                    _chapterGuideState.update { it.copy(chatMessages = msgs) }
                }
            }
        }

        chapterGuideJob = viewModelScope.launch {
            _chapterGuideState.update {
                it.copy(
                    visible = true,
                    loading = true,
                    missingApiKey = false,
                    error = null,
                )
            }

            val result = studyRepository.getChapterGuide(
                book = book,
                chapter = chapter,
                chapterVerses = uiState.value.verses,
                forceRefresh = forceRefresh,
            )

            result.onSuccess { res ->
                _chapterGuideState.update {
                    it.copy(
                        loading = false,
                        guide = res.data,
                        fromCache = res.fromCache,
                        modelUsed = res.model,
                        missingApiKey = false,
                        error = null,
                    )
                }
            }.onFailure { err ->
                _chapterGuideState.update {
                    it.copy(
                        loading = false,
                        missingApiKey = err is AiError.MissingApiKey,
                        error = if (err is AiError.MissingApiKey) null else (err.message ?: "Erro ao gerar guia."),
                    )
                }
            }
        }
    }

    fun closeChapterGuide() {
        chapterGuideJob?.cancel()
        chapterChatJob?.cancel()
        _chapterGuideState.value = ChapterGuideUiState()
    }

    fun sendChapterChat(question: String) {
        val book = _book.value ?: return
        val chapter = _chapter.value
        val scopeKey = studyRepository.chapterCacheKey(book.bookId, chapter)
        val excerpt = uiState.value.verses
            .joinToString("\n") { "${it.verse}. ${it.text}" }
            .take(12_000)
        val contextPrefix = StudyPrompts.buildChapterChatContext(
            bookName = book.name,
            chapter = chapter,
            chapterExcerpt = excerpt,
        )

        viewModelScope.launch {
            _chapterGuideState.update { it.copy(chatSending = true, chatError = null) }
            val res = studyRepository.sendChatMessage(scopeKey, contextPrefix, question)
            _chapterGuideState.update {
                it.copy(
                    chatSending = false,
                    chatError = res.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun clearChapterChat() {
        val book = _book.value ?: return
        val scopeKey = studyRepository.chapterCacheKey(book.bookId, _chapter.value)
        viewModelScope.launch { studyRepository.clearChat(scopeKey) }
    }

    /** Reexecuta a resolução do livro (usado pelo fallback de banco vazio). */
    fun retry() {
        if (_book.value != null) return
        _dbChecked.value = false
        startResolving()
    }

    // -------------------------------------------------------- resolução

    /**
     * Observa a lista de livros continuamente: resolve o parâmetro recebido e
     * reage caso a importação da Bíblia termine depois da tela estar aberta.
     */
    private fun startResolving() {
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            try {
                var firstEmission = true
                bibleRepository.books().collect { all ->
                    if (firstEmission) {
                        _dbChecked.value = true
                        firstEmission = false
                    }
                    // Continua tentando até achar o livro pedido: durante a
                    // importação inicial um fallback provisório pode estar em
                    // exibição (ex.: aba Bíblia aberta antes do fim da semente);
                    // o livro exato o substitui assim que entra no banco.
                    if (_book.value == null || !exactMatch) {
                        val exact = findExactBook(all)
                        if (exact != null) {
                            // Zera aqui (e não a cada emissão): um banco que
                            // emite e depois falha repetidamente não pode
                            // reiniciar a resolução indefinidamente.
                            exactMatch = true
                            resolveAttempts = 0
                            if (_book.value != exact) {
                                // Restaura o capítulo pedido ANTES de trocar o
                                // livro — o coletor de _book reajusta os limites.
                                _chapter.value = requestedChapter
                                // Zera o total antigo para não exibir o
                                // capítulo do livro fallback um instante demais.
                                _chapterCount.value = 0
                                _book.value = exact
                            }
                        } else if (_book.value == null) {
                            _book.value = resolveBook(all) // fallback provisório
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _dbChecked.value = true // banco inacessível → fallback com retry
                // Exceção com um fallback provisório na tela: sem isso, o livro
                // exato jamais seria aplicado. Tenta retomar a resolução (com
                // limite para não gerar loop infinito num banco quebrado).
                if (_book.value != null && !exactMatch && resolveAttempts < 5) {
                    resolveAttempts++
                    delay(1_000)
                    startResolving()
                }
            }
        }
    }

    /** Livro que casa o parâmetro recebido exatamente (bookId ou nome normalizado). */
    private fun findExactBook(all: List<Book>): Book? {
        val input = normalize(bookInput)
        return all.firstOrNull { it.bookId == bookInput }
            ?: all.firstOrNull { normalize(it.name) == input }
    }

    /**
     * Fallback provisório quando o livro pedido ainda não está no banco
     * (importação inicial em andamento): prefixo do nome ou primeiro livro.
     */
    private fun resolveBook(all: List<Book>): Book? {
        if (all.isEmpty()) return null
        val input = normalize(bookInput)
        return all.firstOrNull { normalize(it.name).startsWith(input) }
            ?: all.firstOrNull()
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "")
            .trim()
            .lowercase()

    private fun currentKey(): ChapterKey? =
        _book.value?.let { ChapterKey(it.bookId, _chapter.value) }

    companion object {
        /** Fábrica da tela: cada entrada de navegação cria o seu ViewModel. */
        fun factory(bookInput: String, chapter: Int): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ReaderViewModel(bookInput, chapter) }
            }
    }
}
