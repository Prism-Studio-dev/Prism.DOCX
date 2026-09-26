package com.prismdocx.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.prismdocx.metadata.CustomProperty
import com.prismdocx.metadata.customTypes

@Composable
internal fun CustomEditor(
    properties: List<CustomProperty>,
    enabled: Boolean,
    onChange: (CustomProperty) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (properties.isEmpty()) Text("Своих свойств пока нет. Добавьте первое поле — например, номер проекта.",
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    properties.forEach { property ->
        key(property.id) {
            Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RectangleShape).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(property.name, { onChange(property.copy(name = it)) }, label = { Text("Название свойства") },
                        enabled = enabled, singleLine = true, modifier = Modifier.weight(1f), shape = RectangleShape)
                    TextButton(shape = RectangleShape, enabled = enabled, onClick = { onRemove(property.id) }) { Text("Удалить") }
                }
                if (property.type in customTypes) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(150.dp)) {
                            Choice(property.type, customTypes, enabled) { type ->
                                onChange(property.copy(type = type, value = defaultValue(type, property.value)))
                            }
                        }
                        OutlinedTextField(property.value, { onChange(property.copy(value = it)) }, label = { Text("Значение") },
                            enabled = enabled, modifier = Modifier.weight(1f), shape = RectangleShape)
                    }
                } else Text("Тип ${property.type}. Значение доступно в XML-редакторе.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    OutlinedButton(shape = RectangleShape, enabled = enabled, onClick = {
        var number = 1
        while (properties.any { it.name.equals("Свойство $number", true) }) number++
        onChange(CustomProperty("", "Свойство $number", "lpwstr", ""))
    }) { Text("+  Добавить свойство") }
}

private fun defaultValue(type: String, currentValue: String): String = when (type) {
    "bool" -> "false"
    "i4", "r8" -> "0"
    "filetime" -> java.time.Instant.now().toString()
    else -> currentValue
}
