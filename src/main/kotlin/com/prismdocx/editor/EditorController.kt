package com.prismdocx.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.prismdocx.metadata.CustomProperty
import com.prismdocx.metadata.MetadataEditor
import com.prismdocx.metadata.MetadataField
import com.prismdocx.metadata.MetadataPart
import com.prismdocx.metadata.MetadataRepository
import com.prismdocx.metadata.MetadataSection
import com.prismdocx.metadata.MetadataSnapshot
import com.prismdocx.metadata.MetadataValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Владеет состоянием одной сессии редактора. Методы вызываются из UI-потока.
 * Файловые операции выполняются в ioDispatcher; отмена scope не превращается в сообщение об ошибке.
 */
class EditorController(
    private val repository: MetadataRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    initialDocument: OpenedDocument? = null,
) {
    var source by mutableStateOf(initialDocument?.file)
        private set
    private var baseline by mutableStateOf(initialDocument?.metadata ?: MetadataEditor.empty())
    var draft by mutableStateOf(baseline)
        private set
    var busy by mutableStateOf(false)
        private set
    var notice by mutableStateOf<EditorNotice?>(null)
        private set
    var pendingAction by mutableStateOf<PendingAction?>(null)
        private set
    var section by mutableStateOf(MetadataSection.DESCRIPTION)
        private set
    var search by mutableStateOf("")
        private set
    var darkTheme by mutableStateOf(false)
        private set
    var xmlPart by mutableStateOf(MetadataPart.CORE)
        private set
    var xmlDraft by mutableStateOf<String?>(null)
        private set

    // Все производные данные вычисляются вместе по одному разбору каждой XML-части.
    private var inspection by mutableStateOf(MetadataValidation.inspect(draft))
    val values: Map<String, String> get() = inspection.values
    val customProperties: List<CustomProperty> get() = inspection.customProperties
    val validationErrors: List<String> get() = inspection.validationErrors

    val dirty: Boolean get() = draft != baseline || xmlDraft != null
    val canEdit: Boolean get() = source != null && !busy && pendingAction == null && xmlDraft == null
    val canSave: Boolean get() = canEdit && validationErrors.isEmpty()
    val canOpen: Boolean get() = !busy && pendingAction == null
    val xmlText: String get() = xmlDraft ?: draft.xml.getValue(xmlPart)

    fun toggleTheme() { darkTheme = !darkTheme }

    fun selectSection(value: MetadataSection) {
        section = value
        search = ""
    }

    fun search(value: String) { search = value }

    fun requestOpen(file: File) {
        if (!canOpen) return
        confirmIfDirty("Открыть «${file.name}» и потерять несохранённые изменения?") { load(file) }
    }

    fun requestClose(onExit: () -> Unit, onCancel: () -> Unit) {
        if (!canOpen) {
            onCancel()
            return
        }
        confirmIfDirty("Закрыть приложение и потерять несохранённые изменения?", onCancel, onExit)
    }

    fun requestReset() {
        if (!canOpen || source == null) return
        confirmIfDirty("Сбросить все несохранённые изменения?") {
            updateDraft(baseline)
            xmlDraft = null
            notice = null
        }
    }

    fun confirmPending() {
        val action = pendingAction ?: return
        pendingAction = null
        action.confirm()
    }

    fun cancelPending() {
        val action = pendingAction ?: return
        pendingAction = null
        action.cancel()
    }

    fun changeField(field: MetadataField, value: String) {
        if (canEdit) updateDraft(MetadataEditor.setValue(draft, field, value))
    }

    fun changeCustom(property: CustomProperty) {
        if (canEdit) updateDraft(MetadataEditor.setCustom(draft, property))
    }

    fun removeCustom(id: String) {
        if (canEdit) updateDraft(MetadataEditor.removeCustom(draft, id))
    }

    fun selectXmlPart(part: MetadataPart) {
        if (!busy && xmlDraft == null) xmlPart = part
    }

    fun changeXml(value: String) {
        if (!busy) xmlDraft = value
    }

    fun discardXml() {
        if (!busy) xmlDraft = null
    }

    fun formatXml() = editXml {
        xmlDraft = MetadataEditor.formatXml(xmlText)
        notice = null
    }

    fun applyXml() = editXml {
        val xml = xmlDraft ?: return@editXml
        updateDraft(MetadataEditor.replaceXml(draft, xmlPart, xml))
        xmlDraft = null
        notice = EditorNotice("XML применён к форме. Сохраните копию, чтобы записать документ.")
    }

    fun showError(message: String) {
        notice = EditorNotice(message, isError = true)
    }

    fun save(target: SaveTarget) {
        val sourceFile = source ?: return
        if (!canSave) return
        val saving = draft
        runFileOperation("Ошибка сохранения") {
            withContext(ioDispatcher) {
                repository.write(sourceFile, target.file, saving, target.overwrite)
            }
            // Сброс возвращает форму к последней сохранённой копии, а не перечитывает исходник.
            baseline = saving
            notice = EditorNotice("Сохранено: ${target.file.absolutePath}")
        }
    }

    private fun load(file: File) = runFileOperation("Не удалось открыть документ.") {
        val loaded = withContext(ioDispatcher) { repository.read(file) }
        // Текущий документ заменяется только после успешного чтения нового.
        updateDraft(loaded)
        source = file
        baseline = loaded
        xmlDraft = null
        notice = null
    }

    private fun confirmIfDirty(message: String, onCancel: () -> Unit = {}, action: () -> Unit) {
        if (dirty) pendingAction = PendingAction(message, action, onCancel)
        else action()
    }

    private fun updateDraft(snapshot: MetadataSnapshot) {
        val updated = MetadataValidation.inspect(snapshot)
        draft = snapshot
        inspection = updated
    }

    private fun editXml(action: () -> Unit) {
        if (busy || source == null) return
        try {
            action()
        } catch (error: Exception) {
            showError(error.message ?: "Некорректный XML")
        }
    }

    private fun runFileOperation(fallbackMessage: String, operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showError(error.message ?: fallbackMessage)
            } finally {
                busy = false
            }
        }
    }
}
