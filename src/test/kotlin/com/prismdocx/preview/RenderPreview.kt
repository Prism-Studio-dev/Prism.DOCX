package com.prismdocx.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.prismdocx.ui.App
import com.prismdocx.metadata.MetadataEditor
import com.prismdocx.metadata.metadataFields
import com.prismdocx.editor.OpenedDocument
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.use
import java.io.File
import com.prismdocx.resources.Res
import com.prismdocx.resources.prism_docx_app_icon
import org.jetbrains.compose.resources.painterResource

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
    ImageComposeScene(450, 450) {
        Image(painterResource(Res.drawable.prism_docx_app_icon), contentDescription = null, modifier = Modifier.fillMaxSize())
    }.use { capture(it, "svg-icon.png") }
    ImageComposeScene(1200, 850) { App() }.use { scene ->
        capture(scene, "home.png")
        scene.sendPointerEvent(PointerEventType.Press, Offset(1100f, 43f))
        scene.sendPointerEvent(PointerEventType.Release, Offset(1100f, 43f))
        capture(scene, "home-dark.png")
    }
    ImageComposeScene(820, 640) { App() }.use { capture(it, "home-compact.png") }
    ImageComposeScene(1200, 850) { App(initialDocument = OpenedDocument(File("Исследование.docx"), sample)) }.use { scene ->
        capture(scene, "editor.png")
        scene.sendPointerEvent(PointerEventType.Press, Offset(100f, 770f))
        scene.sendPointerEvent(PointerEventType.Release, Offset(100f, 770f))
        capture(scene, "editor-dark.png")
    }
    ImageComposeScene(820, 640) { App(initialDocument = OpenedDocument(File("Исследование.docx"), sample)) }.use {
        capture(it, "editor-compact.png")
    }
    println("Previews: ${output.absolutePath}")
}
