package com.prismdocx.nativecore

import com.prismdocx.metadata.XmlBackend
import com.prismdocx.metadata.XmlValidation
import com.prismdocx.metadata.selectXmlValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrismNativeCoreIntegrationTest {
    @Test
    fun requiresRustBackendAndCompatibleAbi() {
        assertEquals(1, PrismNativeCore.apiVersion)
        assertEquals(XmlBackend.RUST, XmlValidation.backend)
        println("Rust integration: ABI=${PrismNativeCore.apiVersion}; DLL=${PrismNativeCore.libraryPath}")
    }
    @Test
    fun addsThroughRustDll() {
        assertEquals(5, PrismNativeCore.add(2, 3))
    }

    @Test
    fun wrapsThroughRustDllLikeKotlinInt() {
        assertEquals(Int.MIN_VALUE, PrismNativeCore.add(Int.MAX_VALUE, 1))
        assertEquals(Int.MAX_VALUE, PrismNativeCore.add(Int.MIN_VALUE, -1))
    }

    @Test
    fun validatesXmlThroughRustDll() {
        assertTrue(PrismNativeCore.isXmlWellFormed("<root><value>42</value></root>"))
        assertFalse(PrismNativeCore.isXmlWellFormed("<root><value></root>"))
        assertFalse(PrismNativeCore.isXmlWellFormed(""))
    }

    @Test
    fun reportsCompatibilityAndGuardsAutomaticProductionBackend() {
        val differences = buildMap {
            for (sample in xmlValidationSamples) {
                val kotlin = kotlinXmlWellFormed(sample.xml)
                val rust = PrismNativeCore.isXmlWellFormed(sample.xml)
                if (kotlin != rust) put(sample.name, "Kotlin=$kotlin, Rust=$rust")
            }
        }
        println("Kotlin vs Rust: ${xmlValidationSamples.size} XML samples, ${differences.size} differences.")
        differences.forEach { (name, result) -> println("$name: $result") }
        val unexpected = differences.keys - documentedExperimentalGaps
        assertTrue(unexpected.isEmpty(), "New XML compatibility regressions: $unexpected")
        // Comparison is an honest compatibility report, not a claim of equivalence.
        // Until the full corpus agrees, automatic production selection must stay Kotlin.
        if (differences.isNotEmpty()) {
            assertEquals(XmlBackend.KOTLIN, selectXmlValidator("auto", diagnostic = {}).backend)
        }
    }
}

// These are reported limitations, not accepted production XML semantics.
// New mismatches fail the test; fixed gaps may disappear without weakening the oracle.
private val documentedExperimentalGaps = setOf(
    "predefined xml namespace element",
    "duplicate namespace declaration",
    "declaration version 1.2",
    "namespace duplicate same URI",
    "duplicate default namespace",
    "empty prefix binding XML 1.1",
    "xml element nested",
    "xml element explicit binding",
    "namespace empty prefix name",
    "XML 1.1 next line outside root",
    "Unicode supplementary name",
)
