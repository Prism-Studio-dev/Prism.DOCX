package com.prismdocx.platform.filepicker

import java.nio.file.Files
import java.nio.file.FileSystemException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveDestinationTest {
    @Test fun appendsDocxOnlyWhenMissing() = withDirectory { directory, source ->
        assertEquals("report.docx", resolveSaveDestination(directory, "report", source).path.fileName.toString())
        assertEquals("report.docx", resolveSaveDestination(directory, "report.docx", source).path.fileName.toString())
        assertEquals("REPORT.DOCX", resolveSaveDestination(directory, "REPORT.DOCX", source).path.fileName.toString())
    }

    @Test fun rejectsEmptyAndInvalidWindowsNames() = withDirectory { directory, source ->
        for (name in listOf("", "  ", "bad/name", "bad:name", "file?", "CON", "NUL.docx", "report.", "report ")) {
            assertFailsWith<InvalidSaveDestinationException>(name) {
                resolveSaveDestination(directory, name, source)
            }
        }
    }

    @Test fun existingFileRequiresExplicitOverwriteDecision() = withDirectory { directory, source ->
        assertFalse(resolveSaveDestination(directory, "new", source).overwriteRequired)
        Files.createFile(directory.resolve("existing.docx"))
        assertTrue(resolveSaveDestination(directory, "existing", source).overwriteRequired)
    }

    @Test fun sourceCannotBeSelectedAsCopyDestination() = withDirectory { directory, source ->
        assertFailsWith<InvalidSaveDestinationException> {
            resolveSaveDestination(directory, source.fileName.toString(), source)
        }
    }

    @Test fun missingDirectoryIsRejected() = withDirectory { directory, source ->
        assertFailsWith<InvalidSaveDestinationException> {
            resolveSaveDestination(directory.resolve("missing"), "copy", source)
        }
    }

    @Test fun linkedDirectoryCanBeUsedForSaveCopyWhenAvailable() = withDirectory { directory, source ->
        val target = Files.createDirectory(directory.resolve("target"))
        val link = directory.resolve("linked")
        try {
            Files.createSymbolicLink(link, target)
        } catch (_: FileSystemException) {
            return@withDirectory // Windows may require Developer Mode or symlink privileges.
        } catch (_: UnsupportedOperationException) {
            return@withDirectory
        } catch (_: SecurityException) {
            return@withDirectory
        }

        assertEquals(link.resolve("copy.docx"), resolveSaveDestination(link, "copy", source).path)
    }

    private fun withDirectory(block: (Path, Path) -> Unit) {
        val directory = Files.createTempDirectory("prism-save-picker-")
        try {
            block(directory, Files.createFile(directory.resolve("source.docx")))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
