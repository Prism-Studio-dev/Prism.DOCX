package com.prismdocx.platform.filepicker

import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NotDirectoryException
import java.nio.file.Path

data class PickerEntry(val path: Path, val isDirectory: Boolean) {
    val name: String get() = path.fileName?.toString() ?: path.toString()
}

data class PickerShortcut(val label: String, val path: Path)

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
) : PickerFileSystem {
    override fun saveDestination(directory: Path, name: String, source: Path): SaveDestination =
        resolveSaveDestination(directory, name, source)
    override fun list(directory: Path): List<PickerEntry> {
        if (!isDirectory(directory)) throw NotDirectoryException(directory.toString())

        try {
            return Files.list(directory).use { stream ->
                stream.iterator().asSequence().mapNotNull { path ->
                    when {
                        isDirectory(path) -> PickerEntry(path, isDirectory = true)
                        isDocxFile(path) -> PickerEntry(path, isDirectory = false)
                        else -> null
                    }
                }.toList().sortedWith(ENTRY_ORDER)
            }
        } catch (error: UncheckedIOException) {
            // Directory streams can report late I/O failures while iterating.
            throw (error.cause ?: error)
        }
    }

    override fun isDirectory(path: Path): Boolean =
        Files.isDirectory(path, NOFOLLOW_LINKS) && Files.isReadable(path)

    override fun isDocxFile(path: Path): Boolean =
        path.fileName?.toString()?.endsWith(".docx", ignoreCase = true) == true &&
            Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.isReadable(path)

    override fun initialDirectory(lastDirectory: Path?): Path {
        val documents = homeDirectory.resolve("Documents")
        return sequenceOf(lastDirectory, documents, homeDirectory)
            .filterNotNull()
            .firstOrNull(::isUsableDirectory)
            ?.toAbsolutePath()?.normalize()
            ?: roots().firstOrNull()
            ?: homeDirectory.toAbsolutePath().normalize()
    }

    override fun shortcuts(): List<PickerShortcut> = listOf(
        PickerShortcut("Домашняя папка", homeDirectory),
        PickerShortcut("Рабочий стол", homeDirectory.resolve("Desktop")),
        PickerShortcut("Документы", homeDirectory.resolve("Documents")),
        PickerShortcut("Загрузки", homeDirectory.resolve("Downloads")),
    ).filter { isUsableDirectory(it.path) }

    override fun roots(): List<Path> = try {
        rootDirectories().filter(::isUsableDirectory).sortedWith(PATH_ORDER)
    } catch (_: SecurityException) {
        emptyList()
    }

    private fun isUsableDirectory(path: Path): Boolean = try {
        isDirectory(path)
    } catch (_: SecurityException) {
        false
    }
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
