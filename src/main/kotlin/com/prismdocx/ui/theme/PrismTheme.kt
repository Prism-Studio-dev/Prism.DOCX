package com.prismdocx.ui.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

private val lightColors = lightColorScheme(
    primary = Color(0xFF171717), onPrimary = Color.White,
    primaryContainer = Color(0xFFECECEC), onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF555555), secondaryContainer = Color(0xFFF0F0F0), onSecondaryContainer = Color(0xFF171717),
    background = Color.White, surface = Color.White, onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFF686868),
    outline = Color(0xFFBEBEBE), outlineVariant = Color(0xFFE7E7E7),
)
private val darkColors = darkColorScheme(
    primary = Color(0xFFF2F2F2), onPrimary = Color.Black,
    primaryContainer = Color(0xFF1C1C1C), onPrimaryContainer = Color.White,
    secondary = Color(0xFFB4B4B4), secondaryContainer = Color(0xFF1C1C1C), onSecondaryContainer = Color.White,
    background = Color.Black, surface = Color.Black, onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color.Black, onSurfaceVariant = Color(0xFFAAAAAA),
    surfaceDim = Color.Black, surfaceBright = Color(0xFF181818),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black, surfaceContainerHigh = Color(0xFF101010),
    surfaceContainerHighest = Color(0xFF1C1C1C),
    outline = Color(0xFF555555), outlineVariant = Color(0xFF303030),
)

@Composable
internal fun PrismTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(0.dp),
            small = RoundedCornerShape(0.dp),
            medium = RoundedCornerShape(0.dp),
            large = RoundedCornerShape(0.dp),
            extraLarge = RoundedCornerShape(0.dp),
        ),
    ) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides LocalScrollbarStyle.current.copy(shape = RectangleShape),
            content = content,
        )
    }
}
