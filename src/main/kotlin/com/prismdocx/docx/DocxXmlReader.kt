package com.prismdocx.docx

import com.prismdocx.metadata.XmlSupport
import org.w3c.dom.Document
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Читает XML-часть пакета с ограничением размера до распаковки в DOM. */
internal fun readXml(zip: ZipFile, entry: ZipEntry): Document = zip.getInputStream(entry).use { input ->
    val bytes = input.readNBytes(XmlSupport.MAX_XML + 1)
    require(bytes.size <= XmlSupport.MAX_XML) { "Часть ${entry.name} превышает 4 МБ." }
    // Байтовый парсер учитывает исходную кодировку XML-декларации, включая UTF-16.
    XmlSupport.parse(bytes)
}
