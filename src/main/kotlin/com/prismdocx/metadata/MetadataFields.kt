package com.prismdocx.metadata

/** Каталог отображаемых полей и их привязка к именам и пространствам имён OOXML. */
val metadataFields = listOf(
    MetadataField("title", "Название", MetadataSection.DESCRIPTION, namespace = DC_NS, prefix = "dc"),
    MetadataField("subject", "Тема", MetadataSection.DESCRIPTION, namespace = DC_NS, prefix = "dc"),
    MetadataField("description", "Описание / комментарий", MetadataSection.DESCRIPTION, namespace = DC_NS, prefix = "dc", kind = FieldKind.MULTILINE),
    MetadataField("keywords", "Ключевые слова", MetadataSection.DESCRIPTION),
    MetadataField("category", "Категория", MetadataSection.DESCRIPTION),
    MetadataField("contentStatus", "Статус документа", MetadataSection.DESCRIPTION),
    MetadataField("contentType", "Тип содержимого", MetadataSection.DESCRIPTION),
    MetadataField("identifier", "Идентификатор", MetadataSection.DESCRIPTION, namespace = DC_NS, prefix = "dc"),
    MetadataField("language", "Язык", MetadataSection.DESCRIPTION, namespace = DC_NS, prefix = "dc", hint = "Например, ru-RU"),
    MetadataField("creator", "Автор", MetadataSection.PEOPLE, namespace = DC_NS, prefix = "dc"),
    MetadataField("lastModifiedBy", "Последний редактор", MetadataSection.PEOPLE),
    MetadataField("created", "Дата создания", MetadataSection.DATES, namespace = DATE_NS, prefix = "dcterms", kind = FieldKind.DATE),
    MetadataField("modified", "Дата изменения", MetadataSection.DATES, namespace = DATE_NS, prefix = "dcterms", kind = FieldKind.DATE),
    MetadataField("lastPrinted", "Последняя печать", MetadataSection.DATES, kind = FieldKind.DATE),
    MetadataField("revision", "Номер редакции", MetadataSection.DATES),
    MetadataField("version", "Версия документа", MetadataSection.DATES),
) + listOf(
    Triple("Manager", "Руководитель", MetadataSection.PEOPLE),
    Triple("Company", "Организация", MetadataSection.PEOPLE),
    Triple("Application", "Приложение", MetadataSection.APPLICATION),
    Triple("AppVersion", "Версия приложения", MetadataSection.APPLICATION),
    Triple("Template", "Шаблон", MetadataSection.APPLICATION),
    Triple("HyperlinkBase", "База гиперссылок", MetadataSection.APPLICATION),
    Triple("PresentationFormat", "Формат презентации", MetadataSection.APPLICATION),
    Triple("DocSecurity", "Код безопасности", MetadataSection.APPLICATION),
    Triple("ScaleCrop", "Обрезка эскиза", MetadataSection.APPLICATION),
    Triple("LinksUpToDate", "Ссылки обновлены", MetadataSection.APPLICATION),
    Triple("SharedDoc", "Общий документ", MetadataSection.APPLICATION),
    Triple("HyperlinksChanged", "Гиперссылки изменены", MetadataSection.APPLICATION),
    Triple("Pages", "Страницы", MetadataSection.STATISTICS),
    Triple("Words", "Слова", MetadataSection.STATISTICS),
    Triple("Characters", "Символы без пробелов", MetadataSection.STATISTICS),
    Triple("CharactersWithSpaces", "Символы с пробелами", MetadataSection.STATISTICS),
    Triple("Lines", "Строки", MetadataSection.STATISTICS),
    Triple("Paragraphs", "Абзацы", MetadataSection.STATISTICS),
    Triple("TotalTime", "Время редактирования, мин", MetadataSection.STATISTICS),
    Triple("Slides", "Слайды", MetadataSection.STATISTICS),
    Triple("Notes", "Заметки", MetadataSection.STATISTICS),
    Triple("HiddenSlides", "Скрытые слайды", MetadataSection.STATISTICS),
    Triple("MMClips", "Мультимедийные клипы", MetadataSection.STATISTICS),
).map { (key, label, section) ->
    MetadataField(key, label, section, MetadataPart.APP, MetadataPart.APP.namespace, "",
        when {
            section == MetadataSection.STATISTICS || key == "DocSecurity" -> FieldKind.INTEGER
            key in listOf("ScaleCrop", "LinksUpToDate", "SharedDoc", "HyperlinksChanged") -> FieldKind.BOOLEAN
            key == "AppVersion" -> FieldKind.VERSION
            else -> FieldKind.TEXT
        }, if (key == "DocSecurity") "Информационный флаг; не устанавливает пароль." else "")
}
