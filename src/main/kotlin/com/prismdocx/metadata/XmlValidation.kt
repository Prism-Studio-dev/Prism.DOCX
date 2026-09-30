package com.prismdocx.metadata

import com.prismdocx.nativecore.NativeCoreException
import com.prismdocx.nativecore.PrismNativeCore
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.w3c.dom.Document

internal enum class XmlBackend { KOTLIN, RUST }

internal interface XmlValidator {
    val backend: XmlBackend
    fun isXmlWellFormed(xml: String): Boolean
}

internal object KotlinXmlValidator : XmlValidator {
    override val backend = XmlBackend.KOTLIN
    override fun isXmlWellFormed(xml: String): Boolean = try {
        XmlSupport.parseWithKotlin(xml)
        true
    } catch (_: SAXException) {
        false
    }
}

internal class RustXmlValidator : XmlValidator {
    init {
        // Eager ABI/symbol check: no native availability failure is mistaken for bad XML.
        PrismNativeCore.apiVersion
    }
    override val backend = XmlBackend.RUST
    override fun isXmlWellFormed(xml: String) = PrismNativeCore.isXmlWellFormed(xml)
}

/** Switch only on a native-layer failure, never on INVALID_XML. */
internal class FallbackXmlValidator(
    primary: XmlValidator,
    private val diagnostic: (String) -> Unit,
) : XmlValidator {
    @Volatile private var active: XmlValidator = primary
    override val backend get() = active.backend

    override fun isXmlWellFormed(xml: String): Boolean {
        val selected = active
        try {
            return selected.isXmlWellFormed(xml)
        } catch (error: NativeCoreException) {
            switchToKotlin(error)
        } catch (error: LinkageError) {
            switchToKotlin(error)
        }
        return active.isXmlWellFormed(xml)
    }

    @Synchronized private fun switchToKotlin(error: Throwable) {
        if (active.backend != XmlBackend.KOTLIN) {
            active = KotlinXmlValidator
            diagnostic("XML backend=Kotlin; Rust Core fallback: ${error.message}")
        }
    }
}

internal fun selectXmlValidator(
    mode: String,
    rust: () -> XmlValidator = { RustXmlValidator() },
    diagnostic: (String) -> Unit = { System.err.println("[Prism.DOCX Unstable] $it") },
    rustCompatible: Boolean = false,
): XmlValidator = when (mode.lowercase()) {
    "kotlin" -> KotlinXmlValidator.also { diagnostic("XML backend=Kotlin (explicit selection).") }
    "rust" -> rust() // Strict mode for integration tests: no fallback.
    "auto" -> try {
        val native = rust()
        if (rustCompatible) FallbackXmlValidator(native, diagnostic) else {
            diagnostic("XML backend=Kotlin; Rust Core loaded, XML validator experimental (compatibility gaps).")
            KotlinXmlValidator
        }
    } catch (error: NativeCoreException) {
        diagnostic("XML backend=Kotlin; Rust Core fallback: ${error.message}")
        KotlinXmlValidator
    } catch (error: LinkageError) {
        diagnostic("XML backend=Kotlin; Rust Core fallback: ${error.message}")
        KotlinXmlValidator
    }
    else -> throw IllegalArgumentException("Unknown prism.docx.xml.backend '$mode'; use auto, kotlin, or rust.")
}

internal object XmlValidation {
    private val validator by lazy {
        selectXmlValidator(System.getProperty("prism.docx.xml.backend", "auto"))
    }
    val backend: XmlBackend get() = validator.backend

    fun initialize() {
        if (backend == XmlBackend.RUST) {
            System.err.println("[Prism.DOCX Unstable] XML backend=Rust; ABI=${PrismNativeCore.apiVersion}; DLL=${PrismNativeCore.libraryPath}")
        } else if (System.getProperty("prism.docx.xml.backend", "auto") == "auto") {
            // Report the origin only if probing succeeded; a missing DLL was already logged.
            try {
                System.err.println("[Prism.DOCX Unstable] Rust Core available (experimental); ABI=${PrismNativeCore.apiVersion}; DLL=${PrismNativeCore.libraryPath}")
            } catch (_: NativeCoreException) {
                // selectXmlValidator already emitted the fallback diagnostic.
            }
        }
    }

    fun parse(xml: String): Document {
        if (backend == XmlBackend.RUST && !validator.isXmlWellFormed(xml)) {
            throw SAXParseException("XML is not well-formed.", null)
        }
        // DOM creation, metadata editing and serialization remain in Kotlin/JAXP.
        return XmlSupport.parseWithKotlin(xml)
    }
}
