package com.prismdocx.settings

import com.prismdocx.platform.filepicker.SystemPickerFileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileSettingsStoreTest {
    @Test fun missingFileUsesPrismThemeWithoutCreatingSettings() = withSettingsFile { path ->
        assertEquals(AppSettings(), FileSettingsStore(path).load())
        assertFalse(Files.exists(path))
    }

    @Test fun selectedThemeSurvivesReloadAndCanBeChanged() = withSettingsFile { path ->
        val store = FileSettingsStore(path)
        store.save(AppSettings(AppThemeId.DARK))
        assertEquals(AppSettings(AppThemeId.DARK), FileSettingsStore(path).load())

        store.save(AppSettings(AppThemeId.LIGHT))
        assertEquals(AppSettings(AppThemeId.LIGHT), FileSettingsStore(path).load())
    }

    @Test fun lastOpenDirectorySurvivesReloadAndCanBeCleared() = withSettingsFile { path ->
        val directory = Files.createDirectory(path.parent.resolve("Documents"))
        val store = FileSettingsStore(path)
        store.save(AppSettings(AppThemeId.VIOLET, directory))
        assertEquals(AppSettings(AppThemeId.VIOLET, directory), FileSettingsStore(path).load())

        store.save(AppSettings(AppThemeId.VIOLET))
        assertEquals(AppSettings(AppThemeId.VIOLET), FileSettingsStore(path).load())
    }

    @Test fun oldThemeOnlySettingsLoadWithoutLastOpenDirectory() = withSettingsFile { path ->
        Files.writeString(path, "theme=DARK\n")
        assertEquals(AppSettings(AppThemeId.DARK), FileSettingsStore(path).load())
    }

    @Test fun legacyLastOpenDirectoryRemainsAvailableForOpenAndSave() = withSettingsFile { path ->
        val directory = Files.createDirectory(path.parent.resolve("previous"))
        Files.writeString(path, "theme=Light\nlastOpenDirectory=${directory.toString().replace("\\", "\\\\")}\n")
        val settings = FileSettingsStore(path).load()
        assertEquals(AppThemeId.LIGHT, settings.theme)
        assertEquals(directory, settings.lastFileDirectory)
        assertEquals(directory, SystemPickerFileSystem(homeDirectory = path.parent).initialDirectory(settings.lastFileDirectory))
    }

    @Test fun invalidLastOpenDirectoryDoesNotDiscardValidTheme() = withSettingsFile { path ->
        Files.writeString(path, "theme=EMERALD\nlastOpenDirectory=\\u0000\n")
        assertEquals(AppSettings(AppThemeId.EMERALD), FileSettingsStore(path).load())
    }

    @Test fun newThemesAreSavedWithStableIdsAndSurviveReload() = withSettingsFile { path ->
        val store = FileSettingsStore(path)
        for (theme in newThemes) {
            store.save(AppSettings(theme))
            assertTrue(Files.readString(path).contains("theme=${theme.name}"), "Saved ID for $theme")
            assertEquals(AppSettings(theme), FileSettingsStore(path).load(), "Reloaded $theme")
        }
    }

    @Test fun newThemesLoadFromExistingSettingsFiles() = withSettingsFile { path ->
        for (theme in newThemes) {
            Files.writeString(path, "theme=${theme.name}\n")
            assertEquals(AppSettings(theme), FileSettingsStore(path).load(), "Loaded $theme")
        }
    }

    @Test fun legacyTitleCaseThemeIdsStillLoad() = withSettingsFile { path ->
        val legacyThemes = mapOf(
            "Prism" to AppThemeId.PRISM,
            "Light" to AppThemeId.LIGHT,
            "Dark" to AppThemeId.DARK,
        )
        for ((storedId, theme) in legacyThemes) {
            Files.writeString(path, "theme=$storedId\n")
            assertEquals(AppSettings(theme), FileSettingsStore(path).load(), "Loaded $storedId")
        }
    }

    @Test fun unknownOrMalformedThemeFallsBackToPrism() = withSettingsFile { path ->
        Files.writeString(path, "theme=UNKNOWN")
        assertEquals(AppSettings(), FileSettingsStore(path).load())

        Files.writeString(path, "theme=" + '\\' + "uZZZZ")
        assertEquals(AppSettings(), FileSettingsStore(path).load())
    }

    @Test fun savingKnownSettingsPreservesUnknownKeys() = withSettingsFile { path ->
        Files.writeString(path, "theme=PRISM\nfutureSetting=keep-me\n")
        FileSettingsStore(path).save(AppSettings(AppThemeId.NORD))
        val saved = Files.readString(path)
        assertTrue(saved.contains("futureSetting=keep-me"))
        assertEquals(AppSettings(AppThemeId.NORD), FileSettingsStore(path).load())
    }

    @Test fun malformedSettingsCanBeReplacedOnNextSave() = withSettingsFile { path ->
        Files.writeString(path, "theme=" + '\\' + "uZZZZ")
        FileSettingsStore(path).save(AppSettings(AppThemeId.LIGHT))
        assertEquals(AppSettings(AppThemeId.LIGHT), FileSettingsStore(path).load())
    }

    @Test fun invalidAppDataPathFallsBackToUserHome() = withSettingsFile { path ->
        assertEquals(
            path.parent.resolve("AppData").resolve("Roaming").resolve("Prism.DOCX").resolve("settings.properties"),
            defaultSettingsPath("\u0000", path.parent.toString()),
        )
    }

    private fun withSettingsFile(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("prism-settings-test-")
        try {
            block(directory.resolve("settings.properties"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private val newThemes = listOf(
        AppThemeId.GRAPHITE,
        AppThemeId.VIOLET,
        AppThemeId.EMERALD,
        AppThemeId.NORD,
    )
}
