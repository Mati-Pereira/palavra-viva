package com.palavraviva.app.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.palavraviva.app.ui.theme.Green
import androidx.compose.material.icons.outlined.CheckCircle

/**
 * Barra inferior do leitor: capítulo anterior/próximo (dentro dos limites do
 * livro) e marcar o capítulo como lido, com feedback de concluído.
 */
@Composable
fun ReaderBottomBar(
    chapter: Int,
    chapterCount: Int,
    isRead: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = onPrevious,
                enabled = chapter > 1,
                label = { Text("Anterior") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                },
            )
            AssistChip(
                onClick = onMarkRead,
                label = { Text(if (isRead) "Capítulo lido" else "Marcar como lido") },
                leadingIcon = {
                    Icon(
                        if (isRead) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (isRead) Green else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            AssistChip(
                onClick = onNext,
                enabled = chapterCount > 0 && chapter < chapterCount,
                label = { Text("Próximo") },
                trailingIcon = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
            )
        }
    }
}
