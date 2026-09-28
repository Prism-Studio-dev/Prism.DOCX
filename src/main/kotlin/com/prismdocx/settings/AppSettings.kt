package com.prismdocx.settings

import java.nio.file.Path

enum class AppThemeId { PRISM, LIGHT, DARK, GRAPHITE, VIOLET, EMERALD, NORD }

data class AppSettings(
    val theme: AppThemeId = AppThemeId.PRISM,
    val lastFileDirectory: Path? = null,
)

interface SettingsStore {
    fun load(): AppSettings
    fun save(settings: AppSettings)
}
