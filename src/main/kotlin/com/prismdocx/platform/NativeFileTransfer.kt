package com.prismdocx.platform

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

/** Получаем File напрямую из native file list: URL-декодирование повредило бы '%' и '+' в имени. */
internal fun droppedDocx(transferable: Transferable): File {
    require(transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        "Перетащите файл DOCX из проводника."
    }
    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
    require(files?.size == 1) { "Перетащите один DOCX за раз." }
    val file = files.single() as? File
    require(file != null && file.isFile && file.extension.equals("docx", ignoreCase = true)) {
        "Нужен файл .docx. Папки и другие форматы не поддерживаются."
    }
    return file
}
