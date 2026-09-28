package com.prismdocx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.prismdocx.resources.Res
import com.prismdocx.resources.prism_docx_logo
import com.prismdocx.ui.theme.LocalThemePalette
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.prism_docx_logo),
        contentDescription = "Prism.DOCX",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(LocalThemePalette.current.brand),
        modifier = modifier,
    )
}
