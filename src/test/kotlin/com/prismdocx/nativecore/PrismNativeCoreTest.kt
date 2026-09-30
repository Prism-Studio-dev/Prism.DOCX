package com.prismdocx.nativecore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrismNativeCoreTest {
    @Test
    fun onlyXmlValidityStatusesBecomeBoolean() {
        assertTrue(xmlWellFormedFromStatus(RustXmlStatus.VALID.code))
        assertFalse(xmlWellFormedFromStatus(RustXmlStatus.INVALID_XML.code))
        for (status in listOf(RustXmlStatus.INVALID_ARGUMENT, RustXmlStatus.INVALID_UTF8, RustXmlStatus.INTERNAL_ERROR)) {
            val error = assertFailsWith<IllegalStateException> { xmlWellFormedFromStatus(status.code) }
            assertTrue(error.message.orEmpty().contains(status.name))
        }
        val error = assertFailsWith<IllegalStateException> { xmlWellFormedFromStatus(99) }
        assertTrue(error.message.orEmpty().contains("unknown"))
    }

    @Test
    fun xmlSamplesCaptureExistingProductionParsingPolicyWithoutNativeCode() {
        for (sample in xmlValidationSamples) {
            assertEquals(sample.expected, kotlinXmlWellFormed(sample.xml), sample.name)
        }
    }
}
