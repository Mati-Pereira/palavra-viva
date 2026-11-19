package com.palavraviva.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.palavraviva.app.MainActivity
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.ui.theme.Gold
import com.palavraviva.app.ui.theme.GoldLight
import com.palavraviva.app.ui.theme.NightInk
import java.text.Normalizer

/**
 * Widget "Versículo do dia" (Glance).
 *
 * O receiver já está registrado pelo orquestrador no AndroidManifest.xml como
 * ".widget.VerseOfDayWidget", com os metadados em res/xml/verse_of_day_widget_info.xml.
 * A escolha do versículo é determinística pela data (garantida pelo BibleRepository),
 * e o clique em qualquer área do widget abre a MainActivity.
 */
class VerseOfDayWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseOfDayGlanceWidget
}

internal object VerseOfDayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Carregado fora da composição (chamadas suspend não podem ocorrer
        // dentro do conteúdo Glance).
        val verse = loadVerseOfTheDay()
        provideContent { VerseOfDayContent(verse) }
    }

    /**
     * Busca o versículo do dia e monta a referência no formato "Gn 1:1"
     * (abreviação do nome do livro). Retorna null quando o banco ainda não foi
     * populado — a UI então mostra um texto fixo motivacional e convida a
     * abrir o app.
     */
    private suspend fun loadVerseOfTheDay(): WidgetVerse? {
        val verse = runCatching { PalavraVivaApp.bibleRepository.verseOfTheDay() }.getOrNull()
            ?: return null
        val bookName = runCatching { PalavraVivaApp.bibleRepository.book(verse.bookId)?.name }
            .getOrNull()
        val abbreviation = bookName
            ?.let(::abbreviateBook)
            ?.takeIf { it.isNotBlank() }
            ?: verse.bookId.uppercase()
        return WidgetVerse(
            text = "“${verse.text}”",
            reference = "$abbreviation ${verse.chapter}:${verse.verse}",
        )
    }
}

private data class WidgetVerse(val text: String, val reference: String)

// Corpo do texto sobre o índigo escuro — mesma cor do tema escuro (onBackground).
private val VerseText = Color(0xFFF0EDF7)

@Composable
private fun VerseOfDayContent(verse: WidgetVerse?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(NightInk))
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "VERSÍCULO DO DIA",
            style = TextStyle(
                color = ColorProvider(GoldLight),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(10.dp))
        Text(
            text = verse?.text
                ?: "“Lâmpada para os meus pés é a tua palavra, e luz para o meu caminho.”",
            style = TextStyle(
                color = ColorProvider(VerseText),
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
            ),
            maxLines = 5,
        )
        Spacer(GlanceModifier.height(10.dp))
        Text(
            text = verse?.let { "— ${it.reference}" } ?: "Abra o app para começar",
            style = TextStyle(
                color = ColorProvider(Gold),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

/**
 * Abreviações padrão (ARA) dos 66 livros, por nome normalizado
 * (minúsculas, sem acentos e sem espaços — ex.: "1 Samuel" → "1samuel").
 * Inclui apelidos alternativos para livros com nomes longos.
 */
private val BookAbbreviations: Map<String, String> = mapOf(
    "genesis" to "Gn", "exodo" to "Ex", "levitico" to "Lv", "numeros" to "Nm",
    "deuteronomio" to "Dt", "josue" to "Js", "juizes" to "Jz", "rute" to "Rt",
    "1samuel" to "1Sm", "2samuel" to "2Sm", "1reis" to "1Rs", "2reis" to "2Rs",
    "1cronicas" to "1Cr", "2cronicas" to "2Cr", "esdras" to "Ed", "neemias" to "Ne",
    "ester" to "Et", "jo" to "Jó", "salmos" to "Sl", "proverbios" to "Pv",
    "eclesiastes" to "Ec", "cantares" to "Ct", "canticos" to "Ct",
    "cantaresdoscanticos" to "Ct", "isaias" to "Is", "jeremias" to "Jr",
    "lamentacoes" to "Lm", "ezequiel" to "Ez", "daniel" to "Dn", "oseias" to "Os",
    "joel" to "Jl", "amos" to "Am", "obadias" to "Ob", "jonas" to "Jn",
    "miqueias" to "Mq", "naum" to "Na", "habacuque" to "Hc", "sofonias" to "Sf",
    "ageu" to "Ag", "zacarias" to "Zc", "malaquias" to "Ml", "mateus" to "Mt",
    "marcos" to "Mc", "lucas" to "Lc", "joao" to "Jo", "atos" to "At",
    "atosdosapostolos" to "At", "romanos" to "Rm", "1corintios" to "1Co",
    "2corintios" to "2Co", "galatas" to "Gl", "efesios" to "Ef",
    "filipenses" to "Fp", "colossenses" to "Cl", "1tesalonicenses" to "1Ts",
    "2tesalonicenses" to "2Ts", "1timoteo" to "1Tm", "2timoteo" to "2Tm",
    "tito" to "Tt", "filemom" to "Fm", "hebreus" to "Hb", "tiago" to "Tg",
    "1pedro" to "1Pe", "2pedro" to "2Pe", "1joao" to "1Jo", "2joao" to "2Jo",
    "3joao" to "3Jo", "judas" to "Jd", "apocalipse" to "Ap",
)

/**
 * Converte o nome do livro em abreviação ("Gênesis" → "Gn").
 * Para nomes fora do mapa, deriva um fallback determinístico:
 * numerais preservados + primeiras letras do nome ("1 Macabeus" → "1Ma").
 */
private fun abbreviateBook(name: String): String {
    val normalized = normalizeBookName(name)
    BookAbbreviations[normalized]?.let { return it }
    val digits = normalized.takeWhile { it.isDigit() }
    val letters = normalized.drop(digits.length).take(2)
    if (letters.isEmpty()) return name.take(3)
    return digits + letters.replaceFirstChar { it.uppercaseChar() }
}

/** "1 Samuel" → "1samuel" (minúsculas, sem acentos, espaços ou pontuação). */
private fun normalizeBookName(name: String): String =
    Normalizer.normalize(name, Normalizer.Form.NFD)
        .filter { it.isLetterOrDigit() }
        .lowercase()
