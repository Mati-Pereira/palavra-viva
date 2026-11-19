package com.palavraviva.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.data.local.Book
import com.palavraviva.app.data.local.Bookmark
import com.palavraviva.app.data.local.Highlight
import com.palavraviva.app.data.local.Note
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
    val isChapterRead: Boolean = false,
) {
    val isReady: Boolean get() = book != null

    fun highlightFor(verseNumber: Int): Highlight? = highlights.firstOrNull { it.verse == verseNumber }

    fun isBookmarked(verseNumber: Int): Boolean = bookmarks.any { it.verse == verseNumber }

    fun noteFor(verseNumber: Int): Note? = notes.firstOrNull { it.verse == verseNumber }
}

private data class ChapterKey(val bookId: String, val chapter: Int)

private data class ChapterContent(
    val verses: List<Verse>,
    val highlights: List<Highlight>,
    val bookmarks: List<Bookmark>,
    val notes: List<Note>,
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

    private val _book = MutableStateFlow<Book?>(null)
    private val _chapterCount = MutableStateFlow(0)
    private val _chapter = MutableStateFlow(startChapter.coerceAtLeast(1))
    private val _dbChecked = MutableStateFlow(false)
    private val _versesLoaded = MutableStateFlow(false)
    private var resolveJob: Job? = null

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
                progressRepository.readChapters().map { reads ->
                    reads.any { it.bookId == key.bookId && it.chapter == key.chapter }
                },
            ) { verses, highlights, bookmarks, notes, isRead ->
                ChapterContent(verses, highlights, bookmarks, notes, isRead)
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
        val limit = if (_chapterCount.value > 0) _chapterCount.value else Int.MAX_VALUE
        _chapter.value = target.coerceIn(1, limit)
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
