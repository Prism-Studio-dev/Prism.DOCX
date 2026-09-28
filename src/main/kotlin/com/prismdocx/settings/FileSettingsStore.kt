package com.prismdocx.settings

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.Properties

class FileSettingsStore(private val path: Path = defaultSettingsPath()) : SettingsStore {
    override fun load(): AppSettings {
        return try {
            if (!Files.isRegularFile(path)) return AppSettings()
            val properties = Properties()
            Files.newInputStream(path).use { properties.load(it) }
            val storedTheme = properties.getProperty(THEME_KEY)
            val theme = AppThemeId.entries.firstOrNull { it.name.equals(storedTheme, ignoreCase = true) }
            val lastFileDirectory = properties.getProperty(LAST_OPEN_DIRECTORY_KEY)
                ?.takeIf(String::isNotBlank)
                ?.let { storedPath ->
                    try {
                        Path.of(storedPath)
                    } catch (_: InvalidPathException) {
                        null
                    }
                }
            AppSettings(theme ?: AppThemeId.PRISM, lastFileDirectory)
        } catch (_: IOException) {
            AppSettings()
        } catch (_: IllegalArgumentException) {
            AppSettings()
        } catch (_: SecurityException) {
            AppSettings()
        }
    }

    override fun save(settings: AppSettings) {
        val directory = requireNotNull(path.parent) { "Settings path must have a parent directory" }
        Files.createDirectories(directory)
        val properties = Properties()
        if (Files.isRegularFile(path)) {
            try {
                Files.newInputStream(path).use { properties.load(it) }
            } catch (_: IllegalArgumentException) {
                // Replace a malformed properties file with valid settings on the next save.
                properties.clear()
            }
        }
        properties.setProperty(THEME_KEY, settings.theme.name)
        if (settings.lastFileDirectory == null) properties.remove(LAST_OPEN_DIRECTORY_KEY)
        else properties.setProperty(LAST_OPEN_DIRECTORY_KEY, settings.lastFileDirectory.toString())
        val temporary = Files.createTempFile(directory, ".settings-", ".tmp")
        try {
            Files.newOutputStream(temporary).use { properties.store(it, null) }
            try {
                Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

private const val THEME_KEY = "theme"
// Keep the stored key so settings.properties created by earlier v1.1.0 builds still loads.
private const val LAST_OPEN_DIRECTORY_KEY = "lastOpenDirectory"

internal fun defaultSettingsPath(
    appData: String? = System.getenv("APPDATA"),
    userHome: String = System.getProperty("user.home", "."),
): Path {
    val home = try { Path.of(userHome) } catch (_: InvalidPathException) { Path.of(".") }
    val directory = appData?.takeIf(String::isNotBlank)?.let {
        try { Path.of(it) } catch (_: InvalidPathException) { null }
    } ?: home.resolve("AppData").resolve("Roaming")
    return directory.resolve("Prism.DOCX").resolve("settings.properties")
}
