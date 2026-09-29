package com.prismdocx.platform.filepicker

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import javax.swing.filechooser.FileSystemView

data class PickerEntry(val path: Path, val isDirectory: Boolean) {
    val name: String get() = path.fileName?.toString() ?: path.toString()
}

data class PickerShortcut(val label: String, val path: Path)

data class PickerUserFolders(
    val desktop: Path? = null,
    val documents: Path? = null,
    val downloads: Path? = null,
)

interface PickerFileSystem {
    fun list(directory: Path): List<PickerEntry>
    fun isDirectory(path: Path): Boolean
    fun isDocxFile(path: Path): Boolean
    fun initialDirectory(lastDirectory: Path?): Path
    fun shortcuts(): List<PickerShortcut>
    fun roots(): List<Path>
    fun saveDestination(directory: Path, name: String, source: Path): SaveDestination
}

class SystemPickerFileSystem(
    private val homeDirectory: Path = Path.of(System.getProperty("user.home", ".")),
    private val rootDirectories: () -> Iterable<Path> = { FileSystems.getDefault().rootDirectories },
    private val userFolders: () -> PickerUserFolders = ::systemUserFolders,
) : PickerFileSystem {
    private val folders by lazy(userFolders)

    override fun saveDestination(directory: Path, name: String, source: Path): SaveDestination =
        resolveSaveDestination(directory, name, source)
    override fun list(directory: Path): List<PickerEntry> {
        if (!isDirectory(directory)) throw NotDirectoryException(directory.toString())

        return listPickerEntries(directory) { path ->
            when {
                isDirectory(path) -> PickerEntry(path, isDirectory = true)
                isDocxFile(path) -> PickerEntry(path, isDirectory = false)
                else -> null
            }
        }
    }

    // Follow directory links; Files.list reports access errors when a folder is opened.
    override fun isDirectory(path: Path): Boolean = Files.isDirectory(path)

    override fun isDocxFile(path: Path): Boolean =
        path.fileName?.toString()?.endsWith(".docx", ignoreCase = true) == true &&
            Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.isReadable(path)

    override fun initialDirectory(lastDirectory: Path?): Path {
        return sequenceOf(lastDirectory, folders.documents, homeDirectory.resolve("Documents"), homeDirectory)
            .filterNotNull()
            .firstOrNull(::isUsableDirectory)
            ?.toAbsolutePath()?.normalize()
            ?: roots().firstOrNull()
            ?: homeDirectory.toAbsolutePath().normalize()
    }

    override fun shortcuts(): List<PickerShortcut> = listOfNotNull(
        shortcut("Домашняя папка", homeDirectory),
        shortcut("Рабочий стол", folders.desktop, homeDirectory.resolve("Desktop")),
        shortcut("Документы", folders.documents, homeDirectory.resolve("Documents")),
        shortcut("Загрузки", folders.downloads, homeDirectory.resolve("Downloads")),
    )

    override fun roots(): List<Path> = try {
        rootDirectories().filter(::isUsableDirectory).sortedWith(PATH_ORDER)
    } catch (_: SecurityException) {
        emptyList()
    }

    private fun isUsableDirectory(path: Path): Boolean = try {
        isDirectory(path) && Files.isReadable(path)
    } catch (_: SecurityException) {
        false
    }

    private fun shortcut(label: String, vararg paths: Path?): PickerShortcut? =
        paths.asSequence().filterNotNull().firstOrNull(::isUsableDirectory)?.let { PickerShortcut(label, it) }
}

/** A failed metadata lookup for one child must not erase the rest of the listing. */
internal fun listPickerEntries(directory: Path, classify: (Path) -> PickerEntry?): List<PickerEntry> = try {
    Files.list(directory).use { stream ->
        stream.iterator().asSequence().mapNotNull { path ->
            try {
                classify(path)
            } catch (_: IOException) {
                null
            } catch (_: UncheckedIOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }.toList().sortedWith(ENTRY_ORDER)
    }
} catch (error: UncheckedIOException) {
    // A failure of the directory stream itself is different from one inaccessible child.
    throw (error.cause ?: error)
}

private fun systemUserFolders(): PickerUserFolders {
    if (!System.getProperty("os.name", "").startsWith("Windows", ignoreCase = true)) return PickerUserFolders()
    val view = try {
        FileSystemView.getFileSystemView()
    } catch (_: SecurityException) {
        return PickerUserFolders()
    } catch (_: UnsupportedOperationException) {
        return PickerUserFolders()
    }
    return PickerUserFolders(
        desktop = shellPath { view.homeDirectory.toPath() },
        documents = shellPath { view.defaultDirectory.toPath() },
        downloads = shellPath {
            view.chooserComboBoxFiles.firstOrNull { file ->
                file.name.equals("Downloads", ignoreCase = true) && view.isFileSystem(file)
            }?.toPath()
        },
    )
}

private fun shellPath(resolve: () -> Path?): Path? = try {
    resolve()
} catch (_: InvalidPathException) {
    null
} catch (_: SecurityException) {
    null
} catch (_: UnsupportedOperationException) {
    null
}

private val PATH_ORDER = Comparator<Path> { first, second ->
    compareNames(first.toString(), second.toString())
}

private val ENTRY_ORDER = Comparator<PickerEntry> { first, second ->
    when {
        first.isDirectory && !second.isDirectory -> -1
        !first.isDirectory && second.isDirectory -> 1
        else -> compareNames(first.name, second.name)
    }
}

private fun compareNames(first: String, second: String): Int {
    val caseInsensitive = first.compareTo(second, ignoreCase = true)
    return if (caseInsensitive != 0) caseInsensitive else first.compareTo(second)
}
