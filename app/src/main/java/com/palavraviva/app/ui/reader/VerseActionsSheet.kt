package com.palavraviva.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.palavraviva.app.data.local.Highlight
import com.palavraviva.app.data.local.Note
import com.palavraviva.app.data.local.Verse
import com.palavraviva.app.data.local.Bookmark as BookmarkEntity

/**
 * Painel de ações sobre um versículo (tap/long-press): destaque com paleta,
 * marcador com etiqueta opcional e nota editável.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseActionsSheet(
    verse: Verse,
    highlight: Highlight?,
    bookmark: BookmarkEntity?,
    note: Note?,
    hasCachedStudy: Boolean,
    onOpenStudy: () -> Unit,
    onToggleHighlight: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveBookmarkLabel: (String) -> Unit,
    onSaveNote: (String) -> Unit,
    onDeleteNote: () -> Unit,
    onDismiss: () -> Unit,
) {
    val currentHighlightArgb: Int? = highlight?.color?.toInt()
    var noteText by remember(verse, note?.id) { mutableStateOf(note?.text.orEmpty()) }
    var labelText by remember(verse, bookmark != null) { mutableStateOf(bookmark?.label.orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            // Prévia do versículo selecionado
            Text(
                "Versículo ${verse.verse}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                verse.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))

            // Estudo bíblico contextual (IA + glossário offline)
            FilledTonalButton(
                onClick = onOpenStudy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasCachedStudy) "Estudar esta passagem · Salvo offline"
                    else "Estudar esta passagem",
                )
            }
            Spacer(Modifier.height(18.dp))

            // Destaque — paleta de 4 cores
            SectionLabel("DESTAQUE")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HighlightPalette.forEach { option ->
                    val selected = currentHighlightArgb == option.argb
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .clickable { onToggleHighlight(option.argb) }
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = option.label,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            if (currentHighlightArgb != null) {
                TextButton(onClick = { currentHighlightArgb?.let(onToggleHighlight) }) {
                    Text("Remover destaque")
                }
            }
            SheetDivider()

            // Marcador com etiqueta opcional
            SectionLabel("MARCADOR")
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleBookmark() }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            ) {
                Icon(
                    if (bookmark != null) Icons.Filled.Bookmark
                    else Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (bookmark != null) "Remover marcador" else "Adicionar marcador",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (bookmark != null) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Etiqueta (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (labelText.trim() != (bookmark.label ?: "").trim()) {
                    TextButton(onClick = { onSaveBookmarkLabel(labelText) }) {
                        Text("Salvar etiqueta")
                    }
                }
            }
            SheetDivider()

            // Nota — campo multiline
            SectionLabel("NOTA")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Sua nota sobre este versículo") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (note != null) {
                    TextButton(onClick = onDeleteNote) { Text("Excluir") }
                    Spacer(Modifier.width(8.dp))
                }
                Button(
                    onClick = { onSaveNote(noteText) },
                    enabled = noteText.trim() != (note?.text ?: "").trim(),
                ) { Text("Salvar nota") }
            }
        }
    }
}

@Composable
private fun SheetDivider() {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
