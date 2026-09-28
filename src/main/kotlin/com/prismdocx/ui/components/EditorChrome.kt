package com.prismdocx.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prismdocx.editor.EditorNotice

@Composable
internal fun EditorHeader(loaded: Boolean, busy: Boolean, dirty: Boolean, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = if (loaded) 18.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (loaded) {
            Text("Документы  /  Метаданные", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(if (busy) "Обработка…" else if (dirty) "●  Есть изменения" else "●  Нет изменений",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            BrandLogo(Modifier.width(178.dp).height(62.dp))
            Spacer(Modifier.weight(1f))
            OutlinedButton(shape = RectangleShape, onClick = onOpenSettings) {
                Text("Настройки")
            }
        }
    }
}

@Composable
internal fun EditorFooter(errors: List<String>, canSave: Boolean, canReset: Boolean,
    onReset: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(when {
                errors.isNotEmpty() -> "Проверьте поля: ${errors.size}"
                canReset -> "Есть несохранённые изменения"
                else -> "Документ готов к редактированию"
            },
                style = MaterialTheme.typography.labelLarge,
                color = if (errors.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(errors.firstOrNull() ?: "Исходный файл не изменится.",
                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(shape = RectangleShape, enabled = canReset, onClick = onReset) { Text("Сбросить правки") }
        Button(enabled = canSave, shape = RectangleShape,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp), onClick = onSave) {
            Text("Сохранить копию")
        }
    }
}

@Composable
internal fun NoticePanel(notice: EditorNotice) {
    Surface(color = if (notice.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RectangleShape,
        border = BorderStroke(1.dp, if (notice.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant)) {
        SelectionContainer {
            Text(notice.text, Modifier.fillMaxWidth().padding(16.dp),
                color = if (notice.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
internal fun PendingActionDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(shape = RectangleShape, onDismissRequest = onDismiss,
        title = { Text("Несохранённые изменения") },
        text = { Text(message) },
        confirmButton = { TextButton(shape = RectangleShape, onClick = onConfirm) { Text("Продолжить") } },
        dismissButton = { TextButton(shape = RectangleShape, onClick = onDismiss) { Text("Остаться") } })
}
