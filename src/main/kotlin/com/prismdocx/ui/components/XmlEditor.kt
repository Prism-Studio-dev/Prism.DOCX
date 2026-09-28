package com.prismdocx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismdocx.metadata.MetadataPart
import com.prismdocx.ui.theme.themedOutlinedTextFieldColors

@Composable
internal fun XmlEditor(part: MetadataPart, xml: String, changed: Boolean, busy: Boolean, onPart: (MetadataPart) -> Unit,
    onChange: (String) -> Unit, onFormat: () -> Unit, onDiscard: () -> Unit, onApply: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetadataPart.entries.forEach { item ->
            FilterChip(shape = RectangleShape, selected = item == part, onClick = { onPart(item) }, enabled = !busy && !changed,
                label = { Text(item.path.substringAfter('/')) })
        }
    }
    Text("Здесь доступны также HeadingPairs, TitlesOfParts, HLinks, DigSig и составные пользовательские значения. Проверяется XML и типы обычных полей; полной проверки по схеме OOXML нет.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(xml, onChange, enabled = !busy,
        modifier = Modifier.fillMaxWidth().height(360.dp), shape = RectangleShape,
        colors = themedOutlinedTextFieldColors(),
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 21.sp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(shape = RectangleShape, onClick = onApply, enabled = changed && !busy) { Text("Применить XML") }
        TextButton(shape = RectangleShape, onClick = onFormat, enabled = !busy) { Text("Форматировать") }
        TextButton(shape = RectangleShape, onClick = onDiscard, enabled = changed && !busy) { Text("Отменить правку XML") }
    }
}
