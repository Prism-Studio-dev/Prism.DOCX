import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

tasks.register<JavaExec>("renderPreview") {
    group = "verification"
    description = "Render the desktop UI to build/previews without opening a window."
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.prismdocx.preview.RenderPreviewKt")
    systemProperty("java.awt.headless", "true")
}

group = "com.prismdocx"
version = "1.1.1"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation("net.java.dev.jna:jna:5.19.1")
    testImplementation(kotlin("test"))
}

val rustCoreDirectory = layout.projectDirectory.dir("native/prism-docx-core")
val preparedRustDebugDll = layout.buildDirectory.file("native/rust-core/debug/prism_docx_core.dll")
val preparedRustReleaseDll = layout.buildDirectory.file("native/rust-core/release/prism_docx_core.dll")
val packagedRustResources = layout.buildDirectory.dir("native/rust-core/app-resources")

fun registerRustBuild(name: String, profile: String) = tasks.register<Exec>(name) {
    group = "build"
    description = "Build the Windows Rust Core DLL ($profile)."
    workingDir = rustCoreDirectory.asFile
    commandLine(listOf("cargo", "build", "--locked", "--target-dir", "target") +
        if (profile == "release") listOf("--release") else emptyList())
    doFirst {
        check(System.getProperty("os.name").startsWith("Windows")) { "Rust Core packaging currently supports Windows only." }
    }
    doLast {
        check(rustCoreDirectory.file("target/$profile/prism_docx_core.dll").asFile.isFile) {
            "Cargo did not produce the $profile Rust Core DLL."
        }
    }
}

val buildRustCore = registerRustBuild("buildRustCore", "debug")
val buildRustCoreRelease = registerRustBuild("buildRustCoreRelease", "release")

val prepareRustCore by tasks.registering(Sync::class) {
    group = "build"
    description = "Prepare the Debug Rust Core DLL for native tests."
    dependsOn(buildRustCore)
    from(rustCoreDirectory.file("target/debug/prism_docx_core.dll"))
    into(preparedRustDebugDll.map { it.asFile.parentFile })
}
val prepareRustCoreRelease by tasks.registering(Sync::class) {
    group = "build"
    description = "Prepare the Release Rust Core DLL for distribution."
    dependsOn(buildRustCoreRelease)
    from(rustCoreDirectory.file("target/release/prism_docx_core.dll"))
    into(preparedRustReleaseDll.map { it.asFile.parentFile })
}
val preparePackagedRustCore by tasks.registering(Sync::class) {
    dependsOn(prepareRustCoreRelease)
    from(preparedRustReleaseDll)
    into(packagedRustResources.map { it.dir("windows/rust-core") })
}

tasks.named<Test>("test") {
    systemProperty("prism.docx.xml.backend", "kotlin")
    filter {
        excludeTestsMatching("com.prismdocx.nativecore.PrismNativeCoreIntegrationTest")
    }
}

fun registerRustIntegrationTest(name: String, release: Boolean) = tasks.register<Test>(name) {
    group = "verification"
    description = "Require actual ${if (release) "Release" else "Debug"} Rust calls through JNA."
    val dll = if (release) preparedRustReleaseDll else preparedRustDebugDll
    dependsOn(if (release) prepareRustCoreRelease else prepareRustCore, tasks.named("testClasses"))
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    systemProperty("prism.docx.core.library.path", dll.get().asFile.absolutePath)
    systemProperty("prism.docx.xml.backend", "rust")
    inputs.file(dll).withPropertyName("rustCoreLibrary")
    filter {
        includeTestsMatching("com.prismdocx.nativecore.PrismNativeCoreIntegrationTest")
        includeTestsMatching("com.prismdocx.nativecore.PrismDocumentSmokeTest")
    }
}
registerRustIntegrationTest("rustCoreIntegrationTest", false)
registerRustIntegrationTest("rustCoreReleaseIntegrationTest", true)

tasks.register<JavaExec>("benchmarkRustCoreXml") {
    group = "verification"
    description = "Report XML validation timings for Kotlin and Rust through JNA."
    dependsOn(prepareRustCore, prepareRustCoreRelease, tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.prismdocx.nativecore.RustCoreXmlBenchmarkKt")
    systemProperty("prism.docx.core.debug.library.path", preparedRustDebugDll.get().asFile.absolutePath)
    systemProperty("prism.docx.core.release.library.path", preparedRustReleaseDll.get().asFile.absolutePath)
    systemProperty("prism.docx.xml.backend", "kotlin")
}

tasks.named("build") { dependsOn(prepareRustCoreRelease) }
tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(preparePackagedRustCore) }

compose.desktop {
    application {
        mainClass = "com.prismdocx.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "PrismDOCXUnstable"
            appResourcesRootDir.set(packagedRustResources)
            packageVersion = project.version.toString()
            description = "DOCX metadata editor"
            vendor = "Prism.DOCX"
            windows {
                iconFile.set(project.file("packaging/windows/prism-docx.ico"))
                menu = true
                menuGroup = "Prism.DOCX Unstable"
                upgradeUuid = "CC7C20C8-281A-4687-BD10-A2F455734925"
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.prismdocx.resources"
}
