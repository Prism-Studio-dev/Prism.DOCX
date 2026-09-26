package com.prismdocx.docx

import com.prismdocx.metadata.*
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.*

class DocxRepositoryTest {
    private val repository = DocxRepository()
    private lateinit var directory: File

    @BeforeTest fun prepare() { directory = Files.createTempDirectory("prism-test-").toFile() }
    @AfterTest fun cleanup() { directory.deleteRecursively() }

    private val content = """<?xml version="1.0"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>Keep this text.</w:t></w:r></w:p></w:body></w:document>"""
    private val binary = ByteArray(256) { it.toByte() }

    private fun fixture(extra: Map<String, ByteArray> = emptyMap(), extraRelations: String = ""): File {
        val file = File(directory, "source.docx")
        val entries = linkedMapOf(
            "[Content_Types].xml" to """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""".toByteArray(),
            "_rels/.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="main" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>$extraRelations</Relationships>""".toByteArray(),
            "word/document.xml" to content.toByteArray(),
            "word/media/image.bin" to binary,
        ) + extra
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
        }
        return file
    }
    private fun field(key: String) = metadataFields.single { it.key == key }
    private fun set(snapshot: MetadataSnapshot, key: String, value: String) = MetadataEditor.setValue(snapshot, field(key), value)
    private fun entry(file: File, path: String) = ZipFile(file).use { zip -> zip.getInputStream(zip.getEntry(path)).readBytes() }

    @Test fun createsAndRegistersAllMissingPartsAndPreservesDocument() {
        val source = fixture()
        val original = source.readBytes()
        var model = repository.read(source)
        model = set(model, "title", "  Документ & <данные> 😀  ")
        model = set(model, "Company", "Prism")
        model = MetadataEditor.setCustom(model, CustomProperty("", "Номер", "i4", "42"))
        val target = File(directory, "copy.docx")
        repository.write(source, target, model)
        val result = repository.read(target)
        assertEquals("  Документ & <данные> 😀  ", MetadataEditor.value(result, field("title")))
        assertEquals("Prism", MetadataEditor.value(result, field("Company")))
        assertEquals("42", MetadataEditor.customProperties(result).single().value)
        assertContentEquals(original, source.readBytes())
        assertContentEquals(content.toByteArray(), entry(target, "word/document.xml"))
        assertContentEquals(binary, entry(target, "word/media/image.bin"))
        val rels = entry(target, "_rels/.rels").toString(Charsets.UTF_8)
        val types = entry(target, "[Content_Types].xml").toString(Charsets.UTF_8)
        MetadataPart.entries.forEach {
            assertTrue(rels.contains(it.relation))
            assertTrue(types.contains(it.contentType))
        }
    }

    @Test fun preservesDatesUnknownElementsAndNonstandardPartPaths() {
        val corePath = "properties/details.xml"
        val xml = """<cp:coreProperties xmlns:cp="$CORE_NS" xmlns:dc="$DC_NS" xmlns:dcterms="$DATE_NS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:extra="urn:extra"><dc:title>Before</dc:title><dcterms:modified xsi:type="dcterms:W3CDTF">2001-01-01T00:00:00Z</dcterms:modified><extra:unknown flag="keep">untouched</extra:unknown></cp:coreProperties>"""
        val source = fixture(mapOf(corePath to xml.toByteArray()),
            """<Relationship Id="core" Type="${MetadataPart.CORE.relation}" Target="$corePath"/>""")
        val model = set(repository.read(source), "title", "After")
        val target = File(directory, "copy.docx")
        repository.write(source, target, model)
        val result = repository.read(target)
        assertEquals("2001-01-01T00:00:00Z", MetadataEditor.value(result, field("modified")))
        val saved = entry(target, corePath).toString(Charsets.UTF_8)
        assertTrue(saved.contains("untouched"))
        assertTrue(saved.contains("flag=\"keep\""))
        ZipFile(target).use { assertNull(it.getEntry(MetadataPart.CORE.path)) }
    }

    @Test fun editsEveryStandardPropertyAndDeletesEmptyValues() {
        val source = fixture()
        var model = repository.read(source)
        val expected = metadataFields.associate { field ->
            field.key to when (field.kind) {
                FieldKind.DATE -> "2020-05-06T12:30:00+03:00"
                FieldKind.INTEGER -> "123"
                FieldKind.BOOLEAN -> "true"
                FieldKind.VERSION -> "16.0000"
                else -> "Тест ${field.key}"
            }
        }
        metadataFields.forEach { model = MetadataEditor.setValue(model, it, expected.getValue(it.key)) }
        val target = File(directory, "all.docx")
        repository.write(source, target, model)
        assertEquals(expected, MetadataEditor.values(repository.read(target)))
        metadataFields.forEach { model = MetadataEditor.setValue(model, it, "") }
        assertTrue(MetadataEditor.values(model).values.all { it.isEmpty() })
        assertFalse(model.xml.getValue(MetadataPart.CORE).contains("dc:title"))
    }

    @Test fun customTypesNamesDeletionAndComplexValuesRoundTrip() {
        val custom = """<Properties xmlns="${MetadataPart.CUSTOM.namespace}" xmlns:vt="$VT_NS"><property fmtid="{D5CDD505-2E9C-101B-9397-08002B2CF9AE}" pid="2" name="List"><vt:vector size="2" baseType="lpwstr"><vt:lpwstr>one</vt:lpwstr><vt:lpwstr>two</vt:lpwstr></vt:vector></property></Properties>"""
        val source = fixture(mapOf(MetadataPart.CUSTOM.path to custom.toByteArray()))
        var model = repository.read(source)
        mapOf("lpwstr" to "abc", "i4" to "-12", "r8" to "1.25", "bool" to "true", "filetime" to "2020-01-01T00:00:00Z").forEach { (type, value) ->
            model = MetadataEditor.setCustom(model, CustomProperty("", type, type, value))
        }
        val properties = MetadataEditor.customProperties(model)
        model = MetadataEditor.setCustom(model, properties.single { it.type == "lpwstr" }.copy(name = "Renamed", value = "новое"))
        model = MetadataEditor.removeCustom(model, properties.single { it.type == "i4" }.id)
        val target = File(directory, "custom.docx")
        repository.write(source, target, model)
        val result = MetadataEditor.customProperties(repository.read(target))
        assertEquals(5, result.size)
        assertTrue(result.any { it.name == "Renamed" && it.value == "новое" })
        assertTrue(entry(target, MetadataPart.CUSTOM.path).toString(Charsets.UTF_8).contains("<vt:lpwstr>two</vt:lpwstr>"))
    }

    @Test fun renamingCustomPropertyPreservesValueAttributes() {
        val custom = """<Properties xmlns="${MetadataPart.CUSTOM.namespace}" xmlns:vt="$VT_NS"><property fmtid="{D5CDD505-2E9C-101B-9397-08002B2CF9AE}" pid="2" name="Original"><vt:lpwstr xml:space="preserve">  value  </vt:lpwstr></property></Properties>"""
        val source = fixture(mapOf(MetadataPart.CUSTOM.path to custom.toByteArray()))
        val snapshot = repository.read(source)
        val renamed = MetadataEditor.setCustom(snapshot, MetadataEditor.customProperties(snapshot).single().copy(name = "Renamed"))
        val target = File(directory, "renamed.docx")
        repository.write(source, target, renamed)

        val saved = entry(target, MetadataPart.CUSTOM.path).toString(Charsets.UTF_8)
        assertTrue(saved.contains("name=\"Renamed\""))
        assertTrue(saved.contains("xml:space=\"preserve\""))
        assertEquals("  value  ", MetadataEditor.customProperties(repository.read(target)).single().value)
    }

    @Test fun invalidTypesAndDuplicateNamesCannotBeSaved() {
        val source = fixture()
        val target = File(directory, "invalid.docx")
        val empty = repository.read(source)
        for ((key, value) in mapOf("Pages" to "-1", "modified" to "tomorrow", "SharedDoc" to "maybe", "AppVersion" to "bad")) {
            assertFailsWith<IllegalArgumentException> { repository.write(source, target, set(empty, key, value)) }
            assertFalse(target.exists())
        }
        var custom = MetadataEditor.setCustom(empty, CustomProperty("", "Duplicate", "i4", "NaN"))
        custom = MetadataEditor.setCustom(custom, CustomProperty("", "duplicate", "lpwstr", ""))
        assertTrue(MetadataValidation.validationErrors(custom).size >= 2)
        assertFailsWith<IllegalArgumentException> { repository.write(source, target, custom) }

        val malformedId = """<Properties xmlns="${MetadataPart.CUSTOM.namespace}" xmlns:vt="$VT_NS"><property fmtid="{------------------------------------}" pid="2" name="Bad"><vt:lpwstr>value</vt:lpwstr></property></Properties>"""
        val malformed = MetadataEditor.replaceXml(empty, MetadataPart.CUSTOM, malformedId)
        assertTrue(MetadataValidation.validationErrors(malformed).any { it.contains("fmtid") })
    }

    @Test fun refusesSourceOverwriteAndRequiresExplicitTargetOverwrite() {
        val source = fixture()
        val original = source.readBytes()
        val model = set(repository.read(source), "creator", "Someone")
        assertFailsWith<IllegalArgumentException> { repository.write(source, source, model, overwrite = true) }
        assertContentEquals(original, source.readBytes())
        val target = File(directory, "exists.docx")
        target.writeText("Existing content")
        assertFails { repository.write(source, target, model) }
        assertEquals("Existing content", target.readText())
        repository.write(source, target, model, overwrite = true)
        assertEquals("Someone", MetadataEditor.value(repository.read(target), field("creator")))
        assertTrue(directory.listFiles()!!.none { it.extension == "tmp" })
    }

    @Test fun rejectsMalformedWrongRootAndExternalEntityXml() {
        val empty = MetadataEditor.empty()
        assertFails { MetadataEditor.replaceXml(empty, MetadataPart.CORE, "<broken>") }
        assertFails { MetadataEditor.replaceXml(empty, MetadataPart.CORE, "<wrong/>") }
        assertFails { MetadataEditor.replaceXml(empty, MetadataPart.CORE,
            """<!DOCTYPE root [<!ENTITY xxe SYSTEM "file:///does-not-exist">]><cp:coreProperties xmlns:cp="$CORE_NS">&xxe;</cp:coreProperties>""") }
        val file = File(directory, "fake.docx")
        ZipOutputStream(file.outputStream()).use { it.putNextEntry(ZipEntry("text.txt")); it.write("fake".toByteArray()); it.closeEntry() }
        assertFails { repository.read(file) }
    }

    @Test fun rejectsPackageWhoseMainDocumentHasWrongContentType() {
        val types = """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/><Override PartName="/other.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/document.xml" ContentType="application/xml"/></Types>"""
        val source = fixture(mapOf("[Content_Types].xml" to types.toByteArray()))

        assertFailsWith<IllegalArgumentException> { repository.read(source) }
    }

    @Test fun acceptsUtf16PropertiesAndBindsDateTypeNamespace() {
        val xml = """<?xml version="1.0" encoding="UTF-16"?><cp:coreProperties xmlns:cp="$CORE_NS" xmlns:dc="$DC_NS"><dc:title>Привет</dc:title></cp:coreProperties>"""
        val source = fixture(mapOf(MetadataPart.CORE.path to xml.toByteArray(Charsets.UTF_16)))
        var model = repository.read(source)
        assertEquals("Привет", MetadataEditor.value(model, field("title")))
        model = set(model, "created", "2026-01-01T00:00:00Z")
        val target = File(directory, "date.docx")
        repository.write(source, target, model)
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            .parse(entry(target, MetadataPart.CORE.path).inputStream())
        val created = doc.getElementsByTagNameNS(DATE_NS, "created").item(0)
        assertEquals(DATE_NS, created.lookupNamespaceURI("dcterms"))
    }
}
