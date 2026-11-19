package com.palavraviva.app.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Linha de um versículo: número discreto, texto serifado, fundo colorido quando
 * destacado e ícones laterais para marcador e nota. Tap e long-press abrem o
 * painel de ações.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseRow(
    number: Int,
    text: String,
    highlightColor: Color?,
    isBookmarked: Boolean,
    hasNote: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlightBackground = highlightColor?.copy(alpha = 0.24f) ?: Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(highlightBackground)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // Coluna lateral: número + indicadores de marcador/nota
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(22.dp),
        ) {
            Text(
                "$number",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 7.dp),
            )
            if (isBookmarked) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Versículo marcado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(13.dp),
                )
            }
            if (hasNote) {
                Icon(
                    Icons.Filled.StickyNote2,
                    contentDescription = "Versículo com nota",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(13.dp),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        // Texto bíblico — tipografia serifada do app (bodyLarge)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
