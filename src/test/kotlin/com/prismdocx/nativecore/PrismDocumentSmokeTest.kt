package com.prismdocx.nativecore

import com.prismdocx.docx.DocxRepository
import com.prismdocx.editor.EditorController
import com.prismdocx.editor.SaveTarget
import com.prismdocx.metadata.CORE_NS
import com.prismdocx.metadata.CustomProperty
import com.prismdocx.metadata.DC_NS
import com.prismdocx.metadata.MetadataPart
import com.prismdocx.metadata.VT_NS
import com.prismdocx.metadata.XmlBackend
import com.prismdocx.metadata.XmlValidation
import com.prismdocx.metadata.metadataFields
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Real document workflow, run with Kotlin by test and strict Rust by native integration tasks. */
class PrismDocumentSmokeTest {
    @Test
    fun opensEditsValidatesSavesAndReopensWithoutChangingDocumentPayload() {
        val expectedBackend = when (System.getProperty("prism.docx.xml.backend", "kotlin")) {
            "rust" -> XmlBackend.RUST
            "kotlin" -> XmlBackend.KOTLIN
            else -> error("The document smoke test requires an explicit kotlin or rust XML backend.")
        }
        assertEquals(expectedBackend, XmlValidation.backend, "Native integration must use Rust without fallback.")
        val directory = Files.createTempDirectory("prism-document-smoke-").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val payload = linkedMapOf(
                "word/document.xml" to """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>Original document content — исходный текст.</w:t></w:r></w:p></w:body></w:document>""".toByteArray(Charsets.UTF_8),
                "word/media/image.bin" to ByteArray(256) { it.toByte() },
            )
            val source = createFixture(directory, payload)
            val originalArchive = source.readBytes()
            val repository = DocxRepository()
            val editor = EditorController(repository, scope, Dispatchers.Unconfined)
            var opened = false
            editor.requestOpen(source) { opened = true }

            assertTrue(opened, "The real DOCX repository must finish opening the fixture.")
            assertFalse(editor.busy)
            assertNull(editor.notice)
            assertEquals(source, editor.source)
            assertEquals("Original title", editor.values.getValue("title"))
            assertEquals("Original company", editor.values.getValue("Company"))
            assertEquals("7", editor.customProperties.single().value)
            assertTrue(editor.validationErrors.isEmpty())

            val title = "Прототип & <Rust> 🦀"
            editor.changeField(metadataFields.single { it.key == "title" }, title)
            editor.changeField(metadataFields.single { it.key == "Company" }, "Prism Unstable")
            editor.changeCustom(editor.customProperties.single().copy(name = "Число", value = "42"))
            editor.changeCustom(CustomProperty("", "Temporary", "bool", "true"))
            editor.removeCustom(editor.customProperties.single { it.name == "Temporary" }.id)
            editor.changeCustom(CustomProperty("", "Channel", "lpwstr", "Unstable"))

            assertEquals(title, editor.values.getValue("title"))
            assertEquals("Prism Unstable", editor.values.getValue("Company"))
            assertEquals(setOf("Число", "Channel"), editor.customProperties.map { it.name }.toSet())
            assertEquals("42", editor.customProperties.single { it.name == "Число" }.value)
            assertTrue(editor.canSave)

            for (part in MetadataPart.entries) {
                editor.selectXmlPart(part)
                val before = editor.draft
                val xml = editor.xmlText
                assertEquals(before.xml.getValue(part), xml, "The XML editor must open ${part.path}.")

                val malformed = "<root><value></root>"
                editor.changeXml(malformed)
                editor.applyXml()
                assertTrue(editor.notice?.isError == true, "Malformed XML must be rejected in ${part.path}.")
                assertEquals(before, editor.draft, "Rejected XML must preserve the last valid metadata snapshot.")
                assertEquals(malformed, editor.xmlDraft)
                assertFalse(editor.canSave)
                val rejectedTarget = File(directory, "rejected-${part.name}.docx")
                editor.save(SaveTarget(rejectedTarget, overwrite = false))
                assertFalse(rejectedTarget.exists(), "An invalid XML draft must not be saved.")
                editor.discardXml()

                editor.changeXml("<wrong/>")
                editor.applyXml()
                assertTrue(editor.notice?.isError == true, "Well-formed XML with the wrong metadata root must still be rejected.")
                assertEquals(before, editor.draft)
                editor.discardXml()

                val closingTag = xml.lastIndexOf("</")
                assertTrue(closingTag >= 0)
                val comment = "<!-- smoke-${part.name} -->"
                editor.changeXml(xml.substring(0, closingTag) + comment + xml.substring(closingTag))
                editor.applyXml()
                assertFalse(editor.notice?.isError ?: true, "Valid XML must be accepted in ${part.path}.")
                assertNull(editor.xmlDraft)
                assertTrue(editor.draft.xml.getValue(part).contains(comment))
                assertTrue(editor.canSave)
            }

            val expected = editor.draft
            val target = File(directory, "saved-copy.docx")
            editor.save(SaveTarget(target, overwrite = false))
            assertTrue(target.isFile, editor.notice?.text ?: "DOCX save did not produce the copy.")
            assertFalse(editor.busy)
            assertFalse(editor.notice?.isError ?: true)
            assertFalse(editor.dirty)
            assertEquals(source, editor.source)
            assertContentEquals(originalArchive, source.readBytes(), "Saving a copy must preserve the entire source DOCX.")
            ZipFile(target).use { archive ->
                payload.forEach { (path, bytes) ->
                    archive.getInputStream(archive.getEntry(path)).use {
                        assertContentEquals(bytes, it.readBytes(), "Saving metadata must preserve $path byte for byte.")
                    }
                }
            }

            val reopened = EditorController(repository, scope, Dispatchers.Unconfined)
            reopened.requestOpen(target)
            assertEquals(target, reopened.source, "The saved copy must reopen through the real editor controller.")
            assertNull(reopened.notice)
            assertEquals(expected, reopened.draft)
            assertEquals(title, reopened.values.getValue("title"))
            assertEquals("Prism Unstable", reopened.values.getValue("Company"))
            assertEquals("42", reopened.customProperties.single { it.name == "Число" }.value)
            assertEquals("Unstable", reopened.customProperties.single { it.name == "Channel" }.value)
            assertTrue(reopened.validationErrors.isEmpty())
            assertFalse(reopened.dirty)
            println("DOCX smoke passed: opening, metadata/custom editing, all XML parts, malformed/root rejection, save/reopen, unchanged document/media/source bytes.")
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    private fun createFixture(directory: File, payload: Map<String, ByteArray>): File {
        val overrides = MetadataPart.entries.joinToString("") {
            """<Override PartName="/${it.path}" ContentType="${it.contentType}"/>"""
        }
        val relationships = MetadataPart.entries.joinToString("") {
            """<Relationship Id="${it.name}" Type="${it.relation}" Target="${it.path}"/>"""
        }
        val metadata = mapOf(
            MetadataPart.CORE.path to """<?xml version="1.0" encoding="UTF-8"?><cp:coreProperties xmlns:cp="$CORE_NS" xmlns:dc="$DC_NS"><dc:title>Original title</dc:title><dc:creator>Fixture author</dc:creator></cp:coreProperties>""",
            MetadataPart.APP.path to """<?xml version="1.0" encoding="UTF-8"?><Properties xmlns="${MetadataPart.APP.namespace}"><Application>Fixture</Application><AppVersion>16.0000</AppVersion><Company>Original company</Company><Pages>3</Pages></Properties>""",
            MetadataPart.CUSTOM.path to """<?xml version="1.0" encoding="UTF-8"?><Properties xmlns="${MetadataPart.CUSTOM.namespace}" xmlns:vt="$VT_NS"><property fmtid="{D5CDD505-2E9C-101B-9397-08002B2CF9AE}" pid="2" name="Count"><vt:i4>7</vt:i4></property></Properties>""",
        ).mapValues { it.value.toByteArray(Charsets.UTF_8) }
        val entries = linkedMapOf(
            "[Content_Types].xml" to """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>$overrides</Types>""".toByteArray(Charsets.UTF_8),
            "_rels/.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="main" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>$relationships</Relationships>""".toByteArray(Charsets.UTF_8),
        ) + metadata + payload
        val source = File(directory, "source.docx")
        ZipOutputStream(source.outputStream()).use { archive ->
            entries.forEach { (path, bytes) ->
                archive.putNextEntry(ZipEntry(path))
                archive.write(bytes)
                archive.closeEntry()
            }
        }
        return source
    }
}
