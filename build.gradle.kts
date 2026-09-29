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
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.prismdocx.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "PrismDOCX"
            packageVersion = project.version.toString()
            description = "DOCX metadata editor"
            vendor = "Prism.DOCX"
            windows {
                iconFile.set(project.file("packaging/windows/prism-docx.ico"))
                menu = true
                upgradeUuid = "D7FBFFD8-2405-33A0-95BE-558DA26580AC"
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.prismdocx.resources"
}
