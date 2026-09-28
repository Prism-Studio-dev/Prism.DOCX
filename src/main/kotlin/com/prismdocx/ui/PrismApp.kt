package com.prismdocx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.prismdocx.docx.DocxRepository
import com.prismdocx.editor.EditorController
import com.prismdocx.editor.OpenedDocument
import com.prismdocx.editor.SaveTarget
import com.prismdocx.settings.AppSettings
import com.prismdocx.settings.FileSettingsStore
import com.prismdocx.settings.SettingsStore
import com.prismdocx.ui.components.FilePickerDialog
import com.prismdocx.ui.components.PickerMode
import com.prismdocx.ui.components.PickerResult
import com.prismdocx.ui.theme.PrismTheme
import java.io.IOException

/** Composition root: связывает UI, контроллер и платформенные зависимости. */
@Composable
fun App(
    onExit: () -> Unit = {},
    closeRequested: Boolean = false,
    onCancelClose: () -> Unit = {},
    initialDocument: OpenedDocument? = null,
    settingsStore: SettingsStore? = null,
) {
    val scope = rememberCoroutineScope()
    val store = remember(settingsStore) { settingsStore ?: FileSettingsStore() }
    var settings by remember(store) { mutableStateOf(store.load()) }
    var settingsError by remember(store) { mutableStateOf<String?>(null) }
    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }
    val controller = remember(scope) {
        EditorController(DocxRepository(), scope, initialDocument = initialDocument)
    }
    fun updateSettings(updated: AppSettings) {
        settings = updated
        try {
            store.save(updated)
            settingsError = null
        } catch (_: IOException) {
            settingsError = "Не удалось сохранить настройки. Изменения действуют до закрытия приложения."
        } catch (_: SecurityException) {
            settingsError = "Не удалось сохранить настройки. Изменения действуют до закрытия приложения."
        }
    }
    LaunchedEffect(closeRequested) {
        if (closeRequested) controller.requestClose(onExit, onCancelClose)
    }
    PrismTheme(settings.theme) {
        EditorScreen(
            controller = controller,
            closeRequested = closeRequested,
            selectedTheme = settings.theme,
            settingsError = settingsError,
            onThemeSelected = { theme ->
                if (theme != settings.theme || settingsError != null) {
                    updateSettings(settings.copy(theme = theme))
                }
            },
            onOpen = {
                if (controller.canOpen) pickerMode = PickerMode.OPEN
            },
            onSave = {
                if (controller.canSave && controller.source != null) pickerMode = PickerMode.SAVE
            },
        )
        pickerMode?.let { mode ->
            val source = controller.source
            FilePickerDialog(
                lastDirectory = settings.lastFileDirectory ?: if (mode == PickerMode.SAVE) source?.toPath()?.parent else null,
                mode = mode,
                source = source?.toPath(),
                suggestedFileName = if (mode == PickerMode.SAVE && source != null)
                    source.nameWithoutExtension + "_metadata.docx" else "",
                onSelect = { result ->
                    pickerMode = null
                    when (result) {
                        is PickerResult.Open -> controller.requestOpen(result.path.toFile()) {
                            result.path.parent?.let { directory ->
                                if (directory != settings.lastFileDirectory) {
                                    updateSettings(settings.copy(lastFileDirectory = directory))
                                }
                            }
                        }
                        is PickerResult.Save -> {
                            result.destination.path.parent?.let { directory ->
                                if (directory != settings.lastFileDirectory) {
                                    updateSettings(settings.copy(lastFileDirectory = directory))
                                }
                            }
                            controller.save(SaveTarget(result.destination.path.toFile(), result.destination.overwriteRequired))
                        }
                    }
                },
                onDismiss = { pickerMode = null },
            )
        }
    }
}
