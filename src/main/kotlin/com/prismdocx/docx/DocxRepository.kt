package com.prismdocx.docx

import com.prismdocx.metadata.MetadataPart
import com.prismdocx.metadata.MetadataRepository
import com.prismdocx.metadata.MetadataSnapshot
import com.prismdocx.metadata.MetadataValidation
import com.prismdocx.metadata.XmlSupport.checkRoot
import com.prismdocx.metadata.XmlSupport.children
import com.prismdocx.metadata.XmlSupport.newPart
import com.prismdocx.metadata.XmlSupport.parse
import com.prismdocx.metadata.XmlSupport.serialize
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Читает и сохраняет пакет DOCX; правила редактирования свойств находятся в MetadataEditor. */
class DocxRepository : MetadataRepository {
    override fun read(file: File): MetadataSnapshot = ZipFile(file).use { zip ->
        val manifest = DocxPackageManifest.open(zip)
        MetadataSnapshot(MetadataPart.entries.associateWith { part ->
            zip.getEntry(manifest.pathOf(part))?.let { entry ->
                val document = readXml(zip, entry)
                checkRoot(document, part)
                serialize(document)
            } ?: serialize(newPart(part))
        })
    }

    override fun write(source: File, target: File, snapshot: MetadataSnapshot, overwrite: Boolean) {
        require(source.canonicalFile != target.canonicalFile) { "Выберите другое имя: сохранение создаёт копию." }
        require(!target.exists() || !Files.isSameFile(source.toPath(), target.toPath())) { "Нельзя перезаписать исходный документ." }
        val errors = MetadataValidation.validationErrors(snapshot)
        require(errors.isEmpty()) { errors.joinToString("\n") }
        val destination = target.toPath().toAbsolutePath()
        // Собираем пакет рядом с назначением; исходник и существующая копия не затрагиваются до завершения ZIP.
        val temporary = Files.createTempFile(destination.parent, "prism-docx-", ".tmp")
        try {
            ZipFile(source).use { zip ->
                val manifest = DocxPackageManifest.open(zip)
                val replacements = linkedMapOf<String, ByteArray>()
                MetadataPart.entries.forEach { part ->
                    val path = manifest.pathOf(part)
                    val existing = zip.getEntry(path)
                    val doc = parse(snapshot.xml.getValue(part))
                    if (existing != null || children(doc.documentElement).isNotEmpty()) {
                        replacements[path] = serialize(doc).toByteArray(Charsets.UTF_8)
                        manifest.register(part)
                    }
                }
                replacements.putAll(manifest.updatedParts())
                copyArchive(zip, temporary, replacements)
            }
            if (overwrite) Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            else Files.move(temporary, destination)
        } finally { Files.deleteIfExists(temporary) }
    }

    private fun copyArchive(zip: ZipFile, destination: Path, replacements: MutableMap<String, ByteArray>) {
        ZipOutputStream(Files.newOutputStream(destination)).use { output ->
            zip.entries().asSequence().forEach { entry ->
                output.putNextEntry(ZipEntry(entry.name).apply {
                    time = entry.time
                    comment = entry.comment
                })
                val replacement = replacements.remove(entry.name)
                if (replacement != null) output.write(replacement)
                else if (!entry.isDirectory) zip.getInputStream(entry).use { it.copyTo(output) }
                output.closeEntry()
            }
            replacements.forEach { (path, bytes) ->
                output.putNextEntry(ZipEntry(path))
                output.write(bytes)
                output.closeEntry()
            }
        }
    }
}
