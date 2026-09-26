package com.prismdocx.metadata

import java.io.File

/** Граница файлового ввода-вывода; реализация вызывается контроллером вне UI-потока. */
interface MetadataRepository {
    fun read(file: File): MetadataSnapshot
    fun write(source: File, target: File, snapshot: MetadataSnapshot, overwrite: Boolean = false)
}
