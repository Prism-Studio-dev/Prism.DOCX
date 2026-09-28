package com.prismdocx.ui.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.prismdocx.settings.AppThemeId

internal data class ThemePalette(
    val colors: ColorScheme,
    val brand: Color,
    val hover: Color,
) {
    val selected: Color get() = colors.primaryContainer
    val inputBackground: Color get() = colors.surface
    val disabled: Color get() = colors.onSurface.copy(alpha = 0.38f)
}

private val prismPalette = ThemePalette(
    colors = lightColorScheme(
        primary = Color(0xFFB43E49), onPrimary = Color.White,
        primaryContainer = Color(0xFFF9E8EB), onPrimaryContainer = Color(0xFF60212A),
        secondary = Color(0xFF65585A), onSecondary = Color.White,
        secondaryContainer = Color(0xFFF2EBEB), onSecondaryContainer = Color(0xFF292123),
        tertiary = Color(0xFF9A454D), onTertiary = Color.White,
        background = Color.White, onBackground = Color(0xFF1B1B1D),
        surface = Color.White, onSurface = Color(0xFF1B1B1D),
        surfaceVariant = Color(0xFFF7F6F6), onSurfaceVariant = Color(0xFF625D60),
        surfaceDim = Color(0xFFE6E3E3), surfaceBright = Color.White,
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBF9F9),
        surfaceContainer = Color(0xFFF7F5F5), surfaceContainerHigh = Color(0xFFF0EEEE),
        surfaceContainerHighest = Color(0xFFEAE7E8),
        outline = Color(0xFFB9B1B3), outlineVariant = Color(0xFFE4DEDF),
        error = Color(0xFFB3261E), onError = Color.White,
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        inversePrimary = Color(0xFFFFB2BA), surfaceTint = Color(0xFFB43E49),
    ),
    brand = Color(0xFFB43E49),
    hover = Color(0xFFF4EDEE),
)

private val lightPalette = ThemePalette(
    colors = lightColorScheme(
        primary = Color(0xFF365D95), onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE8FA), onPrimaryContainer = Color(0xFF17365F),
        secondary = Color(0xFF53657C), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2EAF5), onSecondaryContainer = Color(0xFF25374C),
        tertiary = Color(0xFF4D6397), onTertiary = Color.White,
        background = Color(0xFFF6F8FC), onBackground = Color(0xFF1A2432),
        surface = Color.White, onSurface = Color(0xFF1A2432),
        surfaceVariant = Color(0xFFEDF1F7), onSurfaceVariant = Color(0xFF596779),
        surfaceDim = Color(0xFFDEE4EC), surfaceBright = Color.White,
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFBFD),
        surfaceContainer = Color(0xFFF1F4F9), surfaceContainerHigh = Color(0xFFE8EDF5),
        surfaceContainerHighest = Color(0xFFDFE6F0),
        outline = Color(0xFF9CA9BA), outlineVariant = Color(0xFFD8E0EA),
        error = Color(0xFFBA1A1A), onError = Color.White,
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        inversePrimary = Color(0xFFA9C8FF), surfaceTint = Color(0xFF365D95),
    ),
    brand = Color(0xFF365D95),
    hover = Color(0xFFE5ECF6),
)

private val darkPalette = ThemePalette(
    colors = darkColorScheme(
        primary = Color(0xFF9CC5FF), onPrimary = Color(0xFF102B4A),
        primaryContainer = Color(0xFF263B58), onPrimaryContainer = Color(0xFFDEEBFF),
        secondary = Color(0xFFB4C3DA), onSecondary = Color(0xFF233247),
        secondaryContainer = Color(0xFF303C50), onSecondaryContainer = Color(0xFFDCE5F5),
        tertiary = Color(0xFFBDD0FF), onTertiary = Color(0xFF233760),
        background = Color.Black, onBackground = Color(0xFFF3F4F7),
        surface = Color(0xFF101216), onSurface = Color(0xFFF3F4F7),
        surfaceVariant = Color(0xFF191C22), onSurfaceVariant = Color(0xFFB8C0CC),
        surfaceDim = Color(0xFF0B0C0F), surfaceBright = Color(0xFF292E36),
        surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF101216),
        surfaceContainer = Color(0xFF191C22), surfaceContainerHigh = Color(0xFF232832),
        surfaceContainerHighest = Color(0xFF2E3540),
        outline = Color(0xFF7C8795), outlineVariant = Color(0xFF3B444F),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF5B2428), onErrorContainer = Color(0xFFFFDAD6),
        inversePrimary = Color(0xFF365D95), surfaceTint = Color(0xFF9CC5FF),
    ),
    brand = Color(0xFF9CC5FF),
    hover = Color(0xFF27303D),
)

