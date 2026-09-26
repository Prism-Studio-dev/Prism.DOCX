package com.prismdocx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
internal fun WelcomeContent(busy: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("РЕДАКТОР ДОКУМЕНТОВ", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Метаданные DOCX —\nпод вашим контролем", style = MaterialTheme.typography.headlineLarge,
            fontSize = 40.sp, lineHeight = 47.sp, fontWeight = FontWeight.Medium)
        Text("Откройте документ, измените нужные свойства и сохраните отдельную копию.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Приём файла работает во всём окне; эта область показывает пользователю основное действие.
        Surface(shape = RectangleShape, color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 42.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Перетащите DOCX сюда", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                Text("или выберите файл на компьютере", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Button(onClick = onOpen, enabled = !busy, shape = RectangleShape,
                    modifier = Modifier.padding(top = 26.dp)) {
                    Text("Выбрать файл DOCX", modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp))
                }
            }
        }
        Text("Исходный файл останется без изменений.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun DocumentPanel(file: File, busy: Boolean, onOpen: () -> Unit) {
    Surface(shape = RectangleShape, color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RectangleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text("DOCX", Modifier.padding(horizontal = 12.dp, vertical = 18.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${(file.length() / 1024).coerceAtLeast(1)} КБ · ${file.absoluteFile.parent}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            }
            OutlinedButton(shape = RectangleShape, onClick = onOpen, enabled = !busy) { Text("Другой файл") }
        }
    }
}
