package com.prismdocx.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prismdocx.settings.AppThemeId
import com.prismdocx.ui.theme.LocalThemePalette
import com.prismdocx.ui.theme.paletteFor

@Composable
internal fun SettingsDialog(
    selectedTheme: AppThemeId,
    onThemeSelected: (AppThemeId) -> Unit,
    errorMessage: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        title = { Text("Настройки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Внешний вид", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Тема приложения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppThemeId.entries.forEach { theme ->
                        ThemeOption(
                            theme = theme,
                            selected = theme == selectedTheme,
                            onClick = { onThemeSelected(theme) },
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, shape = RectangleShape) { Text("Закрыть") }
        },
    )
}

@Composable
private fun ThemeOption(theme: AppThemeId, selected: Boolean, onClick: () -> Unit) {
    val palette = paletteFor(theme)
    val label = when (theme) {
        AppThemeId.PRISM -> "Prism"
        AppThemeId.LIGHT -> "Светлая"
        AppThemeId.DARK -> "Тёмная"
        AppThemeId.GRAPHITE -> "Graphite"
        AppThemeId.VIOLET -> "Violet"
        AppThemeId.EMERALD -> "Emerald"
        AppThemeId.NORD -> "Nord"
    }
    Surface(
        shape = RectangleShape,
        color = if (selected) LocalThemePalette.current.selected else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) "Выбрана" else "Не выбрана" },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                ThemeSwatch(palette.colors.background, palette.colors.outlineVariant)
                ThemeSwatch(palette.colors.surfaceVariant, palette.colors.outlineVariant)
                ThemeSwatch(palette.brand, palette.colors.outlineVariant)
            }
            Text(
                label,
                modifier = Modifier.weight(1f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun ThemeSwatch(color: Color, borderColor: Color) {
    Box(Modifier.size(18.dp).background(color).border(1.dp, borderColor))
}
