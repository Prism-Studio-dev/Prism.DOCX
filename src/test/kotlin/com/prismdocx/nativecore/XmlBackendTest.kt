package com.prismdocx.nativecore

import com.prismdocx.metadata.XmlBackend
import com.prismdocx.metadata.XmlValidator
import com.prismdocx.metadata.selectXmlValidator
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmlBackendTest {
    private fun native(validate: (String) -> Boolean) = object : XmlValidator {
        override val backend = XmlBackend.RUST
        override fun isXmlWellFormed(xml: String) = validate(xml)
    }

    @Test fun absentOrIncompatibleNativeLayerFallsBackButStrictModeFails() {
        for (failure in listOf(NativeCoreException("missing DLL"), NativeCoreException("incompatible ABI"))) {
            val logs = mutableListOf<String>()
            val backend = selectXmlValidator("auto", { throw failure }, logs::add)
            assertEquals(XmlBackend.KOTLIN, backend.backend)
            assertTrue(backend.isXmlWellFormed("<root/>"))
            assertFalse(backend.isXmlWellFormed("<root>"))
            assertEquals(1, logs.size)
            assertTrue(logs.single().contains(failure.message!!))
            assertFailsWith<NativeCoreException> { selectXmlValidator("rust", { throw failure }) }
        }
    }

    @Test fun semanticInvalidXmlNeverTriggersFallback() {
        val logs = mutableListOf<String>()
        val backend = selectXmlValidator("auto", { native { false } }, logs::add, rustCompatible = true)
        assertFalse(backend.isXmlWellFormed("<root>"))
        assertEquals(XmlBackend.RUST, backend.backend)
        assertTrue(logs.isEmpty())
    }

    @Test fun runtimeNativeFailureSwitchesOnceAndPreservesXmlValidation() {
        val logs = mutableListOf<String>()
        val backend = selectXmlValidator("auto", { native { throw NativeCoreException("INTERNAL_ERROR") } }, logs::add, rustCompatible = true)
        assertTrue(backend.isXmlWellFormed("<root/>"))
        assertFalse(backend.isXmlWellFormed("<root>"))
        assertEquals(XmlBackend.KOTLIN, backend.backend)
        assertEquals(1, logs.size)
    }

    @Test fun incompatibleApiVersionIsExplicit() {
        requireCompatibleApiVersion(1)
        val error = assertFailsWith<NativeCoreException> { requireCompatibleApiVersion(2) }
        assertTrue(error.message!!.contains("expected 1, found 2"))
    }

    @Test fun packagedResolutionNeverFallsThroughToSourceTree() {
        val resources = Path.of("installation", "app", "resources").toAbsolutePath()
        assertEquals(resources.resolve("rust-core/prism_docx_core.dll"), resolveRustCorePath(null, null, resources.toString()))
        assertEquals(Path.of("override.dll").toAbsolutePath(), resolveRustCorePath("override.dll", "env.dll", resources.toString()))
        assertEquals(Path.of("env.dll").toAbsolutePath(), resolveRustCorePath(" ", "env.dll", resources.toString()))
        assertFailsWith<NativeCoreException> { PrismNativeCore.load(resources.resolve("missing.dll")) }
    }
}
