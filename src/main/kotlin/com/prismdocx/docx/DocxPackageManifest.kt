package com.prismdocx.docx

import com.prismdocx.metadata.MetadataPart
import com.prismdocx.metadata.XmlSupport.children
import com.prismdocx.metadata.XmlSupport.serialize
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.net.URI
import java.util.zip.ZipFile

/**
 * Связи OOXML определяют реальные пути частей: они не обязаны лежать в docProps.
 * Этот класс проверяет пакет и регистрирует отсутствующие части при сохранении.
 */
internal class DocxPackageManifest private constructor(
    private val relationships: Document,
    private val contentTypes: Document,
    private val paths: Map<MetadataPart, String>,
) {
    private var changed = false

    fun pathOf(part: MetadataPart): String = paths.getValue(part)

    fun register(part: MetadataPart) {
        val path = pathOf(part)
        val relationshipsRoot = relationships.documentElement
        if (children(relationshipsRoot).none { it.isRelationFor(part) }) {
            val usedIds = children(relationshipsRoot).map { it.getAttribute("Id") }.toSet()
            val id = generateSequence(1) { it + 1 }.map { "prism$it" }.first { it !in usedIds }
            relationshipsRoot.appendChild(relationships.createElementNS(REL_NS, "Relationship").apply {
                setAttribute("Id", id)
                setAttribute("Type", part.relation)
                setAttribute("Target", "/$path".asPartName())
            })
            changed = true
        }

        val partName = "/$path".asPartName()
        val typesRoot = contentTypes.documentElement
        if (children(typesRoot).none { it.localName == "Override" && it.getAttribute("PartName") == partName }) {
            typesRoot.appendChild(contentTypes.createElementNS(CT_NS, "Override").apply {
                setAttribute("PartName", partName)
                setAttribute("ContentType", part.contentType)
            })
            changed = true
        }
    }

    fun updatedParts(): Map<String, ByteArray> =
        if (!changed) emptyMap()
        else mapOf(
            RELS_PATH to serialize(relationships).toByteArray(Charsets.UTF_8),
            TYPES_PATH to serialize(contentTypes).toByteArray(Charsets.UTF_8),
        )

    companion object {
        private const val RELS_PATH = "_rels/.rels"
        private const val TYPES_PATH = "[Content_Types].xml"
        private const val REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
        private const val CT_NS = "http://schemas.openxmlformats.org/package/2006/content-types"
        private const val MAIN_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"

        fun open(zip: ZipFile): DocxPackageManifest {
            val relationshipsEntry = requireNotNull(zip.getEntry(RELS_PATH)) { "Файл не является пакетом DOCX." }
            val typesEntry = requireNotNull(zip.getEntry(TYPES_PATH)) { "Файл не является пакетом DOCX." }
            val entryNames = zip.entries().asSequence().map { it.name }.toList()
            require(entryNames.size == entryNames.toSet().size) { "В архиве повторяются имена частей." }

            val types = readXml(zip, typesEntry)
            require(types.documentElement.namespaceURI == CT_NS && types.documentElement.localName == "Types") {
                "Некорректный список типов DOCX."
            }
            val relationships = readXml(zip, relationshipsEntry)
            require(relationships.documentElement.namespaceURI == REL_NS && relationships.documentElement.localName == "Relationships") {
                "Некорректные связи DOCX."
            }
            val entries = children(relationships.documentElement)
            val main = entries.firstOrNull { it.getAttribute("Type").endsWith("/officeDocument") }
            val mainPath = main?.resolveTarget()
            require(mainPath != null && zip.getEntry(mainPath) != null) {
                "Основная часть документа отсутствует."
            }
            val contentEntries = children(types.documentElement)
            val mainPartName = "/$mainPath".asPartName()
            val mainType = contentEntries.firstOrNull {
                it.localName == "Override" && it.getAttribute("PartName") == mainPartName
            }?.getAttribute("ContentType") ?: contentEntries.firstOrNull {
                it.localName == "Default" && it.getAttribute("Extension").equals(mainPath.substringAfterLast('.'), true)
            }?.getAttribute("ContentType")
            require(mainType == MAIN_TYPE) { "Ожидается документ Word .docx." }

            val paths = MetadataPart.entries.associateWith { part ->
                val matches = entries.filter { it.isRelationFor(part) }
                require(matches.size <= 1) { "Несколько частей свойств ${part.path}." }
                matches.firstOrNull()?.resolveTarget() ?: part.path
            }
            require(paths.values.toSet().size == paths.size) { "Части свойств ссылаются на один путь." }
            return DocxPackageManifest(relationships, types, paths)
        }

        private fun Element.isRelationFor(part: MetadataPart): Boolean {
            val type = getAttribute("Type")
            return type == part.relation || (part != MetadataPart.CORE &&
                type == part.relation.replace(
                    "http://schemas.openxmlformats.org/officeDocument/2006",
                    "http://purl.oclc.org/ooxml/officeDocument",
                ))
        }

        private fun Element.resolveTarget(): String {
            require(getAttribute("TargetMode") != "External") { "Внешние части свойств не поддерживаются." }
            val uri = URI(getAttribute("Target"))
            require(!uri.isAbsolute && uri.query == null && uri.fragment == null) { "Некорректный путь части." }
            val path = URI("/").resolve(uri).normalize().path.removePrefix("/")
            require(path.isNotBlank() && !path.contains('\\')) { "Некорректный путь части." }
            return path
        }

        private fun String.asPartName(): String = URI(null, null, this, null).toASCIIString()
    }
}
