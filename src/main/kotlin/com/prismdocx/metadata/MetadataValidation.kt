package com.prismdocx.metadata

import com.prismdocx.metadata.XmlSupport.checkRoot
import com.prismdocx.metadata.XmlSupport.parse
import com.prismdocx.metadata.XmlSupport.children
import java.time.OffsetDateTime

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

    fun validationErrors(snapshot: MetadataSnapshot): List<String> = buildList {
        val values = MetadataEditor.values(snapshot)
        metadataFields.forEach { field ->
            fieldError(field, values.getValue(field.key))?.let { add("${field.label}: $it") }
        }
        val docs = snapshot.xml.mapValues { parse(it.value).also { doc -> checkRoot(doc, it.key) } }
        metadataFields.forEach { field ->
            if (children(docs.getValue(field.part).documentElement).count {
                    it.namespaceURI == field.namespace && it.localName == field.key
                } > 1) {
                add("Повторяющееся свойство: ${field.label}")
            }
        }
        val properties = children(docs.getValue(MetadataPart.CUSTOM).documentElement)
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