private val graphitePalette = ThemePalette(
    colors = darkColorScheme(
        primary = Color(0xFFBBC8D3), onPrimary = Color(0xFF253543),
        primaryContainer = Color(0xFF3C4B5B), onPrimaryContainer = Color(0xFFEAF3FC),
        secondary = Color(0xFFC0C8D0), onSecondary = Color(0xFF29333D),
        secondaryContainer = Color(0xFF414C57), onSecondaryContainer = Color(0xFFE8EEF4),
        tertiary = Color(0xFFAFC4D4), onTertiary = Color(0xFF263846),
        background = Color(0xFF25282D), onBackground = Color(0xFFF2F4F5),
        surface = Color(0xFF2D3239), onSurface = Color(0xFFF2F4F5),
        surfaceVariant = Color(0xFF353B44), onSurfaceVariant = Color(0xFFBDC7CF),
        surfaceDim = Color(0xFF202329), surfaceBright = Color(0xFF454D56),
        surfaceContainerLowest = Color(0xFF22262B), surfaceContainerLow = Color(0xFF2A2F35),
        surfaceContainer = Color(0xFF323840), surfaceContainerHigh = Color(0xFF3B424B),
        surfaceContainerHighest = Color(0xFF454D57),
        outline = Color(0xFF8997A5), outlineVariant = Color(0xFF505B66),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF5B2428), onErrorContainer = Color(0xFFFFDAD6),
        inversePrimary = Color(0xFF4D6479), surfaceTint = Color(0xFFBBC8D3),
    ),
    brand = Color(0xFFBBC8D3),
    hover = Color(0xFF3B4651),
)

private val violetPalette = ThemePalette(
    colors = darkColorScheme(
        primary = Color(0xFFC8B1FA), onPrimary = Color(0xFF332451),
        primaryContainer = Color(0xFF4D3B6D), onPrimaryContainer = Color(0xFFEBDDFF),
        secondary = Color(0xFFC9BBD9), onSecondary = Color(0xFF382D47),
        secondaryContainer = Color(0xFF453951), onSecondaryContainer = Color(0xFFF0E8FA),
        tertiary = Color(0xFFD2B8EA), onTertiary = Color(0xFF3A2849),
        background = Color(0xFF1E1A28), onBackground = Color(0xFFF7F3FD),
        surface = Color(0xFF282233), onSurface = Color(0xFFF7F3FD),
        surfaceVariant = Color(0xFF332B42), onSurfaceVariant = Color(0xFFC7BFD3),
        surfaceDim = Color(0xFF191521), surfaceBright = Color(0xFF473C58),
        surfaceContainerLowest = Color(0xFF1B1724), surfaceContainerLow = Color(0xFF251F30),
        surfaceContainer = Color(0xFF2F283B), surfaceContainerHigh = Color(0xFF392F49),
        surfaceContainerHighest = Color(0xFF453956),
        outline = Color(0xFF938AA6), outlineVariant = Color(0xFF554B66),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF5B2428), onErrorContainer = Color(0xFFFFDAD6),
        inversePrimary = Color(0xFF694A9D), surfaceTint = Color(0xFFC8B1FA),
    ),
    brand = Color(0xFFC8B1FA),
    hover = Color(0xFF3B3152),
)

