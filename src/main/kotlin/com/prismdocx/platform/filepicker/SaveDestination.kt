package com.prismdocx.platform.filepicker

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path

data class SaveDestination(val path: Path, val overwriteRequired: Boolean)

class InvalidSaveDestinationException(message: String) : Exception(message)

/** Validates a copy target without changing the source document or touching its contents. */
fun resolveSaveDestination(directory: Path, inputName: String, source: Path): SaveDestination {
    val name = inputName
    if (name.isBlank()) throw InvalidSaveDestinationException("Укажите имя файла.")
    if (name == "." || name == ".." || name.any { it.code < 32 || it in "<>:\"/\\|?*" } ||
        name.endsWith('.') || name.endsWith(' ') ||
        name.substringBefore('.').endsWith(' ') ||
        name.substringBefore('.').uppercase() in RESERVED_WINDOWS_NAMES
    ) throw InvalidSaveDestinationException("Недопустимое имя файла. Укажите другое имя.")

    val fileName = if (name.endsWith(".docx", ignoreCase = true)) name else "$name.docx"
    val target = try {
        directory.resolve(fileName).toAbsolutePath().normalize()
    } catch (_: InvalidPathException) {
        throw InvalidSaveDestinationException("Недопустимое имя файла. Укажите другое имя.")
    }
    if (!Files.isDirectory(directory)) {
        throw InvalidSaveDestinationException("Папка больше не существует. Выберите другую папку.")
    }
    if (!Files.isWritable(directory)) {
        throw InvalidSaveDestinationException("Нет доступа к записи в эту папку.")
    }
    val exists = Files.exists(target, NOFOLLOW_LINKS)
    if (exists && !Files.isRegularFile(target, NOFOLLOW_LINKS)) {
        throw InvalidSaveDestinationException("По этому пути находится не файл. Укажите другое имя.")
    }
    if (target == source.toAbsolutePath().normalize() ||
        exists && Files.isSameFile(target, source)
    ) throw InvalidSaveDestinationException("Укажите другое имя для копии.")
    if (exists && !Files.isWritable(target)) {
        throw InvalidSaveDestinationException("Нет доступа к записи в этот файл.")
    }
    return SaveDestination(target, overwriteRequired = exists)
}

private val RESERVED_WINDOWS_NAMES = buildSet {
    addAll(listOf("CON", "PRN", "AUX", "NUL"))
    (1..9).forEach { number -> add("COM$number"); add("LPT$number") }
}
