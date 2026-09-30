package com.prismdocx.nativecore

import com.sun.jna.Library
import com.sun.jna.Native
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.InvalidPathException

/** Strict native bridge: failures are reported, never replaced by Kotlin arithmetic/parsing. */
object PrismNativeCore {
    internal const val EXPECTED_API_VERSION = 1
    private val connection by lazy {
        try {
            load(resolveRustCorePath())
        } catch (error: InvalidPathException) {
            throw NativeCoreException("Invalid Rust Core library path: ${error.message}", error)
        }
    }

    val libraryPath: Path get() = connection.libraryPath
    val apiVersion: Int get() = connection.apiVersion

    fun add(a: Int, b: Int): Int = connection.add(a, b)
    fun isXmlWellFormed(xml: String): Boolean = connection.isXmlWellFormed(xml)

    internal fun load(path: Path): RustCoreConnection = RustCoreConnection.open(path)
}

internal const val LIBRARY_PATH_PROPERTY = "prism.docx.core.library.path"
internal const val LIBRARY_PATH_ENV = "PRISM_DOCX_RUST_CORE_PATH"

internal fun resolveRustCorePath(
    property: String? = System.getProperty(LIBRARY_PATH_PROPERTY),
    environment: String? = System.getenv(LIBRARY_PATH_ENV),
    applicationResources: String? = System.getProperty("compose.application.resources.dir"),
): Path {
    val override = property?.takeIf { it.isNotBlank() } ?: environment?.takeIf { it.isNotBlank() }
    return when {
        override != null -> Path.of(override)
        !applicationResources.isNullOrBlank() -> Path.of(applicationResources, "rust-core", "prism_docx_core.dll")
        else -> Path.of("build", "native", "rust-core", "debug", "prism_docx_core.dll")
    }.toAbsolutePath().normalize()
}

internal class RustCoreConnection private constructor(
    val libraryPath: Path,
    private val library: RustLibrary,
    val apiVersion: Int,
) {
    // No JNA types leave this file. JNA borrows/copies ByteArray for a synchronous call.
    private interface RustLibrary : Library {
        fun prism_core_api_version(): Int
        fun prism_core_add(a: Int, b: Int): Int
        fun prism_core_validate_xml_utf8(data: ByteArray, len: Int): Int
    }

    fun add(a: Int, b: Int): Int = library.prism_core_add(a, b)

    fun isXmlWellFormed(xml: String): Boolean {
        // REPORT rejects lone UTF-16 surrogates instead of silently replacing them.
        val bytes = try {
            val encoded = Charsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(xml))
            ByteArray(encoded.remaining()).also { encoded.get(it) }
        } catch (_: CharacterCodingException) {
            return false
        }
        return xmlWellFormedFromStatus(library.prism_core_validate_xml_utf8(bytes, bytes.size))
    }

    companion object {
        fun open(requestedPath: Path): RustCoreConnection {
            val path = requestedPath.toAbsolutePath().normalize()
            if (!Files.isRegularFile(path)) {
                throw NativeCoreException("Rust Core DLL not found at $path. Run prepareRustCore or set $LIBRARY_PATH_PROPERTY / $LIBRARY_PATH_ENV.")
            }
            try {
                val library = Native.load(path.toString(), RustLibrary::class.java)
                val version = library.prism_core_api_version()
                requireCompatibleApiVersion(version)
                // Resolve all ABI symbols during initialization, before selecting the backend.
                if (library.prism_core_add(Int.MAX_VALUE, 1) != Int.MIN_VALUE ||
                    !xmlWellFormedFromStatus(library.prism_core_validate_xml_utf8(byteArrayOf(60, 114, 47, 62), 4))) {
                    throw NativeCoreException("Rust Core ABI self-check failed at $path.")
                }
                return RustCoreConnection(path, library, version)
            } catch (error: LinkageError) {
                throw NativeCoreException("Unable to load compatible Rust Core at $path: ${error.message}", error)
            }
        }
    }
}

internal fun requireCompatibleApiVersion(version: Int) {
    if (version != PrismNativeCore.EXPECTED_API_VERSION) {
        throw NativeCoreException("Incompatible Rust Core ABI: expected ${PrismNativeCore.EXPECTED_API_VERSION}, found ${Integer.toUnsignedString(version)}.")
    }
}

internal class NativeCoreException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

internal enum class RustXmlStatus(val code: Int) {
    VALID(1), INVALID_XML(0), INVALID_ARGUMENT(-1), INVALID_UTF8(-2), INTERNAL_ERROR(-3),
}

internal fun xmlWellFormedFromStatus(code: Int): Boolean {
    return when (val status = RustXmlStatus.entries.firstOrNull { it.code == code }
        ?: throw NativeCoreException("Rust Core returned an unknown XML validation status: $code.")) {
        RustXmlStatus.VALID -> true
        RustXmlStatus.INVALID_XML -> false
        else -> throw NativeCoreException("Rust Core XML validation failed: ${status.name} ($code).")
    }
}
