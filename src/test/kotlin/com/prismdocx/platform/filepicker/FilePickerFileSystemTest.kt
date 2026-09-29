package com.prismdocx.platform.filepicker

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.AccessDeniedException
import java.nio.file.FileSystemException
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
        Files.createDirectory(directory.resolve("folder.docx"))
        Files.createFile(directory.resolve("z.DOCX"))
        Files.createFile(directory.resolve("A.Docx"))
        Files.createFile(directory.resolve("readme.txt"))
        Files.createFile(directory.resolve("draft.docx.bak"))

        val entries = SystemPickerFileSystem(homeDirectory = directory).list(directory)

        assertEquals(listOf("alpha", "folder.docx", "Zoo", "A.Docx", "z.DOCX"), entries.map { it.name })
        assertEquals(listOf(true, true, true, false, false), entries.map { it.isDirectory })
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
        val pickerFileSystem = SystemPickerFileSystem(homeDirectory = home, userFolders = { PickerUserFolders() })

        assertEquals(last, pickerFileSystem.initialDirectory(last))
        assertEquals(documents, pickerFileSystem.initialDirectory(directory.resolve("missing")))
        assertEquals(documents, pickerFileSystem.initialDirectory(null))
    }

    @Test fun initialDirectoryFallsBackToHomeThenAvailableRoot() = withTemporaryDirectory { directory ->
        val home = Files.createDirectory(directory.resolve("home"))
        val missingHome = directory.resolve("missing-home")
        val rootProvider = { listOf(directory) }

        assertEquals(home, SystemPickerFileSystem(home, rootProvider) { PickerUserFolders() }.initialDirectory(null))
        assertEquals(directory, SystemPickerFileSystem(missingHome, rootProvider) { PickerUserFolders() }.initialDirectory(null))
    }

    @Test fun redirectedUserFoldersAreUsedForShortcutsAndInitialDirectory() = withTemporaryDirectory { directory ->
        val home = Files.createDirectory(directory.resolve("home"))
        val desktop = Files.createDirectory(directory.resolve("redirected-desktop"))
        val documents = Files.createDirectory(directory.resolve("redirected-documents"))
        val downloads = Files.createDirectory(directory.resolve("redirected-downloads"))
        val pickerFileSystem = SystemPickerFileSystem(homeDirectory = home, userFolders = {
            PickerUserFolders(desktop, documents, downloads)
        })

        assertEquals(documents, pickerFileSystem.initialDirectory(null))
        assertEquals(listOf(home, desktop, documents, downloads), pickerFileSystem.shortcuts().map { it.path })
    }

    @Test fun missingRedirectedFolderFallsBackToHomeFolder() = withTemporaryDirectory { directory ->
        val home = Files.createDirectory(directory.resolve("home"))
        val documents = Files.createDirectory(home.resolve("Documents"))
        val pickerFileSystem = SystemPickerFileSystem(homeDirectory = home, userFolders = {
            PickerUserFolders(documents = directory.resolve("missing"))
        })

        assertEquals(documents, pickerFileSystem.initialDirectory(null))
        assertEquals(documents, pickerFileSystem.shortcuts().first { it.label == "Документы" }.path)
    }

    @Test fun hiddenDirectoryIsNotFilteredOut() = withTemporaryDirectory { directory ->
        val hidden = Files.createDirectory(directory.resolve(".hidden"))
        if (Files.getFileStore(hidden).supportsFileAttributeView("dos")) {
            Files.setAttribute(hidden, "dos:hidden", true)
        }

        assertTrue(SystemPickerFileSystem(homeDirectory = directory).list(directory).any { it.path == hidden && it.isDirectory })
    }

    @Test fun aFailedEntryMetadataLookupDoesNotHideOtherEntries() = withTemporaryDirectory { directory ->
        Files.createDirectory(directory.resolve("available"))
        Files.createFile(directory.resolve("report.DOCX"))
        Files.createFile(directory.resolve("denied"))
        Files.createFile(directory.resolve("unknown"))
        Files.createFile(directory.resolve("unavailable"))

        val entries = listPickerEntries(directory) { path ->
            when (path.fileName.toString()) {
                "denied" -> throw AccessDeniedException(path.toString())
                "unknown" -> throw SecurityException("Metadata unavailable")
                "unavailable" -> throw UncheckedIOException(IOException("Metadata unavailable"))
                "available" -> PickerEntry(path, isDirectory = true)
                "report.DOCX" -> PickerEntry(path, isDirectory = false)
                else -> null
            }
        }

        assertEquals(listOf("available", "report.DOCX"), entries.map { it.name })
    }

    @Test fun linkedDirectoryIsNavigableWhenSymbolicLinksAreAvailable() = withTemporaryDirectory { directory ->
        val target = Files.createDirectory(directory.resolve("target"))
        Files.createFile(target.resolve("inside.DOCX"))
        val link = directory.resolve("linked")
        try {
            Files.createSymbolicLink(link, target)
        } catch (_: FileSystemException) {
            return@withTemporaryDirectory // Windows may require Developer Mode or symlink privileges.
        } catch (_: UnsupportedOperationException) {
            return@withTemporaryDirectory
        } catch (_: SecurityException) {
            return@withTemporaryDirectory
        }

        val pickerFileSystem = SystemPickerFileSystem(homeDirectory = directory)
        assertTrue(pickerFileSystem.isDirectory(link))
        assertTrue(pickerFileSystem.list(directory).any { it.path == link && it.isDirectory })
        assertEquals(listOf("inside.DOCX"), pickerFileSystem.list(link).map { it.name })
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
