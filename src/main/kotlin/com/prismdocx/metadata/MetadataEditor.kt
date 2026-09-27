package com.prismdocx.metadata

import com.prismdocx.metadata.XmlSupport.MAX_XML
import com.prismdocx.metadata.XmlSupport.newPart
import com.prismdocx.metadata.XmlSupport.checkRoot
import com.prismdocx.metadata.XmlSupport.parse
import com.prismdocx.metadata.XmlSupport.children
import com.prismdocx.metadata.XmlSupport.serialize
import org.w3c.dom.Element
import javax.xml.XMLConstants

/** Чистые преобразования снимка: неизвестные элементы XML сохраняются вместе с известными полями. */
object MetadataEditor {
    private const val XSI_NS = "http://www.w3.org/2001/XMLSchema-instance"

    fun empty() = MetadataSnapshot(MetadataPart.entries.associateWith { serialize(newPart(it)) })

    fun value(snapshot: MetadataSnapshot, field: MetadataField): String =
        children(parse(snapshot.xml.getValue(field.part)).documentElement)
            .firstOrNull { it.namespaceURI == field.namespace && it.localName == field.key }?.textContent.orEmpty()

    fun values(snapshot: MetadataSnapshot): Map<String, String> =
        values(snapshot.xml.mapValues { children(parse(it.value).documentElement) })

    internal fun values(parts: Map<MetadataPart, List<Element>>): Map<String, String> =
        metadataFields.associate { field ->
            field.key to parts.getValue(field.part)
                .firstOrNull { it.namespaceURI == field.namespace && it.localName == field.key }?.textContent.orEmpty()
        }

    fun setValue(snapshot: MetadataSnapshot, field: MetadataField, value: String): MetadataSnapshot {
        val doc = parse(snapshot.xml.getValue(field.part))
        val matches = children(doc.documentElement).filter { it.namespaceURI == field.namespace && it.localName == field.key }
        matches.drop(1).forEach { doc.documentElement.removeChild(it) }
        if (value.isEmpty()) {
            matches.firstOrNull()?.let { doc.documentElement.removeChild(it) }
        } else {
            val element = matches.firstOrNull() ?: doc.createElementNS(
                field.namespace, if (field.prefix.isEmpty()) field.key else "${field.prefix}:${field.key}"
            ).also { doc.documentElement.appendChild(it) }
            element.textContent = value
            if (field.namespace == DATE_NS) {
                doc.documentElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:dcterms", DATE_NS)
                element.setAttributeNS(XSI_NS, "xsi:type", "dcterms:W3CDTF")
            }
        }
        return snapshot.copy(xml = snapshot.xml + (field.part to serialize(doc)))
    }

    fun customProperties(snapshot: MetadataSnapshot): List<CustomProperty> =
        customProperties(children(parse(snapshot.xml.getValue(MetadataPart.CUSTOM)).documentElement))

    internal fun customProperties(properties: List<Element>): List<CustomProperty> =
        properties.map { property ->
            val value = children(property).firstOrNull()
            CustomProperty(property.getAttribute("pid"), property.getAttribute("name"), value?.localName.orEmpty(),
                if (value != null && children(value).isEmpty()) value.textContent else "[Составное значение — XML]")
        }

    fun setCustom(snapshot: MetadataSnapshot, property: CustomProperty): MetadataSnapshot {
        val doc = parse(snapshot.xml.getValue(MetadataPart.CUSTOM))
        val root = doc.documentElement
        val existing = children(root).firstOrNull { it.getAttribute("pid") == property.id }
        val element = existing ?: doc.createElementNS(MetadataPart.CUSTOM.namespace, "property").also {
            val next = (children(root).mapNotNull { el -> el.getAttribute("pid").toIntOrNull() }.maxOrNull() ?: 1) + 1
            it.setAttribute("pid", next.toString())
            it.setAttribute("fmtid", "{D5CDD505-2E9C-101B-9397-08002B2CF9AE}")
            root.appendChild(it)
        }
        element.setAttribute("name", property.name)
        // Составные VT-значения нельзя превращать в текст при переименовании свойства.
        if (property.type in customTypes) {
            val currentValue = children(element).singleOrNull()
            if (currentValue?.namespaceURI == VT_NS && currentValue.localName == property.type && children(currentValue).isEmpty()) {
                // При правке имени или текста сохраняем атрибуты существующего VT-элемента.
                if (currentValue.textContent != property.value) currentValue.textContent = property.value
            } else {
                while (element.hasChildNodes()) element.removeChild(element.firstChild)
                element.appendChild(doc.createElementNS(VT_NS, "vt:${property.type}").apply { textContent = property.value })
            }
        }
        return snapshot.copy(xml = snapshot.xml + (MetadataPart.CUSTOM to serialize(doc)))
    }

    fun removeCustom(snapshot: MetadataSnapshot, id: String): MetadataSnapshot {
        val doc = parse(snapshot.xml.getValue(MetadataPart.CUSTOM))
        children(doc.documentElement).firstOrNull { it.getAttribute("pid") == id }?.let { doc.documentElement.removeChild(it) }
        return snapshot.copy(xml = snapshot.xml + (MetadataPart.CUSTOM to serialize(doc)))
    }

    fun replaceXml(snapshot: MetadataSnapshot, part: MetadataPart, xml: String): MetadataSnapshot {
        require(xml.toByteArray().size <= MAX_XML) { "XML превышает 4 МБ." }
        val doc = parse(xml)
        checkRoot(doc, part)
        return snapshot.copy(xml = snapshot.xml + (part to serialize(doc)))
    }

    fun formatXml(xml: String): String = serialize(parse(xml), indent = true)
}
