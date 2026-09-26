package com.prismdocx.platform

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DocxFileDropTest {
    private lateinit var directory: File
    @BeforeTest fun prepare() { directory = Files.createTempDirectory("prism-drop-").toFile() }
    @AfterTest fun cleanup() { directory.deleteRecursively() }

    private fun transfer(files: List<File>) = object : Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.javaFileListFlavor
        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return files
        }
    }

    @Test fun acceptsUnicodeAndSpecialCharactersWithoutDecodingOrMovingFile() {
        val file = File(directory, "Отчёт 100% + данные #1.DOCX").apply { writeText("fixture") }
        assertEquals(file, droppedDocx(transfer(listOf(file))))
        assertTrue(file.exists())
        assertEquals("fixture", file.readText())
    }

    @Test fun rejectsMultipleFilesRatherThanSilentlyOpeningOnlyOne() {
        val files = listOf("one.docx", "two.docx").map { File(directory, it).apply { createNewFile() } }
        assertFailsWith<IllegalArgumentException> { droppedDocx(transfer(files)) }
        assertFailsWith<IllegalArgumentException> { droppedDocx(transfer(emptyList())) }
    }

    @Test fun rejectsFoldersOtherFormatsMissingFilesAndTextDrags() {
        val folder = File(directory, "folder.docx").apply { mkdir() }
        val pdf = File(directory, "document.pdf").apply { createNewFile() }
        listOf(folder, pdf, File(directory, "missing.docx")).forEach {
            assertFailsWith<IllegalArgumentException> { droppedDocx(transfer(listOf(it))) }
        }
        assertFailsWith<IllegalArgumentException> { droppedDocx(StringSelection("C:/document.docx")) }
    }
}
