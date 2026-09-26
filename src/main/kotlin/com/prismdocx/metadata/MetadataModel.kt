package com.prismdocx.metadata

const val CORE_NS = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
const val DC_NS = "http://purl.org/dc/elements/1.1/"
const val DATE_NS = "http://purl.org/dc/terms/"
const val VT_NS = "http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"

enum class MetadataPart(val path: String, val namespace: String, val root: String, val relation: String, val contentType: String) {
    CORE("docProps/core.xml", CORE_NS, "cp:coreProperties",
        "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties",
        "application/vnd.openxmlformats-package.core-properties+xml"),
    APP("docProps/app.xml", "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties", "Properties",
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties",
        "application/vnd.openxmlformats-officedocument.extended-properties+xml"),
    CUSTOM("docProps/custom.xml", "http://schemas.openxmlformats.org/officeDocument/2006/custom-properties", "Properties",
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/custom-properties",
        "application/vnd.openxmlformats-officedocument.custom-properties+xml"),
}

enum class FieldKind { TEXT, MULTILINE, DATE, INTEGER, BOOLEAN, VERSION }
enum class MetadataSection(val title: String, val subtitle: String) {
    DESCRIPTION("Описание", "Название, тема и всё, что помогает найти документ."),
    PEOPLE("Авторство", "Люди и организация, связанные с документом."),
    DATES("Даты и версии", "История документа под вашим контролем."),
    APPLICATION("Приложение", "Параметры программы и свойства документа."),
    STATISTICS("Статистика", "Сохранённые счётчики. Word может пересчитать их при следующем сохранении."),
    CUSTOM("Свои свойства", "Добавляйте поля, которые нужны именно вам."),
    XML("XML-редактор", "Полный доступ к трём частям свойств, включая составные и редкие значения."),
}

data class MetadataField(
    val key: String, val label: String, val section: MetadataSection,
    val part: MetadataPart = MetadataPart.CORE,
    val namespace: String = CORE_NS, val prefix: String = "cp",
    val kind: FieldKind = FieldKind.TEXT,
    val hint: String = "",
)

/** XML остаётся источником истины, чтобы формы не теряли неизвестные свойства и атрибуты. */
data class MetadataSnapshot(val xml: Map<MetadataPart, String>)
data class CustomProperty(val id: String, val name: String, val type: String, val value: String)
val customTypes = linkedMapOf("lpwstr" to "Текст", "i4" to "Целое", "r8" to "Число", "bool" to "Да / нет", "filetime" to "Дата")
