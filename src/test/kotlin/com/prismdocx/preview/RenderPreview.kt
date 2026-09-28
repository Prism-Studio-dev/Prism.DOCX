package com.prismdocx.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.prismdocx.ui.App
import com.prismdocx.metadata.MetadataEditor
import com.prismdocx.metadata.metadataFields
import com.prismdocx.editor.OpenedDocument
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.use
import androidx.compose.runtime.remember
import java.io.File
import com.prismdocx.settings.AppSettings
import com.prismdocx.settings.AppThemeId
import com.prismdocx.settings.SettingsStore
import com.prismdocx.resources.Res
import com.prismdocx.resources.prism_docx_app_icon
import com.prismdocx.platform.filepicker.PickerEntry
import com.prismdocx.platform.filepicker.PickerFileSystem
import com.prismdocx.platform.filepicker.PickerShortcut
import com.prismdocx.platform.filepicker.SaveDestination
import com.prismdocx.ui.components.FilePickerPanel
import com.prismdocx.ui.components.FilePickerDialog
import com.prismdocx.ui.components.PickerMode
import com.prismdocx.ui.theme.PrismTheme
import org.jetbrains.compose.resources.painterResource
import java.nio.file.Path

private class PreviewSettingsStore(theme: AppThemeId) : SettingsStore {
    private var settings = AppSettings(theme)
    override fun load(): AppSettings = settings
    override fun save(settings: AppSettings) { this.settings = settings }
}

private class PreviewPickerFileSystem : PickerFileSystem {
    private val root = Path.of("Demo")
    private val documents = root.resolve("Documents")
    private val projects = documents.resolve("Projects")
    private val archive = documents.resolve("Archive")
    private val report = documents.resolve("Report.docx")
    private val draft = documents.resolve("Draft.DOCX")

    override fun initialDirectory(lastDirectory: Path?): Path = documents
    override fun shortcuts(): List<PickerShortcut> = listOf(
        PickerShortcut("Домашняя папка", root),
        PickerShortcut("Документы", documents),
    )
    override fun roots(): List<Path> = listOf(root)
    override fun isDirectory(path: Path): Boolean = path in setOf(root, documents, projects, archive)
    override fun isDocxFile(path: Path): Boolean = path in setOf(report, draft)
    override fun saveDestination(directory: Path, name: String, source: Path): SaveDestination =
        SaveDestination(directory.resolve(if (name.endsWith(".docx", true)) name else "$name.docx"),
            overwriteRequired = name.equals("Report.docx", true))
    override fun list(directory: Path): List<PickerEntry> = when (directory) {
        root -> listOf(PickerEntry(documents, true))
        documents -> listOf(
            PickerEntry(archive, true), PickerEntry(projects, true),
            PickerEntry(draft, false), PickerEntry(report, false),
        )
        projects, archive -> emptyList()
        else -> throw java.nio.file.NoSuchFileException(directory.toString())
    }
}

