package com.prismdocx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.prismdocx.metadata.MetadataField
import com.prismdocx.metadata.MetadataSection
import com.prismdocx.metadata.metadataFields

@Composable
internal fun MetadataForm(
    section: MetadataSection,
    search: String,
    values: Map<String, String>,
    compact: Boolean,
    enabled: Boolean,
    onSearch: (String) -> Unit,
    onChange: (MetadataField, String) -> Unit,
) {
    OutlinedTextField(search, onSearch, placeholder = { Text("Поиск по всем свойствам") },
        trailingIcon = if (search.isBlank()) null else {
            { TextButton(onClick = { onSearch("") }, shape = RectangleShape) { Text("Очистить") } }
        },
        singleLine = true, shape = RectangleShape, modifier = Modifier.fillMaxWidth())
    val fields = metadataFields.filter {
        (search.isNotBlank() || it.section == section) &&
            (search.isBlank() || it.label.contains(search, true) || it.key.contains(search, true))
    }
    Text(if (search.isBlank()) "${fields.size} СВОЙСТВ" else "РЕЗУЛЬТАТОВ: ${fields.size}",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    fields.chunked(if (compact) 1 else 2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            row.forEach { field ->
                PropertyInput(field, values.getValue(field.key), enabled, Modifier.weight(1f),
                    label = if (search.isBlank()) field.label else "${field.section.title} · ${field.label}") {
                    onChange(field, it)
                }
            }
            if (!compact && row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Text("Пустое поле удаляет свойство. Даты сохраняются ровно такими, как вы их указали.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
