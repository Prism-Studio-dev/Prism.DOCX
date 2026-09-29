package com.prismdocx.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as DrawPath
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prismdocx.platform.filepicker.PickerEntry
import com.prismdocx.platform.filepicker.PickerFileSystem
import com.prismdocx.platform.filepicker.PickerShortcut
import com.prismdocx.platform.filepicker.InvalidSaveDestinationException
import com.prismdocx.platform.filepicker.SaveDestination
import com.prismdocx.platform.filepicker.SystemPickerFileSystem
import com.prismdocx.ui.theme.LocalThemePalette
import com.prismdocx.ui.theme.themedOutlinedTextFieldColors
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.InvalidPathException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class PickerMode { OPEN, SAVE }

private const val MAX_NAVIGATION_HISTORY = 100

internal sealed interface PickerResult {
    data class Open(val path: Path) : PickerResult
    data class Save(val destination: SaveDestination) : PickerResult
}

/** A themed DOCX-only picker. File system work is performed outside composition. */
@Composable
internal fun FilePickerDialog(
    lastDirectory: Path?,
    mode: PickerMode,
    source: Path? = null,
    suggestedFileName: String = "",
    onSelect: (PickerResult) -> Unit,
    onDismiss: () -> Unit,
    fileSystem: PickerFileSystem = remember { SystemPickerFileSystem() },
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        FilePickerPanel(
            lastDirectory = lastDirectory,
            mode = mode,
            source = source,
            suggestedFileName = suggestedFileName,
            onSelect = onSelect,
            onDismiss = onDismiss,
            fileSystem = fileSystem,
            modifier = Modifier.sizeIn(maxWidth = 900.dp, maxHeight = 650.dp)
                .fillMaxWidth(0.94f).fillMaxHeight(0.90f),
        )
    }
}

