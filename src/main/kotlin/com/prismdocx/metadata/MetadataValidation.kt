package com.prismdocx.metadata

import com.prismdocx.metadata.XmlSupport.checkRoot
import com.prismdocx.metadata.XmlSupport.parse
import com.prismdocx.metadata.XmlSupport.children
import org.w3c.dom.Element
import java.time.OffsetDateTime

internal data class MetadataInspection(
    val values: Map<String, String>,
    val customProperties: List<CustomProperty>,
    val validationErrors: List<String>,
)

/** Проверки общих полей и пользовательских значений; не заменяют полную XSD-валидацию OOXML. */
object MetadataValidation {
    private val appVersionPattern = Regex("[0-9]{2}\\.[0-9]{4}")
    private val customFormatPattern = Regex("\\{[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}}")

    fun fieldError(field: MetadataField, value: String): String? {
        if (value.isEmpty()) return null
        return when (field.kind) {
            FieldKind.INTEGER -> if (value.toIntOrNull()?.let { it >= 0 } == true) null else "Нужно целое число от 0 до 2147483647"
            FieldKind.BOOLEAN -> if (value in listOf("true", "false", "0", "1")) null else "Используйте true или false"
            FieldKind.DATE -> if (runCatching { OffsetDateTime.parse(value) }.isSuccess) null else "Формат: 2026-09-23T12:00:00+03:00"
            FieldKind.VERSION -> if (appVersionPattern.matches(value)) null else "Формат версии: 16.0000"
            else -> null
        }
    }

    fun validationErrors(snapshot: MetadataSnapshot): List<String> {
        val parts = parsedParts(snapshot)
        return validationErrors(parts, MetadataEditor.values(parts))
    }

    internal fun inspect(snapshot: MetadataSnapshot): MetadataInspection {
        val parts = parsedParts(snapshot)
        val values = MetadataEditor.values(parts)
        return MetadataInspection(
            values,
            MetadataEditor.customProperties(parts.getValue(MetadataPart.CUSTOM)),
            validationErrors(parts, values),
        )
    }

    private fun parsedParts(snapshot: MetadataSnapshot): Map<MetadataPart, List<Element>> =
        snapshot.xml.mapValues { (part, xml) ->
            val document = parse(xml)
            checkRoot(document, part)
            children(document.documentElement)
        }

    private fun validationErrors(parts: Map<MetadataPart, List<Element>>, values: Map<String, String>): List<String> = buildList {
        metadataFields.forEach { field ->
            fieldError(field, values.getValue(field.key))?.let { add("${field.label}: $it") }
        }
        metadataFields.forEach { field ->
            if (parts.getValue(field.part).count {
                    it.namespaceURI == field.namespace && it.localName == field.key
                } > 1) {
                add("Повторяющееся свойство: ${field.label}")
            }
        }
        val properties = parts.getValue(MetadataPart.CUSTOM)
        val names = mutableSetOf<String>()
        val ids = mutableSetOf<String>()
        properties.forEach { property ->
            val name = property.getAttribute("name")
            val id = property.getAttribute("pid")
            if (name.isBlank() || !names.add(name.lowercase())) add("Название своего свойства должно быть заполнено и уникально: $name")
            if (id.toIntOrNull()?.let { it >= 2 } != true || !ids.add(id)) add("Некорректный или повторяющийся pid: $id")
            if (!customFormatPattern.matches(property.getAttribute("fmtid"))) add("Некорректный fmtid: $name")
            val entries = children(property)
            if (entries.size != 1 || entries.firstOrNull()?.namespaceURI != VT_NS) add("Свойство $name должно содержать одно типизированное значение")
            entries.firstOrNull()?.let { el ->
                val text = el.textContent
                val valid = when (el.localName) {
                    "i4", "int" -> text.toIntOrNull() != null
                    "i8" -> text.toLongOrNull() != null
                    "r8", "r4", "decimal" -> text.toBigDecimalOrNull() != null
                    "bool" -> text in listOf("true", "false", "0", "1")
                    "filetime", "date" -> runCatching { OffsetDateTime.parse(text) }.isSuccess
                    else -> true // Редкие VT-типы сохраняются и редактируются через XML.
                }
                if (!valid) add("Некорректное значение «$name» для типа ${el.localName}")
            }
        }
    }
}
