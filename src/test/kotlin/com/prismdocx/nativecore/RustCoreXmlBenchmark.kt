package com.prismdocx.nativecore

import java.util.Locale
import java.nio.file.Path
import kotlin.system.measureNanoTime

@Volatile
private var benchmarkSink = 0

fun main() {
    val debug = PrismNativeCore.load(requiredLibraryPath("prism.docx.core.debug.library.path"))
    val release = PrismNativeCore.load(requiredLibraryPath("prism.docx.core.release.library.path"))
    val validators = listOf(
        BenchmarkValidator("Kotlin", ::kotlinXmlWellFormed),
        BenchmarkValidator("Rust Debug + JNA", debug::isXmlWellFormed),
        BenchmarkValidator("Rust Release + JNA", release::isXmlWellFormed),
    )
    println("XML validation development benchmark (median of $BATCH_COUNT batches, microseconds per call)")
    println("Kotlin: production XmlSupport.parse(String), including builder configuration and DOM creation.")
    println("Rust: String to UTF-8 conversion, JNA/FFI call, parsing and returning the result are included.")
    println("Debug DLL: ${debug.libraryPath}; ABI ${debug.apiVersion}")
    println("Release DLL: ${release.libraryPath}; ABI ${release.apiVersion}")
    println("JVM: ${System.getProperty("java.vm.name")} ${System.getProperty("java.version")}")
    benchmarkXml("Small XML", corePropertiesXml, validators, warmUp = 1_000, iterationsPerBatch = 2_000)
    benchmarkXml("Medium XML", repeatedElementsXml(500), validators, warmUp = 100, iterationsPerBatch = 200)
    benchmarkXml("Large XML", repeatedElementsXml(5_000), validators, warmUp = 30, iterationsPerBatch = 40)
    println("Sink: $benchmarkSink. These timings are a development report, not a performance gate.")
}

private const val BATCH_COUNT = 7

private data class BenchmarkValidator(val name: String, val validate: (String) -> Boolean)

private fun requiredLibraryPath(property: String): Path = Path.of(
    requireNotNull(System.getProperty(property)?.takeIf(String::isNotBlank)) {
        "Missing $property. Run the benchmarkRustCoreXml Gradle task to prepare both Rust DLLs."
    },
)

private fun repeatedElementsXml(count: Int): String = buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>")
    repeat(count) { index ->
        append("<value index=\"$index\">Пример &amp; XML $index</value>")
    }
    append("</root>")
}

private fun benchmarkXml(
    label: String,
    xml: String,
    validators: List<BenchmarkValidator>,
    warmUp: Int,
    iterationsPerBatch: Int,
) {
    validators.forEach { validator ->
        check(validator.validate(xml)) { "$label must be valid for ${validator.name}." }
    }
    repeat(warmUp) {
        validators.forEach { validator ->
            benchmarkSink += if (validator.validate(xml)) 1 else 0
        }
    }
    val times = validators.map { DoubleArray(BATCH_COUNT) }
    repeat(BATCH_COUNT) { batch ->
        // Rotate the first validator and reverse alternate batches to vary measurement order.
        val order = validators.indices.map { (it + batch) % validators.size }
            .let { if (batch % 2 == 0) it else it.reversed() }
        order.forEach { index ->
            times[index][batch] = measureBatch(xml, iterationsPerBatch, validators[index].validate)
        }
    }
    val medians = times.map { it.sorted()[BATCH_COUNT / 2] }
    println("$label: ${xml.toByteArray(Charsets.UTF_8).size} UTF-8 bytes; $warmUp warm-up calls and ${BATCH_COUNT * iterationsPerBatch} measured calls per validator")
    validators.forEachIndexed { index, validator ->
        println("  ${validator.name}: ${formatNumber(medians[index])} us/call")
    }
    println("  Rust Release / Kotlin: ${formatNumber(medians[2] / medians[0])}x elapsed time")
}

private fun measureBatch(xml: String, iterations: Int, validate: (String) -> Boolean): Double {
    var validCount = 0
    val elapsed = measureNanoTime {
        repeat(iterations) { if (validate(xml)) validCount++ }
    }
    benchmarkSink += validCount
    check(validCount == iterations) { "A validator returned an unexpected result during measurement." }
    return elapsed.toDouble() / iterations / 1_000
}

private fun formatNumber(value: Double): String = String.format(Locale.ROOT, "%.2f", value)
