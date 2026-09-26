package com.prismdocx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prismdocx.metadata.MetadataSection

private val documentSections = listOf(
    MetadataSection.DESCRIPTION,
    MetadataSection.PEOPLE,
    MetadataSection.DATES,
    MetadataSection.APPLICATION,
    MetadataSection.STATISTICS,
)
private val advancedSections = listOf(MetadataSection.CUSTOM, MetadataSection.XML)

@Composable
internal fun Sidebar(
    section: MetadataSection,
    compact: Boolean,
    darkTheme: Boolean,
    onSelectSection: (MetadataSection) -> Unit,
    onToggleTheme: () -> Unit,
) {
    Column(
        Modifier.width(if (compact) 195.dp else 238.dp).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp),
    ) {
        BrandLogo(darkTheme, Modifier.fillMaxWidth().height(68.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp)) {
            SidebarGroupLabel("СВОЙСТВА ДОКУМЕНТА")
            documentSections.forEach { item ->
                SidebarItem(item, section == item) { onSelectSection(item) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 20.dp))
            SidebarGroupLabel("ДОПОЛНИТЕЛЬНО")
            advancedSections.forEach { item ->
                SidebarItem(item, section == item) { onSelectSection(item) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        OutlinedButton(shape = RectangleShape, onClick = onToggleTheme,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                .semantics { stateDescription = if (darkTheme) "Тёмная тема" else "Светлая тема" }) {
            Text("Сменить тему")
        }
        Text("PRISM.DOCX", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun SidebarGroupLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
}

@Composable
private fun SidebarItem(item: MetadataSection, selected: Boolean, onClick: () -> Unit) {
    Surface(color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RectangleShape, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
