package com.prismdocx.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismdocx.metadata.FieldKind
import com.prismdocx.metadata.MetadataField
import com.prismdocx.metadata.MetadataValidation
import com.prismdocx.ui.theme.themedOutlinedTextFieldColors

private val booleanOptions = linkedMapOf(
    "" to "Не задано",
    "true" to "Да",
    "false" to "Нет",
    "1" to "Да (1)",
    "0" to "Нет (0)",
)

@Composable
internal fun PropertyInput(field: MetadataField, value: String, enabled: Boolean, modifier: Modifier,
    label: String = field.label, onChange: (String) -> Unit) {
    val error = MetadataValidation.fieldError(field, value)
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        if (field.kind == FieldKind.BOOLEAN) {
            Choice(value, booleanOptions, enabled, onChange)
        } else {
            OutlinedTextField(value, onChange, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape, singleLine = field.kind != FieldKind.MULTILINE,
                minLines = if (field.kind == FieldKind.MULTILINE) 3 else 1, isError = error != null,
                colors = themedOutlinedTextFieldColors(),
                placeholder = { Text(if (field.kind == FieldKind.DATE) "2026-09-23T12:00:00+03:00" else "Не задано", fontSize = 13.sp) })
        }
        val hint = error ?: field.hint
        if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodySmall,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
internal fun Choice(value: String, options: Map<String, String>, enabled: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(options[value] ?: value, modifier = Modifier.weight(1f))
            Text("⌄")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onChange(key) })
            }
        }
    }
}
