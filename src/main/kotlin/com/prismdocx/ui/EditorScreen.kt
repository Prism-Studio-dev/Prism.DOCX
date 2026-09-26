package com.prismdocx.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prismdocx.editor.EditorController
import com.prismdocx.metadata.MetadataSection
import com.prismdocx.ui.components.CustomEditor
import com.prismdocx.ui.components.DocumentPanel
import com.prismdocx.ui.components.DocxDropArea
import com.prismdocx.ui.components.EditorFooter
import com.prismdocx.ui.components.EditorHeader
import com.prismdocx.ui.components.WelcomeContent
import com.prismdocx.ui.components.MetadataForm
import com.prismdocx.ui.components.NoticePanel
import com.prismdocx.ui.components.PendingActionDialog
import com.prismdocx.ui.components.Sidebar
import com.prismdocx.ui.components.XmlEditor

@Composable
internal fun EditorScreen(
    controller: EditorController,
    closeRequested: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        DocxDropArea(
            enabled = controller.canOpen && !closeRequested,
            onFile = controller::requestOpen,
            onError = controller::showError,
        ) {
            BoxWithConstraints {
                val compact = maxWidth < 1000.dp
                Row(Modifier.fillMaxSize()) {
                    if (controller.source != null) {
                        Sidebar(
                            controller.section, compact, controller.darkTheme,
                            controller::selectSection, controller::toggleTheme
                        )
                        Box(
                            Modifier.width(1.dp).fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        EditorHeader(
                            controller.source != null, controller.busy, controller.dirty,
                            controller.darkTheme, controller::toggleTheme
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        if (controller.busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                        }
                        EditorContent(controller, compact, onOpen, Modifier.weight(1f).fillMaxWidth())
                        if (controller.source != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            EditorFooter(
                                controller.validationErrors, controller.canSave,
                                controller.canOpen && controller.dirty, controller::requestReset, onSave
                            )
                        }
                    }
                }
            }
        }
    }
    controller.pendingAction?.let { pending ->
        PendingActionDialog(pending.message, controller::confirmPending, controller::cancelPending)
    }
}

@Composable
private fun EditorContent(controller: EditorController, compact: Boolean, onOpen: () -> Unit, modifier: Modifier) {
    val scroll = rememberScrollState()
    val source = controller.source
    LaunchedEffect(controller.section) { scroll.scrollTo(0) }
    Box(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll)
                .padding(horizontal = if (compact) 28.dp else 48.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            if (source == null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    WelcomeContent(
                        busy = !controller.canOpen,
                        onOpen = onOpen,
                        modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(),
                    )
                }
            } else {
                Column {
                    Text(controller.section.title, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        controller.section.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                DocumentPanel(source, !controller.canOpen, onOpen)
            }
            controller.notice?.let { NoticePanel(it) }
            if (source != null) {
                EditorSectionContent(controller, compact)
            }
            Spacer(Modifier.height(10.dp))
        }
        VerticalScrollbar(
            rememberScrollbarAdapter(scroll),
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun EditorSectionContent(controller: EditorController, compact: Boolean) {
    if (controller.xmlDraft != null && controller.section != MetadataSection.XML) {
        Text(
            "Есть неприменённый XML. Примените или отмените его в XML-редакторе.",
            color = MaterialTheme.colorScheme.error
        )
    }
    when (controller.section) {
        MetadataSection.CUSTOM -> CustomEditor(
            properties = controller.customProperties,
            enabled = controller.canEdit,
            onChange = controller::changeCustom,
            onRemove = controller::removeCustom,
        )

        MetadataSection.XML -> XmlEditor(
            part = controller.xmlPart,
            xml = controller.xmlText,
            changed = controller.xmlDraft != null,
            busy = controller.busy,
            onPart = controller::selectXmlPart,
            onChange = controller::changeXml,
            onFormat = controller::formatXml,
            onDiscard = controller::discardXml,
            onApply = controller::applyXml,
        )

        else -> MetadataForm(
            section = controller.section,
            search = controller.search,
            values = controller.values,
            compact = compact,
            enabled = controller.canEdit,
            onSearch = controller::search,
            onChange = controller::changeField,
        )
    }
}