/** Exposed separately so the picker can be previewed with an in-memory file system. */
@Composable
internal fun FilePickerPanel(
    lastDirectory: Path?,
    mode: PickerMode,
    source: Path? = null,
    suggestedFileName: String = "",
    onSelect: (PickerResult) -> Unit,
    onDismiss: () -> Unit,
    fileSystem: PickerFileSystem,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val backStack = remember(fileSystem, lastDirectory) { mutableStateListOf<Path>() }
    var directory by remember(fileSystem, lastDirectory) { mutableStateOf<Path?>(null) }
    var entries by remember(fileSystem, lastDirectory) { mutableStateOf<List<PickerEntry>>(emptyList()) }
    var shortcuts by remember(fileSystem) { mutableStateOf<List<PickerShortcut>>(emptyList()) }
    var roots by remember(fileSystem) { mutableStateOf<List<Path>>(emptyList()) }
    var selectedPath by remember(fileSystem, lastDirectory) { mutableStateOf<Path?>(null) }
    var search by remember(fileSystem, lastDirectory) { mutableStateOf("") }
    var fileName by remember(fileSystem, mode, suggestedFileName) { mutableStateOf(suggestedFileName) }
    var pendingOverwrite by remember(fileSystem, mode) { mutableStateOf<SaveDestination?>(null) }
    var searchFocused by remember { mutableStateOf(false) }
    var fileNameFocused by remember { mutableStateOf(false) }
    var panelFocused by remember { mutableStateOf(false) }
    var loading by remember(fileSystem, lastDirectory) { mutableStateOf(true) }
    var processingSelection by remember { mutableStateOf(false) }
    var errorMessage by remember(fileSystem, lastDirectory) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(fileSystem, lastDirectory) {
        try {
            directory = withContext(Dispatchers.IO) { fileSystem.initialDirectory(lastDirectory) }
        } catch (_: SecurityException) {
            loading = false
            errorMessage = "Нет доступа к начальной папке."
        } catch (_: IOException) {
            loading = false
            errorMessage = "Не удалось определить начальную папку."
        }
    }
    LaunchedEffect(fileSystem) {
        try {
            val locations = withContext(Dispatchers.IO) { fileSystem.shortcuts() to fileSystem.roots() }
            shortcuts = locations.first
            roots = locations.second
        } catch (_: SecurityException) {
            // Quick locations are optional; the current folder remains usable.
        } catch (_: IOException) {
            // Quick locations are optional; the current folder remains usable.
        }
    }
    LaunchedEffect(fileSystem, directory) {
        val path = directory ?: return@LaunchedEffect
        loading = true
        entries = emptyList()
        errorMessage = null
        try {
            entries = withContext(Dispatchers.IO) { fileSystem.list(path) }
        } catch (_: NoSuchFileException) {
            errorMessage = "Папка больше не существует. Выберите другую папку."
        } catch (_: NotDirectoryException) {
            errorMessage = "Папка больше не существует или недоступна. Выберите другую папку."
        } catch (_: AccessDeniedException) {
            errorMessage = "Нет доступа к этой папке. Выберите другую папку."
        } catch (_: SecurityException) {
            errorMessage = "Нет доступа к этой папке. Выберите другую папку."
        } catch (_: IOException) {
            errorMessage = "Не удалось прочитать папку. Выберите другую папку."
        } finally {
            loading = false
        }
    }

    fun resetForNavigation() {
        selectedPath = null
        search = ""
        entries = emptyList()
        loading = true
        errorMessage = null
    }

    fun navigateTo(path: Path) {
        if (path.toAbsolutePath().normalize() == directory?.toAbsolutePath()?.normalize()) return
        directory?.let {
            if (backStack.size == MAX_NAVIGATION_HISTORY) backStack.removeAt(0)
            backStack.add(it)
        }
        directory = path
        resetForNavigation()
    }

    fun goBack() {
        if (backStack.isEmpty()) return
        directory = backStack.removeAt(backStack.lastIndex)
        resetForNavigation()
    }

    fun openEntry(entry: PickerEntry) {
        if (entry.isDirectory) {
            navigateTo(entry.path)
            return
        }
        if (mode == PickerMode.SAVE) {
            selectedPath = entry.path
            fileName = entry.name
            errorMessage = null
            return
        }
        if (processingSelection) return
        processingSelection = true
        scope.launch {
            try {
                if (withContext(Dispatchers.IO) { fileSystem.isDocxFile(entry.path) }) {
                    onSelect(PickerResult.Open(entry.path))
                } else {
                    selectedPath = null
                    entries = entries.filterNot { it.path == entry.path }
                    errorMessage = "Файл больше не доступен или не является DOCX. Выберите другой файл."
                }
            } catch (_: SecurityException) {
                errorMessage = "Нет доступа к выбранному файлу."
            } catch (_: IOException) {
                errorMessage = "Не удалось открыть выбранный файл."
            } finally {
                processingSelection = false
            }
        }
    }

    fun saveCopy() {
        val currentDirectory = directory ?: return
        val original = source ?: return
        if (processingSelection || loading) return
        processingSelection = true
        scope.launch {
            try {
                val destination = withContext(Dispatchers.IO) {
                    fileSystem.saveDestination(currentDirectory, fileName, original)
                }
                if (destination.overwriteRequired) pendingOverwrite = destination
                else onSelect(PickerResult.Save(destination))
                errorMessage = null
            } catch (error: InvalidSaveDestinationException) {
                errorMessage = error.message
            } catch (_: SecurityException) {
                errorMessage = "Нет доступа к выбранной папке или файлу."
            } catch (_: IOException) {
                errorMessage = "Не удалось проверить путь сохранения. Выберите другую папку или имя."
            } catch (_: InvalidPathException) {
                errorMessage = "Недопустимый путь сохранения."
            } finally {
                processingSelection = false
            }
        }
    }

    val visibleEntries = remember(entries, search) {
        if (search.isBlank()) entries else entries.filter { it.name.contains(search.trim(), ignoreCase = true) }
    }
    val selectedEntry = visibleEntries.firstOrNull { it.path == selectedPath }
    LaunchedEffect(directory, search) { listState.scrollToItem(0) }

    Surface(
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when {
                event.key == Key.Escape -> { onDismiss(); true }
                event.isAltPressed && event.key == Key.DirectionLeft && !searchFocused && !fileNameFocused -> { goBack(); true }
                event.key == Key.Backspace && !searchFocused && !fileNameFocused -> { goBack(); true }
                event.key == Key.Enter && panelFocused -> {
                    selectedEntry?.let(::openEntry)
                    selectedEntry != null
                }
                event.key == Key.DirectionDown && panelFocused && visibleEntries.isNotEmpty() -> {
                    val index = visibleEntries.indexOfFirst { it.path == selectedPath }
                    val next = (index + 1).coerceAtMost(visibleEntries.lastIndex)
                    selectedPath = visibleEntries[next].path
                    scope.launch { listState.animateScrollToItem(next) }
                    true
                }
                event.key == Key.DirectionUp && panelFocused && visibleEntries.isNotEmpty() -> {
                    val index = visibleEntries.indexOfFirst { it.path == selectedPath }
                    val next = if (index < 0) 0 else (index - 1).coerceAtLeast(0)
                    selectedPath = visibleEntries[next].path
                    scope.launch { listState.animateScrollToItem(next) }
                    true
                }
                else -> false
            }
        }.focusRequester(focusRequester).onFocusChanged { panelFocused = it.isFocused }.focusable(),
        color = MaterialTheme.colorScheme.surface,
        shape = RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 12.dp,
    ) {
        BoxWithConstraints {
            val compact = maxWidth < 750.dp
            Column(Modifier.fillMaxSize()) {
                PickerHeader(mode, onDismiss)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PickerNavigation(
                    directory = directory,
                    canGoBack = backStack.isNotEmpty(),
                    onBack = ::goBack,
                    onUp = { directory?.parent?.let(::navigateTo) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    PickerSidebar(
                        shortcuts = shortcuts,
                        roots = roots,
                        directory = directory,
                        compact = compact,
                        onNavigate = ::navigateTo,
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                                .onFocusChanged { searchFocused = it.hasFocus },
                            placeholder = { Text("Поиск в папке") },
                            singleLine = true,
                            colors = themedOutlinedTextFieldColors(),
                        )
                        if (errorMessage != null) {
                            Text(
                                errorMessage.orEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when {
                                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp))
                                errorMessage != null && entries.isEmpty() -> Unit
                                visibleEntries.isEmpty() -> Text(
                                    if (search.isBlank()) "В этой папке нет документов DOCX." else "Ничего не найдено.",
                                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                else -> {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 14.dp, bottom = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        items(visibleEntries, key = { it.path.toString() }) { entry ->
                                            PickerEntryRow(
                                                entry = entry,
                                                selected = entry.path == selectedPath,
                                                onSelect = {
                                                    selectedPath = entry.path
                                                    if (mode == PickerMode.SAVE && !entry.isDirectory) fileName = entry.name
                                                    focusRequester.requestFocus()
                                                },
                                                onActivate = { openEntry(entry) },
                                            )
                                        }
                                    }
                                    VerticalScrollbar(
                                        adapter = rememberScrollbarAdapter(listState),
                                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (mode == PickerMode.SAVE) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it; selectedPath = null; errorMessage = null },
                        label = { Text("Имя файла") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            .onFocusChanged { fileNameFocused = it.hasFocus },
                        singleLine = true,
                        colors = themedOutlinedTextFieldColors(),
                    )
                }
                PickerFooter(
                    mode = mode,
                    canProceed = !loading && !processingSelection && if (mode == PickerMode.OPEN) selectedEntry?.isDirectory == false else fileName.isNotBlank() && directory != null,
                    onProceed = if (mode == PickerMode.OPEN) ({ selectedEntry?.let(::openEntry); Unit }) else ::saveCopy,
                    onDismiss = onDismiss,
                )
            }
        }
    }
    pendingOverwrite?.let { destination ->
        AlertDialog(
            onDismissRequest = { pendingOverwrite = null },
            title = { Text("Файл уже существует") },
            text = { Text("Файл уже существует. Заменить его?") },
            confirmButton = {
                Button(onClick = {
                    pendingOverwrite = null
                    onSelect(PickerResult.Save(destination))
                }, shape = RectangleShape) { Text("Заменить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverwrite = null }, shape = RectangleShape) { Text("Отмена") }
            },
            shape = RectangleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PickerHeader(mode: PickerMode, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (mode == PickerMode.OPEN) "Открыть DOCX" else "Сохранить копию", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onDismiss, shape = RectangleShape) { Text("Закрыть") }
    }
}

@Composable
private fun PickerNavigation(
    directory: Path?,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onUp: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onBack,
            enabled = canGoBack,
            shape = RectangleShape,
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.width(68.dp).height(38.dp),
        ) {
            Text("Назад", style = MaterialTheme.typography.labelSmall)
        }
        OutlinedButton(
            onClick = onUp,
            enabled = directory?.parent != null,
            shape = RectangleShape,
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.width(68.dp).height(38.dp),
        ) {
            Text("Вверх", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            directory?.toString() ?: "Определение каталога…",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickerSidebar(
    shortcuts: List<PickerShortcut>,
    roots: List<Path>,
    directory: Path?,
    compact: Boolean,
    onNavigate: (Path) -> Unit,
) {
    val sidebarWidth = if (compact) 150.dp else 190.dp
    Column(
        Modifier.width(sidebarWidth).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
    ) {
        PickerSidebarHeading("БЫСТРЫЙ ДОСТУП")
        shortcuts.forEach { shortcut ->
            PickerSidebarItem(shortcut.label, shortcut.path == directory) { onNavigate(shortcut.path) }
        }
        if (roots.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            PickerSidebarHeading("ДИСКИ И ТОМА")
            roots.forEach { path ->
                PickerSidebarItem(path.toString(), path == directory) { onNavigate(path) }
            }
        }
    }
}

@Composable
private fun PickerSidebarHeading(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PickerSidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalThemePalette.current
    Surface(
        color = if (selected) palette.selected else Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp).clickable(onClick = onClick),
        shape = RectangleShape,
        border = if (selected) BorderStroke(1.dp, palette.brand) else null,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickerEntryRow(entry: PickerEntry, selected: Boolean, onSelect: () -> Unit, onActivate: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val palette = LocalThemePalette.current
    val colors = MaterialTheme.colorScheme
    val background = when {
        selected -> palette.selected
        hovered -> palette.hover
        else -> colors.surface
    }
    Surface(
        modifier = Modifier.fillMaxWidth().hoverable(interactionSource)
            .pointerInput(entry.path) { detectTapGestures(onTap = { onSelect() }, onDoubleTap = { onActivate() }) }
            .semantics { role = Role.Button; this.selected = selected },
        color = background,
        shape = RectangleShape,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.brand else colors.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerEntryIcon(isDirectory = entry.isDirectory, color = if (selected) palette.brand else colors.onSurfaceVariant)
            Text(
                entry.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PickerEntryIcon(isDirectory: Boolean, color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val stroke = Stroke(1.5.dp.toPx())
        val width = size.width
        val height = size.height
        val outline = DrawPath().apply {
            if (isDirectory) {
                moveTo(width * 0.08f, height * 0.22f)
                lineTo(width * 0.40f, height * 0.22f)
                lineTo(width * 0.50f, height * 0.38f)
                lineTo(width * 0.92f, height * 0.38f)
                lineTo(width * 0.92f, height * 0.84f)
                lineTo(width * 0.08f, height * 0.84f)
            } else {
                moveTo(width * 0.20f, height * 0.10f)
                lineTo(width * 0.63f, height * 0.10f)
                lineTo(width * 0.82f, height * 0.30f)
                lineTo(width * 0.82f, height * 0.90f)
                lineTo(width * 0.20f, height * 0.90f)
            }
            close()
        }
        drawPath(outline, color, style = stroke)
        if (!isDirectory) {
            drawLine(color, Offset(width * 0.63f, height * 0.10f), Offset(width * 0.63f, height * 0.30f), stroke.width)
            drawLine(color, Offset(width * 0.63f, height * 0.30f), Offset(width * 0.82f, height * 0.30f), stroke.width)
            drawLine(color, Offset(width * 0.34f, height * 0.52f), Offset(width * 0.68f, height * 0.52f), stroke.width)
            drawLine(color, Offset(width * 0.34f, height * 0.66f), Offset(width * 0.68f, height * 0.66f), stroke.width)
        }
    }
}

@Composable
private fun PickerFooter(mode: PickerMode, canProceed: Boolean, onProceed: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Документы Word (*.docx)",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onDismiss, shape = RectangleShape) { Text("Отмена") }
        Button(onClick = onProceed, enabled = canProceed, shape = RectangleShape) {
            Text(if (mode == PickerMode.OPEN) "Открыть" else "Сохранить")
        }
    }
}
