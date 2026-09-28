package com.prismdocx.platform.filepicker

import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FilePickerFileSystemTest {
    @Test fun listsOnlyDirectoriesAndDocxFilesWithDirectoriesFirst() = withTemporaryDirectory { directory ->
        Files.createDirectory(directory.resolve("Zoo"))
        Files.createDirectory(directory.resolve("alpha"))
        Files.createFile(directory.resolve("z.DOCX"))
        Files.createFile(directory.resolve("A.Docx"))
        Files.createFile(directory.resolve("readme.txt"))
        Files.createFile(directory.resolve("draft.docx.bak"))

        val entries = SystemPickerFileSystem(homeDirectory = directory).list(directory)

        assertEquals(listOf("alpha", "Zoo", "A.Docx", "z.DOCX"), entries.map { it.name })
        assertEquals(listOf(true, true, false, false), entries.map { it.isDirectory })
    }

    @Test fun acceptsOnlyExistingRegularDocxFilesRegardlessOfExtensionCase() = withTemporaryDirectory { directory ->
        val pickerFileSystem = SystemPickerFileSystem(homeDirectory = directory)
        val lower = Files.createFile(directory.resolve("lower.docx"))
        val upper = Files.createFile(directory.resolve("upper.DOCX"))
        val mixed = Files.createFile(directory.resolve("mixed.Docx"))
        val other = Files.createFile(directory.resolve("other.doc"))
        val namedDirectory = Files.createDirectory(directory.resolve("folder.docx"))

        assertTrue(pickerFileSystem.isDocxFile(lower))
        assertTrue(pickerFileSystem.isDocxFile(upper))
        assertTrue(pickerFileSystem.isDocxFile(mixed))
        assertFalse(pickerFileSystem.isDocxFile(other))
        assertFalse(pickerFileSystem.isDocxFile(namedDirectory))
        assertFalse(pickerFileSystem.isDocxFile(directory.resolve("deleted.docx")))
    }

    @Test fun initialDirectoryPrefersLastDirectoryThenDocuments() = withTemporaryDirectory { directory ->
        val home = Files.createDirectory(directory.resolve("home"))
        val documents = Files.createDirectory(home.resolve("Documents"))
        val last = Files.createDirectory(directory.resolve("last"))
        val pickerFileSystem = SystemPickerFileSystem(homeDirectory = home)

        assertEquals(last, pickerFileSystem.initialDirectory(last))
        assertEquals(documents, pickerFileSystem.initialDirectory(directory.resolve("missing")))
        assertEquals(documents, pickerFileSystem.initialDirectory(null))
    }

    @Test fun initialDirectoryFallsBackToHomeThenAvailableRoot() = withTemporaryDirectory { directory ->
        val home = Files.createDirectory(directory.resolve("home"))
        val missingHome = directory.resolve("missing-home")
        val rootProvider = { listOf(directory) }

        assertEquals(home, SystemPickerFileSystem(home, rootProvider).initialDirectory(null))
        assertEquals(directory, SystemPickerFileSystem(missingHome, rootProvider).initialDirectory(null))
    }

    @Test fun missingDirectoryIsReportedToCaller() = withTemporaryDirectory { directory ->
        val pickerFileSystem = SystemPickerFileSystem(homeDirectory = directory)
        assertFailsWith<NotDirectoryException> { pickerFileSystem.list(directory.resolve("missing")) }
    }

    private fun withTemporaryDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("prism-picker-test-")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