private val emeraldPalette = ThemePalette(
    colors = darkColorScheme(
        primary = Color(0xFF93D8B2), onPrimary = Color(0xFF113925),
        primaryContainer = Color(0xFF315A43), onPrimaryContainer = Color(0xFFDAF8E5),
        secondary = Color(0xFFB3CEBE), onSecondary = Color(0xFF213D2C),
        secondaryContainer = Color(0xFF345041), onSecondaryContainer = Color(0xFFE1F2E6),
        tertiary = Color(0xFFA7D6C3), onTertiary = Color(0xFF1D3E33),
        background = Color(0xFF17231F), onBackground = Color(0xFFF0F6F2),
        surface = Color(0xFF1F2E28), onSurface = Color(0xFFF0F6F2),
        surfaceVariant = Color(0xFF293A32), onSurfaceVariant = Color(0xFFBDCBC0),
        surfaceDim = Color(0xFF14201B), surfaceBright = Color(0xFF395348),
        surfaceContainerLowest = Color(0xFF16221D), surfaceContainerLow = Color(0xFF1C2A23),
        surfaceContainer = Color(0xFF26362D), surfaceContainerHigh = Color(0xFF2F4237),
        surfaceContainerHighest = Color(0xFF3B4D41),
        outline = Color(0xFF88A092), outlineVariant = Color(0xFF476052),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF5B2428), onErrorContainer = Color(0xFFFFDAD6),
        inversePrimary = Color(0xFF3E7956), surfaceTint = Color(0xFF93D8B2),
    ),
    brand = Color(0xFF93D8B2),
    hover = Color(0xFF315041),
)

private val nordPalette = ThemePalette(
    colors = darkColorScheme(
        primary = Color(0xFF9ED4E7), onPrimary = Color(0xFF173848),
        primaryContainer = Color(0xFF3B5970), onPrimaryContainer = Color(0xFFDCF4FF),
        secondary = Color(0xFFB9CEDA), onSecondary = Color(0xFF253C49),
        secondaryContainer = Color(0xFF364C5C), onSecondaryContainer = Color(0xFFE1EFF5),
        tertiary = Color(0xFFAED5DE), onTertiary = Color(0xFF1F3E46),
        background = Color(0xFF222D3B), onBackground = Color(0xFFEEF5F8),
        surface = Color(0xFF2B3948), onSurface = Color(0xFFEEF5F8),
        surfaceVariant = Color(0xFF354659), onSurfaceVariant = Color(0xFFBBCDD7),
        surfaceDim = Color(0xFF1C2734), surfaceBright = Color(0xFF465B6B),
        surfaceContainerLowest = Color(0xFF1F2A37), surfaceContainerLow = Color(0xFF273544),
        surfaceContainer = Color(0xFF304051), surfaceContainerHigh = Color(0xFF3A4C5D),
        surfaceContainerHighest = Color(0xFF45596A),
        outline = Color(0xFF8299A7), outlineVariant = Color(0xFF4A6273),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF5B2428), onErrorContainer = Color(0xFFFFDAD6),
        inversePrimary = Color(0xFF4D7790), surfaceTint = Color(0xFF9ED4E7),
    ),
    brand = Color(0xFF9ED4E7),
    hover = Color(0xFF36536A),
)

internal fun paletteFor(theme: AppThemeId): ThemePalette = when (theme) {
    AppThemeId.PRISM -> prismPalette
    AppThemeId.LIGHT -> lightPalette
    AppThemeId.DARK -> darkPalette
    AppThemeId.GRAPHITE -> graphitePalette
    AppThemeId.VIOLET -> violetPalette
    AppThemeId.EMERALD -> emeraldPalette
    AppThemeId.NORD -> nordPalette
}

internal val LocalThemePalette = staticCompositionLocalOf { prismPalette }

@Composable
internal fun themedOutlinedTextFieldColors(): TextFieldColors {
    val palette = LocalThemePalette.current
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.inputBackground,
        unfocusedContainerColor = palette.inputBackground,
        disabledContainerColor = palette.inputBackground,
        errorContainerColor = palette.inputBackground,
        cursorColor = palette.brand,
        errorCursorColor = palette.colors.error,
        disabledTextColor = palette.disabled,
    )
}

private val flatShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
internal fun PrismTheme(theme: AppThemeId, content: @Composable () -> Unit) {
    val palette = paletteFor(theme)
    MaterialTheme(colorScheme = palette.colors, shapes = flatShapes) {
        CompositionLocalProvider(
            LocalThemePalette provides palette,
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = palette.brand,
                backgroundColor = palette.brand.copy(alpha = 0.32f),
            ),
            LocalScrollbarStyle provides LocalScrollbarStyle.current.copy(
                shape = RectangleShape,
                hoverColor = palette.brand.copy(alpha = 0.82f),
                unhoverColor = palette.colors.outline.copy(alpha = 0.78f),
            ),
            content = content,
        )
    }
}