/** Manual visual inspection helper: ./gradlew renderPreview. */
fun main() {
    val output = File("build/previews").apply { mkdirs() }
    var sample = MetadataEditor.empty()
    mapOf("title" to "Исследование новых возможностей", "subject" to "Проект Prism",
        "creator" to "Александр", "Company" to "Prism.DOCX", "language" to "ru-RU",
        "description" to "Материалы и результаты исследования.", "keywords" to "исследование, дизайн",
        "created" to "2026-09-23T12:00:00+03:00").forEach { (key, value) ->
        sample = MetadataEditor.setValue(sample, metadataFields.single { it.key == key }, value)
    }
    fun capture(scene: ImageComposeScene, name: String) {
        repeat(3) { scene.render((it + 1) * 100_000_000L).close() }
        scene.render(500_000_000L).use { image ->
            image.encodeToData()!!.use { File(output, name).writeBytes(it.bytes) }
        }
    }
    fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
    }
    ImageComposeScene(450, 450) {
        Image(painterResource(Res.drawable.prism_docx_app_icon), contentDescription = null, modifier = Modifier.fillMaxSize())
    }.use { capture(it, "svg-icon.png") }
    ImageComposeScene(1200, 850) { App(settingsStore = remember { PreviewSettingsStore(AppThemeId.PRISM) }) }.use { scene ->
        capture(scene, "home.png")
        click(scene, 1100f, 43f)
        capture(scene, "settings-prism.png")
    }
    ImageComposeScene(1200, 850) { App(settingsStore = remember { PreviewSettingsStore(AppThemeId.LIGHT) }) }.use { scene ->
        capture(scene, "home-light.png")
        click(scene, 1100f, 43f)
        capture(scene, "settings-light.png")
    }
    ImageComposeScene(1200, 850) { App(settingsStore = remember { PreviewSettingsStore(AppThemeId.DARK) }) }.use { scene ->
        capture(scene, "home-dark.png")
        click(scene, 1100f, 43f)
        capture(scene, "settings-dark.png")
    }
    val additionalThemes = listOf(
        AppThemeId.GRAPHITE,
        AppThemeId.VIOLET,
        AppThemeId.EMERALD,
        AppThemeId.NORD,
    )
    additionalThemes.forEach { theme ->
        val suffix = theme.name.lowercase()
        ImageComposeScene(1200, 850) { App(settingsStore = remember { PreviewSettingsStore(theme) }) }.use { scene ->
            capture(scene, "home-$suffix.png")
            click(scene, 1100f, 43f)
            capture(scene, "settings-$suffix.png")
        }
    }
    ImageComposeScene(820, 640) { App(settingsStore = remember { PreviewSettingsStore(AppThemeId.PRISM) }) }.use { scene ->
        capture(scene, "home-compact.png")
        click(scene, 735f, 43f)
        capture(scene, "settings-compact.png")
    }
    ImageComposeScene(1200, 850) {
        App(initialDocument = OpenedDocument(File("Исследование.docx"), sample),
            settingsStore = remember { PreviewSettingsStore(AppThemeId.PRISM) })
    }.use { scene ->
        capture(scene, "editor.png")
        click(scene, 90f, 514f)
        capture(scene, "xml-prism.png")
        click(scene, 100f, 765f)
        capture(scene, "settings-editor-prism.png")
        click(scene, 500f, 396f)
        capture(scene, "settings-editor-dark.png")
        click(scene, 810f, 642f)
        capture(scene, "xml-after-theme-switch.png")
    }
    ImageComposeScene(1200, 850) {
        App(initialDocument = OpenedDocument(File("Исследование.docx"), sample),
            settingsStore = remember { PreviewSettingsStore(AppThemeId.LIGHT) })
    }.use { scene ->
        capture(scene, "editor-light.png")
        click(scene, 90f, 514f)
        capture(scene, "xml-light.png")
    }
    ImageComposeScene(1200, 850) {
        App(initialDocument = OpenedDocument(File("Исследование.docx"), sample),
            settingsStore = remember { PreviewSettingsStore(AppThemeId.DARK) })
    }.use { scene ->
        capture(scene, "editor-dark.png")
        click(scene, 90f, 514f)
        capture(scene, "xml-dark.png")
    }
    additionalThemes.forEach { theme ->
        val suffix = theme.name.lowercase()
        ImageComposeScene(1200, 850) {
            App(initialDocument = OpenedDocument(File("Исследование.docx"), sample),
                settingsStore = remember { PreviewSettingsStore(theme) })
        }.use { scene ->
            capture(scene, "editor-$suffix.png")
            click(scene, 90f, 514f)
            capture(scene, "xml-$suffix.png")
        }
    }
    ImageComposeScene(820, 640) {
        App(initialDocument = OpenedDocument(File("Исследование.docx"), sample),
            settingsStore = remember { PreviewSettingsStore(AppThemeId.PRISM) })
    }.use {
        capture(it, "editor-compact.png")
    }
    AppThemeId.entries.forEach { theme ->
        ImageComposeScene(900, 650) {
            PrismTheme(theme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    FilePickerPanel(
                        lastDirectory = null,
                        mode = PickerMode.OPEN,
                        onSelect = {},
                        onDismiss = {},
                        fileSystem = remember { PreviewPickerFileSystem() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }.use { scene ->
            val suffix = theme.name.lowercase()
            capture(scene, "picker-$suffix.png")
            click(scene, 310f, 330f)
            Thread.sleep(350)
            capture(scene, "picker-selected-$suffix.png")
        }
    }
    ImageComposeScene(700, 500) {
        PrismTheme(AppThemeId.PRISM) {
            FilePickerPanel(
                lastDirectory = null,
                mode = PickerMode.OPEN,
                onSelect = {},
                onDismiss = {},
                fileSystem = remember { PreviewPickerFileSystem() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }.use { capture(it, "picker-compact.png") }
    ImageComposeScene(900, 650) {
        PrismTheme(AppThemeId.PRISM) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                FilePickerDialog(
                    lastDirectory = null,
                    mode = PickerMode.OPEN,
                    onSelect = {},
                    onDismiss = {},
                    fileSystem = remember { PreviewPickerFileSystem() },
                )
            }
        }
    }.use { capture(it, "picker-dialog-prism.png") }
    ImageComposeScene(700, 500) {
        PrismTheme(AppThemeId.DARK) {
            FilePickerDialog(
                lastDirectory = null,
                mode = PickerMode.OPEN,
                onSelect = {},
                onDismiss = {},
                fileSystem = remember { PreviewPickerFileSystem() },
            )
        }
    }.use { capture(it, "picker-dialog-compact-dark.png") }
    ImageComposeScene(700, 500) {
        PrismTheme(AppThemeId.PRISM) {
            FilePickerPanel(
                lastDirectory = null,
                mode = PickerMode.SAVE,
                source = Path.of("Demo", "Documents", "Source.docx"),
                onSelect = {},
                onDismiss = {},
                fileSystem = remember { PreviewPickerFileSystem() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }.use { capture(it, "picker-save-compact-prism.png") }
    listOf(AppThemeId.GRAPHITE, AppThemeId.VIOLET).forEach { theme ->
        ImageComposeScene(900, 650) {
            PrismTheme(theme) {
                FilePickerPanel(
                    lastDirectory = null,
                    mode = PickerMode.SAVE,
                    source = Path.of("Demo", "Documents", "Source.docx"),
                    suggestedFileName = "Report.docx",
                    onSelect = {},
                    onDismiss = {},
                    fileSystem = remember { PreviewPickerFileSystem() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }.use { scene ->
            val suffix = theme.name.lowercase()
            capture(scene, "picker-save-$suffix.png")
            click(scene, 830f, 614f)
            Thread.sleep(350)
            capture(scene, "picker-save-overwrite-$suffix.png")
        }
    }
    println("Previews: ${output.absolutePath}")
}
